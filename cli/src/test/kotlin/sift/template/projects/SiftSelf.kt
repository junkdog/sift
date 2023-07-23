package sift.template.projects

val SIFT = """
── scope
   ├─ Annotations
   │  ├─ Entity.Type.set(key, children)
   │  ├─ entity(id, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, properties)
   │  ├─ entity(id, properties)
   │  ├─ explodeTypes(element, synthesize, f)
   │  ├─ filter(entity)
   │  ├─ filter(regex, invert)
   │  ├─ filter(string, invert)
   │  ├─ label(pattern, ops)
   │  ├─ log(tag)
   │  ├─ logCount(tag)
   │  ├─ nested(element, f)
   │  ├─ property(entity, key, extract)
   │  ├─ property(entity, key, strategy, extract)
   │  ├─ property(key, extract)
   │  ├─ property(key, strategy, extract)
   │  ├─ scope(label, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ ⚙ editor(ops)
   │  ├─ ⚙ readAttribute(attribute)
   │  ├─ ⚙ readName(shorten)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  └─ ⚙ withValue(value)
   ├─ Classes
   │  ├─ Entity.Type.set(key, children)
   │  ├─ annotatedBy(annotation)
   │  ├─ annotations(filter, f)
   │  ├─ annotations(label, filter, f)
   │  ├─ entity(id, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, properties)
   │  ├─ entity(id, properties)
   │  ├─ enums(f)
   │  ├─ enums(label, f)
   │  ├─ fields(inherited, f)
   │  ├─ fields(label, inherited, f)
   │  ├─ filter(access, invert)
   │  ├─ filter(entity)
   │  ├─ filter(modifiers, invert)
   │  ├─ filter(modifiers, invert)
   │  ├─ filter(regex, invert)
   │  ├─ filter(string, invert)
   │  ├─ filterType(type)
   │  ├─ implements(type)
   │  ├─ interfaces(recursive, synthesize, f)
   │  ├─ label(pattern, ops)
   │  ├─ log(tag)
   │  ├─ logCount(tag)
   │  ├─ methods(inherited, f)
   │  ├─ methods(label, selection, f)
   │  ├─ methods(selection, f)
   │  ├─ outerScope(label, f)
   │  ├─ property(entity, key, extract)
   │  ├─ property(entity, key, strategy, extract)
   │  ├─ property(key, extract)
   │  ├─ property(key, strategy, extract)
   │  ├─ scope(label, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ superclass(f)
   │  ├─ ⚙ editor(ops)
   │  ├─ ⚙ readAnnotation(annotation, attribute)
   │  ├─ ⚙ readName(shorten)
   │  ├─ ⚙ readType()
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  └─ ⚙ withValue(value)
   ├─ Elements
   │  ├─ Entity.Type.set(key, children)
   │  ├─ entity(id, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, properties)
   │  ├─ entity(id, properties)
   │  ├─ filter(entity)
   │  ├─ filter(modifiers, invert)
   │  ├─ filter(modifiers, invert)
   │  ├─ filter(regex, invert)
   │  ├─ filter(string, invert)
   │  ├─ label(pattern, ops)
   │  ├─ log(tag)
   │  ├─ logCount(tag)
   │  ├─ property(entity, key, extract)
   │  ├─ property(entity, key, strategy, extract)
   │  ├─ property(key, extract)
   │  ├─ property(key, strategy, extract)
   │  ├─ scope(label, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ ⚙ editor(ops)
   │  ├─ ⚙ readName(shorten)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  └─ ⚙ withValue(value)
   ├─ Fields
   │  ├─ Entity.Type.set(key, children)
   │  ├─ annotatedBy(annotation)
   │  ├─ annotations(filter, f)
   │  ├─ annotations(label, filter, f)
   │  ├─ entity(id, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, properties)
   │  ├─ entity(id, properties)
   │  ├─ explodeType(synthesize, f)
   │  ├─ filter(access, invert)
   │  ├─ filter(entity)
   │  ├─ filter(modifiers, invert)
   │  ├─ filter(modifiers, invert)
   │  ├─ filter(regex, invert)
   │  ├─ filter(string, invert)
   │  ├─ filterType(type)
   │  ├─ label(pattern, ops)
   │  ├─ log(tag)
   │  ├─ logCount(tag)
   │  ├─ outerScope(label, f)
   │  ├─ property(entity, key, extract)
   │  ├─ property(entity, key, strategy, extract)
   │  ├─ property(key, extract)
   │  ├─ property(key, strategy, extract)
   │  ├─ scope(label, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ signature(f)
   │  ├─ ⚙ editor(ops)
   │  ├─ ⚙ readAnnotation(annotation, attribute)
   │  ├─ ⚙ readName(shorten)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  └─ ⚙ withValue(value)
   ├─ Methods
   │  ├─ Entity.Type.set(key, children)
   │  ├─ Entity.Type.set(key, rhs)
   │  ├─ EntityResolution.set(key, rhs)
   │  ├─ annotatedBy(annotation)
   │  ├─ annotations(filter, f)
   │  ├─ annotations(label, filter, f)
   │  ├─ entity(id, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, properties)
   │  ├─ entity(id, properties)
   │  ├─ fieldAccess(f)
   │  ├─ filter(access, invert)
   │  ├─ filter(entity)
   │  ├─ filter(modifiers, invert)
   │  ├─ filter(modifiers, invert)
   │  ├─ filter(regex, invert)
   │  ├─ filter(string, invert)
   │  ├─ filterName(name, invert)
   │  ├─ filterName(regex, invert)
   │  ├─ instantiationsOf(type, f)
   │  ├─ invocationsOf(type, synthesize, f)
   │  ├─ invokes(type)
   │  ├─ label(pattern, ops)
   │  ├─ log(tag)
   │  ├─ logCount(tag)
   │  ├─ outerScope(label, f)
   │  ├─ parameters(label, selection, f)
   │  ├─ parameters(selection, f)
   │  ├─ property(entity, key, extract)
   │  ├─ property(entity, key, strategy, extract)
   │  ├─ property(key, extract)
   │  ├─ property(key, strategy, extract)
   │  ├─ returns(f)
   │  ├─ scope(label, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ ⚙ editor(ops)
   │  ├─ ⚙ readAnnotation(annotation, attribute)
   │  ├─ ⚙ readName(shorten)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  └─ ⚙ withValue(value)
   ├─ Parameters
   │  ├─ Entity.Type.set(key, children)
   │  ├─ annotatedBy(annotation)
   │  ├─ annotations(filter, f)
   │  ├─ annotations(label, filter, f)
   │  ├─ entity(id, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, properties)
   │  ├─ entity(id, properties)
   │  ├─ explodeType(synthesize, f)
   │  ├─ filter(entity)
   │  ├─ filter(modifiers, invert)
   │  ├─ filter(modifiers, invert)
   │  ├─ filter(regex, invert)
   │  ├─ filter(string, invert)
   │  ├─ filterType(type)
   │  ├─ label(pattern, ops)
   │  ├─ log(tag)
   │  ├─ logCount(tag)
   │  ├─ outerScope(label, f)
   │  ├─ parameter(nth)
   │  ├─ property(entity, key, extract)
   │  ├─ property(entity, key, strategy, extract)
   │  ├─ property(key, extract)
   │  ├─ property(key, strategy, extract)
   │  ├─ scope(label, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ signature(f)
   │  ├─ ⚙ editor(ops)
   │  ├─ ⚙ readAnnotation(annotation, attribute)
   │  ├─ ⚙ readName(shorten)
   │  ├─ ⚙ readType()
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  └─ ⚙ withValue(value)
   ├─ Signature
   │  ├─ Entity.Type.set(key, children)
   │  ├─ entity(id, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, properties)
   │  ├─ entity(id, properties)
   │  ├─ explodeType(synthesize, f)
   │  ├─ explodeTypeT(signature, synthesize, f)
   │  ├─ filter(regex, invert)
   │  ├─ filter(s, invert)
   │  ├─ label(pattern, ops)
   │  ├─ log(tag)
   │  ├─ logCount(tag)
   │  ├─ property(entity, key, extract)
   │  ├─ property(entity, key, strategy, extract)
   │  ├─ property(key, extract)
   │  ├─ property(key, strategy, extract)
   │  ├─ scope(label, f)
   │  ├─ typeArgument(index, f)
   │  ├─ typeArguments(f)
   │  ├─ ⚙ editor(ops)
   │  ├─ ⚙ readName(shorten)
   │  ├─ ⚙ readName()
   │  ├─ ⚙ readType()
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  └─ ⚙ withValue(value)
   ├─ Synthesize
   │  ├─ entity(id, type, labelFormatter)
   │  └─ label(pattern, ops)
   └─ Template
      ├─ Entity.Type.set(key, rhs)
      ├─ classes(f)
      ├─ classes(label, f)
      ├─ classesOf(entity, f)
      ├─ classesOf(label, entity, f)
      ├─ elementsOf(entity, f)
      ├─ elementsOf(label, entity, f)
      ├─ fieldsOf(entity, f)
      ├─ fieldsOf(label, entity, f)
      ├─ include(template)
      ├─ methodsOf(entity, f)
      ├─ methodsOf(label, entity, f)
      ├─ scope(label, f)
      ├─ scope(label, op, entity, f)
      └─ synthesize(f)
"""

