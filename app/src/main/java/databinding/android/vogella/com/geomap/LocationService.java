package databinding.android.vogella.com.geomap;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

// aunque se cierre la aplicación el servicio seguirá guardando geolocalización
/* Existen 2 tipos de servicios
* Servicios normales guardarían la geolocalización cuando le diese la gana, low battery
* Necesitamos un servicio Foreground*/
public class LocationService extends Service {
    public LocationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        // Obligar al servicio a ser Foreground
        super.onCreate();
        Intent i = new Intent(this, LocationService.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Notification.Builder constructorNotificacion = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("notificación servicio")
                .setContentText("texto servicio")
                .setContentIntent(PendingIntent.getActivity(this, 0, i, 0));
        startForeground(1, constructorNotificacion.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Aquí hay que hacer toda la geolocalización de la clase principal
        // excepto permisos y resolver problemas
        return START_STICKY;
        // Si en algún momento matas al servicio, cuando se pueda revívelo
    }
}
