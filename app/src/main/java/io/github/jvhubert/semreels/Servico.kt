package io.github.jvhubert.semreels

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * O serviço de Acessibilidade. Só recebe eventos do Instagram (res/xml/servico.xml).
 * Olha a tela, pergunta ao [Decisor] o que fazer e faz: volta, vai para o Início ou cobre a grade.
 * Enquanto uma gravação do modo detetive está aberta, o bloqueio pausa e o serviço só grava.
 */
class Servico : AccessibilityService() {
    private val mao = Handler(Looper.getMainLooper())
    private lateinit var regras: Regras
    private lateinit var decisor: Decisor
    private lateinit var capa: Capa
    private var pronto = false

    private var ultimaOlhada = 0L
    private val olharAtrasado = Runnable { olhar() }
    private val vigia = Runnable { vigiar() }

    private var ultimaFoto = 0L
    private val fotoAtrasada = Runnable { foto("parou") }

    override fun onServiceConnected() {
        super.onServiceConnected()
        regras = Regras.ler(assets.open("regras.json").bufferedReader().use { it.readText() })
        decisor = Decisor(regras)
        capa = Capa(this)
        pronto = true
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        if (!pronto) return
        val g = Detetive.atual
        if (g != null) {
            capa.tirar()
            gravar(e, g)
            return
        }
        if (e.packageName?.toString() != Detetive.INSTAGRAM) return
        if (Config.pausado(this)) {
            capa.tirar()
            return
        }
        val agora = SystemClock.uptimeMillis()

        // Passou para outro Reel dentro do visualizador.
        if (e.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && e.fromIndex >= 0 &&
            e.source?.viewIdResourceName == PREFIXO_ID + regras.paginador
        ) {
            executar(decisor.paginaDoVisualizador(e.fromIndex, agora))
        }

        // Mudança de janela e toque: olha na hora. O resto (vídeo tocando, rolagem) no máximo a cada 250 ms.
        val urgente = e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            e.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            e.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED
        if (urgente || agora - ultimaOlhada > 250) olhar()
        mao.removeCallbacks(olharAtrasado)
        mao.postDelayed(olharAtrasado, 200)
    }

    private fun olhar() {
        if (!pronto || Detetive.atual != null || Config.pausado(this)) return
        ultimaOlhada = SystemClock.uptimeMillis()
        val tela = montarTela()
        if (tela == null) {
            capa.tirar()
            return
        }
        val r = decisor.olhar(tela, ultimaOlhada)
        val c = r.capa?.let { it.copy(base = minOf(it.base, limiteDeBaixo())) }
        if (c != null && c.base - c.topo > 0) {
            capa.mostrar(c)
            mao.removeCallbacks(vigia)
            mao.postDelayed(vigia, 700)
        } else {
            capa.tirar()
        }
        executar(r.acao)
    }

    /** Monta a [Tela] com as janelas do Instagram. Null se o Instagram não está na frente. */
    private fun montarTela(): Tela? {
        val apps = try {
            windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        } catch (_: RuntimeException) {
            return null
        }
        val frente = apps.maxByOrNull { it.layer } ?: return null
        if (frente.root?.packageName?.toString() != Detetive.INSTAGRAM) return null

        val raizes = apps.mapNotNull { j -> j.root?.takeIf { it.packageName?.toString() == Detetive.INSTAGRAM } }
        val r = Rect()
        var largura = 0
        var altura = 0
        for (j in apps) {
            j.getBoundsInScreen(r)
            largura = maxOf(largura, r.right)
            altura = maxOf(altura, r.bottom)
        }
        val tela = Tela(largura, altura)
        for (raiz in raizes) {
            for (id in regras.todos) {
                for (n in raiz.findAccessibilityNodeInfosByViewId(PREFIXO_ID + id)) {
                    n.getBoundsInScreen(r)
                    tela.anotar(id, n.isVisibleToUser, n.isSelected, Caixa(r.left, r.top, r.right, r.bottom))
                }
            }
        }
        return tela
    }

    /** Enquanto a capa está na tela: se o Instagram saiu da frente, tira a capa. */
    private fun vigiar() {
        if (!pronto || !capa.visivel) return
        val naFrente = try {
            windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
                .maxByOrNull { it.layer }?.root?.packageName?.toString() == Detetive.INSTAGRAM
        } catch (_: RuntimeException) {
            false
        }
        if (naFrente) mao.postDelayed(vigia, 700) else capa.tirar()
    }

