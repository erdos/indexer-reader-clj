(ns indexer-reader-clj.core
  (:require [clojure.java.io :as io])
  (:import org.apache.maven.index.reader.Record))

(set! *warn-on-reflection* true)

(defprotocol ReadableRes
  (locate-in [_ name] "Returns an input-stream"))


(defprotocol WritableRes
  (locate-out [_ name] "Returns an output-stream")
  (close-out [_] "Closes the resource")
  (close-named-out [_ name] "Closes the resource"))


(defn ->ResourceHandler [iores]
  (assert (satisfies? ReadableRes iores))
  (reify
    org.apache.maven.index.reader.ResourceHandler
    (locate [_ name]
      (reify org.apache.maven.index.reader.ResourceHandler$Resource
        (read [_] (locate-in iores name))))
    java.lang.AutoCloseable
    (close [_] (comment))))


(defn ->WritableResourceHandler [iores]
  (assert (satisfies? ReadableRes iores))
  (assert (satisfies? WritableRes iores))
  (reify
    org.apache.maven.index.reader.WritableResourceHandler
    (locate [_ name]
      (reify org.apache.maven.index.reader.WritableResourceHandler$WritableResource
        (read [_] (locate-in iores name))
        (write [_] (locate-out iores name))
        java.lang.AutoCloseable
        (close [_] (close-named-out iores name))))
    java.lang.AutoCloseable
    (close [_] (close-out iores))))


(extend-type java.io.File
  WritableRes
  (locate-out [file name] (io/output-stream (io/file file name)))
  (close-out [file] nil)
  (close-named-out [file name] nil)
  ReadableRes
  (locate-in [file name] (io/input-stream (io/file file name))))


(extend-type java.net.URI
  ReadableRes (locate-in [uri name] (io/input-stream (.resolve ^java.net.URI uri ^String name))))


(defn index-reader [reader writer]
  (new org.apache.maven.index.reader.IndexReader
       (some-> writer ->WritableResourceHandler)
       (->ResourceHandler reader)))


(def ^:private record-expander (new org.apache.maven.index.reader.RecordExpander))


(defmulti ^:private map-record-impl (fn [^Record record] (.name (.getType record))))


(defn map-record [map]
  (map-record-impl (.apply ^org.apache.maven.index.reader.RecordExpander record-expander map)))


(defmethod map-record-impl "ALL_GROUPS" [^Record record]
  {:record-type :all-groups
   :all-groups (vec (.get record Record/ALL_GROUPS))})


(defmethod map-record-impl "DESCRIPTOR" [^Record record]
  {:record-type  :descriptor
   :repository-id (.get record Record/REPOSITORY_ID)})


(defmethod map-record-impl "ROOT_GROUPS" [^Record record]
  {:record-type :root-groups
   :root-groups (vec (.get record Record/ROOT_GROUPS))})


(defmethod map-record-impl "ARTIFACT_REMOVE" [^Record record]
  {:record-type    :artifact-remove
   :rec-modified   (.get record Record/REC_MODIFIED)
   :group-id       (.get record Record/GROUP_ID)
   :artifact-id    (.get record Record/ARTIFACT_ID)
   :version        (.get record Record/VERSION)
   :classifier     (.get record Record/CLASSIFIER)
   :file-extension (.get record Record/FILE_EXTENSION)
   :packaging      (.get record Record/PACKAGING)})


(defmethod map-record-impl "ARTIFACT_ADD" [^Record record]
  {:record-type    :artifact-add
   :rec-modified   (.get record Record/REC_MODIFIED)
   :group-id       (.get record Record/GROUP_ID)
   :artifact-id    (.get record Record/ARTIFACT_ID)
   :version        (.get record Record/VERSION)
   :classifier     (.get record Record/CLASSIFIER)
   :file-extension (.get record Record/FILE_EXTENSION)
   :file-modified  (.get record Record/FILE_MODIFIED)
   :file-size      (.get record Record/FILE_SIZE)
   :packaging      (.get record Record/PACKAGING)
   :has-sources    (.get record Record/HAS_SOURCES)
   :has-javadoc    (.get record Record/HAS_JAVADOC)
   :has-signature  (.get record Record/HAS_SIGNATURE)
   :name           (.get record Record/NAME)
   :description    (.get record Record/DESCRIPTION)
   :sha1           (.get record Record/SHA1)
   :classnames     (vec (.get record Record/CLASSNAMES))
   :plugin-prefix  (.get record Record/PLUGIN_PREFIX)
   :plugin-goals   (vec (.get record Record/PLUGIN_GOALS))})


(comment
    :osgi-bundle-symbolic-name  (.get record Record/OSGI_BUNDLE_SYMBOLIC_NAME)
    :osgi-bundle-version        (.get record Record/OSGI_BUNDLE_VERSION)
    :osgi-export-package        (.get record Record/OSGI_EXPORT_PACKAGE)
    :osgi-export-service        (.get record Record/OSGI_EXPORT_SERVICE)
    :osgi-bundle-description    (.get record Record/OSGI_BUNDLE_DESCRIPTION)
    :osgi-bundle-name           (.get record Record/OSGI_BUNDLE_NAME)
    :osgi-bundle-license        (.get record Record/OSGI_BUNDLE_LICENSE)
    :osgi-export-docurl         (.get record Record/OSGI_EXPORT_DOCURL)
    :osgi-import-package        (.get record Record/OSGI_IMPORT_PACKAGE)
    :osgi-require-bundle        (.get record Record/OSGI_REQUIRE_BUNDLE)
    :osgi-provide-capability    (.get record Record/OSGI_PROVIDE_CAPABILITY)
    :osgi-require-capability    (.get record Record/OSGI_REQUIRE_CAPABILITY)
    :osgi-fragment-host         (.get record Record/OSGI_FRAGMENT_HOST)
    :osgi-bree                  (.get record Record/OSGI_BREE)
    :sha-256                    (.get record Record/SHA_256)
)

(deftype GetSetStore [^:unsynchronized-mutable state accessor]
  WritableRes
    (locate-out [store name]
     (doto (new java.io.ByteArrayOutputStream)
       (->> (assoc state name) (set! state))))
     (close-out [store] nil)
     (close-named-out [store name]
       (accessor name (str (get state name)))
       (set! state (dissoc state name)))
  ReadableRes
    (locate-in [store name]
      (some-> (accessor name) (str) (.getBytes) (java.io.ByteArrayInputStream.))))

(ns-unmap *ns* '->GetSetStore)

(defn ->GetSetStore [accessor]
  (assert (fn? accessor))
  (GetSetStore. {} accessor))

(defn ->InMemoryStore []
  (let [mem (volatile! {})]
    (->GetSetStore (fn ([name] (get @mem name))
                       ([name value] (vswap! mem assoc name value))))))
