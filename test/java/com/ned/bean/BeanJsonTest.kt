package com.ned.bean

import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

@RunWith(RobolectricTestRunner::class)
class BeanJsonTest {

    private lateinit var bean: Bean
    private val json = Json { 
        prettyPrint = true 
        encodeDefaults = true
    }

    @Before
    fun setUp() {
        bean = Bean()
    }

    @Test
    fun testBasicTypesToJson() {
        // Given: 다양한 기본 타입의 데이터를 Bean에 저장
        bean.putInt("age", 25)
        bean.putString("name", "홍길동")
        bean.putBoolean("isStudent", true)
        bean.putFloat("height", 175.5f)
        bean.putDouble("weight", 70.2)
        bean.putLong("timestamp", 1640995200000L)

        // When: JSON으로 직렬화
        val jsonString = json.encodeToString(BeanSerializer, bean)
        
        // Then: JSON 출력 확인
        println("=== 기본 타입 JSON 출력 ===")
        println(jsonString)
        
        // JSON이 올바르게 생성되었는지 확인
        assertTrue(jsonString.contains("\"age\""))
        assertTrue(jsonString.contains("\"name\""))
        assertTrue(jsonString.contains("\"isStudent\""))
        assertTrue(jsonString.contains("25"))
        assertTrue(jsonString.contains("홍길동"))
        assertTrue(jsonString.contains("true"))
    }

    @Test
    fun testComplexTypesToJson() {
        // Given: 복잡한 타입의 데이터를 Bean에 저장
        val bundle = Bundle()
        bundle.putString("bundleKey", "bundleValue")
        bundle.putInt("bundleInt", 42)
        
        val testParcelable = TestParcelable("테스트", 100)
        
        bean.putBundle("userBundle", bundle)
        bean.putParcelable("userParcelable", testParcelable)
        bean.putString("description", "복잡한 객체 테스트")

        // When: JSON으로 직렬화
        val jsonString = json.encodeToString(BeanSerializer, bean)
        
        // Then: JSON 출력 확인
        println("=== 복잡한 타입 JSON 출력 ===")
        println(jsonString)
        
        // JSON이 올바르게 생성되었는지 확인
        assertTrue(jsonString.contains("\"userBundle\""))
        assertTrue(jsonString.contains("\"userParcelable\""))
        assertTrue(jsonString.contains("\"description\""))
    }

    @Test
    fun testNullValuesToJson() {
        // Given: null 값들을 Bean에 저장
        bean.putString("nullString", null)
        bean.putInt("nullInt", 0) // null 대신 0으로 저장됨
        bean.putBoolean("nullBoolean", false) // null 대신 false로 저장됨

        // When: JSON으로 직렬화
        val jsonString = json.encodeToString(BeanSerializer, bean)
        
        // Then: JSON 출력 확인
        println("=== null 값 JSON 출력 ===")
        println(jsonString)
        
        // null 값이 올바르게 처리되었는지 확인
        assertTrue(jsonString.contains("\"nullString\""))
    }

    @Test
    fun testJsonToBeanRoundTrip() {
        // Given: Bean에 데이터 저장
        bean.putInt("id", 123)
        bean.putString("title", "테스트 제목")
        bean.putBoolean("active", true)
        bean.putFloat("score", 95.5f)

        // When: JSON으로 직렬화 후 다시 역직렬화
        val jsonString = json.encodeToString(BeanSerializer, bean)
        val restoredBean = json.decodeFromString(BeanSerializer, jsonString)
        
        // Then: 원본과 복원된 데이터가 일치하는지 확인
        println("=== JSON 라운드트립 테스트 ===")
        println("원본 JSON: $jsonString")
        println("복원된 Bean의 id: ${restoredBean.getInt("id")}")
        println("복원된 Bean의 title: ${restoredBean.getString("title")}")
        println("복원된 Bean의 active: ${restoredBean.getBoolean("active")}")
        println("복원된 Bean의 score: ${restoredBean.getFloat("score")}")
        
        assertEquals(123, restoredBean.getInt("id"))
        assertEquals("테스트 제목", restoredBean.getString("title"))
        assertTrue(restoredBean.getBoolean("active"))
        assertEquals(95.5f, restoredBean.getFloat("score"), 0.001f)
    }

