(ns lms.cljs.register
  (:require [goog.net.XhrIo :as xhr]
            [cljs.reader :as reader]
            [domina :as d]
            [domina.events :as events]
            [goog.dom.forms :as gforms]))

(def user-id "user-id")
(def pass-id "pass-id")
(def user-result "user-result")
(def pass-result "pass-result")
(def button-id "submit-button-id")
(def register-result "register-result")
(def user-url "/register/eval-user")
(def pass-url "/register/eval-pass")
(def register-url "register/add-user")

(defn serialize [m] (str m))
(defn de-serialize [s] (reader/read-string s))


(defn set-user-result [event]
  (d/set-text! (d/by-id user-result)
               (.getResponseText (.-target event))))

(defn set-pass-result [event]
  (d/set-text! (d/by-id pass-result)
               (.getResponseText (.-target event))))
(defn set-register-result [event]
  (d/set-text! (d/by-id register-result)
               (.getResponseText (.-target event))))

(defn callbackfn [event] )


(defn post-for-userCheck [user-str]
  (xhr/send user-url set-user-result "POST" user-str))

(defn post-for-passCheck [pass-str]
  (xhr/send pass-url set-pass-result "POST" pass-str))

(defn post-for-register [register-data]
  (xhr/send register-url set-register-result "POST" register-data))



(defn get-userText []
  (let [ username (.-value (d/by-id user-id))]
    (serialize {:username username})))

(defn get-passText []
  (let [ password (.-value (d/by-id pass-id))]
  (serialize {:password password})))

(defn get-form []
  (let [ username (.-value (d/by-id user-id))
         password (.-value (d/by-id pass-id))]
    (serialize {:username username :password password})))


(defn ^:export main []
  (events/listen! (d/by-id user-id)
                  :keyup
                  (fn [event]
                    (post-for-userCheck (get-userText))
                    (events/stop-propagation event)
                    (events/prevent-default event)
                    ))
  (events/listen! (d/by-id pass-id)
                  :keyup
                  (fn [event]
                    (post-for-passCheck (get-passText))
                    (events/stop-propagation event)
                    (events/prevent-default event)
                    ))
 (events/listen! (d/by-id button-id)
                  :click
                  (fn [event]
                    (post-for-register (get-form))
                    )))
