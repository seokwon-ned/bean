package com.ned.bean

import android.os.Bundle
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Custom serializer for the [Bean] class.
 *
 * This serializer converts the Bean's internal data to a `Map<String, JsonElement>`
 * for JSON serialization, and reverses the process for deserialization. It intelligently
 * handles primitive types and falls back to a string representation for complex types
 * that are not directly serializable by `kotlinx.serialization`.
 */
object BeanSerializer : KSerializer<Bean> {
    // We will serialize the Bean's content as a map of key-value pairs.
    private val mapSerializer = MapSerializer(String.serializer(), MapSerializer(String.serializer(), kotlinx.serialization.json.JsonElement.serializer()))

    override val descriptor: SerialDescriptor = mapSerializer.descriptor

    // Bean -> JSON
    override fun serialize(encoder: Encoder, value: Bean) {
        val jsonElementMap = mutableMapOf<String, Map<String, kotlinx.serialization.json.JsonElement>>()
        
        // beanKey를 먼저 추가
        value.beanKey?.let { key ->
            jsonElementMap["beanKey"] = mapOf(
                "type" to Json.encodeToJsonElement(String.serializer(), "String"),
                "value" to Json.encodeToJsonElement(String.serializer(), key)
            )
        }
        
        // 기존 데이터 추가
        val dataMap = value.toMap().mapValues { entry ->
            entry.value.mapValues { encodeValueToJsonElement(it.value) }
        }
        jsonElementMap.putAll(dataMap)
        
        encoder.encodeSerializableValue(mapSerializer, jsonElementMap)
    }

    // JSON -> Bean
    override fun deserialize(decoder: Decoder): Bean {
        val jsonElementMap = decoder.decodeSerializableValue(mapSerializer)
        val anyMap = jsonElementMap.mapValues { entry: Map.Entry<String, Map<String, JsonElement>> ->
            entry.value.mapValues { innerEntry: Map.Entry<String, JsonElement> -> decodeJsonElement(innerEntry.value) }
        }
        val beanKeyEntry = anyMap["beanKey"]
        val beanKeyValue = beanKeyEntry?.get("value") as? String
        val dataMap = anyMap.filterKeys { it != "beanKey" }
        val bean = Bean(Bundle(), beanKeyValue)
        bean.restoreFromMap(dataMap as Map<String, Map<String, Any?>>)
        return bean
    }

    /**
     * Encodes a value of `Any?` type to a `JsonElement`. This function handles
     * primitive types explicitly and falls back to string representation for complex types.
     */
    private fun encodeValueToJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is String -> Json.encodeToJsonElement(String.serializer(), value)
            is Int -> Json.encodeToJsonElement(Int.serializer(), value)
            is Long -> Json.encodeToJsonElement(Long.serializer(), value)
            is Boolean -> Json.encodeToJsonElement(Boolean.serializer(), value)
            is Float -> Json.encodeToJsonElement(Float.serializer(), value)
            is Double -> Json.encodeToJsonElement(Double.serializer(), value)
            else -> Json.encodeToJsonElement(String.serializer(), value.toString())
        }
    }

    /**
     * Decodes a `JsonElement` into a primitive Kotlin type or its string representation.
     * This is a simplified approach as we cannot reliably infer the original complex type
     * (e.g., a specific Parcelable class) from JSON alone.
     */
    private fun decodeJsonElement(jsonElement: JsonElement): Any? {
        if (jsonElement is JsonNull) return null
        if (jsonElement !is JsonPrimitive) return jsonElement.toString()
        if (jsonElement.isString) return jsonElement.content
        jsonElement.booleanOrNull?.let { return it }
        jsonElement.longOrNull?.let { longValue ->
            if (longValue in Int.MIN_VALUE..Int.MAX_VALUE) return longValue.toInt()
            return longValue
        }
        jsonElement.doubleOrNull?.let { return it }
        return jsonElement.content
    }
}