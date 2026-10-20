package ar.esqr.g75macro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/**
 * Servicio fijo con notificación. Escucha el desbloqueo y la corriente.
 * Tiene que quedar vivo (aunque esté en Pausa) para enterarse de los eventos.
 */
public class MacroService extends Service {
    static final String CANAL = "macro";
    static final int ID_NOTIF = 1;
    /** Espera antes de cerrar, por si la corriente se corta un instante (ej. al dar arranque). */
    static final long ESPERA_DESENCHUFE_MS = 3000;

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable cierre = () -> {
        if (Prefs.activo(this) && !enchufado(this)) {
            Acciones.cerrarTodo(this, h);
        }
    };

    private final BroadcastReceiver receptor = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (a == null) return;
            switch (a) {
                case Intent.ACTION_USER_PRESENT:
                    if (Prefs.activo(c)) Acciones.abrirHeadunit(c);
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    h.removeCallbacks(cierre);
                    if (Prefs.activo(c)) h.postDelayed(cierre, ESPERA_DESENCHUFE_MS);
                    break;
                case Intent.ACTION_POWER_CONNECTED:
                    h.removeCallbacks(cierre); // volvió la corriente: no cerrar nada
                    break;
            }
        }
    };

    static void arrancar(Context c) {
        c.startForegroundService(new Intent(c, MacroService.class));
    }

    /** Redibuja la notificación (al cambiar Activado/Pausado). */
    static void actualizar(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null) {
            crearCanal(c);
            nm.notify(ID_NOTIF, armarNotificacion(c));
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        crearCanal(this);
        Notification n = armarNotificacion(this);
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ID_NOTIF, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(ID_NOTIF, n);
        }

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_USER_PRESENT);
        f.addAction(Intent.ACTION_POWER_DISCONNECTED);
        f.addAction(Intent.ACTION_POWER_CONNECTED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receptor, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receptor, f);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        actualizar(this);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        h.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(receptor);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static boolean enchufado(Context c) {
        Intent bat = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int p = bat == null ? 0 : bat.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        return p != 0;
    }

    private static void crearCanal(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CANAL) != null) return;
        NotificationChannel ch = new NotificationChannel(CANAL, "Macro del auto", NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Interruptor para activar o pausar la macro");
        ch.setShowBadge(false);
        nm.createNotificationChannel(ch);
    }

    private static Notification armarNotificacion(Context c) {
        boolean activo = Prefs.activo(c);
        int fl = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent abrirApp = PendingIntent.getActivity(c, 0,
                new Intent(c, MainActivity.class), fl);
        PendingIntent alternar = PendingIntent.getBroadcast(c, 1,
                new Intent(c, ToggleReceiver.class).setAction(ToggleReceiver.ALTERNAR), fl);
        // Si la deslizás para borrarla, vuelve a aparecer (así el botón nunca se pierde)
        PendingIntent reponer = PendingIntent.getBroadcast(c, 2,
                new Intent(c, ToggleReceiver.class).setAction(ToggleReceiver.REPONER), fl);

        String titulo = activo ? "Auto: ACTIVADO" : "Auto: PAUSADO";
        String texto = activo
                ? "Desbloquear abre Open Headunit · Desenchufar cierra todo"
                : "No hace nada. Tocá Activar para volver a usarlo en el auto";
        Notification.Action accion = new Notification.Action.Builder(
                null, activo ? "Pausar" : "Activar", alternar).build();

        return new Notification.Builder(c, CANAL)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setContentIntent(abrirApp)
                .setDeleteIntent(reponer)
                .addAction(accion)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }
}
