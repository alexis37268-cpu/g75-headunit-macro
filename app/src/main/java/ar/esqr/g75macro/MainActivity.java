package ar.esqr.g75macro;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Pantalla única: interruptor, permisos y botones de prueba. */
public class MainActivity extends Activity {
    private LinearLayout raiz;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#1F2A36"));
        raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20);
        raiz.setPadding(p, p, p, p);
        scroll.addView(raiz);
        setContentView(scroll);

        // Con targetSdk 28 Android pide el permiso de notificaciones solo, al crear el canal
        MacroService.arrancar(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dibujar();
    }

    @Override
    public void onRequestPermissionsResult(int rc, String[] perms, int[] res) {
        MacroService.actualizar(this);
        dibujar();
    }

    private void dibujar() {
        raiz.removeAllViews();
        boolean activo = Prefs.activo(this);

        titulo("G75 Macro");
        texto("Desbloquear → abre Open Headunit.\n"
                + "Desenchufar (3 s) → stop a la música, cierra Open Headunit, va a inicio, apaga la pantalla "
                + "y reinicia el WiFi (así el Wi-Fi Direct arranca limpio en el próximo viaje).");

        Button interruptor = boton(activo ? "ACTIVADO  (tocá para pausar)" : "PAUSADO  (tocá para activar)");
        interruptor.setBackgroundColor(Color.parseColor(activo ? "#2E7D32" : "#6D4C41"));
        interruptor.setOnClickListener(v -> {
            Prefs.setActivo(this, !Prefs.activo(this));
            MacroService.actualizar(this);
            dibujar();
        });

        subtitulo("Permisos (una sola vez)");
        if (!Acciones.headunitInstalado(this)) {
            aviso("⚠ No encuentro Open Headunit instalado (necesita la v3.4.0 o más nueva).");
        }
        permiso("Notificaciones (para los botones de pausa y WiFi)",
                notificacionesActivas(),
                () -> startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
        permiso("Mostrar sobre otras apps (para poder abrir Open Headunit)",
                Settings.canDrawOverlays(this),
                () -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()))));
        permiso("Administrador del dispositivo (para apagar la pantalla)",
                adminActivo(),
                () -> {
                    Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this, AdminReceiver.class));
                    i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Solo se usa para apagar la pantalla al desenchufar.");
                    startActivity(i);
                });
        permiso("Batería sin restricciones (para que Android no la mate)",
                sinOptimizacion(),
                () -> startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + getPackageName()))));

        subtitulo("Probar");
        boton("Probar: abrir Open Headunit").setOnClickListener(v -> Acciones.abrirHeadunit(this));
        boton("Probar: cierre (como si desenchufaras)").setOnClickListener(v -> {
            Toast.makeText(this, "Cerrando todo…", Toast.LENGTH_SHORT).show();
            Acciones.cerrarTodo(this, h);
        });
        boton("Probar: reiniciar WiFi").setOnClickListener(v -> {
            boolean ok = Acciones.reiniciarWifi(this, h);
            Toast.makeText(this, ok ? "WiFi apagado, se prende en 3 s…"
                    : "Android no dejó reiniciar el WiFi", Toast.LENGTH_LONG).show();
        });

        texto("\nPara desinstalar la app, primero sacale el permiso de administrador "
                + "en Ajustes → Seguridad → Apps de administración del dispositivo.");
    }

    private boolean notificacionesActivas() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        return nm != null && nm.areNotificationsEnabled();
    }

    private boolean adminActivo() {
        DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);
        return dpm != null && dpm.isAdminActive(new ComponentName(this, AdminReceiver.class));
    }

    private boolean sinOptimizacion() {
        PowerManager pm = getSystemService(PowerManager.class);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void permiso(String nombre, boolean ok, Runnable pedir) {
        if (ok) {
            texto("✓ " + nombre);
        } else {
            boton("Falta: " + nombre).setOnClickListener(v -> pedir.run());
        }
    }

    private void titulo(String s) {
        TextView t = texto(s);
        t.setTextSize(26);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(Color.parseColor("#FFB74D"));
    }

    private void subtitulo(String s) {
        TextView t = texto(s);
        t.setTextSize(18);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(18), 0, dp(4));
    }

    private void aviso(String s) {
        texto(s).setTextColor(Color.parseColor("#FF8A65"));
    }

    private TextView texto(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(15);
        t.setTextColor(Color.parseColor("#ECEFF1"));
        t.setPadding(0, dp(4), 0, dp(4));
        raiz.addView(t);
        return t;
    }

    private Button boton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.parseColor("#37474F"));
        b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        b.setLayoutParams(lp);
        raiz.addView(b);
        return b;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
