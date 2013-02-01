# muir

A library to help you write Clojure source code translations via an abstract
syntax tree (AST), based on Ambrose'
[analyze](https://github.com/frenchy64/analyze).

`muir` makes it easy to process the nodes in the AST that you care about,
without having to set up traversal of the whole tree.  The traversal also
provides a mechanism for pushing arbitrary data down the tree branches, and back
up to parent nodes. Finally it provides an emitter to turn the AST back into
clojure forms, which can be extended simply, so you only have to provide emitter
functions for non-standard code output.

## Artifacts

Available in [clojars](http://clojars.org/muir).

```clj
:dependencies [[muir "0.1.0"]
```

## Usage

### Analyse

The `analyse-form` function is provided for use within a clojure macros.  It
provides the bindings required to run the analyzer within the context of a
macroexpansion.

```clj
(require '[muir.ast :refer :all])

(defmacro form-ast [form]
  (list 'quote (analyse-form (ns-name *ns*))))

(def ast (form-ast (let [x 1] x)))

(clojure.pprint/pprint ast)
```

### Output

The `emit` function allows you to output s-expressions for an AST, given an
emitter function.  The `emit-node` multi-method provides a default emitter
function.

```clj
(require '[muir.ast :refer :all])

(defmacro identity-transform [form]
  (emit
    (analyse-form (ns-name *ns*))
    emit-node))
```

### Transformation

Transformation of the AST is facilitated by the provision of traversal functions
that can be used a basis for a transformation.  To write a transformation, you
define a base traversal using the `deftraversal` macro, define your
transformation as a multi-method (dispatched on the AST :op), and forward the
:default method to the traversal functions.

```clj
;; Define a basic traversal to default to
(deftraversal transform-local-traversal {} #{})

;; Define our transformation
(defmulti transform-local-impl :op)

;; Default the transformation to the traversal
(defmethod transform-local-impl :default
  [ast] (transform-local-traversal ast))

;; Modify binding inits to call str on the init forms
(defmethod transform-local-impl :binding-init
  [ast-node]
  (let [[{:keys [local-binding init] :as node} parent]
        (transform-local-traversal ast-node)]
    [(assoc node :init (op :invoke (op :var #'str {:tag String}) [init] {}))
     parent]))

;; Create a top level form to invoke the transformation
(defmacro transform-bindings [form]
  (with-emit-fn emit-node
    (emit-node (first (traverse
                       (analyse-form (ns-name *ns*) form)
                       transform-local-impl)))))

(transform-bindings (let [x 1] x)) => "1"
(transform-bindings (let [x :a] x)) => ":a"
```

The traversal and transformation functions return an ast-node and an ast-node
fragment for merging with the parent.  The `deftraversal` macro also allows
specification of a set of keys to be merged from the parent on the child nodes
before they are traversed.  Together this allows you to keep track of visible
local bindings, for example.

The `deftraversal` macro also allows the specification of a traversal order for
child nodes.

## License

Copyright © 2013 Hugo Duncan

Distributed under the Eclipse Public License.
