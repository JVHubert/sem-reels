package io.github.jvhubert.semreels

import android.content.Context

/**
 * Configurações do app. A tela e o serviço rodam no mesmo processo, então o que a tela grava
 * aqui o serviço enxerga na hora.
 */
object Config {
    const val PAUSA_MS = 15 * 60 * 1000L

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("config", Context.MODE_PRIVATE)

    /** Desligada por padrão: sem ela, não há como pausar pelo app. */
    fun permitirPausa(ctx: Context) = prefs(ctx).getBoolean("permitirPausa", false)

    fun mudarPermitirPausa(ctx: Context, permitir: Boolean) {
        prefs(ctx).edit().putBoolean("permitirPausa", permitir).apply()
        if (!permitir) encerrarPausa(ctx)
    }

    fun pausaAte(ctx: Context) = prefs(ctx).getLong("pausaAte", 0L)

    fun pausado(ctx: Context) = permitirPausa(ctx) && System.currentTimeMillis() < pausaAte(ctx)

    fun pausar(ctx: Context) {
        prefs(ctx).edit().putLong("pausaAte", System.currentTimeMillis() + PAUSA_MS).apply()
    }

    fun encerrarPausa(ctx: Context) {
        prefs(ctx).edit().putLong("pausaAte", 0L).apply()
    }
}
