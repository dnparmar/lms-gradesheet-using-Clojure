(ns lms.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor.helpers :refer [definterceptor]]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [ring.util.response :as ring-resp]
            [ring.middleware.session.cookie :as cookie]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [lms.layout :as layout]
            [clojure.pprint :refer :all]
            [clojure.pprint :as pprint]
            [cheshire.core :as ches]
            [cognitect.transit :as t]
            [lms.models.quiz :as quiz]
            [lms.models.register :as register]
            [cemerick.url :refer (url url-encode)]
            [clj-http.client :as client])
  (:import [java.io File]))



(require '[clojurewerkz.scrypt.core :as sc])

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def upper (re-pattern "[A-Z]+"))
(def number (re-pattern "[0-9]+"))
(def special (re-pattern "[\"'!@#$%^&*()?]+"))

(def oauth
    {:auth-url           "https://accounts.google.com/o/oauth2/auth"
            :client-id          (or (System/getenv "google_client_id") "717553677417-45pmifonp9gks4u1h71ul6phmm4fiv8m.apps.googleusercontent.com")
            :client-secret      (or (System/getenv "google_client_secret") "F6qNGpaxgwweE6iy9LbMYXnn")
            :redirect-uri       "http://localhost:8080/google"
            :scope              "profile email"
            :access-token-uri          "https://accounts.google.com/o/oauth2/token"
            :user-info-url      "https://www.googleapis.com/plus/v1/people/me"})

(defn authorize-uri [client-params csrf-token]
    (str
        (:auth-url client-params)
                "?response_type=code"
                "&client_id="
                (url-encode (:client-id client-params))
                "&redirect_uri="
                (url-encode (:redirect-uri client-params))
                "&scope="
                (url-encode (:scope client-params))
                "&state="
                (url-encode csrf-token)))

(defn get-authentication-response [csrf-token response-params]
    (if (= csrf-token (:state (:params response-params)))
    (try
        (-> (client/post (:access-token-uri oauth)
        {:form-params {:code         (:code (:params response-params))
                                                        :grant_type   "authorization_code"
                                                        :client_id    (:client-id oauth)
                                                        :redirect_uri (:redirect-uri oauth)}
                                          :basic-auth [(:client-id oauth) (:client-secret oauth)]
                                          :as          :json
                                          })
                              :body)
                          (catch Exception _ nil))
                        nil))

(defn get-user-info
    "User info API call"
    [access-token]
    (let [url (str "https://www.googleapis.com/oauth2/v1/userinfo?access_token=" access-token)]
    (pprint url)
    (-> (client/get url)
    :body)))

(defn google
    [request]
    (let [access_token (get (get-authentication-response "1234" request) :access_token)
            user-info (get-user-info access_token)
            parse-data (ches/parse-string user-info true)
            user (:name parse-data)]
            (pprint user)
            (-> (ring-resp/redirect "/home")
                (assoc :session {:name user}))))

(defn strength? [password]
  (not (nil? (and (re-find upper password)
                  (re-find number password)
                  (re-find special password)))))

(defn length? [password]
  (> (count password) 8))

(defn valid-password? [password]
  (and (strength? password) (length? password)))

(defn serialize [m] (str m))
(defn de-serialize [s] (read-string s))

(defn welcome-page
  [request]
  (layout/render "welcome.html"))

(defn register-page
  [request]
  (layout/render "register.html"))

(defn login-page
  [request]
  (let [ session (get-in request [:session]) ]
    (if (empty? session)
    (layout/render "login.html" {:string (authorize-uri oauth "1234")})
    (ring-resp/redirect "/home"))))

(defn home-page
  [request]
  (pprint request)
  (let [ session (get-in request [:session])
         name (:name session)]
    (pprint session)
  (if (empty? session)
    (ring-resp/redirect "/login")
    (do (let [ quizlist (into [] (quiz/getQuiz))]
    (layout/render "home.html" {:username name :quizlist quizlist}))))))

(defn logout-page
  [request]
  (let [ session (get-in request [:session])
         name (:name session)]
    (pprint session)
  (-> (ring-resp/redirect "/login")
            (assoc :session nil))))


(defn invalidRegistration []
  (layout/render "register.html"
                 {:error (str "This username is already taken.")}))

(defn invalidLogin []
  (layout/render "login.html"
                 {:error (str "Invalid username and/or password.")}))

(defn eval-user
  [request]
    (let [data (read-string (slurp (:body request)))
          username (String. (:username data))]
      (if (not-empty username)
      (if (register/exist-user? username)
        (ring-resp/response (str "Username Taken!"))
        (ring-resp/response (str "Username Available!")))
        (ring-resp/response (str "")))))


(defn eval-pass
  [request]
    (let [data (read-string (slurp (:body request)))
      password (String. (:password data)) ]
      (if (not-empty password)
      (if (length? password)
        (if (valid-password? password)
          (ring-resp/response (str "Strong Password!"))
          (ring-resp/response (str "Weak Password!")))
        (ring-resp/response
         (str "Minimum 8 characters, 1 uppercase, 1 number, 1 special character required!")))
        (ring-resp/response (str "")))))

(defn registerUser
  [request]
    (let [data (read-string (slurp (:body request)))
          username (String. (:username data))
          password (String. (:password data))]
      (if (and (not (register/exist-user? username)) (valid-password? password))
        (do (register/add-user {:username username :password (sc/encrypt password 16384 8 1)})
            (ring-resp/response (str "Successfully Registered!")))
        (if (register/exist-user? username)
          (ring-resp/response (str "Please enter a different username"))
          (ring-resp/response (str "Please enter a valid password"))))))

