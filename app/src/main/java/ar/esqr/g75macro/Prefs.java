package ar.esqr.g75macro;

import android.content.Context;

/** Guarda si el método está Activado o Pausado. */
final class Prefs {
    private static final String ARCHIVO = "g75macro";
    private static final String ACTIVO = "activo";

    private Prefs() {}

    static boolean activo(Context c) {
        return c.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).getBoolean(ACTIVO, true);
    }

    static void setActivo(Context c, boolean v) {
        c.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit().putBoolean(ACTIVO, v).apply();
    }
}
