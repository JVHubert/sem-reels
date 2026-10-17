package io.github.jvhubert.semreels

enum class Acao { NADA, IR_PARA_INICIO, VOLTAR }

enum class Lugar { CONVERSA, PERFIL, STORIES, EXPLORAR, FEED, DESCONHECIDO }

/** [capa]: área da tela a cobrir (a grade de vídeos), ou null para tirar a capa. */
data class Resposta(val acao: Acao, val capa: Caixa?)

/**
 * O cérebro do bloqueio, sem nada de Android, para dar para testar no PC com as gravações.
 *
 * - Aba Reels selecionada: vai para o Início.
 * - Reel em tela cheia vindo da lupinha, da busca ou de uma hashtag: volta na hora. De qualquer
 *   outro lugar (chat, perfil, feed, stories): toca, mas passar para o próximo volta.
 * - Grade de vídeos de desconhecidos: coberta pela capa, que engole os toques.
 * - Prévia do toque longo: se abrir mesmo assim, fecha.
 */
class Decisor(private val r: Regras) {
    var origem = Lugar.DESCONHECIDO
        private set
    var reelLiberado = false
        private set
    private var visualizadorAberto = false
    private var paginaInicial: Int? = null
    private var ultimoBloqueio = Long.MIN_VALUE / 2

    fun olhar(t: Tela, agora: Long): Resposta {
        if (t.algumSelecionado(r.abaReels)) return Resposta(bloquear(agora, Acao.IR_PARA_INICIO), null)

        if (t.algumVisivel(r.visualizador)) {
            val acao = when {
                !visualizadorAberto -> abriu(agora)
                reelLiberado -> Acao.NADA
                else -> bloquear(agora, Acao.VOLTAR) // o voltar anterior não funcionou
            }
            return Resposta(acao, null)
        }
        visualizadorAberto = false
        reelLiberado = false
        paginaInicial = null

        // Telas de passagem (vazias, animação) não apagam de onde a pessoa veio.
        val lugar = lugar(t)
        if (lugar != Lugar.DESCONHECIDO) origem = lugar

        val capa = capa(t, lugar)
        if (t.algumVisivel(r.previa) && !origemLiberada()) return Resposta(bloquear(agora, Acao.VOLTAR), capa)
        return Resposta(Acao.NADA, capa)
    }

    /** O visualizador avisou que está mostrando o Reel número [pagina]. */
    fun paginaDoVisualizador(pagina: Int, agora: Long): Acao {
        if (!visualizadorAberto) {
            val acao = abriu(agora)
            if (reelLiberado) paginaInicial = pagina
            return acao
        }
        if (!reelLiberado) return bloquear(agora, Acao.VOLTAR)
        val inicial = paginaInicial ?: run {
            paginaInicial = pagina
            return Acao.NADA
        }
        return if (pagina != inicial) bloquear(agora, Acao.VOLTAR) else Acao.NADA
    }

    private fun abriu(agora: Long): Acao {
        visualizadorAberto = true
        paginaInicial = null
        reelLiberado = origemLiberada()
        return if (reelLiberado) Acao.NADA else bloquear(agora, Acao.VOLTAR)
    }

    private fun origemLiberada() = origem != Lugar.EXPLORAR

    /** Depois de bloquear, espera um pouco antes de bloquear de novo, para não entrar em loop. */
    private fun bloquear(agora: Long, acao: Acao): Acao {
        if (agora - ultimoBloqueio < ESPERA_MS) return Acao.NADA
        ultimoBloqueio = agora
        return acao
    }

    fun lugar(t: Tela): Lugar = when {
        t.algumVisivel(r.conversa) -> Lugar.CONVERSA
        t.algumVisivel(r.perfil) -> Lugar.PERFIL
        t.algumVisivel(r.stories) -> Lugar.STORIES
        t.algumVisivel(r.grade) || t.algumVisivel(r.topoDaGrade) || t.algumSelecionado(r.abaBusca) -> Lugar.EXPLORAR
        // Ao abrir o Instagram, a aba Início às vezes não vem marcada: o feed é reconhecido pelo conteúdo.
        t.algumSelecionado(r.abaInicio) || t.algumVisivel(r.feed) -> Lugar.FEED
        else -> Lugar.DESCONHECIDO
    }

    /** Cobre da barra de busca (ou das abas do resultado) até a barra de abas lá embaixo. */
    private fun capa(t: Tela, lugar: Lugar): Caixa? {
        if (lugar == Lugar.CONVERSA || lugar == Lugar.PERFIL) return null
        val celulas = t.caixas(r.grade)
        if (celulas.isEmpty()) return null
        val topo = t.caixas(r.topoDaGrade).maxOfOrNull { it.base } ?: celulas.minOf { it.topo }
        val base = t.caixas(r.barraDeAbas).minOfOrNull { it.topo } ?: t.altura
        if (base - topo < ALTURA_MINIMA) return null
        return Caixa(0, maxOf(0, topo), t.largura, base)
    }

    companion object {
        const val ESPERA_MS = 1500L
        private const val ALTURA_MINIMA = 100
    }
}
