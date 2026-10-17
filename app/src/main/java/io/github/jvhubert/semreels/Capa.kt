package io.github.jvhubert.semreels

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * A capa escura que o serviço desenha sobre a grade de vídeos. Ela engole todos os toques
 * (toque, toque longo, arrastar), então nenhum vídeo da grade abre nem toca a prévia.
 * Janela de Acessibilidade: não precisa da permissão de "aparecer sobre outros apps".
 */
class Capa(private val servico: AccessibilityService) {
    private val wm = servico.getSystemService(WindowManager::class.java)
    private var vista: View? = null
    private var atual: Caixa? = null

    val visivel get() = vista != null

    fun mostrar(c: Caixa) {
        if (c == atual && vista != null) return
        val p = parametros(c)
        try {
            val v = vista
            if (v == null) {
                val nova = criarVista()
                wm.addView(nova, p)
                vista = nova
            } else {
                wm.updateViewLayout(v, p)
            }
            atual = c
        } catch (_: RuntimeException) {
            tirar()
        }
    }

    fun tirar() {
        vista?.let {
            try {
                wm.removeView(it)
            } catch (_: RuntimeException) {
            }
        }
        vista = null
        atual = null
    }

    private fun parametros(c: Caixa) = WindowManager.LayoutParams(
        c.dir - c.esq,
        c.base - c.topo,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.OPAQUE,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = c.esq
        y = c.topo
        // Posição exata, em pixels da tela, sem o Android empurrar por causa das barras do sistema.
        if (Build.VERSION.SDK_INT >= 30) {
            fitInsetsTypes = 0
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        title = "Sem Reels - capa"
    }

    private fun criarVista(): View {
        val ctx = servico
        val dp = ctx.resources.displayMetrics.density
        fun rotulo(t: String, sp: Float, cor: Int, negrito: Boolean) = TextView(ctx).apply {
            text = t
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
            setTextColor(cor)
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.25f)
            if (negrito) typeface = Typeface.DEFAULT_BOLD
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(FUNDO)
            val m = (24 * dp).toInt()
            setPadding(m, (96 * dp).toInt(), m, m)
            addView(rotulo("Vídeos escondidos", 22f, DESTAQUE, negrito = true))
            addView(
                rotulo("Toque na busca, lá em cima, para procurar alguém.", 17f, TEXTO, negrito = false),
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = (10 * dp).toInt() },
            )
            addView(
                rotulo("Sem Reels", 14f, TEXTO_2, negrito = false),
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = (24 * dp).toInt() },
            )
            // Engole todos os toques: nada passa para o vídeo que está embaixo.
            setOnTouchListener { _, _ -> true }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
    }

    private companion object {
        const val FUNDO = 0xFF0F1216.toInt()
        const val TEXTO = 0xFFE7E5E0.toInt()
        const val TEXTO_2 = 0xFF9AA1AB.toInt()
        const val DESTAQUE = 0xFFF0B03F.toInt()
    }
}
