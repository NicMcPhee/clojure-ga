(ns clojure-ga.core
  (:gen-class))

(defn count-ones
  [bits]
  (count (filter #(= % 1) bits)))

(defn hiff
  [bits]
  (let [len (count bits)
        half-len (quot len 2)]
    (if (< len 2)
      len
      (+ (hiff (take half-len bits))
         (hiff (drop half-len bits))
         (if (every? #(= % (first bits)) bits)
           len
           0)))))

(defn make-individual
  [num-bits compute-fitness]
  (let [bits (repeatedly num-bits #(rand-int 2))
        num-ones (compute-fitness bits)]
    {
      :bits bits
      :fitness num-ones
    }))

(defn make-population
  [pop-size num-bits compute-fitness]
  (repeatedly
    pop-size
    #(make-individual num-bits compute-fitness)))

(defn pmap-make-population
  [pop-size num-bits compute-fitness]
  (pmap (fn [_] (make-individual num-bits compute-fitness))
        (range pop-size)))

(defn best-individual 
  [pop]
  (apply max-key :fitness pop))

(defn -main 
  [& _args]
  (let [pop (pmap-make-population 100 128 hiff)
        best (best-individual pop)]
    (println best)
    (shutdown-agents)))
