package databinding.android.vogella.com.geomap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.GregorianCalendar;

/*TAREA A REALIZAR
* 1º Actividad -> permisos + resolver problemas
* 2º Servicio -> foreground, geolocalizar, guardar
* 3º Actividad -> Ruta de un día dado
* */
public class GPSActivity extends AppCompatActivity {
    // ESTA APLICACIÓN TIENE PERMISOS IMPLEMENTADOS COMO HAY QUE HACERLO
    private static final String TAG = "xyzyx";
    private static final int PERMISO_LOCATION = 1;
    private static final int RESOLVE_RESULT = 2;

    private FusedLocationProviderClient clienteLocalizacion;
    private LocationCallback callbackLocalizacion;
    private LocationRequest peticionLocalizacion;
    private LocationSettingsRequest ajustesPeticionLocalizacion;
    private SettingsClient ajustesCliente;
    private Location localizacion;

    private boolean checkPermissions() {
        int estadoPermisos = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return estadoPermisos == PackageManager.PERMISSION_GRANTED;
    }

    private void init() {
        if (checkPermissions()) {
            // startService(new Intent(this, LocationService.class));
            startLocations();
        } else {
            requestPermissions();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Resolver problemas en startLocations
        switch (requestCode) {
            case RESOLVE_RESULT:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.v(TAG, "Permiso ajustes localización");
                        startLocations();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.v(TAG, "Sin permiso ajustes localización");
                        break;
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);
        init();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && requestCode == PERMISO_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocations();
                // Si finalmente das permiso empieza la aplicación
            }
        }
    }

    private void requestPermissions() {
        boolean solicitarPermiso = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (solicitarPermiso) {
            Log.v(TAG, "Explicación racional del permiso");
            showSnackbar(R.string.app_name, android.R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityCompat.requestPermissions(GPSActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISO_LOCATION);
                }
            });
        } else {
            Log.v(TAG, "Solicitando permiso");
            ActivityCompat.requestPermissions(GPSActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISO_LOCATION);
        }
    }

    private void showSnackbar(final int idTexto, final int textoAccion,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(idTexto),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(textoAccion), listener).show();
    }

    // Como los permisos ya están otorgados si entras aquí le quitamos la solicitud de permisos
    @SuppressLint("MissingPermission")
    private void startLocations() {
        // clienteLocalizacion es el objeto encargado de obtener las geolocalizaciones
        clienteLocalizacion = LocationServices.getFusedLocationProviderClient(this);
        // ajustesCliente se encarga de como va a obtenerlas (intervalo, precisión, etc.)
        ajustesCliente = LocationServices.getSettingsClient(this);
        clienteLocalizacion.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null. En las máquinas virtuales suele ser null la primera vez
                if (location != null) {
                    Log.v(TAG, "última localización: " + location.toString());
                } else {
                    Log.v(TAG, "no hay última localización");
                }
            }
        });
        // Aquí obtienes la localización, como puede tardar más o menos la haces con callback
        callbackLocalizacion = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // AQUÍ se recogen los datos y hay que guardarlos en una base de datos
                localizacion = locationResult.getLastLocation();
                Intent i = new Intent(GPSActivity.this, Db4oActivity.class);
                i.putExtra("localizacion", localizacion);
                startActivity(i);

            }
        };
        // Aquí preparamos el objeto que va a lanzar la geolocalización
        peticionLocalizacion = new LocationRequest();
        peticionLocalizacion.setInterval(10000); // cada 10 segundos, en nuestra app poner 30
        peticionLocalizacion.setFastestInterval(5000); // si en este intervalo otra app ha obtenido una geolocalización la aprovechas
        peticionLocalizacion.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // máxima precisión posible en geolocalizaciones
        // Construir y lanzar cliente de peticiones, dependiendo del móvil podría no funcionar (No GPS o desactivado)
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(peticionLocalizacion);
        ajustesPeticionLocalizacion = builder.build();

        ajustesCliente.checkLocationSettings(ajustesPeticionLocalizacion)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    // Este se lanza si todo va bien
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.v(TAG, "Se cumplen todos los requisitos");
                        // Callback nos da la nueva geolocalización
                        clienteLocalizacion.requestLocationUpdates(peticionLocalizacion, callbackLocalizacion, null);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                // Error subsanable
                                Log.v(TAG, "Falta algún requisito, intento de adquisición");
                                try {
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(GPSActivity.this, RESOLVE_RESULT);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.v(TAG, "No se puede adquirir.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                // Error no subsanable
                                Log.v(TAG, "Falta algún requisito, que no se puede adquirir.");
                        }
                    }
                });
    }
}
