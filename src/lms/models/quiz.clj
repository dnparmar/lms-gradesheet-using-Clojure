(ns lms.models.quiz
  (:require [lms.layout :as layout]
            [compojure.core :refer :all]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [cheshire.core :as cheshire]
            [ring.util.anti-forgery :as af]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.result :refer [ok? has-error?]]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clojure.pprint :refer :all]))

(def conn (mg/connect))
(def db (mg/get-db conn "lms"))
(def document "quiz")
(def document1 "quizquestions")

(defn read-file
  [file-path]
  (slurp (io/file (io/resource file-path))))

(defn read-exam
  [v]
  (json/read-str (read-file v) :key-fn keyword))

(defn format-time
  [date]
  (def custom-formatter (f/formatter "yyyyMMdd hh:mm:ss"))
  (f/unparse custom-formatter date))

(defn findquiz
  [quiz]
  (mc/find-maps db document1 {:exam-number quiz}))


(defn insert-quiz
  [quiz]
  (mc/insert db document1 quiz))

(defn get-time-now
  []
  (format-time (l/local-now)))

(defn shuffle-choices
  [exam]
  (map (fn [x]
         {:c (shuffle (:c x))
          :q (:q x)
          :l (:l x)}) (shuffle (:questions exam))))

(defn record-grade
  [cin stu-name quiz grade uname]
    (mc/insert db document {:cin cin :name stu-name :quiz quiz :grade grade :username uname} ))

(defn getgrades [uname]
  (mc/find-maps db document {:username uname}))

(defn getQuiz []
  (mc/find-maps db document1 {}))

(defn checkQuiz [v]
  (if (empty? (mc/find-maps db document {:quiz v}))
    false
    true))
