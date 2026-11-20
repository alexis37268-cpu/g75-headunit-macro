package ar.esqr.g75macro;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;

/** Las dos macros: abrir Open Headunit y cerrar todo. */
final class Acciones {
    static final String TAG = "G75Macro";

    static final String OHU = "com.andrerinas.headunitrevived";
    static final String OHU_RECEPTOR = "com.andrerinas.openheadunit.automation.AutomationReceiver";
    static final String OHU_SALIR = "com.andrerinas.openheadunit.ACTION_STOP_SERVICE";
    static final String YT_MUSIC = "com.google.android.apps.youtube.music";

    private Acciones() {}

    static boolean headunitInstalado(Context c) {
        return c.getPackageManager().getLaunchIntentForPackage(OHU) != null;
    }

    /** Macro 1: al desbloquear, abrir Open Headunit. */
    static void abrirHeadunit(Context c) {
        Intent i = c.getPackageManager().getLaunchIntentForPackage(OHU);
        if (i == null) {
            Log.w(TAG, "Open Headunit no está instalado");
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            c.startActivity(i);
        } catch (Exception e) {
            Log.e(TAG, "No pude abrir Open Headunit", e);
        }
    }

    /**
     * Macro 2: al desenchufar.
     * Stop a la música → cerrar Open Headunit completo → inicio → apagar pantalla.
     */
    static void cerrarTodo(Context c, Handler h) {
        Context app = c.getApplicationContext();

        // 1) Música: pausa y stop a lo que esté sonando (YouTube Music o el reproductor que sea)
        teclaMultimedia(app, KeyEvent.KEYCODE_MEDIA_PAUSE);
        h.postDelayed(() -> teclaMultimedia(app, KeyEvent.KEYCODE_MEDIA_STOP), 300);

        // 2) Open Headunit: comando oficial de salida (corta sesión, WiFi y servicios, y cierra la app)
        h.postDelayed(() -> {
            try {
                Intent salir = new Intent(OHU_SALIR);
                salir.setComponent(new ComponentName(OHU, OHU_RECEPTOR));
                app.sendBroadcast(salir);
            } catch (Exception e) {
                Log.e(TAG, "No pude mandar la salida a Open Headunit", e);
            }
        }, 500);

        // 3) Pantalla de inicio
        h.postDelayed(() -> {
            Intent inicio = new Intent(Intent.ACTION_MAIN);
            inicio.addCategory(Intent.CATEGORY_HOME);
            inicio.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                app.startActivity(inicio);
            } catch (Exception e) {
                Log.e(TAG, "No pude ir a inicio", e);
            }
        }, 1500);

        // 4) Rematar lo que haya quedado en segundo plano
        h.postDelayed(() -> {
            ActivityManager am = (ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(OHU);
                am.killBackgroundProcesses(YT_MUSIC);
            }
        }, 2500);

        // 5) Apagar la pantalla
        h.postDelayed(() -> {
            DevicePolicyManager dpm = (DevicePolicyManager) app.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm != null && dpm.isAdminActive(new ComponentName(app, AdminReceiver.class))) {
                dpm.lockNow();
            } else {
                Log.w(TAG, "Falta el permiso de administrador para apagar la pantalla");
            }
        }, 3000);

        // 6) Reiniciar el WiFi: limpia el Wi-Fi Direct trabado para que el próximo viaje arranque de cero
        h.postDelayed(() -> reiniciarWifi(app, h), 3500);
    }

    /**
     * Apaga el WiFi, espera 3 s y lo prende. Es lo mismo que hace Open Headunit con root
     * ("svc wifi disable/enable") para destrabar el Wi-Fi Direct.
     * Sin root anda porque esta app declara targetSdk 28: Android 10+ solo le niega
     * setWifiEnabled() a las apps que apuntan a la API 29 o más nueva.
     * Devuelve false si Android no lo dejó.
     */
    @SuppressWarnings("deprecation")
    static boolean reiniciarWifi(Context c, Handler h) {
        WifiManager wifi = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi == null) return false;
        boolean ok;
        try {
            ok = wifi.setWifiEnabled(false);
        } catch (Exception e) {
            Log.e(TAG, "No pude apagar el WiFi", e);
            return false;
        }
        if (!ok) {
            Log.w(TAG, "Android no dejó apagar el WiFi");
            return false;
        }
        h.postDelayed(() -> prenderWifi(c), 3000);
        return true;
    }

    /** Prende el WiFi si está apagado (al enchufar, por las dudas). */
    @SuppressWarnings("deprecation")
    static void prenderWifi(Context c) {
        WifiManager wifi = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi == null || wifi.isWifiEnabled()) return;
        try {
            if (!wifi.setWifiEnabled(true)) Log.w(TAG, "Android no dejó prender el WiFi");
        } catch (Exception e) {
            Log.e(TAG, "No pude prender el WiFi", e);
        }
    }

    private static void teclaMultimedia(Context c, int codigo) {
        AudioManager audio = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) return;
        long t = SystemClock.uptimeMillis();
        audio.dispatchMediaKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, codigo, 0));
        audio.dispatchMediaKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_UP, codigo, 0));
    }
}
