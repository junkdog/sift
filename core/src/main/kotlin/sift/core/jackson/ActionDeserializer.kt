package sift.core.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import sift.core.api.Action

class ActionDeserializer : JsonSerializer<Action<*, *>>() {
    override fun serialize(
        value: Action<*, *>,
        gen: JsonGenerator,
        serializers: SerializerProvider
    ) {
        when (value) {
            is Action.Instrumenter.ClassesOf -> TODO()
            is Action.Instrumenter.InstrumentClasses -> TODO()
            is Action.Instrumenter.InstrumenterScope -> TODO()
            is Action.Instrumenter.MethodsOf -> TODO()

            is Action.Class.ClassScope -> TODO()
            is Action.Class.Filter -> TODO()
            is Action.Class.FilterImplemented -> TODO()
            is Action.Class.IntoFields -> TODO()
            is Action.Class.IntoMethods -> TODO()
            is Action.Class.ReadName -> TODO()
            is Action.Class.ReadType -> TODO()
            is Action.Class.ToInstrumenterScope -> TODO()

            is Action.Field.Filter -> TODO()
            is Action.Field.IntoParents -> TODO()
            is Action.Field.FieldScope -> TODO()

            is Action.Method.DeclaredMethods -> TODO()
            is Action.Method.Filter -> TODO()
            is Action.Method.Instantiations -> TODO()
            is Action.Method.IntoParameters -> TODO()
            is Action.Method.IntoParents -> TODO()
            is Action.Method.InvocationsOf -> TODO()
            is Action.Method.Invokes -> TODO()
            is Action.Method.MethodScope -> TODO()

            is Action.Parameter.ExplodeType -> TODO()
            is Action.Parameter.Filter -> TODO()
            is Action.Parameter.FilterNth -> TODO()
            is Action.Parameter.IntoParents -> TODO()
            is Action.Parameter.ParameterScope -> TODO()
            is Action.Parameter.ReadType -> TODO()

            is Action.Compose<*, *, *> -> TODO()
            is Action.Chain -> TODO()
            is Action.DebugLog<*> -> TODO()
            is Action.EntityFilter<*> -> TODO()
            is Action.Fork<*, *> -> TODO()
            is Action.ForkOnEntityExistence<*, *> -> TODO()
            is Action.HasAnnotation<*> -> TODO()
            is Action.ReadAnnotation<*> -> TODO()
            is Action.RegisterChildren<*> -> TODO()
            is Action.RegisterChildrenFromResolver -> TODO()
            is Action.RegisterSynthesizedEntity -> TODO()
            is Action.SimpleAction<*> -> TODO()
            is Action.UpdateEntityProperty -> TODO()
            is Action.WithValue<*> -> TODO()
        }
    }

}
