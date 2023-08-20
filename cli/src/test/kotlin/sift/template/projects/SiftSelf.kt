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
   │  ├─ scope(label, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ ⚙ editor(ops)
   │  ├─ ⚙ readAttribute(attribute)
   │  ├─ ⚙ readName(shorten)
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
   │  ├─ outerScope(label, f)
   │  ├─ property(entity, key, extract)
   │  ├─ property(entity, key, strategy, extract)
   │  ├─ property(key, extract)
   │  ├─ property(key, strategy, extract)
   │  ├─ scope(label, f)
   │  ├─ scope(label, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ superclass(f)
   │  ├─ ⚙ editor(ops)
   │  ├─ ⚙ readAnnotation(annotation, attribute)
   │  ├─ ⚙ readAnnotation(annotation, attribute)
   │  ├─ ⚙ readName(shorten)
   │  ├─ ⚙ readType()
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
   │  ├─ scope(label, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ ⚙ editor(ops)
   │  ├─ ⚙ readName(shorten)
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
   │  ├─ outerScope(label, f)
   │  ├─ property(entity, key, extract)
   │  ├─ property(entity, key, strategy, extract)
   │  ├─ property(key, extract)
   │  ├─ property(key, strategy, extract)
   │  ├─ scope(label, f)
   │  ├─ scope(label, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ signature(f)
   │  ├─ ⚙ editor(ops)
   │  ├─ ⚙ readAnnotation(annotation, attribute)
   │  ├─ ⚙ readAnnotation(annotation, attribute)
   │  ├─ ⚙ readName(shorten)
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
   │  ├─ outerScope(label, f)
   │  ├─ parameters(label, selection, f)
   │  ├─ parameters(selection, f)
   │  ├─ property(entity, key, extract)
   │  ├─ property(entity, key, strategy, extract)
   │  ├─ property(key, extract)
   │  ├─ property(key, strategy, extract)
   │  ├─ returns(f)
   │  ├─ scope(label, f)
   │  ├─ scope(label, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ ⚙ editor(ops)
   │  ├─ ⚙ readAnnotation(annotation, attribute)
   │  ├─ ⚙ readAnnotation(annotation, attribute)
   │  ├─ ⚙ readName(shorten)
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
   │  ├─ outerScope(label, f)
   │  ├─ parameter(nth)
   │  ├─ property(entity, key, extract)
   │  ├─ property(entity, key, strategy, extract)
   │  ├─ property(key, extract)
   │  ├─ property(key, strategy, extract)
   │  ├─ scope(label, f)
   │  ├─ scope(label, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ scope(label, op, entity, f)
   │  ├─ signature(f)
   │  ├─ ⚙ editor(ops)
   │  ├─ ⚙ readAnnotation(annotation, attribute)
   │  ├─ ⚙ readAnnotation(annotation, attribute)
   │  ├─ ⚙ readName(shorten)
   │  ├─ ⚙ readType()
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  └─ ⚙ withValue(value)
   ├─ Signature
   │  ├─ Entity.Type.set(key, children)
   │  ├─ entity(id, errorIfExists, properties)
   │  ├─ entity(id, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, errorIfExists, properties)
   │  ├─ entity(id, labelFormatter, properties)
   │  ├─ entity(id, labelFormatter, properties)
   │  ├─ entity(id, properties)
   │  ├─ entity(id, properties)
   │  ├─ explodeType(synthesize, f)
   │  ├─ explodeTypeT(signature, synthesize, f)
   │  ├─ filter(regex, invert)
   │  ├─ filter(s, invert)
   │  ├─ label(pattern, ops)
   │  ├─ log(tag)
   │  ├─ logCount(tag)
   │  ├─ property(entity, key, extract)
   │  ├─ property(entity, key, extract)
   │  ├─ property(entity, key, strategy, extract)
   │  ├─ property(entity, key, strategy, extract)
   │  ├─ property(key, extract)
   │  ├─ property(key, extract)
   │  ├─ property(key, strategy, extract)
   │  ├─ property(key, strategy, extract)
   │  ├─ scope(label, f)
   │  ├─ typeArgument(index, f)
   │  ├─ typeArguments(f)
   │  ├─ ⚙ editor(ops)
   │  ├─ ⚙ editor(ops)
   │  ├─ ⚙ readName(shorten)
   │  ├─ ⚙ readName(shorten)
   │  ├─ ⚙ readName()
   │  ├─ ⚙ readType()
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
   │  ├─ ⚙ withValue(value)
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
  0.00 ms   376     0 ->     0  ── template
  0.00 ms     0     0 ->     0     ├─ template-scope
                    0 ->     0     ├─ fork("register actions")
  0.00 ms     0     0 ->   544     │  ├─ classes
  0.00 ms     0   544 ->   544     │  ├─ class-scope
  0.00 ms     0   544 ->   223     │  ├─ filter(sift.core.api)
  0.00 ms     0   223 ->    76     │  ├─ implements(Action)
  0.00 ms     0    76 ->    75     │  ├─ filter(SimpleAction, invert)
  0.00 ms    75    75 ->    75     │  ├─ register-entity(action)
                   75 ->    75     │  └─ fork("scope")
  0.00 ms    75    75 ->    75     │     ├─ outer-class
  0.00 ms    75    75 ->    75     │     ├─ class-scope
  0.00 ms    84    75 ->    75     │     ├─ register-entity(element)
  0.00 ms    84    75 ->    75     │     └─ register-children(element[actions], action)
                    0 ->     0     └─ fork("register dsl")
  0.00 ms    84     0 ->     0        ├─ template-scope
                    0 ->     0        ├─ fork("register dsl scopes")
  0.00 ms    84     0 ->   544        │  ├─ classes
  0.00 ms    84   544 ->   544        │  ├─ class-scope
  0.00 ms    84   544 ->    67        │  ├─ filter(sift\.core\.(dsl|api\.Dsl))
                   67 ->    67        │  ├─ fork("scopes from annotated classes")
  0.00 ms    84    67 ->    67        │  │  ├─ class-scope
  0.00 ms    84    67 ->     9        │  │  ├─ annotated-by(SiftTemplateDsl)
  0.00 ms    93     9 ->     9        │  │  ├─ register-entity(scope)
                    9 ->     9        │  │  └─ fork
  0.00 ms    93     9 ->     9        │  │     ├─ read-name
  0.00 ms    93     9 ->     9        │  │     ├─ edit-text((replace(\QDsl.\E -> )))
  0.00 ms    93     9 ->     9        │  │     └─ update-property(name)
                   67 ->    67        │  └─ fork("scopes from children of Core<Element>")
  0.00 ms    93    67 ->    67        │     ├─ class-scope
  0.00 ms    93    67 ->     6        │     ├─ implements(Core)
  0.00 ms    93     6 ->     6        │     ├─ register-entity(scope)
                    6 ->     6        │     └─ fork
  0.00 ms    93     6 ->     6        │        ├─ read-name
  0.00 ms    93     6 ->     6        │        ├─ edit-text((replace(\QDsl.\E -> )))
  0.00 ms    93     6 ->     6        │        └─ update-property(name)
                    0 ->     0        └─ fork("register functions")
  0.00 ms    93     0 ->     9           ├─ classes-of(scope)
  0.00 ms    93     9 ->     9           ├─ class-scope
                    9 ->     9           ├─ fork
  0.00 ms    93     9 ->   283           │  ├─ methods(inherited + abstractMethods)
  0.00 ms    93   283 ->   283           │  ├─ method-scope
  0.00 ms    93   283 ->   283           │  ├─ filter-visibility(public)
  0.00 ms   376   283 ->   283           │  ├─ register-entity(dsl)
                  283 ->   283           │  ├─ fork
  0.00 ms   376   283 ->   283           │  │  ├─ read-name
  0.00 ms   376   283 ->   283           │  │  └─ update-property(name)
                  283 ->   283           │  ├─ fork("register method owner")
  0.00 ms   376   283 ->     9           │  │  ├─ outer-class
  0.00 ms   376     9 ->     9           │  │  ├─ class-scope
                    9 ->     9           │  │  └─ fork
  0.00 ms   376     9 ->     9           │  │     ├─ read-name
  0.00 ms   376     9 ->     9           │  │     └─ update-property(outer, dsl)
                  283 ->   283           │  ├─ fork
  0.00 ms   376   283 ->   210           │  │  ├─ returns
  0.00 ms   376   210 ->   210           │  │  ├─ signature-scope
  0.00 ms   376   210 ->   210           │  │  ├─ filter-signature(^(.+\.|)Action<|${'$'})
                  210 ->   210           │  │  └─ fork
  0.00 ms   376   210 ->    61           │  │     ├─ filter-nth(1)
  0.00 ms   376    61 ->    61           │  │     ├─ signature-scope
                   61 ->    61           │  │     └─ fork
  0.00 ms   376    61 ->    61           │  │        ├─ filter-nth(0)
  0.00 ms   376    61 ->    61           │  │        ├─ signature-scope
                   61 ->    61           │  │        └─ fork
  0.00 ms   376    61 ->     1           │  │           ├─ explode-raw-type
  0.00 ms   376     1 ->     1           │  │           ├─ class-scope
  0.00 ms   376     1 ->     1           │  │           ├─ filter(ValueNode)
                    1 ->     1           │  │           └─ fork
  0.00 ms   376     1 ->     1           │  │              ├─ with-value(⚙ )
  0.00 ms   376     1 ->     1           │  │              └─ update-property(icon, dsl)
                  283 ->   283           │  └─ fork
  0.00 ms   376   283 ->   575           │     ├─ parameters(excludingReceiver)
  0.00 ms   376   575 ->   575           │     ├─ parameter-scope
                  575 ->   575           │     ├─ fork
  0.00 ms   376   575 ->   575           │     │  ├─ read-name
  0.00 ms   376   575 ->   575           │     │  ├─ edit-text((replace(.+ -> ${'$'}0)))
  0.00 ms   376   575 ->   575           │     │  └─ update-property(params, dsl)
                  575 ->   575           │     └─ fork
  0.00 ms   376   575 ->   575           │        ├─ read-type
  0.00 ms   376   575 ->   575           │        └─ update-property(param-types, dsl)
  0.00 ms   376     9 ->     9           └─ register-children(scope[fns], dsl)
"""

val SIFT_STATS = """
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
traced-elements                             1 284
traces                                      1 344
traces.p50                                      1
traces.p90                                      1
traces.p95                                      1
traces.p99                                      1
traces.max                                     61
traces.depth.p50                                3
traces.depth.p90                                4
traces.depth.p95                                5
traces.depth.p99                                6
traces.depth.max                                6
traces.flatten                              4 004
"""