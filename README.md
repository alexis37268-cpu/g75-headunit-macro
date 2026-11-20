# G75 Macro

App chiquita para el Moto G75 que hace de pantalla de Android Auto en el Gol.

- **Desbloquear** → abre Open Headunit.
- **Desenchufar** (espera 3 s por si la corriente vuelve) → stop a la música, cierra Open Headunit completo (comando oficial `ACTION_STOP_SERVICE`, v3.4.0+), va a inicio y apaga la pantalla.
- **Desenchufar** también **reinicia el WiFi**: destraba el Wi-Fi Direct para que el próximo viaje conecte de cero.
- **Reiniciar WiFi** a mano desde la notificación, si Open Headunit se queda en "conectando".
- **Pausar / Activar** desde la notificación o desde la app, para usar el celu para otra cosa.
- Arranca sola al prender el celu. Sin root ni Shizuku.

## Instalar
Cada push compila el APK con GitHub Actions. Bajalo de **Releases → ultima → g75-macro.apk** desde el G75.

Al abrirla la primera vez, dale los permisos que marca la pantalla (una sola vez).

`firma.txt` es la firma fija: no la borres, o las actualizaciones no van a instalar encima.

## Por qué targetSdk 28
Desde Android 10, `setWifiEnabled()` no hace nada en las apps que apuntan a la API 29 o más nueva.
Las que apuntan a la 28 o anterior todavía pueden apagar/prender el WiFi sin root. Por eso esta app
se compila con `targetSdk 28` (Android puede mostrar un aviso de "app para una versión anterior": es normal).
