{:doc
 {:dependencies [[codox-md "0.1.0"]]
  :codox {:writer codox-md.writer/write-docs
          :output-dir "doc/0.1/"}
  :aliases {"marg" ["marg" "-d" "doc/0.1/"]
            "doc" ["do" "doc," "marg" "-d" "doc/0.1/"]}}
 :release
 {:plugins [[lein-set-version "0.2.1"]]
  :set-version
  {:updates [{:path "README.md" :no-snapshot true}]}}
 :clojure-1.5.0 {:dependencies [[org.clojure/clojure "1.5.0-RC4"]]}}