    @Test
    fun testMultipleBeansToJson() {
        // Given: 여러 개의 Bean 생성
        val userBean = Bean()
        userBean.putString("name", "김철수")
        userBean.putInt("age", 30)
        userBean.putString("email", "kim@example.com")
        
        val settingsBean = Bean()
        settingsBean.putBoolean("notifications", true)
        settingsBean.putString("language", "ko")
        settingsBean.putInt("theme", 1)
        
        val mainBean = Bean()
        mainBean.putString("title", "사용자 정보")
        mainBean.putInt("version", 1)
        
        // When: 각 Bean을 JSON으로 직렬화
        val userJson = json.encodeToString(BeanSerializer, userBean)
        val settingsJson = json.encodeToString(BeanSerializer, settingsBean)
        val mainJson = json.encodeToString(BeanSerializer, mainBean)
        
        // Then: JSON 출력 확인
        println("=== 여러 Bean JSON 출력 ===")
        println("사용자 Bean:")
        println(userJson)
        println("\n설정 Bean:")
        println(settingsJson)
        println("\n메인 Bean:")
        println(mainJson)
        
        // 각 JSON이 올바르게 생성되었는지 확인
        assertTrue(userJson.contains("\"name\""))
        assertTrue(userJson.contains("김철수"))
        assertTrue(settingsJson.contains("\"notifications\""))
        assertTrue(settingsJson.contains("true"))
    }

    @Test
    fun testBeanKeyInJson() {
        // Given: Bean에 beanKey와 데이터를 설정
        val bean = Bean.builder()
            .setBeanKey("user_profile")
            .putString("name", "김철수")
            .putInt("age", 30)
            .putString("email", "kim@example.com")
            .build()

        // When: JSON으로 직렬화
        val jsonString = json.encodeToString(BeanSerializer, bean)
        
        // Then: JSON 출력 확인
        println("=== BeanKey 포함 JSON 출력 ===")
        println(jsonString)
        
        // beanKey가 JSON에 포함되었는지 확인
        assertTrue(jsonString.contains("\"beanKey\""))
        assertTrue(jsonString.contains("user_profile"))
        assertTrue(jsonString.contains("\"name\""))
        assertTrue(jsonString.contains("김철수"))
        
        // JSON에서 다시 Bean으로 역직렬화
        val restoredBean = json.decodeFromString(BeanSerializer, jsonString)
        
        // beanKey가 올바르게 복원되었는지 확인
        assertEquals("user_profile", restoredBean.beanKey)
        assertEquals("김철수", restoredBean.getString("name"))
        assertEquals(30, restoredBean.getInt("age"))
        assertEquals("kim@example.com", restoredBean.getString("email"))
    }

    @Test
    fun testMultipleBeansWithKeys() {
        // Given: 여러 개의 Bean을 각각 다른 키로 생성
        val userBean = Bean.builder()
            .setBeanKey("user_001")
            .putString("name", "홍길동")
            .putInt("age", 30)
            .putString("email", "kim@example.com")
            .build()
        
        val settingsBean = Bean.builder()
            .setBeanKey("settings_001")
            .putBoolean("notifications", true)
            .putString("language", "ko")
            .putInt("theme", 1)
            .build()
        
        val mainBean = Bean.builder()
            .setBeanKey("main_001")
            .putString("title", "사용자 정보")
            .putInt("version", 1)
            .build()
        
        // When: 각 Bean을 JSON으로 직렬화
        val userJson = json.encodeToString(BeanSerializer, userBean)
        val settingsJson = json.encodeToString(BeanSerializer, settingsBean)
        val mainJson = json.encodeToString(BeanSerializer, mainBean)
        
        // Then: JSON 출력 확인
        println("=== 여러 Bean (키 포함) JSON 출력 ===")
        println("사용자 Bean:")
        println(userJson)
        println("\n설정 Bean:")
        println(settingsJson)
        println("\n메인 Bean:")
        println(mainJson)
        
        // 각 JSON에 beanKey가 포함되었는지 확인
        assertTrue(userJson.contains("\"beanKey\""))
        assertTrue(userJson.contains("user_001"))
        assertTrue(settingsJson.contains("\"beanKey\""))
        assertTrue(settingsJson.contains("settings_001"))
        assertTrue(mainJson.contains("\"beanKey\""))
        assertTrue(mainJson.contains("main_001"))
        
        // 역직렬화 테스트
        val restoredUser = json.decodeFromString(BeanSerializer, userJson)
        val restoredSettings = json.decodeFromString(BeanSerializer, settingsJson)
        val restoredMain = json.decodeFromString(BeanSerializer, mainJson)
        
        assertEquals("user_001", restoredUser.beanKey)
        assertEquals("settings_001", restoredSettings.beanKey)
        assertEquals("main_001", restoredMain.beanKey)
    }

