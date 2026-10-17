package io.github.jvhubert.semreels

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.ceil

/**
 * Botão que só funciona segurando: enche da esquerda para a direita durante [duracaoMs].
 * Soltar antes zera. O atrito é de propósito, para a pausa não virar hábito.
 * Conta o tempo pelo relógio (não por animação), para funcionar mesmo com animações desligadas.
 */
class BotaoSegurar(
    ctx: Context,
    private val texto: String,
    private val duracaoMs: Long,
    private val aoCompletar: () -> Unit,
) : View(ctx) {
    private val dp = ctx.resources.displayMetrics.density
    private val fundo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SUPERFICIE_2 }
    private val borda = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BORDA
        style = Paint.Style.STROKE
        strokeWidth = dp
    }
    private val enchimento = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = DESTAQUE }
    private val letra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXTO
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, ctx.resources.displayMetrics)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val caixa = RectF()

    private var inicio = 0L
    private var progresso = 0f
    private val passo = object : Runnable {
        override fun run() {
            progresso = ((SystemClock.uptimeMillis() - inicio).toFloat() / duracaoMs).coerceAtMost(1f)
            invalidate()
            if (progresso >= 1f) {
                inicio = 0L
                aoCompletar()
            } else {
                postDelayed(this, 50)
            }
        }
    }

    init {
        contentDescription = texto
        isClickable = true
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true) // a rolagem da tela não rouba o dedo
                inicio = SystemClock.uptimeMillis()
                removeCallbacks(passo)
                post(passo)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> soltar()
        }
        return true
    }

    private fun soltar() {
        removeCallbacks(passo)
        inicio = 0L
        progresso = 0f
        invalidate()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(passo)
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        val r = 12 * dp
        caixa.set(0f, 0f, width.toFloat(), height.toFloat())
        c.drawRoundRect(caixa, r, r, fundo)
        if (progresso > 0f) {
            c.save()
            c.clipRect(0f, 0f, width * progresso, height.toFloat())
            c.drawRoundRect(caixa, r, r, enchimento)
            c.restore()
        }
        caixa.inset(dp / 2, dp / 2)
        c.drawRoundRect(caixa, r, r, borda)

        val rotulo = if (inicio == 0L) {
            texto
        } else {
            val faltam = ceil((1f - progresso) * duracaoMs / 1000f).toInt()
            "Continue segurando… $faltam s"
        }
        letra.color = if (progresso > 0.5f) TEXTO_NO_DESTAQUE else TEXTO
        val y = height / 2f - (letra.descent() + letra.ascent()) / 2
        c.drawText(rotulo, width / 2f, y, letra)
    }

    private companion object {
        const val SUPERFICIE_2 = 0xFF1F242C.toInt()
        const val BORDA = 0xFF2E3540.toInt()
        const val TEXTO = 0xFFE7E5E0.toInt()
        const val DESTAQUE = 0xFFF0B03F.toInt()
        const val TEXTO_NO_DESTAQUE = 0xFF1A1408.toInt()
    }
}
