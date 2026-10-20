package ar.esqr.g75macro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Botón Pausar/Activar de la notificación. */
public class ToggleReceiver extends BroadcastReceiver {
    static final String ALTERNAR = "ar.esqr.g75macro.ALTERNAR";
    static final String REPONER = "ar.esqr.g75macro.REPONER";

    @Override
    public void onReceive(Context c, Intent i) {
        if (ALTERNAR.equals(i.getAction())) {
            Prefs.setActivo(c, !Prefs.activo(c));
        }
        MacroService.actualizar(c);
    }
}
