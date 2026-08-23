package com.group1.dartbud

import com.google.firebase.firestore.PropertyName
import com.group1.dartbud.data.FirestoreGame
import com.group1.dartbud.data.FirestorePlayerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Firestore serialiserer med getterne og deserialiserer (toObject) ved å skrive
 * rett til backing-feltene. Feltnavnet mapperen leter etter utledes fra getteren,
 * så en Boolean som heter isPrimaryProfile skrives som "primaryProfile", mens
 * backing-feltet heter isPrimaryProfile - da finner mapperen ingenting å skrive
 * til, logger "No setter/field for primaryProfile found" og lar verdien være
 * default. Testene her reproduserer den navnematchingen mapperen gjør, slik at
 * et felt som ikke kan leses tilbake feiler her i stedet for stille i produksjon.
 */
class FirestoreMappingTest {

    @Test
    fun `alle felt i FirestorePlayerProfile kan leses tilbake fra Firestore`() {
        assertAllPropertiesReadable(FirestorePlayerProfile::class.java)
    }

    @Test
    fun `alle felt i FirestoreGame kan leses tilbake fra Firestore`() {
        assertAllPropertiesReadable(FirestoreGame::class.java)
    }

    // Dokumenter som allerede ligger i Firestore ble skrevet med nøkkelen
    // "primaryProfile". Endres navnet på nøkkelen mister eksisterende brukere
    // primærprofil-flagget sitt ved neste synk.
    @Test
    fun `primaerprofil lagres fortsatt under noekkelen primaryProfile`() {
        val getter = FirestorePlayerProfile::class.java.declaredMethods
            .single { it.name == "isPrimaryProfile" || it.name == "getPrimaryProfile" }
        assertEquals("primaryProfile", propertyName(getter))
    }

    private fun assertAllPropertiesReadable(clazz: Class<*>) {
        val fieldNames = clazz.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .map { propertyName(it) }
            .toSet()

        clazz.declaredMethods
            .filter { isGetter(it) }
            .forEach { getter ->
                val property = propertyName(getter)
                assertTrue(
                    "Firestore skriver '$property' (fra ${getter.name}()), men " +
                        "${clazz.simpleName} har ingen felt med det navnet - verdien " +
                        "går tapt ved toObject. Felt: $fieldNames",
                    fieldNames.contains(property)
                )
            }
    }

    private fun isGetter(method: Method): Boolean {
        if (method.isSynthetic || method.isBridge) return false
        if (!Modifier.isPublic(method.modifiers) || Modifier.isStatic(method.modifiers)) return false
        if (method.parameterTypes.isNotEmpty()) return false
        if (method.name == "getClass" || method.name == "hashCode" || method.name == "toString") return false
        return method.name.startsWith("get") || method.name.startsWith("is")
    }

    private fun propertyName(field: Field): String =
        field.getAnnotation(PropertyName::class.java)?.value ?: field.name

    private fun propertyName(method: Method): String =
        method.getAnnotation(PropertyName::class.java)?.value ?: serializedName(method.name)

    // Samme regel som CustomClassMapper: fjern get/set/is-prefikset og gjør den
    // ledende sekvensen av store bokstaver om til små (URL -> url, Id -> id).
    private fun serializedName(methodName: String): String {
        val prefix = listOf("get", "set", "is").firstOrNull { methodName.startsWith(it) }
            ?: return methodName
        val stripped = methodName.substring(prefix.length).toCharArray()
        var i = 0
        while (i < stripped.size && Character.isUpperCase(stripped[i])) {
            stripped[i] = Character.toLowerCase(stripped[i])
            i++
        }
        return String(stripped)
    }
}
