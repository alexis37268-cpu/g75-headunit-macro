package ar.esqr.g75macro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Arranca el servicio solo al prender el celu (y al actualizar la app). */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        try {
            MacroService.arrancar(c);
        } catch (Exception e) {
            Log.e(Acciones.TAG, "No pude arrancar el servicio al inicio", e);
        }
    }
}
