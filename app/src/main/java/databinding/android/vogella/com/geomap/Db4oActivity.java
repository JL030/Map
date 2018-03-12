package databinding.android.vogella.com.geomap;

import android.content.Intent;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.config.AndroidSupport;
import com.db4o.config.EmbeddedConfiguration;
import com.db4o.ext.Db4oException;
import com.db4o.query.Predicate;
import com.db4o.query.Query;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

// pestaña proyect
// copiar y pegar en libs db4o-8.0.276.16149-all-java5.jar
// add as library
public class Db4oActivity extends AppCompatActivity {

    private static final String TAG = Db4oActivity.class.getSimpleName();

    private ObjectContainer objectContainer;
    private ArrayList<Date> dates = new ArrayList<>();
    private ArrayList<Location> locations = new ArrayList<>();
    private ListView lista;
    private FusedLocationProviderClient clienteLocalizacion;

    // Este método obtiene la configuración
    public EmbeddedConfiguration getDb4oConfig() throws IOException {
        EmbeddedConfiguration configuration = Db4oEmbedded.newConfiguration();
        configuration.common().add(new AndroidSupport());
        configuration.common().objectClass(Localizacion.class).
                objectField("fecha").indexed(true);
        return configuration;
    }

    private void init(){

        Intent intent = getIntent();

        Location localizacion = (Location) intent.getExtras().get("localizacion");
        Localizacion localNew = new Localizacion(localizacion, new GregorianCalendar(2018, 1, 22).getTime());

        Log.v("LOCAL", "localizacion " + localizacion);

        guardar(localNew);

        lista = findViewById(R.id.listView);
        objectContainer = openDataBase("nuevo1.db4o");
        dates = getDates();
        Log.v("FECHAS", "Fecha devuelta " + dates.toString());
        Log.v("FECHAS", "Localization devuelta " + locations.toString());
        objectContainer.close();
        ArrayAdapter<Date> adaptador = new ArrayAdapter<Date>(this, android.R.layout.simple_list_item_1, dates);
        lista.setAdapter(adaptador);
        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Localizacion localizacion = new Localizacion();
                localizacion.setLocalizacion(locations.get(position));
                //clienteLocalizacion = LocationServices.
            }
        });
        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Localizacion local = new Localizacion();

                local = (Localizacion) findLocation();

                Intent i = new Intent(Db4oActivity.this, MapsActivity.class);
                i.putExtra("localizacion", local.getLocalizacion());
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db4o);
        startService(new Intent(this, LocationService.class));
        init();

       /* Localizacion loc = new Localizacion();
        objectContainer.store(loc); // Creo objeto
        objectContainer.commit(); // Guardo objeto

        loc = new Localizacion(new Location("provider"));
        objectContainer.store(loc);
        objectContainer.commit();

        loc = new Localizacion(new Location("proveedor"), new GregorianCalendar(2018, 1, 22).getTime());
        objectContainer.store(loc);
        objectContainer.commit();*/





        // La base de datos siempre hay que cerrarla o pierdes datos
    }

    // Este método abre la base de datos
    private ObjectContainer openDataBase(String archivo) {
        ObjectContainer objectContainer = null;
        try {
            String name = getExternalFilesDir(null) + "/" + archivo;
            objectContainer = Db4oEmbedded.openFile(getDb4oConfig(), name);
        } catch (IOException e) {
            Log.v(TAG, e.toString());
        }
        return objectContainer;
    }

    public void borrar(Object obj) throws Db4oException {
        objectContainer.delete(obj);
    }

    private ArrayList<Date> getDates(){
        // Lanzar consultas
        Query consulta = objectContainer.query();
        consulta.constrain(Localizacion.class);
        ObjectSet<Localizacion> localizaciones = consulta.execute();
        for (Localizacion localizacion : localizaciones) {
            dates.add(localizacion.getFecha());
            locations.add(localizacion.getLocalizacion());
            Log.v("xyxy", "1: " + localizacion.getFecha().toString());
        }
        return dates;
    }

    private void guardar(Localizacion localizacion) throws Db4oException{
        objectContainer = openDataBase("ejemplo1.db4o");
        objectContainer.store(localizacion); // Creo objeto
        objectContainer.commit(); // Guardo objeto
        objectContainer.close();
    }
    private ObjectSet<Localizacion> findLocation(){
        ObjectSet<Localizacion> locs = objectContainer.query(
                // Aquí creamos consulta y le pasamos la fecha que queramos
                new Predicate<Localizacion>() {
                    @Override
                    public boolean match(Localizacion loc) {
                        return loc.getFecha().equals(dates);
                    } // Mes 1 en java es febrero
                });
        for (Localizacion localizacion : locs) {
            Log.v(TAG, "2: " + localizacion.toString());
        }
        return locs;
    }

}
