package ar.esqr.g75macro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/** Botones de la notificación: Pausar/Activar y Reiniciar WiFi. */
public class ToggleReceiver extends BroadcastReceiver {
    static final String ALTERNAR = "ar.esqr.g75macro.ALTERNAR";
    static final String REPONER = "ar.esqr.g75macro.REPONER";
    static final String REINICIAR_WIFI = "ar.esqr.g75macro.REINICIAR_WIFI";

    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        if (ALTERNAR.equals(a)) {
            Prefs.setActivo(c, !Prefs.activo(c));
        } else if (REINICIAR_WIFI.equals(a)) {
            boolean ok = Acciones.reiniciarWifi(c, new Handler(Looper.getMainLooper()));
            Toast.makeText(c.getApplicationContext(),
                    ok ? "Reiniciando WiFi (3 s)…" : "Android no dejó reiniciar el WiFi",
                    Toast.LENGTH_SHORT).show();
        }
        MacroService.actualizar(c);
    }
}
