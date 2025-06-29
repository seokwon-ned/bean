package com.ned.bean

import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class BeanTest {
    private lateinit var bean: Bean

    @Before
    fun setUp() {
        bean = Bean()
    }

    @Test
    fun testPutGetBasicTypes() {
        bean.putInt("intKey", 42)
        bean.putLong("longKey", 123456789L)
        bean.putFloat("floatKey", 3.14f)
        bean.putDouble("doubleKey", 2.718)
        bean.putBoolean("boolKey", true)
        bean.putString("stringKey", "test")

        assertEquals(42, bean.getInt("intKey"))
        assertEquals(123456789L, bean.getLong("longKey"))
        assertEquals(3.14f, bean.getFloat("floatKey"), 0.001f)
        assertEquals(2.718, bean.getDouble("doubleKey"), 0.001)
        assertTrue(bean.getBoolean("boolKey"))
        assertEquals("test", bean.getString("stringKey"))
    }

    @Test
    fun testDefaultValues() {
        assertEquals(0, bean.getInt("noKey"))
        assertEquals(0L, bean.getLong("noKey"))
        assertEquals(0f, bean.getFloat("noKey"), 0.001f)
        assertEquals(0.0, bean.getDouble("noKey"), 0.001)
        assertFalse(bean.getBoolean("noKey"))
        assertNull(bean.getString("noKey"))
    }

    @Test
    fun testCustomDefaultValues() {
        assertEquals(100, bean.getInt("noKey", 100))
        assertEquals(999L, bean.getLong("noKey", 999L))
        assertEquals(5.5f, bean.getFloat("noKey", 5.5f), 0.001f)
        assertEquals(7.7, bean.getDouble("noKey", 7.7), 0.001)
        assertTrue(bean.getBoolean("noKey", true))
        assertEquals("default", bean.getString("noKey", "default"))
    }

    @Test
    fun testBundlePutGet() {
        val testBundle = Bundle()
        testBundle.putString("bundleKey", "bundleValue")
        bean.putBundle("bundleKey", testBundle)
        val retrieved = bean.getBundle("bundleKey")
        assertNotNull(retrieved)
        assertEquals("bundleValue", retrieved?.getString("bundleKey"))
    }

    @Test
    fun testParcelablePutGet() {
        val testParcelable = TestParcelable("test", 42)
        bean.putParcelable("parcelableKey", testParcelable)
        val retrieved = bean.getParcelable("parcelableKey", TestParcelable::class.java)
        assertNotNull(retrieved)
        assertEquals("test", retrieved?.name)
        assertEquals(42, retrieved?.value)
    }

    @Test
    fun testSerializableObjectPutGet() {
        val testUser = TestUser(1, "John", "john@example.com", true)
        val bean = Bean.builder()
            .putSerializableObject("userKey", testUser, TestUser::class)
            .build()
        val retrieved = bean.getSerializableObject<TestUser>("userKey", TestUser::class)
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.id)
        assertEquals("John", retrieved?.name)
        assertEquals("john@example.com", retrieved?.email)
        assertTrue(retrieved?.isActive == true)
    }

    @Test
    fun testNullValueHandling() {
        bean.putString("nullKey", null)
        assertNull(bean.getString("nullKey"))
    }

    @Test
    fun testDescribeContents() {
        assertEquals(0, bean.describeContents())
    }

    // 테스트용 Parcelable
    data class TestParcelable(val name: String, val value: Int) : Parcelable {
        constructor(parcel: Parcel) : this(parcel.readString() ?: "", parcel.readInt())
        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
            parcel.writeInt(value)
        }
        override fun describeContents(): Int = 0
        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<TestParcelable> {
                override fun createFromParcel(parcel: Parcel): TestParcelable = TestParcelable(parcel)
                override fun newArray(size: Int): Array<TestParcelable?> = arrayOfNulls(size)
            }
        }
    }

    // 테스트용 직렬화 클래스
    @Serializable
    data class TestUser(val id: Int, val name: String, val email: String, val isActive: Boolean)
} 