package io.github.jvhubert.semreels

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * O serviço de Acessibilidade. Só recebe eventos do Instagram (res/xml/servico.xml).
 * Nesta versão (Fase 1) ele não bloqueia nada: só alimenta o [Detetive] quando há gravação aberta.
 */
class Servico : AccessibilityService() {
    private val mao = Handler(Looper.getMainLooper())
    private var ultimaFoto = 0L
    private val fotoAtrasada = Runnable { foto("parou") }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val g = Detetive.atual ?: return
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
}
