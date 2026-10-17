package io.github.jvhubert.semreels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Repassa as gravações reais do celular do JV (reduzidas por ferramentas/reduzir_gravacao.py:
 * só ids e posições, sem texto) pelo [Decisor] e confere o que ele faria em cada situação.
 */
class DecisorTest {
    private val regras = Regras.ler(File("src/main/assets/regras.json").readText())

    private data class Passo(
        val t: Long,
        val foto: Boolean,
        val acao: Acao,
        val capa: Caixa?,
        val origem: Lugar,
        val liberado: Boolean,
        val visualizador: Boolean,
    )

    /** Cada situação gravada começa com um Decisor novo, como se o serviço acabasse de ligar. */
    private fun reproduzir(arquivo: String): Map<String, List<Passo>> {
        val linhas = javaClass.classLoader!!.getResource("capturas/$arquivo")!!.readText()
            .lines().filter { it.isNotBlank() }.map { LeitorJson.ler(it) as Map<*, *> }
        val tamanho = linhas.first()["tela"] as List<*>
        val largura = (tamanho[0] as Long).toInt()
        val altura = (tamanho[1] as Long).toInt()

        val saida = LinkedHashMap<String, MutableList<Passo>>()
        var decisor = Decisor(regras)
        for (d in linhas.drop(1)) {
            val cen = d["cen"] as String
            if (cen !in saida) decisor = Decisor(regras)
            val passos = saida.getOrPut(cen) { ArrayList() }
            val t = d["t"] as Long
            if ("foto" in d) {
                val tela = Tela(largura, altura)
                for (n in d["nos"] as List<*>) {
                    val (id, v, s, b) = n as List<*>
                    val c = (b as List<*>).map { (it as Long).toInt() }
                    tela.anotar(id as String, v == 1L, s == 1L, Caixa(c[0], c[1], c[2], c[3]))
                }
                val r = decisor.olhar(tela, t)
                passos += Passo(t, true, r.acao, r.capa, decisor.origem, decisor.reelLiberado, tela.algumVisivel(regras.visualizador))
            } else if (d["ev"] == "rolou" && d["id"] == regras.paginador) {
                val a = decisor.paginaDoVisualizador((d["de"] as Long).toInt(), t)
                passos += Passo(t, false, a, null, decisor.origem, decisor.reelLiberado, true)
            }
        }
        return saida
    }

    private val gravacao by lazy { reproduzir("2026-10-16_instagram-439.jsonl") }
    private fun voltas(cen: String) = gravacao.getValue(cen).filter { it.acao == Acao.VOLTAR }

    @Test
    fun feedNaoMexe() {
        val passos = gravacao.getValue("feed")
        assertTrue(passos.all { it.acao == Acao.NADA && it.capa == null })
    }

    @Test
    fun lupinhaCobreAGradeMasDeixaABuscaLivre() {
        val capas = gravacao.getValue("lupinha").mapNotNull { it.capa }
        assertTrue("a grade da lupinha deveria ficar coberta", capas.isNotEmpty())
        // Abaixo da barra de busca (que termina em 261) e acima da barra de abas (que começa em 2163).
        assertTrue(capas.contains(Caixa(0, 261, 1080, 2163)))
        assertTrue(capas.all { it.topo >= 103 && it.base <= 2163 })
    }

    @Test
    fun reelAbertoNaLupinhaVolta() {
        val passos = gravacao.getValue("lupinha")
        val abriu = passos.first { it.visualizador }
        assertEquals(Acao.VOLTAR, abriu.acao)
        assertFalse(abriu.liberado)
    }

    @Test
    fun previaDoToqueLongoFecha() {
        assertTrue(voltas("previa_lupinha").isNotEmpty())
        assertTrue(voltas("lupinha").size >= 2) // a prévia e o Reel aberto pela grade
    }

    @Test
    fun digitandoNaBuscaNaoTemCapaEPerfilFicaLiberado() {
        val passos = gravacao.getValue("busca_digitar")
        assertTrue(voltas("busca_digitar").isEmpty())
        // Do meio para o fim a pessoa está digitando ou no perfil: nada coberto.
        assertTrue(passos.drop(2).all { it.capa == null })
        assertEquals(Lugar.PERFIL, passos.last().origem)
    }

    @Test
    fun resultadoDaBuscaCobreAbaixoDasAbasEReelVolta() {
        val passos = gravacao.getValue("busca_resultado")
        assertTrue(passos.mapNotNull { it.capa }.any { it.topo == 384 })
        assertTrue(voltas("busca_resultado").isNotEmpty())
    }

    @Test
    fun reelDoChatToca() {
        val passos = gravacao.getValue("reel_chat")
        assertTrue(voltas("reel_chat").isEmpty())
        assertTrue(passos.filter { it.visualizador }.all { it.liberado })
        assertTrue(passos.any { it.liberado })
    }

