package io.github.jvhubert.semreels

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonTest {
    @Test
    fun escreveObjetosAninhados() {
        val v = linkedMapOf(
            "id" to "clips_tab",
            "s" to 1,
            "b" to listOf(0, 10, 20, 30),
            "n" to listOf(mapOf("d" to null)),
        )
        assertEquals("""{"id":"clips_tab","s":1,"b":[0,10,20,30],"n":[{"d":null}]}""", Json.texto(v))
    }

    @Test
    fun escapaAspasEQuebras() {
        assertEquals("\"a\\\"b\\\\c\\nd\\u0001\"", Json.texto("a\"b\\c\nd\u0001"))
    }
}
