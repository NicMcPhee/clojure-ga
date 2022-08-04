(ns clojure-ga.core
  (:gen-class))

(use 'criterium.core)

(defn make-individual
  [num-bits]
  (let [bits (repeatedly num-bits #(rand-int 2))
        num-ones (count (filter #(= % 1) bits))]
    {
      :bits bits
      :fitness num-ones
    }))

(defn make-population
  [pop-size num-bits]
  (repeatedly
    pop-size
    #(make-individual num-bits)))

(defn pmap-make-population
  [pop-size num-bits]
  (pmap (fn [_] (make-individual num-bits))
        (range pop-size)))

(defn best-individual 
  [pop]
  (apply max-key :fitness pop))

(defn -main 
  [& args]
  (let [pop (pmap-make-population 100 128)
        best (best-individual pop)]
    (println best)
    (shutdown-agents)))
