package sift.core.api

typealias IterClasses    = Iterable<Element.Class>
typealias IterSignatures = Iterable<Element.Signature>
typealias IterMethods    = Iterable<Element.Method>
typealias IterFields     = Iterable<Element.Field>
typealias IterParameters = Iterable<Element.Parameter>
typealias IterValues     = Iterable<Element.Value>
typealias Iter<T>        = Iterable<T>

typealias IsoAction<T>   = Action<Iter<T>, Iter<T>>

typealias AsmParameterNode = org.objectweb.asm.tree.ParameterNode
