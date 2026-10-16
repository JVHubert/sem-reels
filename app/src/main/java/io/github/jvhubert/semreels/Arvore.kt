package io.github.jvhubert.semreels

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

/**
 * Transforma o que o Instagram mostra na tela numa árvore enxuta (Map/List) para gravar.
 * Todo texto passa pela [Higiene]. O que a pessoa digita vira só a quantidade de letras.
 */
object Arvore {
    private const val PREFIXO_ID = "com.instagram.android:id/"
    private const val MAX_PROFUNDIDADE = 80
    private const val MAX_NOS = 3000

    fun janela(j: AccessibilityWindowInfo): Map<String, Any?> {
        val m = linkedMapOf<String, Any?>(
            "tipo" to tipoJanela(j.type),
            "camada" to j.layer,
            "b" to caixa(Rect().also { j.getBoundsInScreen(it) }),
        )
        if (j.isActive) m["ativa"] = 1
        if (j.isFocused) m["foco"] = 1
        Higiene.texto(j.title)?.let { m["titulo"] = it }
        val raiz = j.root
        val pacote = raiz?.packageName?.toString()
        m["pacote"] = pacote
        // Só a árvore do Instagram é gravada; das outras janelas (teclado, barra do sistema) basta saber que existem.
        if (raiz != null && pacote == Detetive.INSTAGRAM) m["raiz"] = no(raiz, 0, intArrayOf(0))
        return m
    }

    fun raizAvulsa(raiz: AccessibilityNodeInfo): Map<String, Any?> =
        linkedMapOf("tipo" to "ativa", "pacote" to raiz.packageName?.toString(), "raiz" to no(raiz, 0, intArrayOf(0)))

    /** Resumo do elemento que gerou um evento (o que foi tocado, rolado...). */
    fun resumo(n: AccessibilityNodeInfo): Map<String, Any?> {
        val m = linkedMapOf<String, Any?>()
        idCurto(n)?.let { m["id"] = it }
        m["c"] = classeCurta(n)
        Higiene.texto(n.contentDescription)?.let { m["d"] = it }
        textoSeguro(n)?.let { m["x"] = it }
        m["b"] = caixa(Rect().also { n.getBoundsInScreen(it) })
        return m
    }

    private fun no(n: AccessibilityNodeInfo, profundidade: Int, contador: IntArray): Map<String, Any?> {
        contador[0]++
        val m = linkedMapOf<String, Any?>()
        idCurto(n)?.let { m["id"] = it }
        m["c"] = classeCurta(n)
        Higiene.texto(n.contentDescription)?.let { m["d"] = it }
        textoSeguro(n)?.let { m["x"] = it }
        Higiene.texto(n.hintText)?.let { m["dica"] = it }
        if (Build.VERSION.SDK_INT >= 28) Higiene.texto(n.paneTitle)?.let { m["painel"] = it }
        if (Build.VERSION.SDK_INT >= 30) Higiene.texto(n.stateDescription)?.let { m["estado"] = it }
        if (n.isSelected) m["s"] = 1
        if (n.isChecked) m["marcado"] = 1
        if (n.isClickable) m["k"] = 1
        if (n.isLongClickable) m["l"] = 1
        if (n.isScrollable) m["r"] = 1
        if (n.isFocused) m["f"] = 1
        if (n.isEditable) m["e"] = 1
        if (!n.isVisibleToUser) m["v"] = 0
        m["b"] = caixa(Rect().also { n.getBoundsInScreen(it) })

        if (n.childCount > 0) {
            if (profundidade >= MAX_PROFUNDIDADE || contador[0] >= MAX_NOS) {
                m["cortado"] = n.childCount
            } else {
                val filhos = ArrayList<Map<String, Any?>>(n.childCount)
                for (i in 0 until n.childCount) {
                    val f = n.getChild(i) ?: continue
                    filhos.add(no(f, profundidade + 1, contador))
                }
                m["n"] = filhos
            }
        }
        return m
    }

    /** Campo de digitação: grava só quantas letras tem, nunca o que foi escrito. */
    private fun textoSeguro(n: AccessibilityNodeInfo): String? {
        val t = n.text ?: return null
        if (t.isBlank()) return null
        return if (n.isEditable) "(${t.length} letras)" else Higiene.texto(t)
    }

    private fun idCurto(n: AccessibilityNodeInfo): String? =
        n.viewIdResourceName?.removePrefix(PREFIXO_ID)

    private fun classeCurta(n: AccessibilityNodeInfo): String? =
        n.className?.toString()?.substringAfterLast('.')

    private fun caixa(r: Rect) = listOf(r.left, r.top, r.right, r.bottom)

    private fun tipoJanela(t: Int) = when (t) {
        AccessibilityWindowInfo.TYPE_APPLICATION -> "app"
        AccessibilityWindowInfo.TYPE_INPUT_METHOD -> "teclado"
        AccessibilityWindowInfo.TYPE_SYSTEM -> "sistema"
        AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> "capa"
        AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER -> "divisor"
        else -> "tipo$t"
    }
}
