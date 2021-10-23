package kotlinx.serialization.hocon

import com.typesafe.config.ConfigFactory
import kotlinx.serialization.*
import org.junit.Assert.*
import org.junit.Test

class HoconPolymorphismTest {
    @Serializable
    sealed class Sealed(val intField: Int) {
        @Serializable
        @SerialName("object")
        object ObjectChild : Sealed(0)

        @Serializable
        @SerialName("data_class")
        data class DataClassChild(val name: String) : Sealed(1)

        @Serializable
        @SerialName("type_child")
        data class TypeChild(val type: String) : Sealed(2)

        @Serializable
        @SerialName("annotated_type_child")
        data class AnnotatedTypeChild(@SerialName("my_type") val type: String) : Sealed(3)
    }

    @Serializable
    data class CompositeClass(var sealed: Sealed)


    private val arrayHocon = Hocon {
        useArrayPolymorphism = true
    }

    private val objectHocon = Hocon {
        useArrayPolymorphism = false
    }


    @Test
    fun testArrayDataClassDecode() {
        val config = ConfigFactory.parseString(
                """{
                sealed: [
                  "data_class"
                  {name="testArrayDataClass"
                   intField=10}
                ]
                }""")
        val root = arrayHocon.decodeFromConfig(CompositeClass.serializer(), config)
        val sealed = root.sealed

        assertTrue(sealed is Sealed.DataClassChild)
        sealed as Sealed.DataClassChild
        assertEquals("testArrayDataClass", sealed.name)
        assertEquals(10, sealed.intField)
    }

    @Test
    fun testArrayObjectDecode() {
        val config = ConfigFactory.parseString(
                """{
                sealed: [
                  "object"
                  {}
                ]
                }""")
        val root = arrayHocon.decodeFromConfig(CompositeClass.serializer(), config)
        val sealed = root.sealed

        assertSame(Sealed.ObjectChild, sealed)
    }

    @Test
    fun testObjectDecode() {
        val config = ConfigFactory.parseString("""{type="object"}""")
        val sealed = objectHocon.decodeFromConfig(Sealed.serializer(), config)

        assertSame(Sealed.ObjectChild, sealed)
    }

    @Test
    fun testNestedDataClassDecode() {
        val config = ConfigFactory.parseString(
                """{
                sealed: {
                  type="data_class"
                  name="test name"
                  intField=10
                }
                }""")
        val root = objectHocon.decodeFromConfig(CompositeClass.serializer(), config)
        val sealed = root.sealed

        assertTrue(sealed is Sealed.DataClassChild)
        sealed as Sealed.DataClassChild
        assertEquals("test name", sealed.name)
        assertEquals(10, sealed.intField)
    }

    @Test
    fun testDataClassDecode() {
        val config = ConfigFactory.parseString(
                """{
                  type="data_class"
                  name="testDataClass"
                  intField=10
                }""")
        val sealed = objectHocon.decodeFromConfig(Sealed.serializer(), config)

        assertTrue(sealed is Sealed.DataClassChild)
        sealed as Sealed.DataClassChild
        assertEquals("testDataClass", sealed.name)
        assertEquals(10, sealed.intField)
    }

    @Test
    fun testDecodeChangedDiscriminator() {
        val hocon = Hocon(objectHocon) {
            classDiscriminator = "key"
        }

        val config = ConfigFactory.parseString(
                """{
                  type="override"
                  key="type_child"
                  intField=11
                }""")
        val sealed = hocon.decodeFromConfig(Sealed.serializer(), config)

        assertTrue(sealed is Sealed.TypeChild)
        sealed as Sealed.TypeChild
        assertEquals("override", sealed.type)
        assertEquals(11, sealed.intField)
    }

    @Test
    fun testDecodeChangedTypePropertyName() {
        val config = ConfigFactory.parseString(
                """{
                  my_type="override"
                  type="annotated_type_child"
                  intField=12
                }""")
        val sealed = objectHocon.decodeFromConfig(Sealed.serializer(), config)

        assertTrue(sealed is Sealed.AnnotatedTypeChild)
        sealed as Sealed.AnnotatedTypeChild
        assertEquals("override", sealed.type)
        assertEquals(12, sealed.intField)
    }

    @Test
    fun testArrayObjectEncode() {
        val obj = CompositeClass(Sealed.ObjectChild)
        val config = arrayHocon.encodeToConfig(obj)

        assertConfigEquals("sealed = [ object, {} ]", config)
    }

    @Test
    fun testArrayDataClassEncode() {
        val obj = CompositeClass(Sealed.DataClassChild("testDataClass"))
        val config = arrayHocon.encodeToConfig(obj)

        assertConfigEquals("sealed = [ data_class, { name = testDataClass, intField = 1 } ]", config)
    }

    @Test
    fun testObjectEncode() {
        val obj = Sealed.ObjectChild
        val config = objectHocon.encodeToConfig(Sealed.serializer(), obj)

        assertConfigEquals("type = object", config)
    }

    @Test
    fun testDataClassEncode() {
        val obj = Sealed.DataClassChild("testDataClass")
        val config = objectHocon.encodeToConfig(Sealed.serializer(), obj)

        assertConfigEquals("type = data_class, name = testDataClass, intField = 1", config)
    }

    @Test
    fun testEncodeChangedDiscriminator() {
        val hocon = Hocon(objectHocon) {
            classDiscriminator = "key"
        }

        val obj = Sealed.TypeChild(type = "override")
        val config = hocon.encodeToConfig(Sealed.serializer(), obj)

        assertConfigEquals("type = override, key = type_child, intField = 2", config)
    }

    @Test
    fun testEncodeChangedTypePropertyName() {
        val obj = Sealed.AnnotatedTypeChild(type = "override")
        val config = objectHocon.encodeToConfig(Sealed.serializer(), obj)

        assertConfigEquals("type = annotated_type_child, my_type = override, intField = 3", config)
    }
}
