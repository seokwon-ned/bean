package com.ned.bean

import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.Serializable as JavaSerializable
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

/**
 * A Parcelable class that internally holds a Bundle to manage data conveniently.
 *
 * Key Features:
 * 1.  Android Optimized: Implements Parcelable for efficient data transfer between components.
 * 2.  JSON Serializable: Can be serialized to/from a JSON string via its custom [BeanSerializer].
 * 3.  Flexible Storage: Automatically converts kotlinx.serialization-only objects into JSON strings
 *     to store them in the Bundle, enhancing data-handling flexibility.
 *
 * @see BeanSerializer for JSON serialization logic.
 */
@Serializable(with = BeanSerializer::class)
class Bean(
    val bundle: Bundle = Bundle(),
    var beanKey: String? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readBundle(Bundle::class.java.classLoader) ?: Bundle()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeBundle(bundle)
    }

    override fun describeContents(): Int = 0

    companion object {
        const val KTX_SERIALIZABLE_PREFIX = "ktx_serializable_json::"

        @JvmField
        val CREATOR = object : Parcelable.Creator<Bean> {
            override fun createFromParcel(parcel: Parcel): Bean = Bean(parcel)
            override fun newArray(size: Int): Array<Bean?> = arrayOfNulls(size)
        }

        /**
         * Builder를 사용하여 Bean을 생성합니다.
         */
        fun builder(): BeanBuilder = BeanBuilder()
    }

    /**
     * Bean Builder 클래스
     */
    class BeanBuilder {
        private val bundle = Bundle()
        private var beanKey: String? = null

        fun setBeanKey(key: String): BeanBuilder {
            this.beanKey = key
            return this
        }

        fun putInt(key: String, value: Int): BeanBuilder {
            bundle.putInt(key, value)
            return this
        }

        fun putLong(key: String, value: Long): BeanBuilder {
            bundle.putLong(key, value)
            return this
        }

        fun putFloat(key: String, value: Float): BeanBuilder {
            bundle.putFloat(key, value)
            return this
        }

        fun putDouble(key: String, value: Double): BeanBuilder {
            bundle.putDouble(key, value)
            return this
        }

        fun putBoolean(key: String, value: Boolean): BeanBuilder {
            bundle.putBoolean(key, value)
            return this
        }

        fun putString(key: String, value: String?): BeanBuilder {
            bundle.putString(key, value)
            return this
        }

        fun putBundle(key: String, value: Bundle): BeanBuilder {
            bundle.putBundle(key, value)
            return this
        }

        fun putParcelable(key: String, value: Parcelable): BeanBuilder {
            bundle.putParcelable(key, value)
            return this
        }

        /**
         * kotlinx.serialization 기반 객체를 JSON 문자열로 저장
         */
        fun <T : Any> putSerializableObject(key: String, value: T, clazz: KClass<T>): BeanBuilder {
            try {
                val jsonString = Json.encodeToString(kotlinx.serialization.serializer(clazz.createType()), value)
                bundle.putString(key, "${Bean.KTX_SERIALIZABLE_PREFIX}$jsonString")
            } catch (e: Exception) {
                bundle.putString(key, value.toString())
            }
            return this
        }

        fun build(): Bean {
            return Bean(bundle, beanKey)
        }
    }

    // --- put methods ---
    fun putInt(key: String, value: Int) = bundle.putInt(key, value)
    fun putLong(key: String, value: Long) = bundle.putLong(key, value)
    fun putFloat(key: String, value: Float) = bundle.putFloat(key, value)
    fun putDouble(key: String, value: Double) = bundle.putDouble(key, value)
    fun putBoolean(key: String, value: Boolean) = bundle.putBoolean(key, value)
    fun putString(key: String, value: String?) = bundle.putString(key, value)
    fun putBundle(key: String, value: Bundle) = bundle.putBundle(key, value)
    fun putParcelable(key: String, value: Parcelable) = bundle.putParcelable(key, value)

    // --- get methods ---
    fun getInt(key: String, default: Int = 0): Int = bundle.getInt(key, default)
    fun getLong(key: String, default: Long = 0L): Long = bundle.getLong(key, default)
    fun getFloat(key: String, default: Float = 0f): Float = bundle.getFloat(key, default)
    fun getDouble(key: String, default: Double = 0.0): Double = bundle.getDouble(key, default)
    fun getBoolean(key: String, default: Boolean = false): Boolean = bundle.getBoolean(key, default)
    fun getString(key: String, default: String? = null): String? = bundle.getString(key, default)
    fun getBundle(key: String): Bundle? = bundle.getBundle(key)
    fun <T : Parcelable> getParcelable(key: String, clazz: Class<T>): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) bundle.getParcelable(key, clazz)
        else @Suppress("DEPRECATION") bundle.getParcelable(key) as? T

    /**
     * kotlinx.serialization 기반 객체를 JSON에서 복원
     */
    fun <T : Any> getSerializableObject(key: String, clazz: KClass<T>): T? {
        val str = bundle.getString(key) ?: return null
        if (!str.startsWith(Bean.KTX_SERIALIZABLE_PREFIX)) return null
        val json = str.removePrefix(Bean.KTX_SERIALIZABLE_PREFIX)
        return try {
            Json.decodeFromString(kotlinx.serialization.serializer(clazz.createType()), json) as? T
        } catch (e: Exception) {
            null
        }
    }

    fun getKeys(): Set<String> {
        return bundle.keySet()
    }

    // --- Map 변환 및 복원 ---
    /**
     * Bundle의 내용을 타입 정보를 포함한 Map<String, Map<String, Any?>>로 변환
     */
    fun toMap(): Map<String, Map<String, Any?>> {
        val map = mutableMapOf<String, Map<String, Any?>>()
        getKeys().forEach { key ->
            val value = bundle.get(key)
            val type = when (value) {
                is Int -> "Int"
                is Long -> "Long"
                is Float -> "Float"
                is Double -> "Double"
                is Boolean -> "Boolean"
                is String -> "String"
                else -> "Other"
            }
            map[key] = mapOf("type" to type, "value" to value)
        }
        return map
    }

    /**
     * 범용 데이터 저장 메서드 (하위 호환용)
     * 타입별 put 메서드 사용을 권장합니다.
     */
    @Deprecated("타입별 put 메서드를 사용하세요.", ReplaceWith("putInt, putFloat, putDouble 등"))
    fun putData(key: String, value: Any?) {
        when (value) {
            null -> putString(key, null)
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Boolean -> putBoolean(key, value)
            is Float -> putFloat(key, value)
            is Double -> putDouble(key, value)
            is Bundle -> putBundle(key, value)
            is Parcelable -> putParcelable(key, value)
            is JavaSerializable -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    throw UnsupportedOperationException("API 33 이상에서는 Serializable 대신 Parcelable을 사용하세요.")
                }
                @Suppress("DEPRECATION")
                bundle.putSerializable(key, value)
            }
            else -> {
                // value가 null이 아닌 경우에만 putSerializableObject 호출
                if (value != null) {
                    try {
                        // 타입 정보를 알 수 없으므로 안전하게 toString()으로 fallback
                        putString(key, value.toString())
                    } catch (e: Exception) {
                        putString(key, value.toString())
                    }
                }
            }
        }
    }

    /**
     * 타입 정보를 포함한 Map<String, Map<String, Any?>>를 Bundle로 복원 (internal)
     */
    internal fun restoreFromMap(map: Map<String, Map<String, Any?>>) {
        bundle.clear()
        for ((key, entry) in map) {
            val type = entry["type"] as? String
            val value = entry["value"]
            when (type) {
                "Int" -> putInt(key, (value as Number).toInt())
                "Long" -> putLong(key, (value as Number).toLong())
                "Float" -> putFloat(key, (value as Number).toFloat())
                "Double" -> putDouble(key, (value as Number).toDouble())
                "Boolean" -> putBoolean(key, value as Boolean)
                "String" -> putString(key, value as? String)
                else -> putString(key, value?.toString())
            }
        }
    }

    /**
     * 두 Bean이 동일한지 비교합니다.
     * beanKey와 bundle의 내용을 모두 비교합니다.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Bean) return false
        
        // beanKey 비교
        if (beanKey != other.beanKey) return false
        
        // bundle의 키 개수 비교
        if (bundle.keySet().size != other.bundle.keySet().size) return false
        
        // bundle의 모든 키-값 쌍 비교
        for (key in bundle.keySet()) {
            if (!other.bundle.containsKey(key)) return false
            if (bundle.get(key) != other.bundle.get(key)) return false
        }
        
        return true
    }

    /**
     * Bean의 해시코드를 생성합니다.
     * beanKey와 bundle의 내용을 기반으로 계산됩니다.
     */
    override fun hashCode(): Int {
        var result = beanKey?.hashCode() ?: 0
        result = 31 * result + bundle.hashCode()
        return result
    }

    /**
     * 두 Bean을 비교합니다.
     * 먼저 beanKey를 비교하고, 같으면 bundle의 내용을 비교합니다.
     * 
     * @param other 비교할 다른 Bean
     * @return 음수: this < other, 0: this == other, 양수: this > other
     */
    fun compareTo(other: Bean): Int {
        // beanKey 비교
        val thisKey = beanKey ?: ""
        val otherKey = other.beanKey ?: ""
        val keyComparison = thisKey.compareTo(otherKey)
        if (keyComparison != 0) return keyComparison
        
        // beanKey가 같으면 bundle의 키 개수 비교
        val thisSize = bundle.keySet().size
        val otherSize = other.bundle.keySet().size
        if (thisSize != otherSize) return thisSize.compareTo(otherSize)
        
        // bundle의 키들을 정렬하여 순서대로 비교
        val thisKeys = bundle.keySet().sorted()
        val otherKeys = other.bundle.keySet().sorted()
        
        for (i in thisKeys.indices) {
            val keyComparison2 = thisKeys[i].compareTo(otherKeys[i])
            if (keyComparison2 != 0) return keyComparison2
            
            val thisValue = bundle.get(thisKeys[i])
            val otherValue = other.bundle.get(otherKeys[i])
            if (thisValue != otherValue) {
                // 값이 다르면 문자열로 변환하여 비교
                val thisStr = thisValue?.toString() ?: ""
                val otherStr = otherValue?.toString() ?: ""
                return thisStr.compareTo(otherStr)
            }
        }
        
        return 0
    }

    /**
     * Bean의 내용을 문자열로 표현합니다.
     */
    override fun toString(): String {
        return "Bean(beanKey='$beanKey', bundle=$bundle)"
    }
}