(defn register-user
  [request]
    (let [form (:form-params (body-params/form-parser request))
          username (String. (get form "username"))
          password (String. (get form "password"))]
      (if (and (not (register/exist-user? username)) (valid-password? password))
        (do (register/add-user {:username username :password (sc/encrypt password 16384 8 1)})
            (ring-resp/redirect "/login"))
        (if (register/exist-user? username)
          (ring-resp/response (str "Please enter a different username"))
          (ring-resp/response (str "Please enter a valid password"))))))


(defn userLogin
  [request]
  (let [ form (:form-params (body-params/form-parser request))
         userName (String. (get form "userName"))
         password (String. (get form "userPassword"))
         user (register/get-user {:username userName })
         hashed (first (map :password user))
         sessionid (uuid)]
    (if (empty? user)
      (do (pprint user)
      (invalidLogin))
    (do (if-not (sc/verify password hashed)
        (invalidLogin)
      (do (pprint hashed)
          (pprint (sc/verify password hashed))
        (-> (ring-resp/redirect "/home")
            (assoc :session {:name userName}))))))))

(defn count-grade [exam json-data]
  (let [questions (:questions exam)
        va (:wa json-data)]
    (loop [iteration 0 grade (atom 0)]
      (if (< iteration (count questions))
        (do (if (= (get-in questions [iteration :a]) (get-in ((keyword (str "q" (+ iteration 1))) va) [0]))
                (swap! grade inc))
            (recur (inc iteration) grade))
         (let [result @grade] result)))))



(defn submit-grades
  [request]
  (let [expr (read-string (slurp (:body request)))
        form-data (:form expr)
        json-data (ches/parse-string form-data true)
        user-input (:wa json-data)
        exam-num (get-in (:exam-number user-input) [0])
        exam-file-db (into {} (quiz/findquiz exam-num))
        ;exam-file (str "exams/" exam-num ".json")
        ;exam (quiz/read-exam exam-file-db)
        grade (count-grade exam-file-db json-data)
        cin (get-in (:cin user-input) [0])
        stu-name (get-in (:name user-input) [0])
        session (get-in request [:session])
        name (:name session)]
     (if (empty? session)
      (ring-resp/response "Please login again")
      (do (quiz/record-grade cin stu-name exam-num grade name)
      (ring-resp/response (str "Your grade is : " grade))))))



(defn show-quiz
  [request]
  (let [expr (read-string (slurp (:body request)))
        number (:number expr)
        exam-file-db (into {} (quiz/findquiz number))
        ;exam-file (str "exams/" number ".json")
        ;exam (quiz/read-exam exam-file)
        session (get-in request [:session])]
    (pprint exam-file-db)
    (if (empty? session)
      (ring-resp/response "<h3>Please login again</h3>")
    (layout/render "quiz.html" exam-file-db))))

(defn getgrades
  [request]
  (let [session (get-in request [:session])
        name (:name session)
        grades (into [] (quiz/getgrades name))]
    (if (empty? session)
      (ring-resp/response "<h3>Please login again</h3>")
    (layout/render "grade.html" {:grades grades}))))

(defn upload-file
  [request]
  (let [params (get-in request [:params "userfile"])
        file-name (:filename params)
        quizfile (:tempfile params)
        quiz-format (ches/parse-string (slurp quizfile) true)]
    (quiz/insert-quiz quiz-format)
      (ring-resp/redirect "/home")))
    ;(pprint quiz-format)
;;     (do
;;       (io/copy quizfile (File. (format "/Users/shreyvithalani/Desktop/%s" file-name)))
;;       ;(println (slurp quizfile))
;;       (ring-resp/response (str "file uploaded"))
;;       (ring-resp/redirect "/home"))))

(defn upload
  [request]
  (layout/render "upload.html"))


(definterceptor session-interceptor
  (middlewares/session {:store (cookie/cookie-store)}))


(defroutes routes
  ;; Defines "/" and "/about" routes with their associated :get handlers.
  ;; The interceptors defined after the verb map (e.g., {:get home-page}
  ;; apply to / and its children (/about).
  [[["/" {:get welcome-page}]
    ["/register" {:get register-page}]
    ["/google" ^:interceptors [session-interceptor] {:get google}]
    ["/register" {:post register-user}]
    ["/register/eval-user" {:post eval-user}]
    ["/register/eval-pass" {:post eval-pass}]
    ["/register/add-user" {:post registerUser}]
    ["/login" ^:interceptors [session-interceptor] {:get login-page}]
    ["/login" ^:interceptors [session-interceptor] {:post userLogin}]
    ["/logout" ^:interceptors [session-interceptor] {:get logout-page}]
    ["/home" ^:interceptors [session-interceptor] {:get home-page}]
    ["/home/showquiz"  ^:interceptors [session-interceptor] {:post show-quiz}]
    ["/home/submit-grades"  ^:interceptors [session-interceptor] {:post submit-grades}]
    ["/home/getgrade"  ^:interceptors [session-interceptor] {:post getgrades}]
    ["/home/uploadquiz" ^:interceptors [session-interceptor] {:post upload}]
    ["/home/uploadquiz/getfile" {:post upload-file}
       ^:interceptors [(middlewares/multipart-params) session-interceptor]]
     ]])

(def service {:env :prod
              ::bootstrap/routes routes

              ::bootstrap/resource-path "/public"

              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
