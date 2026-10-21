package io.github.jvhubert.semreels

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/**
 * Botão "Sem Reels" no painel de atalhos rápidos (o menu que desce do topo da tela).
 * Existe porque apps de banco (Nubank) recusam abrir com um serviço de Acessibilidade de fora da
 * Play Store ligado. Ligado: um toque desliga o serviço de verdade. Desligado: abre direto a tela
 * com o interruptor do Sem Reels, porque o Android não deixa um app religar a própria Acessibilidade.
 */
class Atalho : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        pintar(Servico.ligado(this))
    }

    override fun onClick() {
        super.onClick()
        val s = Servico.atual
        if (s != null) {
            s.disableSelf()
            pintar(false)
            Toast.makeText(this, "Sem Reels desligado. Pode abrir o banco.", Toast.LENGTH_LONG).show()
            return
        }
        if (isLocked) unlockAndRun { abrirInterruptor() } else abrirInterruptor()
    }

    /**
     * Abre o próprio app, que leva ao interruptor. Abrir as Configurações direto daqui falhava na
     * Samsung (o painel não deixava); de dentro do app, que está na frente, sempre abre.
     */
    private fun abrirInterruptor() {
        val i = Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.RELIGAR, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(i)
        }
    }

    private fun pintar(ligado: Boolean) {
        val t = qsTile ?: return
        t.state = if (ligado) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= 29) t.subtitle = if (ligado) "Bloqueando" else "Desligado"
        t.updateTile()
    }

    companion object {
        /** Tela com o interruptor do Sem Reels (Android 11 em diante). Se não abrir, use a lista geral. */
        fun intentDoInterruptor(ctx: Context): Intent =
            Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS")
                .putExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName(ctx, Servico::class.java).flattenToString())

        /** Pede ao painel para redesenhar o botão (quando o serviço liga ou desliga). */
        fun atualizar(ctx: Context) {
            try {
                requestListeningState(ctx, ComponentName(ctx, Atalho::class.java))
            } catch (_: RuntimeException) {
            }
        }
    }
}
