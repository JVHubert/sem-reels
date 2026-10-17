package io.github.jvhubert.semreels

import org.junit.Assert.assertEquals
import org.junit.Test

class LeitorJsonTest {
    @Test
    fun leObjetosAninhados() {
        val v = LeitorJson.ler("""{"id":"clips_tab","s":1,"b":[0,-10,2.5],"n":[{"d":null,"ok":true}]}""")
        assertEquals(
            mapOf("id" to "clips_tab", "s" to 1L, "b" to listOf(0L, -10L, 2.5), "n" to listOf(mapOf("d" to null, "ok" to true))),
            v,
        )
    }

    @Test
    fun idaEVoltaComOEscritor() {
        val original = "a\"b\\c\nd\u0001 ~é"
        assertEquals(original, LeitorJson.ler(Json.texto(original)))
    }
}
