(ns clojure-ga.core
  (:require [criterium.core :as bench])
  (:gen-class))

(defn count-ones
  [bits]
  bits)
  ; (count (filter #(= % 1) bits)))

;; TODO: Fix hiff so it returns a list of scores.
;; (defn hiff
;;   [bits]
;;   (let [len (count bits)
;;         half-len (quot len 2)]
;;     (if (< len 2)
;;       len
;;       (+ (hiff (take half-len bits))
;;          (hiff (drop half-len bits))
;;          (if (every? #(= % (first bits)) bits)
;;            len
;;            0)))))

;; Returns a vector [all-same scores], where `all-same`
;; is true if all the values in `bits` are the same, and
;; `scores` is the score list for this collection of `bits`.
(defn do-hiff
  [bits scores]
  (let [len (count bits)]
    (if (< len 2)
      [true (conj scores len)]
      (let [half-len (/ len 2)
            [left-all-same left-scores] (do-hiff (take half-len bits) scores)
            [right-all-same all-scores] (do-hiff (drop half-len bits) left-scores)]
        (if (and left-all-same right-all-same (= (first bits) (nth bits half-len)))
          [true (conj all-scores len)]
          [false (conj all-scores 0)])))))

(defn hiff
  [bits]
  (vec (second (do-hiff bits (list)))))

(defn make-random-individual
  [num-bits scorer]
  (let [bits (repeatedly num-bits #(rand-int 2))
        scores (scorer bits)]
    {
      :bits bits
      :scores scores
      :fitness (reduce + 0 scores)
    }))

(defn make-individual 
  [scorer bits]
  (let [scores (scorer bits)]
    {
    :bits bits
    :scores scores
    :fitness (reduce + 0 scores)
    }))

(defn make-population
  [pop-size num-bits compute-fitness]
  (repeatedly
    pop-size
    #(make-random-individual num-bits compute-fitness)))

(defn pmap-make-population
  [pop-size num-bits compute-fitness]
  (pmap (fn [_] (make-random-individual num-bits compute-fitness))
        (range pop-size)))

;;; SELECTORS

(defn best-individual 
  [pop]
  (apply max-key :fitness pop))

(defn lexicase-selection
  "Selects an individual from the population using lexicase selection."
  [pop]
  (loop [survivors pop
         cases (shuffle (range (count (:scores (first pop)))))]
    (if (or (empty? cases)
            (empty? (rest survivors)))
      (rand-nth survivors)
      (let [max-score-for-case (apply max (map #(nth % (first cases))
                                             (map :scores survivors)))]
        (recur (filter #(= (nth (:scores %) (first cases)) max-score-for-case)
                       survivors)
               (rest cases))))))

; This is (hopefully) functionally the same as the previous definition,
; but uses Java's `ArrayList` class to reduce the memory management costs.
; In timing tests of just lexicase selection alone, this is slightly faster
; than the previous one.
(defn lexicase-array-list
  "Selects an individual from the population using lexicase selection."
  [pop]
  (let [num-individuals (count pop)]
    ; `candidates` is the list of survivors through the lexicase process so far.
    ; `winners` is the list of individuals that have survived the on the current
    ; test case.
    (loop [candidates (java.util.ArrayList. pop)
           winners (java.util.ArrayList. num-individuals)
           cases (shuffle (range (count (:scores (first pop)))))]
      (if (or (empty? cases)
              (empty? (rest candidates)))
        ; We need to convert the ArrayList back to a vec so we can
        ; call rand-nth on it. Alternatively we could just pick a
        ; random instance and call `.get` with that; this might be
        ; more efficient since it would avoid that conversion.
        (rand-nth (into [] candidates))
        (do
          (.clear winners)
          ; We'll put the first candidate in the winners pool
          ; straight away as "the best we've seen so far"
          (.add winners (.get candidates 0))
          (recur
           (do
             (doseq
              ; We've already "processed" the first candidate by
              ; adding it straight away to the winners, so we'll
              ; only loop over 1..(num-individuals - 1).
              ; pop-indices (range num-individuals)
              [i (range 1 (count candidates))]
               (let [best-score (nth (:scores (.get winners 0)) (first cases))
                     this-score (nth (:scores (.get candidates i)) (first cases))]
                 (when (> this-score best-score)
                   ; This is better than anything we've seen so far so
                   ; clear winners and add this.
                   (.clear winners)
                   (.add winners (.get candidates i)))
                 (when (= this-score best-score)
                   ; This is the same as the best we've seen so far, so
                   ; add it to the winners.
                   (.add winners (.get candidates i)))))
             winners)
           candidates
           (rest cases)))))))

(defn mutate-one-over-length
  [bits]
  (let [len (count bits)
        mutation-rate (/ 1.0 len)]
    (map #(if (< (rand) mutation-rate)
            (- 1 %)
            %)
         bits)))

(defn two_point_xo
  [first-bits second-bits]
  (let [len (count first-bits)
        first-cut (rand-int len)
        second-cut (rand-int len)
        low-cut (min first-cut second-cut)
        high-cut (max first-cut second-cut)]
    (concat (take low-cut first-bits)
            (take (- high-cut low-cut) (drop low-cut second-bits))
            (drop high-cut first-bits))
    ))

(defn make-child
  [scorer pop]
  (let [first-parent (lexicase-array-list pop)
        second-parent (lexicase-array-list pop)]
    (make-individual scorer
     (mutate-one-over-length 
      (two_point_xo (:bits first-parent) (:bits second-parent))))
    )
  )

(defn next-generation
  [scorer pop]
  (let [pop-size (count pop)]
    (pmap (fn [_] (make-child scorer pop)) (range pop-size))))

(defn run-ga
  [pop-size num-bits scorer num-gens]
  (let [initial-population (pmap-make-population pop-size num-bits scorer)
        gens (take num-gens (iterate (partial next-generation scorer) initial-population))]
    ;; (doseq [[gen-num pop] (map #(vector %1 %2) (range) gens)]
    ;;   (println (str "Generation: " gen-num))
    ;;   (println (str "\tBest individual: " (best-individual pop))))
    ;; (println 
     (best-individual (last gens))
    ;;  )
    ))

(defn -main
  [& _args]
  (bench/with-progress-reporting
    (bench/bench
      (run-ga 1000 512 hiff 50)
    ))
  (shutdown-agents))

;; (defn -main
;;   [& _args]
;;   (let [bits (concat (repeat 64 0) (repeat 64 1))]
;;     (bench/with-progress-reporting
;;     (bench/bench
;;       (hiff bits)))))
