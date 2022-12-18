package sift.core.api

import sift.core.element.*

typealias IterClasses    = Iterable<ClassNode>
typealias IterSignatures = Iterable<SignatureNode>
typealias IterMethods    = Iterable<MethodNode>
typealias IterFields     = Iterable<FieldNode>
typealias IterParameters = Iterable<ParameterNode>
typealias IterValues     = Iterable<ValueNode>
typealias Iter<T>        = Iterable<T>

typealias IsoAction<T>   = Action<Iter<T>, Iter<T>>

typealias AsmParameterNode = org.objectweb.asm.tree.ParameterNode
