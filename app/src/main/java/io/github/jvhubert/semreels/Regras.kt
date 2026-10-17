package io.github.jvhubert.semreels

/**
 * Os nomes internos (ids) das peças da tela do Instagram que o bloqueio procura.
 * Vêm de assets/regras.json, para que uma mudança do Instagram não exija mexer no código.
 */
data class Regras(
    val abaReels: List<String>,
    val abaInicio: List<String>,
    val abaBusca: List<String>,
    /** O visualizador de Reels em tela cheia. */
    val visualizador: List<String>,
    /** A peça do visualizador que avisa quando passa para o próximo Reel. */
    val paginador: String,
    val conversa: List<String>,
    val perfil: List<String>,
    /** A tela de stories (o Instagram chama de "reel_viewer", mas não são os Reels). */
    val stories: List<String>,
    /** Peças do feed: a fileira de stories no topo e o cabeçalho dos posts. */
    val feed: List<String>,
    /** Cada quadradinho das grades de desconhecidos (lupinha, busca, hashtag). */
    val grade: List<String>,
    /** O que fica acima da grade e precisa continuar tocável (barra de busca, abas do resultado). */
    val topoDaGrade: List<String>,
    val barraDeAbas: List<String>,
    /** A janelinha que abre ao segurar o dedo num vídeo da grade. */
    val previa: List<String>,
) {
    /** Todos os ids que o serviço precisa procurar na tela. */
    val todos: Set<String> =
        (abaReels + abaInicio + abaBusca + visualizador + paginador + conversa + perfil + stories + feed + grade +
            topoDaGrade + barraDeAbas + previa).toSet()

    companion object {
        fun ler(json: String): Regras {
            val m = LeitorJson.ler(json) as Map<*, *>
            fun lista(k: String) = (m[k] as? List<*>)?.map { it as String }
                ?: error("regras.json sem a lista \"$k\"")
            return Regras(
                abaReels = lista("abaReels"),
                abaInicio = lista("abaInicio"),
                abaBusca = lista("abaBusca"),
                visualizador = lista("visualizador"),
                paginador = m["paginador"] as? String ?: error("regras.json sem \"paginador\""),
                conversa = lista("conversa"),
                perfil = lista("perfil"),
                stories = lista("stories"),
                feed = lista("feed"),
                grade = lista("grade"),
                topoDaGrade = lista("topoDaGrade"),
                barraDeAbas = lista("barraDeAbas"),
                previa = lista("previa"),
            )
        }
    }
}
