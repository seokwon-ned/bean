import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * A Parcelable class that internally holds a Bundle to manage data conveniently.
 * It's optimized for Android component data passing (e.g., Intents, Fragment arguments).
 * It can also be serialized to/from a JSON string using kotlinx.serialization.
 *
 * Use `putData()` to store values and `getData<T>()` to safely retrieve them with automatic type casting.
 */
@Parcelize
@Serializable(with = BeanSerializer::class)
class Bean(
    // Make bundle a property in the primary constructor to be included in Parcelable implementation.
    // Use a backing field to ensure it's never null.
    @JvmField // For Parcelize to work correctly with a custom getter/setter or backing field
    val bundle: Bundle = Bundle()
) : Parcelable {

    /**
     * Stores a value in the Bundle with the specified key.
     * Calls the appropriate Bundle.putXxx() method based on the value's type.
     *
     * @param key The key for the data to be stored.
     * @param value The data to store. Supports standard primitives, Parcelable, Serializable, etc.
     */
    fun putData(key: String, value: Any?) {
        when (value) {
            null -> bundle.putString(key, null) // Explicitly handle null
            is String -> bundle.putString(key, value)
            is Int -> bundle.putInt(key, value)
            is Long -> bundle.putLong(key, value)
            is Boolean -> bundle.putBoolean(key, value)
            is Float -> bundle.putFloat(key, value)
            is Double -> bundle.putDouble(key, value)
            is Bundle -> bundle.putBundle(key, value)
            is Parcelable -> bundle.putParcelable(key, value) // *** Added Parcelable support ***
            is Serializable -> bundle.putSerializable(key, value) // Serializable is still supported
            else -> throw IllegalArgumentException("The value type for key '$key' (${value::class.java.name}) is not supported by Bundle.")
        }
    }

    /**
     * Returns the value corresponding to the specified key, cast to the requested type <T>.
     * Returns null if the type does not match or the key does not exist, preventing ClassCastException.
     * For Parcelable and Serializable on API 33+, this uses the new type-safe getters.
     *
     * @param T The type of data to be returned.
     * @param key The key of the data to retrieve.
     * @return The type-casted value, or null.
     */
    inline fun <reified T> getData(key: String): T? {
        // Use type-safe getters for Parcelable and Serializable on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            when (T::class.java) {
                // Since T is reified, we can access its Java class
                Parcelable::class.java, Serializable::class.java -> {
                    return bundle.get(key, T::class.java)
                }
            }
        }
        
        // Fallback for older Android versions or other types
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        return bundle.get(key) as? T
    }

    /**
     * A convenience method for getData. If the key does not exist or the type does not match,
     * it returns the provided defaultValue.
     */
    inline fun <reified T> getData(key: String, defaultValue: T): T {
        return getData<T>(key) ?: defaultValue
    }

    /**
     * Converts all contents of the Bundle to a Map.
     */
    fun toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        bundle.keySet().forEach { key ->
            map[key] = bundle.get(key)
        }
        return map
    }

    /**
     * Populates the Bundle from a Map.
     */
    fun fromMap(map: Map<String, Any?>) {
        bundle.clear()
        for ((key, value) in map.entries) {
            putData(key, value)
        }
    }
}

// The BeanSerializer remains largely the same as it operates on the Map representation,
// not directly on the Parcelable implementation.
object BeanSerializer : KSerializer<Bean> {
    private val mapSerializer = MapSerializer(String.serializer(), JsonElement.serializer())

    override val descriptor: SerialDescriptor = mapSerializer.descriptor

    // Bean -> JSON
    override fun serialize(encoder: Encoder, value: Bean) {
        val jsonElementMap = value.toMap().mapValues { entry ->
            try {
                // For custom objects, they must be @Serializable or have a registered serializer.
                // We use a generic serializer which works for primitives and serializable classes.
                Json.encodeToJsonElement(serializer(entry.value?.javaClass ?: Any::class.java), entry.value)
            } catch (e: Exception) {
                // If a type is not serializable (e.g., non-serializable custom object),
                // serialize it as a string.
                Json.encodeToJsonElement(entry.value.toString())
            }
        }
        encoder.encodeSerializableValue(mapSerializer, jsonElementMap)
    }

    // JSON -> Bean
    override fun deserialize(decoder: Decoder): Bean {
        val jsonElementMap = decoder.decodeSerializableValue(mapSerializer)
        val anyMap = jsonElementMap.mapValues { (_, jsonElement) ->
            decodeJsonElement(jsonElement)
        }
        val bean = Bean()
        bean.fromMap(anyMap)
        return bean
    }
    
    private fun decodeJsonElement(jsonElement: JsonElement): Any? {
        if (jsonElement !is JsonPrimitive) {
            // For complex objects (JsonObjects/JsonArrays), we can't reliably infer the original
            // Parcelable/Serializable type, so we return it as a Map/List.
            // A more robust solution would require type metadata in the JSON.
            return try {
                Json.decodeFromJsonElement<Map<String, JsonElement>>(jsonElement).mapValues { decodeJsonElement(it.value) }
            } catch (e: Exception) {
                try {
                    Json.decodeFromJsonElement<List<JsonElement>>(jsonElement).map { decodeJsonElement(it) }
                } catch (e2: Exception) {
                    jsonElement.toString()
                }
            }
        }
        if (jsonElement.isString) return jsonElement.content
        jsonElement.booleanOrNull?.let { return it }
        jsonElement.longOrNull?.let { return it }
        jsonElement.doubleOrNull?.let { return it }
        return jsonElement.content
    }
}