    @Test
    fun reelDoPerfilToca() {
        val passos = gravacao.getValue("reel_perfil")
        assertTrue(voltas("reel_perfil").isEmpty())
        assertTrue(passos.any { it.liberado })
    }

    @Test
    fun hashtagSegueARegraDaLupinha() {
        val passos = gravacao.getValue("hashtag_audio")
        // O primeiro Reel veio do perfil: toca. Depois, os abertos pela página da hashtag voltam.
        val primeiroReel = passos.first { it.visualizador }
        assertTrue(primeiroReel.liberado)
        assertEquals(Acao.NADA, primeiroReel.acao)
        assertTrue(voltas("hashtag_audio").isNotEmpty())
        assertTrue(passos.mapNotNull { it.capa }.isNotEmpty())
    }

    // ---- situações que o Instagram do JV não tem ou que a gravação não pegou ----

    private fun tela(vararg pecas: Pair<String, Boolean>) = Tela(1080, 2340).apply {
        for ((id, selecionado) in pecas) anotar(id, true, selecionado, Caixa(0, 1000, 1080, 1200))
    }

    @Test
    fun abaReelsVaiParaOInicio() {
        val d = Decisor(regras)
        assertEquals(Acao.IR_PARA_INICIO, d.olhar(tela("clips_tab" to true, "tab_bar" to false), 0).acao)
        // Logo em seguida não repete (sem loop de volta e entra).
        assertEquals(Acao.NADA, d.olhar(tela("clips_tab" to true), 500).acao)
        assertEquals(Acao.IR_PARA_INICIO, d.olhar(tela("clips_tab" to true), 2000).acao)
    }

    @Test
    fun arrastarNoReelDoChatVolta() {
        val d = Decisor(regras)
        d.olhar(tela("thread_view_root" to false), 0)
        assertEquals(Acao.NADA, d.olhar(tela("clips_viewer_view_pager" to false), 100).acao)
        assertEquals(Acao.NADA, d.paginaDoVisualizador(0, 200))
        assertEquals(Acao.NADA, d.paginaDoVisualizador(0, 3000))
        assertEquals(Acao.VOLTAR, d.paginaDoVisualizador(1, 4000))
    }

    @Test
    fun feedEReconhecidoMesmoSemAAbaMarcada() {
        // Na gravação do feed, a aba Início não vinha selecionada.
        assertEquals(Lugar.FEED, gravacao.getValue("feed").last { it.origem != Lugar.DESCONHECIDO }.origem)
        assertEquals(Lugar.FEED, Decisor(regras).lugar(tela("reels_tray_container" to false)))
    }

    @Test
    fun storiesSaoReconhecidos() {
        assertTrue(gravacao.getValue("aba_reels").any { it.origem == Lugar.STORIES })
    }

    @Test
    fun reelDoFeedTocaUmSo() {
        val d = Decisor(regras)
        d.olhar(tela("row_feed_profile_header" to false), 0)
        assertEquals(Acao.NADA, d.olhar(tela("clips_viewer_view_pager" to false), 100).acao)
        assertEquals(Acao.NADA, d.paginaDoVisualizador(0, 200))
        assertEquals(Acao.VOLTAR, d.paginaDoVisualizador(1, 3000))
    }

    @Test
    fun reelDosStoriesToca() {
        val d = Decisor(regras)
        d.olhar(tela("reel_viewer_root" to false), 0)
        assertEquals(Acao.NADA, d.olhar(tela("clips_viewer_view_pager" to false), 100).acao)
        assertTrue(d.reelLiberado)
    }

    @Test
    fun voltarDaLupinhaParaOFeedLiberaDeNovo() {
        val d = Decisor(regras)
        d.olhar(tela("grid_card_layout_container" to false), 0)
        d.olhar(tela("reels_tray_container" to false), 100)
        assertEquals(Acao.NADA, d.olhar(tela("clips_viewer_view_pager" to false), 200).acao)
    }

    @Test
    fun reelLogoAoAbrirOInstagramToca() {
        val d = Decisor(regras)
        assertEquals(Acao.NADA, d.olhar(tela("clips_viewer_view_pager" to false), 0).acao)
    }

    @Test
    fun telaDePassagemNaoApagaAOrigem() {
        val d = Decisor(regras)
        d.olhar(tela("thread_view_root" to false), 0)
        d.olhar(tela(), 50)
        assertEquals(Lugar.CONVERSA, d.origem)
        assertNull(d.olhar(tela(), 60).capa)
    }

    @Test
    fun regrasTemTodasAsListas() {
        assertTrue("feed_tab" in regras.todos)
        assertTrue(regras.paginador in regras.visualizador)
    }
}
