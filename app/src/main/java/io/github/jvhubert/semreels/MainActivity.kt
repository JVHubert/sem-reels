package io.github.jvhubert.semreels

import android.app.Activity
import android.app.AlertDialog
import android.app.StatusBarManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Uma tela só, montada no código: mostra se o bloqueio está ligado, confere a bateria e, escondido
 * no fim, guarda o modo detetive (grava cada situação no Instagram e manda as gravações).
 */
class MainActivity : Activity() {
    private lateinit var conteudo: LinearLayout
    private var detetiveAberto = false
    private val mao = Handler(Looper.getMainLooper())
    private val atualizar = Runnable { montar() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(40))
        }
        val rolagem = ScrollView(this).apply {
            setBackgroundColor(FUNDO)
            addView(conteudo)
        }
        setContentView(rolagem)
        // Android 15 desenha atrás das barras do sistema: afasta o conteúdo delas.
        ViewCompat.setOnApplyWindowInsetsListener(rolagem) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(b.left, b.top, b.right, b.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onResume() {
        super.onResume()
        // Voltar para cá encerra a gravação que estava aberta.
        Detetive.parar()?.let { telas ->
            val msg = if (telas > 0) "Gravado: $telas telas." else "Nada foi gravado. O serviço está ligado?"
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
        montar()
    }

    override fun onPause() {
        mao.removeCallbacks(atualizar)
        super.onPause()
    }

    private fun montar() {
        // Durante a pausa, a tela se atualiza sozinha para mostrar quando ela acaba.
        mao.removeCallbacks(atualizar)
        if (Config.pausado(this)) mao.postDelayed(atualizar, 15_000)

        conteudo.removeAllViews()
        val ligado = servicoLigado()
        val bateriaOk = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        val gravadas = Detetive.gravadas(this)

        texto("Sem Reels", 30f, TEXTO, negrito = true)
        val pausado = Config.pausado(this)
        if (ligado && pausado) {
            texto("Bloqueio pausado", 24f, TEXTO, negrito = true, topo = 16)
            texto("Volta sozinho às ${horaFimDaPausa()}.", 16f, TEXTO_2)
            botao("Voltar a bloquear agora", principal = true) { Config.encerrarPausa(this); montar() }
        } else if (ligado) {
            texto("Bloqueio ligado", 24f, DESTAQUE, negrito = true, topo = 16)
            texto(
                "Reels da lupinha, da busca e de hashtags voltam na hora, e a grade de vídeos fica coberta. " +
                    "Reels do chat, do feed, dos stories ou do perfil de alguém tocam normal, um de cada vez: " +
                    "arrastar para o próximo volta.",
                16f, TEXTO_2,
            )
        } else {
            texto("Bloqueio desligado", 24f, TEXTO, negrito = true, topo = 16)
        }

        titulo("1. Ligue o serviço", feito = ligado)
        if (ligado) {
            texto("Ligado.", 16f, DESTAQUE)
        } else {
            texto(
                "Em Acessibilidade, toque em Apps instalados > Sem Reels e ligue. " +
                    "Se o Android disser que é uma configuração restrita, toque no segundo botão, depois no menu ⋮ " +
                    "lá em cima > Permitir configurações restritas, e tente de novo.",
                16f, TEXTO_2,
            )
            botao("Abrir Acessibilidade", principal = true) {
                abrir(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            botao("Abrir informações do app", principal = false) { abrirInfoDoApp() }
        }

        titulo("2. Bateria sem restrições", feito = bateriaOk)
        if (bateriaOk) {
            texto("Certo. A Samsung não vai fechar o serviço escondido.", 16f, DESTAQUE)
        } else {
            texto(
                "Sem isso, a Samsung fecha o serviço em segundo plano. Em informações do app, " +
                    "toque em Bateria e marque Sem restrições.",
                16f, TEXTO_2,
            )
            botao("Abrir informações do app", principal = ligado) { abrirInfoDoApp() }
        }

        banco()

        configuracoes(pausado)

        titulo("Modo detetive", feito = false)
        texto(
            "Só use se o bloqueio falhar depois de uma atualização do Instagram. Ele grava como as telas " +
                "são montadas, sem textos nem mensagens. Durante a gravação o bloqueio fica pausado.",
            16f, TEXTO_2,
        )
        if (!detetiveAberto) {
            botao("Mostrar o modo detetive", principal = false) { detetiveAberto = true; montar() }
            texto("Versão ${packageManager.getPackageInfo(packageName, 0).versionName}", 13f, TEXTO_2, topo = 32)
            return
        }
        texto(
            "Toque em Gravar: o Instagram abre. Faça o que está escrito e volte para cá " +
                "(botão de voltar ou de apps recentes). Cada gravação fecha sozinha em 3 minutos.",
            16f, TEXTO_2,
        )
        CENARIOS.forEachIndexed { i, c -> linhaCenario(i + 1, c, gravadas[c.id]) }

        texto(
            "Depois, toque em Enviar e mande o arquivo para o computador (Quick Share ou cabo USB), " +
                "na pasta 99. Input Dump.",
            16f, TEXTO_2, topo = 16,
        )
        botao("Enviar gravações", principal = gravadas.isNotEmpty()) { enviar() }
        if (gravadas.isNotEmpty()) {
            botao("Apagar gravações", principal = false) {
                AlertDialog.Builder(this)
                    .setMessage("Apagar todas as gravações?")
                    .setPositiveButton("Apagar") { _, _ -> Detetive.apagar(this); montar() }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        val versao = packageManager.getPackageInfo(packageName, 0).versionName
        texto("Versão $versao", 13f, TEXTO_2, topo = 32)
    }

    /** Nubank e outros bancos não abrem com o serviço ligado: o botão do painel ([Atalho]) resolve rápido. */
    private fun banco() {
        titulo("Banco não abre?", feito = false)
        texto(
            "O Nubank e outros bancos não abrem com o Sem Reels ligado. Use o botão Sem Reels no painel " +
                "que desce do topo da tela: um toque desliga o bloqueio. Depois do banco, toque nele de novo " +
                "e ligue o interruptor.",
            16f, TEXTO_2,
        )
        if (Build.VERSION.SDK_INT >= 33) {
            botao("Pôr o botão no painel", principal = false) { pedirBotaoNoPainel() }
        } else {
            texto(
                "Para pôr o botão: desça o painel duas vezes, toque no lápis (Editar) e arraste o Sem Reels para cima.",
                16f, TEXTO_2,
            )
        }
    }

    @RequiresApi(33)
    private fun pedirBotaoNoPainel() {
        getSystemService(StatusBarManager::class.java).requestAddTileService(
            ComponentName(this, Atalho::class.java),
            "Sem Reels",
            Icon.createWithResource(this, R.drawable.ic_atalho),
            mainExecutor,
        ) { r ->
            val msg = when (r) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "Pronto. O botão está no painel."
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "O botão já está no painel."
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> null
                else -> "Não deu. Desça o painel duas vezes, toque no lápis e arraste o Sem Reels para cima."
            }
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun configuracoes(pausado: Boolean) {
        titulo("Configurações", feito = false)
        val permitir = Config.permitirPausa(this)
        val chave = Switch(this).apply {
            text = "Permitir pausa"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTextColor(TEXTO)
            isChecked = permitir
            setOnCheckedChangeListener { _, marcado -> Config.mudarPermitirPausa(this@MainActivity, marcado); montar() }
        }
        conteudo.addView(chave, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) })
        if (!permitir) {
            texto("Desligada: o app não oferece pausa. Deixe assim se quiser menos tentação.", 15f, TEXTO_2)
            return
        }
        if (pausado) {
            texto("Pausado até ${horaFimDaPausa()}.", 15f, TEXTO_2)
            return
        }
        texto(
            "Libera tudo por 15 minutos. Para pausar, segure o botão por 10 segundos.",
            15f, TEXTO_2,
        )
        val segurar = BotaoSegurar(this, "Segure para pausar 15 min", 10_000L) {
            Config.pausar(this)
            Toast.makeText(this, "Pausado por 15 minutos.", Toast.LENGTH_SHORT).show()
            montar()
        }
        conteudo.addView(segurar, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(12) })
    }

    private fun horaFimDaPausa(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(Config.pausaAte(this)))

    private fun linhaCenario(numero: Int, c: Cenario, telas: Int?) {
        val caixa = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(SUPERFICIE)
            }
        }
        conteudo.addView(caixa, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        caixa.addView(rotulo("$numero. ${c.titulo}", 17f, TEXTO, negrito = true))
        caixa.addView(rotulo(c.instrucao, 15f, TEXTO_2).apply { setPadding(0, dp(4), 0, 0) })
        if (telas != null) {
            caixa.addView(rotulo("Gravado ($telas telas)", 15f, DESTAQUE).apply { setPadding(0, dp(6), 0, 0) })
        }
        caixa.addView(
            criarBotao(if (telas == null) "Gravar" else "Gravar de novo", principal = telas == null) { gravar(c) },
            LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(10) },
        )
    }

    private fun gravar(c: Cenario) {
        if (!servicoLigado()) {
            AlertDialog.Builder(this)
                .setMessage("Primeiro ligue o serviço (passo 1). Sem ele não dá para gravar.")
                .setPositiveButton("Ok", null)
                .show()
            return
        }
        val abrirInstagram = packageManager.getLaunchIntentForPackage(Detetive.INSTAGRAM)
        if (abrirInstagram == null) {
            Toast.makeText(this, "Não achei o Instagram neste celular.", Toast.LENGTH_LONG).show()
            return
        }
        Detetive.comecar(this, c)
        startActivity(abrirInstagram)
    }

    private fun enviar() {
        if (Detetive.gravadas(this).isEmpty()) {
            Toast.makeText(this, "Ainda não há gravações.", Toast.LENGTH_SHORT).show()
            return
        }
        val arquivo = Detetive.juntar(this)
        val uri = FileProvider.getUriForFile(this, "$packageName.arquivos", arquivo)
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, arquivo.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(envio, "Enviar gravações"))
    }

    private fun servicoLigado() = Servico.ligado(this)

    private fun abrirInfoDoApp() =
        abrir(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))

    private fun abrir(i: Intent) {
        try {
            startActivity(i)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Não consegui abrir essa tela.", Toast.LENGTH_SHORT).show()
        }
    }

    // ---- peças da tela ----

    private fun titulo(t: String, feito: Boolean) {
        texto(if (feito) "$t  ✓" else t, 21f, if (feito) DESTAQUE else TEXTO, negrito = true, topo = 32)
    }

    private fun texto(t: String, tamanho: Float, cor: Int, negrito: Boolean = false, topo: Int = 6) {
        conteudo.addView(
            rotulo(t, tamanho, cor, negrito),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(topo) },
        )
    }

    private fun rotulo(t: String, tamanho: Float, cor: Int, negrito: Boolean = false) = TextView(this).apply {
        text = t
        setTextSize(TypedValue.COMPLEX_UNIT_SP, tamanho)
        setTextColor(cor)
        setLineSpacing(0f, 1.25f)
        if (negrito) typeface = Typeface.DEFAULT_BOLD
    }

    private fun botao(t: String, principal: Boolean, acao: () -> Unit) {
        conteudo.addView(
            criarBotao(t, principal, acao),
            LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(12) },
        )
    }

    private fun criarBotao(t: String, principal: Boolean, acao: () -> Unit) = Button(this).apply {
        text = t
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        stateListAnimator = null
        setTextColor(if (principal) TEXTO_NO_DESTAQUE else TEXTO)
        background = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            if (principal) setColor(DESTAQUE) else {
                setColor(SUPERFICIE_2)
                setStroke(dp(1), BORDA)
            }
        }
        setOnClickListener { acao() }
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private companion object {
        const val FUNDO = 0xFF0F1216.toInt()
        const val SUPERFICIE = 0xFF171B21.toInt()
        const val SUPERFICIE_2 = 0xFF1F242C.toInt()
        const val BORDA = 0xFF2E3540.toInt()
        const val TEXTO = 0xFFE7E5E0.toInt()
        const val TEXTO_2 = 0xFF9AA1AB.toInt()
        const val DESTAQUE = 0xFFF0B03F.toInt()
        const val TEXTO_NO_DESTAQUE = 0xFF1A1408.toInt()
    }
}
