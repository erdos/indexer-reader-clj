(ns indexer-reader-clj.core
  (:require [clojure.java.io :as io])
  (:import org.apache.maven.index.reader.Record))


(defprotocol ReadableRes
  (locate-in [_ name] "Returns an input-stream"))


(defprotocol WritableRes
  (locate-out [_ name] "Returns an output-stream"))


(defn ->ResourceHandler [iores]
  (assert (satisfies? ReadableRes iores))
  (reify
    org.apache.maven.index.reader.ResourceHandler
    (locate [_ name]
      (reify org.apache.maven.index.reader.ResourceHandler$Resource
        (read [_] (locate-in iores name))))))


(defn ->WritableResourceHandler [iores]
  (assert (satisfies? ReadableRes iores))
  (assert (satisfies? WritableRes iores))
  (reify
    org.apache.maven.index.reader.WritableResourceHandler
    (locate [_ name]
      (reify org.apache.maven.index.reader.WritableResourceHandler$WritableResource
        (read [_] (locate-in iores name))
        (write [_] (locate-out iores name))))))


(extend-type java.io.File
  WritableRes (locate-out [file name] (io/output-stream (io/file file name)))
  ReadableRes (locate-in [file name] (io/input-stream (io/file file name))))


(extend-type java.net.URI
  ReadableRes (locate-in [uri name] (io/input-stream (.resolve uri name))))


(defn index-reader [reader writer]
  (assert reader)
  (new org.apache.maven.index.reader.IndexReader writer (->ResourceHandler reader)))


(def ^:private record-expander (new org.apache.maven.index.reader.RecordExpander))


(defn map-record [map]
  (let [record (.apply record-expander map)]
    {:repository-id  (.get record Record/REPOSITORY_ID)
     :all-groups     (vec (.get record Record/ALL_GROUPS))
     :root-groups    (vec (.get record Record/ROOT_GROUPS))
     :rec-modified   (.get record Record/REC_MODIFIED)
     :group-id       (.get record Record/GROUP_ID)
     :artifact-id    (.get record Record/ARTIFACT_ID)
     :version        (.get record Record/VERSION)
     :classifier     (.get record Record/CLASSIFIER)
     :packaging      (.get record Record/PACKAGING)
     :file-extension (.get record Record/FILE_EXTENSION)
     :file-modified  (.get record Record/FILE_MODIFIED)
     :file-size      (.get record Record/FILE_SIZE)
     :has-sources    (.get record Record/HAS_SOURCES)
     :has-javadoc    (.get record Record/HAS_JAVADOC)
     :has-signature  (.get record Record/HAS_SIGNATURE)
     :name           (.get record Record/NAME)
     :description    (.get record Record/DESCRIPTION)
     :sha1           (.get record Record/SHA1)
     :classnames     (vec (.get record Record/CLASSNAMES))
     :plugin-prefix  (.get record Record/PLUGIN_PREFIX)
     :plugin-goals   (vec (.get record Record/PLUGIN_GOALS))
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
    }))