val SIFT_PROFILE = """
     exec  ety#    in      out
 34.66 ms   249     0 ->     0  ── template
  0.00 ms     0     0 ->     0     ├─ template-scope
                    0 ->     0     ├─ fork("register actions")
  0.00 ms     0     0 ->   544     │  ├─ classes
  0.00 ms     0   544 ->   544     │  ├─ class-scope
  0.14 ms     0   544 ->   223     │  ├─ filter(sift.core.api)
  2.51 ms     0   223 ->    76     │  ├─ implements(Action)
  0.02 ms     0    76 ->    75     │  ├─ filter(SimpleAction, invert)
  0.09 ms    75    75 ->    75     │  ├─ register-entity(action)
                   75 ->    75     │  └─ fork("scope")
  0.33 ms    75    75 ->    75     │     ├─ outer-class
  0.00 ms    75    75 ->    75     │     ├─ class-scope
  0.07 ms    84    75 ->    75     │     ├─ register-entity(element)
  0.38 ms    84    75 ->    75     │     └─ register-children(element[actions], action)
                    0 ->     0     └─ fork("register dsl")
  0.00 ms    84     0 ->     0        ├─ template-scope
                    0 ->     0        ├─ fork("register dsl scopes")
  0.00 ms    84     0 ->   544        │  ├─ classes
  0.00 ms    84   544 ->   544        │  ├─ class-scope
  0.18 ms    84   544 ->    67        │  ├─ filter(sift\.core\.(dsl|api\.Dsl))
                   67 ->    67        │  ├─ fork("scopes from annotated classes")
  0.00 ms    84    67 ->    67        │  │  ├─ class-scope
  0.28 ms    84    67 ->     9        │  │  ├─ annotated-by(SiftTemplateDsl)
  0.01 ms    93     9 ->     9        │  │  ├─ register-entity(scope)
                    9 ->     9        │  │  └─ fork
  0.10 ms    93     9 ->     9        │  │     ├─ read-name
  0.08 ms    93     9 ->     9        │  │     ├─ edit-text((replace(\QDsl.\E -> )))
  0.00 ms    93     9 ->     9        │  │     └─ update-property(name)
                   67 ->    67        │  └─ fork("scopes from children of Core<Element>")
  0.00 ms    93    67 ->    67        │     ├─ class-scope
  0.03 ms    93    67 ->     6        │     ├─ implements(Core)
  0.01 ms    93     6 ->     6        │     ├─ register-entity(scope)
                    6 ->     6        │     └─ fork
  0.09 ms    93     6 ->     6        │        ├─ read-name
  0.05 ms    93     6 ->     6        │        ├─ edit-text((replace(\QDsl.\E -> )))
  0.00 ms    93     6 ->     6        │        └─ update-property(name)
                    0 ->     0        └─ fork("register functions")
  0.00 ms    93     0 ->     9           ├─ classes-of(scope)
  0.00 ms    93     9 ->     9           ├─ class-scope
                    9 ->     9           ├─ fork
  0.22 ms    93     9 ->   156           │  ├─ methods(inherited)
  0.00 ms    93   156 ->   156           │  ├─ method-scope
  0.01 ms    93   156 ->   156           │  ├─ filter-visibility(public)
  0.16 ms   249   156 ->   156           │  ├─ register-entity(dsl)
                  156 ->   156           │  ├─ fork
  0.06 ms   249   156 ->   156           │  │  ├─ read-name
  0.03 ms   249   156 ->   156           │  │  └─ update-property(name)
                  156 ->   156           │  ├─ fork
  0.17 ms   249   156 ->   108           │  │  ├─ returns
  0.00 ms   249   108 ->   108           │  │  ├─ signature-scope
  0.13 ms   249   108 ->   108           │  │  ├─ filter-signature(^(.+\.|)Action<|${'$'})
                  108 ->   108           │  │  └─ fork
  0.05 ms   249   108 ->    23           │  │     ├─ filter-nth(1)
  0.00 ms   249    23 ->    23           │  │     ├─ signature-scope
                   23 ->    23           │  │     └─ fork
  0.06 ms   249    23 ->    23           │  │        ├─ filter-nth(0)
  0.00 ms   249    23 ->    23           │  │        ├─ signature-scope
                   23 ->    23           │  │        └─ fork
  0.05 ms   249    23 ->     1           │  │           ├─ explode-raw-type
  0.00 ms   249     1 ->     1           │  │           ├─ class-scope
  0.00 ms   249     1 ->     1           │  │           ├─ filter(ValueNode)
                    1 ->     1           │  │           └─ fork
  0.00 ms   249     1 ->     1           │  │              ├─ with-value(⚙ )
  0.11 ms   249     1 ->     1           │  │              └─ update-property(icon, dsl)
                  156 ->   156           │  └─ fork
  0.24 ms   249   156 ->   310           │     ├─ parameters(excludingReceiver)
  0.00 ms   249   310 ->   310           │     ├─ parameter-scope
                  310 ->   310           │     ├─ fork
  0.10 ms   249   310 ->   310           │     │  ├─ read-name
  0.26 ms   249   310 ->   310           │     │  ├─ edit-text((replace(.+ -> ${'$'}0)))
 10.13 ms   249   310 ->   310           │     │  └─ update-property(params, dsl)
                  310 ->   310           │     └─ fork
  0.09 ms   249   310 ->   310           │        ├─ read-type
 17.34 ms   249   310 ->   310           │        └─ update-property(param-types, dsl)
  0.31 ms   249     9 ->     9           └─ register-children(scope[fns], dsl)
"""

val SIFT_STATS = """
timing.ms.parseAsmClassNodes                   32
timing.ms.parseSiftClassNodes                  62
timing.ms.templateProcessing                   35
allClasses                                    544
classByType                                   544
methodInvocationsCache                          0
methodInvocationsCache.flatten                  0
methodFieldCache                                0
methodFieldCache.flatten                        0
parents                                       544
parents.flatten                               467
implementedInterfaces                         544
implementedInterfaces.flatten                 734
associations.keys                             714
associations.traces                         1 272
associations.traces.p50                         1
associations.traces.p90                         6
associations.traces.max                        58
associations.traces.depth.p50                   3
associations.traces.depth.p90                   4
associations.traces.depth.max                   6
associations.flatten                        3 665
"""