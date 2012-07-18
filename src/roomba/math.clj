(ns roomba.math)

(defmacro selections 
  "Set s choose n. This expands into a for comprehension."
  [s n] 
  (let [leftbind (repeatedly n gensym)
        setbind (gensym)
        rightbind (repeat setbind)
        elem-set (into #{} s)]
    `(let [~setbind ~elem-set]
       (for [~@(interleave leftbind rightbind)] [~@leftbind]))))
 
