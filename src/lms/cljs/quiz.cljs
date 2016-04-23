(ns lms.cljs.quiz
  (:require [goog.net.XhrIo :as xhr]
            [cljs.reader :as reader]
            [domina :as d]
            [domina.events :as events]
            [goog.dom.forms :as gforms]
            [clojure.string :as s]
            [clojure.core.reducers :as r]))


(def quiz-id "quiznumber")

(def quiz-button "quiz")

(def form-id "form")

(def url "/home/showquiz")

(def url1 "/home/submit-grades")

(def text-id "text")

(def grade-id "grades")

(def get-result-id "getresult")

(def gradeurl "/home/getgrade")

(def upload-id "upload")

(def uploadurl "/home/uploadquiz")

(def getfile "getfile")

(def fileurl "/home/uploadquiz/getfile")


(defn serialize [m] (str m))


(defn receive-result [event]
  (d/set-inner-html! (d/by-id "container")
                     (.getResponseText(.-target event))))


(defn receive-form [event]
  (.log js/console (str (.getResponseText (.-target event))))
  (d/set-text! (d/by-id grade-id)
               (.getResponseText (.-target event))))

(defn post-for-eval [expr-str]
  (xhr/send url receive-result "POST" expr-str))

(defn post-for-quizresult [expr-str]
  (xhr/send url1 receive-form "POST" expr-str))

(defn post-for-gradetable []
  (xhr/send gradeurl receive-grades "POST"))


(defn receive-grades [event]
  (d/set-inner-html! (d/by-id "container")
               (.getResponseText (.-target event))))

(defn receive-num [event]
  )

(defn recieve-upload [event]
  (d/set-inner-html! (d/by-id "container")
                     (.getResponseText (.-target event))))

(defn post-for-upload []
  (.log js/console (str "hi"))
  (xhr/send uploadurl recieve-upload "POST"))


(defn post-for-file [expr-str]
  (xhr/send fileurl receive-num "POST"{:body form
                      :response-format (raw-response-format)
                      :timeout 100} expr-str))



(defn get-form []
  (let [form (goog.json.serialize (gforms/getFormDataMap (d/by-id form-id)))]
    (.log js/console (str "formdata : " form))
            {:form form}))

(defn get-quiz-number[]
  (let [number (.-value (d/by-id quiz-id))]
    (serialize {:number number})))

(defn get-file []
  (let [form (d/by-id "foo")
        name (.-name form)
        file (aget (.-files form) 0)
        form-data (doto
                      (js/FormData.)
                    (.append name file))]
    (xhr/send fileurl receive-num "POST"{:params  form-data
                      :response-format (raw-response-format)
                      :timeout 100} expr-str)))

(defn ^:export file []
  (get-file))



(defn ^:export form []
 (post-for-quizresult (get-form)))



(defn ^:export main []
  (events/listen! (d/by-id quiz-id)
                  :change
                  (fn [event]
                    (post-for-eval (get-quiz-number))
                    (events/stop-propagation event)
                    (events/prevent-default event)))
  (events/listen! (d/by-id get-result-id)
                  :click
                  (fn [event]
                    (post-for-gradetable)
                    (events/stop-propagation event)
                    (events/prevent-default event)))
  (events/listen! (d/by-id upload-id)
                  :click
                  (fn [event]
                    (post-for-upload)
                    (events/stop-propagation event)
                    (events/prevent-default event))))

