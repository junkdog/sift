package sift.core.jackson

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.addDeserializer
import com.fasterxml.jackson.module.kotlin.addSerializer
import org.objectweb.asm.Type
import sift.core.api.Action
import sift.core.entity.Entity

fun serializationModule() = SimpleModule()
    .addSerializer(Type::class, AsmTypeSerializer.Serializer())
    .addDeserializer(Type::class, AsmTypeSerializer.Deserializer())
    .addSerializer(Entity.Type::class, EntityTypeSerializer.Serializer())
    .addDeserializer(Entity.Type::class, EntityTypeSerializer.Deserializer())
    .addSerializer(Regex::class, RegexSerializer.Serializer())
    .addDeserializer(Regex::class, RegexSerializer.Deserializer())
    .addSerializer(Action.WithValue::class, WithValueSerializer.Serializer())
    .addDeserializer(Action.WithValue::class, WithValueSerializer.Deserializer())