# VideoQuick Mobile — versão inicial

Aplicativo Android nativo em Kotlin + Jetpack Compose + Android WebView.

## Requisitos
- Android Studio recente (Ladybug ou mais novo recomendado)
- JDK 17
- Android SDK 35
- Internet para sincronizar as dependências Gradle

## Abrir e compilar
1. Extraia o ZIP.
2. No Android Studio, escolha **Open** e selecione a pasta `VideoQuickMobile`.
3. Aguarde a sincronização do Gradle.
4. Execute em um emulador/dispositivo Android.
5. Para criar APK: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. O APK de debug normalmente fica em `app/build/outputs/apk/debug/app-debug.apk`.

## O que já existe
- Navegação por HTTP/HTTPS no WebView
- Campo de endereço que também aceita pesquisas
- Voltar pelo botão físico do Android
- DownloadManager para downloads diretos disparados pelo WebView
- Barra inferior com Navegar, Downloads, Favoritos e Atualizar
- Histórico básico em memória durante a sessão
- Tema escuro com destaque roxo

## Limitações conhecidas desta primeira versão
- É um esqueleto funcional inicial, ainda não testado/compilado neste ambiente.
- Favoritos e histórico não são persistentes após fechar o app.
- A tela de downloads abre o painel informativo; o progresso real fica nas notificações/gerenciador de downloads do Android.
- Não extrai streams de redes sociais nem contorna DRM, autenticação ou restrições de plataforma. Para plataformas como YouTube, Instagram, TikTok e Facebook, use apenas mecanismos oficiais/autorizados.
- Antes de distribuir, revise políticas de privacidade, segurança, tratamento de cookies e requisitos de publicação.

## Próximas melhorias
- Persistência com Room (histórico e favoritos)
- Tela de downloads com DownloadManager.Query
- Gestão real de abas
- Tratamento de erros TLS e páginas não seguras
- Testes em dispositivos físicos e política de segurança de conteúdo
