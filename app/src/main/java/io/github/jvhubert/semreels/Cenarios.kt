package io.github.jvhubert.semreels

/** As situações que o JV grava no modo detetive (Fase 1 do plano). A ordem é a da tela. */
data class Cenario(val id: String, val titulo: String, val instrucao: String)

val CENARIOS = listOf(
    Cenario(
        "feed", "O feed",
        "Fique na aba Início e role o feed um pouco. Se aparecer um vídeo no meio, deixe tocar.",
    ),
    Cenario(
        "aba_reels", "A aba Reels",
        "Toque na aba Reels (o ícone de play na barra de baixo) e passe para o próximo Reel uma vez.",
    ),
    Cenario(
        "lupinha", "A lupinha",
        "Toque na lupinha e role a grade de vídeos um pouco. Depois toque num vídeo da grade.",
    ),
    Cenario(
        "previa_lupinha", "Segurar o dedo na lupinha",
        "Na lupinha, segure o dedo em cima de um vídeo da grade até a prévia abrir. Solte.",
    ),
    Cenario(
        "busca_digitar", "Digitar na busca",
        "Na lupinha, toque na barra de busca e digite o nome de um amigo. Abra o perfil dele.",
    ),
    Cenario(
        "busca_resultado", "Resultado da busca",
        "Na busca, digite \"futebol\" e confirme no teclado. Passe por todas as abas do resultado e segure o dedo num vídeo.",
    ),
    Cenario(
        "reel_chat", "Reel mandado no chat",
        "Abra uma conversa em que alguém te mandou um Reel. Toque no Reel, deixe tocar uns segundos e depois arraste para cima.",
    ),
    Cenario(
        "reel_perfil", "Reel pelo perfil",
        "Abra o perfil de alguém, toque num Reel da grade dele e depois arraste para cima.",
    ),
    Cenario(
        "hashtag_audio", "Hashtag e música",
        "Num post, toque numa #hashtag. Volte. Depois, num Reel, toque no nome da música lá embaixo.",
    ),
)
