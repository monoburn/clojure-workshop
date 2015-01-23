(ns clojure-workshop.cat
  (:gen-class :main)
  (:require
    [clojure-workshop.args :as args]
    [clojure.string :as string]))

(defn- split-retain-empty-lines
  "Splits a line into a seq, where empty lines are retaines as an empty string \"\""
  [text]
  (string/split text #"\r?\n"))

(defn- format-line
  "Formats lines with prefixed numbering"
    [num line]
    (format "%6d  %s" num line))



(defn- infinite-coll-from
  "Creates infinite lazy seq, starts with start integer, increments by one"
  [start]
  (iterate inc start))

(defn read-file
  "Reads a file from disk. What is the easiest way? (Opposite of spit)"
  [f]
  (slurp f))

(defn number-lines
  "Takes state (potentially containg a current :line-cnt) and text  and formats it using format
  line. Where each line is prefixed with a its number starting with 1. Returns a vector containing
  a new state with a new :line-cnt, and the formatted-lines."
  [state text]
  (let [lines (split-retain-empty-lines text)
        ; Fetch :line-cnt from state map or return default, which should be 1
        current-cnt (state :line-cnt 1)
        ; Convert/map over lines. All lines should get a number, use format-line to format.
        ; Hint: map can take several arguments (collections), and check out infinite-coll-from.
        line-numbers (range current-cnt (+ 1 current-cnt (count lines)))
        formatted-lines (map format-line  line-numbers lines)
        ; What is the next count (for the next file)?
        cnt (+ current-cnt (count lines))]
    [(assoc state :line-cnt cnt)
     (string/join \newline formatted-lines)]))



(defn build-numbered [start-cnt cs]
  "go through seq of strings and return a tuple [cnt, numbered-pairs] where numbered-pairs is a vector
  of tuples [line-number/nil s], The first non-empty s is assigned the 'start-cnt'"
  (reduce (fn [[cnt pairs] s]
          "for each non-empty string, update counter and add a tuple. The tuple as count and string, or nil and string"
          (if (clojure.string/blank? s)
            [cnt (conj pairs [nil, s])]
            [(inc cnt) (conj pairs [cnt, s])]))
        [start-cnt, []] cs))

;;(build-numbered  4 '("one", "two", "", "three"))


(defn number-non-blank-lines
  "Takes state (potentially containg a current :line-cnt) and text and formats it using format
  line. If a line is non-empty it will be prefixed with its number starting with 1. Returns a
  vector containing a new state with a new :line-cnt, and the formatted-lines."
  [state text]
  (let [lines (split-retain-empty-lines text)
        ; Fetch :line-cnt from state map or 1
        current-cnt (:line-cnt state 1)
        ; Convert/map/reduce over lines. Only add numbering on lines with content. Use format-line to format.
        numbered (build-numbered current-cnt lines)
        cnt (first numbered)
        s-count-pairs (second numbered)
        conditional-formatting (fn [n s]  (if n (format-line n s) s))
        ]
    [(assoc state :line-cnt cnt)
     (string/join \newline (map #(let [num (first %) s (second %)] (conditional-formatting num s)) s-count-pairs))]))


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
  "Takes options and a set of filenames to handle, and produces one result string. It must do the following:
  1. Map each file over read-file function, which returns a sequence of file-contents.
  2. Map each file-content over the cat function (using opts as the state). Result is [state \"catted string\"].
  3. Map each result vector over a function which fetches the second element of a the vector. Result is \"catted\" string.
  4. Join all these strings with \newline (check out clojure.string functions)"
  [opts files]
  (let [content (map read-file files)
        state-content (map #(cat opts %) content)
        mcs (map second state-content)]
    (string/join \newline mcs)))



(defn cat-in
  "Loops over system/in until ctrl-d is pressed converting input to cat for each new line"
  [opts]
  (loop [[state text] [opts nil]]
    (when text (println text))
    (when-let [line (read-line)]
      (recur (cat state line)))))

;; #################################################################################################################################
;; Instructions:
;;
;; Check out tests in clojure-workshop.cat-test, comment them in one by one, which should help you build your cat program.
;;
;; You should implement the functions starting with (in increasing difficulty):
;; * read-file
;; * cat (only the else part as test will show)
;; * cat-files (after this you have the simplest version of cat!. try 'lein uberjar', and you can run 'java -jar target/cat.jar -h'
;; * number-lines
;; * number-non-blank-lines
;;
;; #################################################################################################################################

(defn -main [& args]
  (let [{files :files :as opts} (args/parse-args args)]
    (cond
      files (println (cat-files opts files))
      :else (cat-in opts))))
