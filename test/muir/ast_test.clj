(ns muir.ast-test
  (:require
   [clojure.test :refer :all]
   [clojure.walk :refer [macroexpand-all]]
   [muir.ast :refer :all]))

;;; simple test of generating an AST
(defmacro form-ast [form]
  (list 'quote (analyse-form (ns-name *ns*) form)))

(deftest ast-test
  (let [ast (form-ast (let [x 1] x))]
    (is (= :invoke (:op ast)))))


;;; Test round trip of forms through ast and emitter
(defn identity-transform [form]
  (emit (analyse-form (ns-name *ns*) form) emit-node))

;; a test method to assert that a form, when put through the analyzer and
;; ast-emitter, evaluates to the same as the original form.
(defmethod assert-expr 'form-round-trip [msg test-form]
  (let [[_ form] test-form
        nf (identity-transform form)]
    `(if (= ~form ~nf)
       (do-report
        {:type :pass :message ~msg :expected '~form :actual '~nf})
       (do-report
        {:type :fail :message ~msg :expected '~form :actual '~nf}))))

(deftest constant-test
  (testing "values"
    (is (form-round-trip 1))
    (is (form-round-trip :a))
    (is (form-round-trip 'a))
    (is (form-round-trip #'identity-transform))
    (is (form-round-trip "abc"))
    (is (form-round-trip [1 :b "abc"]))
    (is (form-round-trip {:a 'a})))
  (testing "expressions"
    (is (form-round-trip (let [a 1] a)))
    (is (form-round-trip (case (+ 1 2) 1 :error 2 :eh 3 :ok 4 :fail)))
    (is (form-round-trip (let [f (fn ff []
                                    (if nil (ff) 1))]; check ff resolves
                           (f))))
    (is (form-round-trip (try 1 (catch Exception e (with-out-str (println e))))))
    (is (form-round-trip (try (throw (Exception. "hi"))
                              (catch Exception e (with-out-str (println e))))))
    (is (form-round-trip (binding [*print-readably* true] (pr-str 1))))))


;;; # A test that translates all local bindings into strings

;; Define a basic traversal to default to
(deftraversal transform-local-traversal {} #{})

;; Define our transformation
(defmulti transform-local-impl :op)

;; Default the transformation to the traversal
(defmethod transform-local-impl :default
  [ast]
  (transform-local-traversal ast))

;; Modify binding inits to call str on the init forms
(defmethod transform-local-impl :binding-init
  [ast-node]
  (let [[{:keys [local-binding init] :as node} parent]
        (transform-local-traversal ast-node)]
    [(assoc node
       :init (op :invoke (op :var #'str {:tag String}) [init] {}))
     parent]))

;; Create a top level form to invoke the transformation
(defmacro transform-bindings [form]
  (with-emit-fn emit-node
    (emit-node (first (traverse
                       (analyse-form (ns-name *ns*) form)
                       transform-local-impl)))))

;; test the transofrmation
(deftest transform-binding-inits-test
  (is (= "1" (transform-bindings (let [x 1] x))))
  (is (= ":a" (transform-bindings (let [x :a] x)))))
