(ns clojure-workshop.cat
  (:gen-class)
  (:import (java.util.regex Pattern))
  (:require
    [clojure-workshop.args :as args]
    [clojure.string :as string]))

(defn split-retain-empty-lines
  "Splits a line into a seq, where empty lines are retaines as an empty string \"\""
  [text]
  (string/split text #"\r?\n"))

(defn format-line
  "Formats lines with prefixed numbering"
  [line num]
  (format "%6d %s" num line))

(declare number-lines number-non-blank-lines read-file)

;; Check out tests in clojure-workshop.cat-test, comment them in one by one, which should
;; Help you build your cat program.
;; You should implement the functions starting with (in increasing difficulty)
;; * read-file
;; * cat
;; * cat-files (after this you have the simplest version of cat!. try 'lein uberjar', and you can run 'java -jar target/cat.jar -h'
;; * number-lines
;; * number-non-blank-lines

(defn read-file
  "Reads a file from disk. What is the easiest way? (Opposite of spit)"
  [f]
  (slurp f))

(defn cat
  "Takes state (opts) and text to be formatted as params. Returns a vector of current state and text
   to be output.
   Should handle each option in the args/cli-options.
   :num - only number blank lines
   :num-non-blank - only number blank line (takes presendence if both present)"
  [state text]
  (cond
    (:num-non-blank-lines state) (number-non-blank-lines state text)
    (:num-all-lines state) (number-lines state text)
    ; in else you should just return a vector of state and the text
    :else [state text]))

(defn cat-files
  "Takes options and a set of filenames to handle. First reads/converts each file to text, then converts text
  for each file over cat. Then converts each result vector to only the text (the second part of cat
  result). Finally joins these with a \newline (check out clojure.string functions)."
  [opts files]
  (string/join \newline
    (map second
         (map (partial cat opts)
              (map read-file files)))))

(defn number-lines
  "Takes state (potentially containg a current :line-count) and text  and formats it using format
  line. Where each line is prefixed with a its number starting with 1. Returns a vector containing
  a new state with a new :line-cnt, and the formatted-lines."
  [state text]
  (let [lines (split-retain-empty-lines text)
        ; Fetch :line-cnt from state map or 1
        current-cnt (:line-cnt state 1)
        ; Convert/map over lines. All lines should get a number, use format-line to format.
        formatted-lines (map format-line lines
                             (iterate inc current-cnt))
        ; What is the next count?
        cnt (+ current-cnt (count lines))]
    [(assoc state :line-cnt cnt)
     (string/join \newline formatted-lines)]))

(defn number-non-blank-lines
  "Takes state (potentially containg a current :line-count) and text and formats it using format
  line. If a line is non-empty it will be prefixed with its number starting with 1. Returns a
  vector containing a new state with a new :line-cnt, and the formatted-lines."
  [state text]
  (let [lines (split-retain-empty-lines text)
        ; Fetch :line-cnt from state map or 1
        current-cnt (:line-cnt state 1)
        ; Convert/map/reduce over lines. Only add numbering on lines with content. Use format-line to format.
        ; In proposed solution, we fetch both formattes lines and the new cnt in the same function.
        [cnt formatted-lines] (reduce (fn [[n xs] x]
                                        (if-not (= "" x)
                                          [(inc n) (conj xs (format-line x n))]
                                          [n (conj xs "")]))
                                      [current-cnt []] lines)]
    [(assoc state :line-cnt cnt)
     (string/join \newline formatted-lines)]))

(defn cat-in
  "Loops over system/in until ctrl-d is pressed converting input to cat for each new line"
  [opts]
  (loop [[state text] [opts nil]]
    (when text (println text))
    (when-let [line (read-line)]
      (recur (cat state line)))))

(defn -main [& args]
  (let [{files :files :as opts} (args/parse-args args)]
    (cond
      files (println (cat-files opts files))
      :else (cat-in opts))))