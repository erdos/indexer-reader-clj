(ns indexer-reader-clj.core-test
  (:require [clojure.test :refer :all]
            [indexer-reader-clj.core :refer :all]))

#_
(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

(def store (->InMemoryStore))

(def clojars-root (new java.net.URI "http://repo.clojars.org/.index/"))

(with-open [chunks (indexer-reader-clj.core/index-reader clojars-root store)]
  (doseq [chunk chunks]
    (println :name (.getName chunk))
    (println (take 10 (map indexer-reader-clj.core/map-record chunk)))
    (time  (println :count (count (seq chunk))))))

(println)

;; same action again, reads 0 chunks!
(with-open [chunks (indexer-reader-clj.core/index-reader clojars-root store)]
  (doseq [chunk chunks] (println :chunk-read!)))