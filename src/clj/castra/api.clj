(ns castra.api
  (:refer-clojure :exclude [defn])
  (:require [castra.rules     :refer [allow deny]])
  (:require [tailrecursion.castra :refer [defrpc]]))

(def polygons (atom []))
(add-watch polygons :state (fn [k r o n] (println n)))

(defrpc get-state []
  {:rpc [(allow)]}
  {:polygons @polygons})

(defrpc add-point [point polygon-id]
  {:rpc [(allow)] 
   :rpc/query [(get-state)]}
  (let [poly  (first  (filter #(= (:id %) polygon-id) @polygons))
          path  (or (:path poly) [])
          npath (conj path point)
          npoly (assoc poly :path npath)
          ndx   (.indexOf @polygons poly)
          id    (:id poly)]
    (when id
      (swap! polygons assoc-in [ndx] npoly))
    (when (not id)
      (swap! polygons conj {:path npath :id (inc (count @polygons)) :status :incomplete}))))

(defrpc finalize-polygon [polygon-id]
  {:rpc [(allow)]
   :rpc/query [(get-state)]}
  (let [poly    (first (filter #(= (:id %) polygon-id) @polygons))
        npoly   (assoc poly :status :complete)
        ndx     (.indexOf @polygons poly)]
    (swap! polygons assoc-in [ndx] npoly)))
