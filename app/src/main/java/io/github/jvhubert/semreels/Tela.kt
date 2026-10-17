package io.github.jvhubert.semreels

data class Caixa(val esq: Int, val topo: Int, val dir: Int, val base: Int)

/**
 * O que o bloqueio precisa saber da tela do Instagram: quais peças estão visíveis, onde estão
 * e quais estão selecionadas. No celular é montada pelo [Servico]; nos testes, pelas gravações.
 */
class Tela(val largura: Int, val altura: Int) {
    private val visiveis = HashMap<String, MutableList<Caixa>>()
    private val selecionados = HashSet<String>()

    /** Peças escondidas ou fora da tela (o Instagram deixa telas antigas montadas atrás) são ignoradas. */
    fun anotar(id: String, visivel: Boolean, selecionado: Boolean, c: Caixa) {
        val naTela = c.dir > c.esq && c.base > c.topo && c.dir > 0 && c.esq < largura && c.base > 0 && c.topo < altura
        if (!visivel || !naTela) return
        visiveis.getOrPut(id) { ArrayList() }.add(c)
        if (selecionado) selecionados.add(id)
    }

    fun algumVisivel(ids: List<String>) = ids.any { it in visiveis }
    fun algumSelecionado(ids: List<String>) = ids.any { it in selecionados }
    fun caixas(ids: List<String>): List<Caixa> = ids.flatMap { visiveis[it].orEmpty() }
}
