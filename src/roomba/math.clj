(ns roomba.math)

(defn selections 
  "Set s choose n. This expands into a for comprehension."
  [selements n] 
  (let [elements (set selements)]
    (loop [acc [[]] slots n]
      (if (> slots 0)
        (recur (for [x acc e elements]
                 (conj x e))
               (dec slots))
        acc))))