    /** A capa não desce sobre a barra de navegação do Android. */
    private fun limiteDeBaixo(): Int {
        if (Build.VERSION.SDK_INT < 30) return Int.MAX_VALUE
        return try {
            val m = getSystemService(WindowManager::class.java).currentWindowMetrics
            m.bounds.bottom - m.windowInsets.getInsets(WindowInsets.Type.navigationBars()).bottom
        } catch (_: RuntimeException) {
            Int.MAX_VALUE
        }
    }

    private fun executar(a: Acao) {
        when (a) {
            Acao.NADA -> return
            Acao.VOLTAR -> performGlobalAction(GLOBAL_ACTION_BACK)
            Acao.IR_PARA_INICIO -> {
                val inicio = regras.abaInicio.firstNotNullOfOrNull { id ->
                    rootInActiveWindow?.findAccessibilityNodeInfosByViewId(PREFIXO_ID + id)?.firstOrNull()
                }
                if (inicio?.performAction(AccessibilityNodeInfo.ACTION_CLICK) != true) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            }
        }
        avisar()
    }

    /** "Reels bloqueado", só nas 3 primeiras vezes do dia, para não irritar. */
    private fun avisar() {
        val prefs = getSharedPreferences("avisos", Context.MODE_PRIVATE)
        val hoje = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
        val vezes = if (prefs.getString("dia", null) == hoje) prefs.getInt("vezes", 0) else 0
        if (vezes >= 3) return
        prefs.edit().putString("dia", hoje).putInt("vezes", vezes + 1).apply()
        Toast.makeText(this, "Reels bloqueado pelo Sem Reels", Toast.LENGTH_SHORT).show()
    }

    // ---- modo detetive ----

    private fun gravar(e: AccessibilityEvent, g: Gravacao) {
        if (Detetive.expirou(g)) {
            Detetive.parar()
            return
        }
        if (e.packageName?.toString() != Detetive.INSTAGRAM) return

        // Mudança de conteúdo acontece o tempo todo (vídeo tocando): não vira linha, só pede foto.
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            g.evento(descrever(e))
        }

        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                foto(nomeDoEvento(e.eventType))
                agendarFoto(600)
            }
            else -> {
                // Tela mexendo sem parar: tira foto pelo menos a cada 1,5 s; quando acalma, mais uma.
                if (SystemClock.uptimeMillis() - ultimaFoto > 1500) foto(nomeDoEvento(e.eventType))
                agendarFoto(400)
            }
        }
    }

    private fun descrever(e: AccessibilityEvent): Map<String, Any?> {
        val m = linkedMapOf<String, Any?>("ev" to nomeDoEvento(e.eventType))
        if (e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // O nome da classe diz qual tela/janela abriu (ex.: uma janela de prévia).
            m["classe"] = e.className?.toString()
        }
        e.source?.let { m["fonte"] = Arvore.resumo(it) }
        if (e.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (Build.VERSION.SDK_INT >= 28) {
                m["dx"] = e.scrollDeltaX
                m["dy"] = e.scrollDeltaY
            }
            m["de"] = e.fromIndex
            m["ate"] = e.toIndex
            m["itens"] = e.itemCount
        }
        return m
    }

    private fun agendarFoto(ms: Long) {
        mao.removeCallbacks(fotoAtrasada)
        mao.postDelayed(fotoAtrasada, ms)
    }

    private fun foto(motivo: String) {
        val g = Detetive.atual ?: return
        ultimaFoto = SystemClock.uptimeMillis()
        val janelas = try {
            windows.map { Arvore.janela(it) }
        } catch (_: RuntimeException) {
            emptyList()
        }
        val lista = janelas.ifEmpty { listOfNotNull(rootInActiveWindow?.let { Arvore.raizAvulsa(it) }) }
        if (lista.isNotEmpty()) g.foto(motivo, lista)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        mao.removeCallbacksAndMessages(null)
        if (pronto) capa.tirar()
        super.onDestroy()
    }

    private fun nomeDoEvento(tipo: Int) = when (tipo) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "janela"
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "conteudo"
        AccessibilityEvent.TYPE_VIEW_SCROLLED -> "rolou"
        AccessibilityEvent.TYPE_VIEW_CLICKED -> "toque"
        AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> "toque_longo"
        AccessibilityEvent.TYPE_VIEW_FOCUSED -> "foco"
        AccessibilityEvent.TYPE_VIEW_SELECTED -> "selecionou"
        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "digitou"
        else -> "evento$tipo"
    }

    private companion object {
        const val PREFIXO_ID = "com.instagram.android:id/"
    }
}
