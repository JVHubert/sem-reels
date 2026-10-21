# Sem Reels

App Android que tira os Reels aleatórios do Instagram, mas deixa assistir os Reels que os
amigos mandam no chat.

- Usa o serviço de Acessibilidade do Android, limitado ao Instagram.
- Não tem permissão de internet: nada sai do celular.
- Versão 0.2: **bloqueia**. Cobre a grade de vídeos da lupinha e da busca, fecha a prévia do
  toque longo e volta de Reels abertos pela lupinha, pela busca ou por hashtag. Reels do chat,
  do feed, dos stories e dos perfis tocam, um de cada vez.
- Versão 0.3: botão **Sem Reels no painel de atalhos rápidos**. Bancos como o Nubank não abrem
  com um serviço de Acessibilidade de fora da Play Store ligado; um toque no botão desliga o
  serviço, e outro abre direto o interruptor para religar.
- Os nomes internos das telas do Instagram ficam em `app/src/main/assets/regras.json`.
- Modo detetive (escondido no fim da tela): grava como as telas são montadas, sem textos e sem
  mensagens, para recalibrar quando o Instagram mudar. `ferramentas/reduzir_gravacao.py`
  transforma a gravação em teste automático.

Baixar: [sem-reels.apk](https://github.com/JVHubert/sem-reels/releases/download/ultima/sem-reels.apk)

Licença MIT.
