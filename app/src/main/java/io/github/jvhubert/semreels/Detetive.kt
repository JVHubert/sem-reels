package io.github.jvhubert.semreels

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import java.io.BufferedWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modo detetive: enquanto uma gravação está aberta, o [Servico] escreve aqui cada evento e cada
 * "foto" da tela do Instagram. Uma gravação por situação ([Cenario]), num arquivo .jsonl
 * (uma linha JSON por registro). A tela do app e o serviço rodam no mesmo processo.
 */
object Detetive {
    const val INSTAGRAM = "com.instagram.android"

    /** Se a pessoa esquecer de voltar ao app, a gravação fecha sozinha. */
    private const val DURACAO_MAXIMA_MS = 3 * 60 * 1000L

    @Volatile
    var atual: Gravacao? = null
        private set

    private fun pasta(ctx: Context) = File(ctx.filesDir, "gravacoes").apply { mkdirs() }

    fun comecar(ctx: Context, cenario: Cenario) {
        parar()
        atual = Gravacao(File(pasta(ctx), "${cenario.id}.jsonl"), cenario.id)
    }

    /** Fecha a gravação aberta e devolve quantas telas ela registrou (null se não havia). */
    fun parar(): Int? {
        val g = atual ?: return null
        atual = null
        g.fechar()
        return g.telas
    }

    fun expirou(g: Gravacao) = SystemClock.uptimeMillis() - g.inicio > DURACAO_MAXIMA_MS

    /** Situações já gravadas e quantas telas cada uma tem. */
    fun gravadas(ctx: Context): Map<String, Int> =
        pasta(ctx).listFiles().orEmpty()
            .filter { it.extension == "jsonl" && it.length() > 0 }
            .associate { f -> f.nameWithoutExtension to f.useLines { l -> l.count { it.contains(",\"foto\":") } } }

    fun apagar(ctx: Context) {
        parar()
        pasta(ctx).listFiles().orEmpty().forEach { it.delete() }
    }

    /** Junta todas as gravações num arquivo só, para mandar pelo "Compartilhar". */
    fun juntar(ctx: Context): File {
        val pastaEnvio = File(ctx.cacheDir, "envio").apply { mkdirs() }
        pastaEnvio.listFiles().orEmpty().forEach { it.delete() }
        val carimbo = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ROOT).format(Date())
        val destino = File(pastaEnvio, "sem-reels-gravacoes_$carimbo.txt")
        destino.bufferedWriter().use { saida ->
            saida.write(Json.texto(cabecalho(ctx)))
            saida.newLine()
            for (c in CENARIOS) {
                val f = File(pasta(ctx), "${c.id}.jsonl")
                if (!f.exists()) continue
                f.bufferedReader().use { entrada -> entrada.copyTo(saida) }
            }
        }
        return destino
    }

    private fun cabecalho(ctx: Context): Map<String, Any?> {
        val pm = ctx.packageManager
        val instagram = try {
            pm.getPackageInfo(INSTAGRAM, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        return linkedMapOf(
            "app" to pm.getPackageInfo(ctx.packageName, 0).versionName,
            "instagram" to instagram,
            "android" to Build.VERSION.SDK_INT,
            "aparelho" to Build.MODEL,
            "idioma" to Locale.getDefault().toLanguageTag(),
            "tela" to ctx.resources.displayMetrics.let { listOf(it.widthPixels, it.heightPixels, it.densityDpi) },
        )
    }
}

class Gravacao(arquivo: File, private val cenario: String) {
    val inicio = SystemClock.uptimeMillis()
    var telas = 0
        private set
    private var ultimaAssinatura = 0
    private val saida: BufferedWriter = arquivo.bufferedWriter()
    private var aberta = true

    @Synchronized
    fun evento(dados: Map<String, Any?>) = escrever(dados)

    /** Grava uma foto da tela, só se ela mudou desde a última. */
    @Synchronized
    fun foto(motivo: String, janelas: List<Map<String, Any?>>) {
        val assinatura = Json.texto(janelas).hashCode()
        if (assinatura == ultimaAssinatura) return
        ultimaAssinatura = assinatura
        telas++
        escrever(linkedMapOf("foto" to motivo, "janelas" to janelas))
    }

    private fun escrever(dados: Map<String, Any?>) {
        if (!aberta) return
        val linha = LinkedHashMap<String, Any?>()
        linha["cen"] = cenario
        linha["t"] = SystemClock.uptimeMillis() - inicio
        linha.putAll(dados)
        saida.write(Json.texto(linha))
        saida.newLine()
        saida.flush()
    }

    @Synchronized
    fun fechar() {
        if (!aberta) return
        aberta = false
        saida.close()
    }
}
