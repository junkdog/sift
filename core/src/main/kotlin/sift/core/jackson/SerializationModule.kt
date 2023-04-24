package sift.core.jackson

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.addDeserializer
import com.fasterxml.jackson.module.kotlin.addSerializer
import sift.core.api.Action
import sift.core.dsl.Type
import sift.core.entity.Entity

fun serializationModule() = SimpleModule()
    .addSerializer(Entity.Type::class, EntityTypeSerializer.Serializer())
    .addDeserializer(Entity.Type::class, EntityTypeSerializer.Deserializer())
    .addSerializer(Regex::class, RegexSerializer.Serializer())
    .addDeserializer(Regex::class, RegexSerializer.Deserializer())
    .addSerializer(Type::class, SiftTypeSerializer.Serializer())
    .addDeserializer(Type::class, SiftTypeSerializer.Deserializer())
    .addSerializer(Action.WithValue::class, WithValueSerializer.Serializer())
    .addDeserializer(Action.WithValue::class, WithValueSerializer.Deserializer())
