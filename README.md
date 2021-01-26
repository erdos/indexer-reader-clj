# indexer-reader-clj

A Clojure wrapper for the maven-indexer-reader project.

## Usage

The `ReadableRes` protocol is used for accessing remote index files. A default implementation for the `URI` type is added.

The `WritableRes` protocol is used to save the state of the indexer.

The following example prints out all the artifacts from Clojars.

```
(def clojars-root (new java.net.URI "http://repo.clojars.org/.index/"))

(doseq [chunk (index-reader clojars-root nil)]
  (println :name (.getName chunk))
  (doseq [record chunk]
    (println (map-record record))))
```

## License

Copyright © 2021 Janos Erdos

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