    @Test
    fun testBeanComparison() {
        // Given: 여러 Bean 생성
        val bean1 = Bean.builder()
            .setBeanKey("user_001")
            .putString("name", "김철수")
            .putInt("age", 25)
            .build()
        
        val bean2 = Bean.builder()
            .setBeanKey("user_001")
            .putString("name", "김철수")
            .putInt("age", 25)
            .build()
        
        val bean3 = Bean.builder()
            .setBeanKey("user_002")
            .putString("name", "이영희")
            .putInt("age", 30)
            .build()
        
        val bean4 = Bean.builder()
            .setBeanKey("user_001")
            .putString("name", "김철수")
            .putInt("age", 26) // 나이가 다름
            .build()
        
        // When & Then: equals 테스트
        println("=== Bean 비교 테스트 ===")
        println("bean1: ${bean1}")
        println("bean2: ${bean2}")
        println("bean3: ${bean3}")
        println("bean4: ${bean4}")
        
        // 동일한 내용의 Bean은 같아야 함
        assertTrue(bean1 == bean2)
        assertTrue(bean1.equals(bean2))
        
        // 다른 beanKey는 다름
        assertFalse(bean1 == bean3)
        assertFalse(bean1.equals(bean3))
        
        // 같은 beanKey지만 내용이 다름
        assertFalse(bean1 == bean4)
        assertFalse(bean1.equals(bean4))
        
        // compareTo 테스트
        println("bean1.compareTo(bean2): ${bean1.compareTo(bean2)}") // 0 (같음)
        println("bean1.compareTo(bean3): ${bean1.compareTo(bean3)}") // 음수 (user_001 < user_002)
        println("bean3.compareTo(bean1): ${bean3.compareTo(bean1)}") // 양수 (user_002 > user_001)
        println("bean1.compareTo(bean4): ${bean1.compareTo(bean4)}") // 음수 (나이가 다름)
        
        assertEquals(0, bean1.compareTo(bean2))
        assertTrue(bean1.compareTo(bean3) < 0) // user_001 < user_002
        assertTrue(bean3.compareTo(bean1) > 0) // user_002 > user_001
        assertTrue(bean1.compareTo(bean4) != 0) // 내용이 다름
    }

    @Test
    fun testBeanSorting() {
        // Given: 정렬 테스트용 Bean들 생성
        val beans = mutableListOf<Bean>()
        
        val bean1 = Bean.builder()
            .setBeanKey("user_003")
            .putString("name", "박민수")
            .build()
        beans.add(bean1)
        
        val bean2 = Bean.builder()
            .setBeanKey("user_001")
            .putString("name", "김철수")
            .build()
        beans.add(bean2)
        
        val bean3 = Bean.builder()
            .setBeanKey("user_002")
            .putString("name", "이영희")
            .build()
        beans.add(bean3)
        
        // When: beanKey로 정렬
        beans.sortWith { a, b -> a.compareTo(b) }
        
        // Then: 정렬 결과 확인
        println("=== Bean 정렬 테스트 ===")
        println("정렬된 Bean들:")
        beans.forEachIndexed { index, bean ->
            println("${index + 1}. ${bean.beanKey}: ${bean.getString("name")}")
        }
        
        assertEquals("user_001", beans[0].beanKey)
        assertEquals("user_002", beans[1].beanKey)
        assertEquals("user_003", beans[2].beanKey)
    }

