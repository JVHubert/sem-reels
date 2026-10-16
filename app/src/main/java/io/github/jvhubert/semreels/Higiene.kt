package io.github.jvhubert.semreels

import java.util.Locale

/**
 * Filtro de privacidade do modo detetive. Do que aparece na tela, só passam as palavras da
 * interface do Instagram ("Reels", "Pesquisar", "curtidas"...). Qualquer outra palavra (nome
 * de amigo, mensagem, legenda) vira um código curto, como "~k3f". A mesma palavra vira sempre
 * o mesmo código, então ainda dá para ver a estrutura da tela sem ler o conteúdo.
 */
object Higiene {
    private const val MAX_PALAVRAS = 12

    private val VOCABULARIO: Set<String> = """
        a o e as os um uma uns umas de do da dos das em no na nos nas por pelo pela para pra
        com sem ao aos à às seu sua seus suas meu minha você vocês que se ou mais menos
        of by the an in on for to with and or your you my from at is
        reel reels vídeo vídeos video videos foto fotos photo photos imagem imagens image
        perfil profile publicação publicações post posts carrossel carousel story stories
        história histórias destaque destaques highlight highlights
        início inicio home feed pesquisar pesquisa buscar busca search explorar explore
        mensagens mensagem messages message direct chat conversa conversas thread inbox
        caixa entrada solicitações requests principal principais geral primary general
        notificações notifications atividade activity
        curtir curtida curtidas curtiu like likes liked comentar comentário comentários
        comment comments compartilhar share enviar enviado enviou send sent salvar salvo
        save saved seguir seguindo seguidores follow following followers
        opções options menu voltar back fechar close cancelar cancel ok concluído done
        áudio audio original música músicas music som sound mudo mute unmute silenciar
        ativar desativar tocar play pausar pause reproduzir
        contas conta accounts account tags tag lugares lugar places place top para você
        recentes recente recent ver see view tudo todos all câmera camera nova novo novas
        novos new criar create aba tab tabs botão button ícone icon selecionado selected
        toque tap duas vezes double campo texto text digitando typing
        sugerido sugeridos sugerida sugeridas suggested sugestões suggestions
        usar use remix legenda caption tradução translation traduzir
        anúncio patrocinado sponsored ad ads lista list grade grid abrir open
        segundo segundos seconds minuto minutos minutes hora horas hours dia dias days
        semana semanas weeks h min s sem há ago ontem yesterday hoje today
        visto vista seen ativo ativa active agora now online reagiu reacted respondeu replied
        editar edit configurações settings arquivo archive marcado marcados tagged
        nota notas notes live vivo transmissão clipes clips mil mi k m
        ir go próximo próxima next anterior previous página page carregando loading
        resultado resultados results pessoas people hashtags locais
        threads facebook instagram meta
    """.trim().split(Regex("\\s+")).toSet()

    private val NUMERO = Regex("^\\d[\\d.,:]*(k|m|mi|mil)?$")
    private val PONTAS = Regex("^[\\p{P}\\p{S}]+|[\\p{P}\\p{S}]+$")

    /** Devolve o texto filtrado, ou null se não havia texto. */
    fun texto(s: CharSequence?): String? {
        if (s == null) return null
        val t = s.toString().trim()
        if (t.isEmpty()) return null
        val palavras = t.split(Regex("\\s+"))
        val saida = palavras.take(MAX_PALAVRAS).joinToString(" ") { palavra(it) }
        return if (palavras.size > MAX_PALAVRAS) "$saida …" else saida
    }

    private fun palavra(p: String): String {
        // Guarda só o sinal de @menção e #hashtag, porque ajuda a reconhecer a tela.
        val prefixo = if (p.startsWith("@") || p.startsWith("#")) p.substring(0, 1) else ""
        val nucleo = p.replace(PONTAS, "").lowercase(Locale.ROOT)
        return when {
            nucleo.isEmpty() -> "·"
            NUMERO.matches(nucleo) -> prefixo + "9"
            nucleo in VOCABULARIO -> prefixo + nucleo
            else -> prefixo + "~" + codigo(nucleo)
        }
    }

    fun codigo(s: String): String = Integer.toString(s.hashCode() and 0xFFFF, 36)
}
