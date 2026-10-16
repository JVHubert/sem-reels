package io.github.jvhubert.semreels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HigieneTest {
    @Test
    fun palavrasDaInterfacePassam() {
        assertEquals("pesquisar e explorar", Higiene.texto("Pesquisar e explorar"))
        assertEquals("reels", Higiene.texto("Reels"))
        assertEquals("9 curtidas", Higiene.texto("1.234 curtidas"))
        assertEquals("9 mil seguidores", Higiene.texto("12,5 mil seguidores"))
    }

    @Test
    fun nomesEMensagensNaoPassam() {
        val casos = listOf(
            "Reel de joao.silva" to "joao",
            "Maria Eduarda enviou um reel" to "maria",
            "Bora no churrasco sábado?" to "churrasco",
            "@pedrohenrique" to "pedrohenrique",
        )
        for ((entrada, proibido) in casos) {
            val saida = Higiene.texto(entrada)!!
            assertFalse("vazou \"$proibido\" em: $saida", saida.contains(proibido, ignoreCase = true))
        }
        assertTrue(Higiene.texto("Reel de joao.silva")!!.startsWith("reel de ~"))
        assertTrue(Higiene.texto("@pedrohenrique")!!.startsWith("@~"))
        assertTrue(Higiene.texto("#futebol")!!.startsWith("#~"))
    }

    @Test
    fun mesmaPalavraMesmoCodigo() {
        assertEquals(Higiene.texto("Fulano"), Higiene.texto("fulano!"))
    }

    @Test
    fun vazioViraNull() {
        assertNull(Higiene.texto(null))
        assertNull(Higiene.texto("   "))
    }

    @Test
    fun textoLongoECortado() {
        val longo = (1..30).joinToString(" ") { "reels" }
        assertTrue(Higiene.texto(longo)!!.endsWith(" …"))
    }
}