    @Test
    fun testBeanBuilderPattern() {
        // Given: Builder Pattern을 사용하여 Bean 생성
        val userBean = Bean.builder()
            .setBeanKey("user_001")
            .putString("name", "김철수")
            .putInt("age", 25)
            .putString("email", "kim@example.com")
            .putBoolean("isActive", true)
            .putFloat("height", 175.5f)
            .putDouble("weight", 70.2)
            .build()

        val settingsBean = Bean.builder()
            .setBeanKey("settings_001")
            .putBoolean("notifications", true)
            .putString("language", "ko")
            .putInt("theme", 1)
            .build()

        // When: JSON으로 직렬화
        val userJson = json.encodeToString(BeanSerializer, userBean)
        val settingsJson = json.encodeToString(BeanSerializer, settingsBean)
        
        // Then: JSON 출력 확인
        println("=== Builder Pattern Bean JSON 출력 ===")
        println("사용자 Bean:")
        println(userJson)
        println("\n설정 Bean:")
        println(settingsJson)
        
        // beanKey와 데이터가 올바르게 포함되었는지 확인
        assertTrue(userJson.contains("\"beanKey\""))
        assertTrue(userJson.contains("user_001"))
        assertTrue(userJson.contains("\"name\""))
        assertTrue(userJson.contains("김철수"))
        assertTrue(userJson.contains("\"age\""))
        assertTrue(userJson.contains("25"))
        
        assertTrue(settingsJson.contains("\"beanKey\""))
        assertTrue(settingsJson.contains("settings_001"))
        assertTrue(settingsJson.contains("\"notifications\""))
        assertTrue(settingsJson.contains("true"))
        
        // 역직렬화 테스트
        val restoredUser = json.decodeFromString(BeanSerializer, userJson)
        val restoredSettings = json.decodeFromString(BeanSerializer, settingsJson)
        
        assertEquals("user_001", restoredUser.beanKey)
        assertEquals("김철수", restoredUser.getString("name"))
        assertEquals(25, restoredUser.getInt("age"))
        assertEquals("kim@example.com", restoredUser.getString("email"))
        assertTrue(restoredUser.getBoolean("isActive"))
        assertEquals(175.5f, restoredUser.getFloat("height"), 0.001f)
        assertEquals(70.2, restoredUser.getDouble("weight"), 0.001)
        
        assertEquals("settings_001", restoredSettings.beanKey)
        assertTrue(restoredSettings.getBoolean("notifications"))
        assertEquals("ko", restoredSettings.getString("language"))
        assertEquals(1, restoredSettings.getInt("theme"))
    }

    @Test
    fun testBeanBuilderWithComplexObjects() {
        // Given: 복잡한 객체를 포함한 Bean 생성
        val bundle = Bundle()
        bundle.putString("bundleKey", "bundleValue")
        bundle.putInt("bundleInt", 42)
        
        val testParcelable = TestParcelable("테스트", 100)
        
        val complexBean = Bean.builder()
            .setBeanKey("complex_001")
            .putString("title", "복잡한 객체 테스트")
            .putBundle("userBundle", bundle)
            .putParcelable("userParcelable", testParcelable)
            .putSerializableObject("userData", mapOf("id" to 1, "name" to "테스트 사용자"), Map::class)
            .build()

        // When: JSON으로 직렬화
        val jsonString = json.encodeToString(BeanSerializer, complexBean)
        
        // Then: JSON 출력 확인
        println("=== 복잡한 객체 Builder Bean JSON 출력 ===")
        println(jsonString)
        
        // 복잡한 객체가 올바르게 포함되었는지 확인
        assertTrue(jsonString.contains("\"beanKey\""))
        assertTrue(jsonString.contains("complex_001"))
        assertTrue(jsonString.contains("\"title\""))
        assertTrue(jsonString.contains("복잡한 객체 테스트"))
        assertTrue(jsonString.contains("\"userBundle\""))
        assertTrue(jsonString.contains("\"userParcelable\""))
    }

    // 테스트용 Parcelable 클래스
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
} 