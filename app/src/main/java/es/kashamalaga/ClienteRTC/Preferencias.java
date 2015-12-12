package es.kashamalaga.ClienteRTC;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;

public class Preferencias {
    private final MainActivity mainActivity;

    public Preferencias(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    void preferencias() {
        if (!mainActivity.isMostrar_preferencias()) {
            mainActivity.setMostrar_preferencias(true);
            try {
                mainActivity.getSwitchFPS().setVisibility(View.VISIBLE);
                mainActivity.getSwitchDebug().setVisibility(View.VISIBLE);

                mainActivity.getCuellotextView().setVisibility(View.VISIBLE);
                mainActivity.getGirotextView().setVisibility(View.VISIBLE);
                mainActivity.getSeekGiroMaximo().setVisibility(View.VISIBLE);
                mainActivity.getSeekCuello().setVisibility(View.VISIBLE);
                mainActivity.getVelocidadtextView().setVisibility(View.VISIBLE);
                mainActivity.getSeekVelocidad().setVisibility(View.VISIBLE);

                mainActivity.getSeekGiroMaximo().setProgress((int) (mainActivity.getGiro_maximo()));
                mainActivity.getSeekCuello().setProgress((int) (mainActivity.getGiro_cuello()));
                mainActivity.getSeekVelocidad().setProgress((int) (mainActivity.getVelocidad_robot()));
            } catch ( Exception e ) {
                Log.i(MainActivity.TAG, "Error en Preferencias: " + e.getMessage());

            }


            if (mainActivity.isMostrar_fps()) {
                mainActivity.getSwitchFPS().setChecked(true);
            }
            if (mainActivity.isMostrar_debug()) {
                mainActivity.getSwitchDebug().setChecked(true);
            }

        } else {
            try {
                mainActivity.getSwitchFPS().setVisibility(View.INVISIBLE);
                mainActivity.getSwitchDebug().setVisibility(View.INVISIBLE);
                mainActivity.getCuellotextView().setVisibility(View.INVISIBLE);
                mainActivity.getGirotextView().setVisibility(View.INVISIBLE);
                mainActivity.getSeekGiroMaximo().setVisibility(View.INVISIBLE);
                mainActivity.getSeekCuello().setVisibility(View.INVISIBLE);
                mainActivity.getVelocidadtextView().setVisibility(View.INVISIBLE);
                mainActivity.getSeekVelocidad().setVisibility(View.INVISIBLE);
            } catch ( Exception e ) {
                Log.i(MainActivity.TAG, "Error en la carga de: MainLayout: " + e.getMessage());
                //Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            mainActivity.setMostrar_preferencias(false);
        }
    }

    public void CargarPreferencias() {

        SharedPreferences settings = mainActivity.getPreferences(Activity.MODE_PRIVATE);


        mainActivity.setMostrar_fps(settings.getBoolean("mostrar_fps", mainActivity.isMostrar_fps()));
        mainActivity.setMostrar_debug(settings.getBoolean("mostrar_debug", mainActivity.isMostrar_debug()));
        mainActivity.setVelocidad_robot(Double.parseDouble(settings.getString("velocidad_robot", String.valueOf(mainActivity.getVelocidad_robot()))));
        mainActivity.setGiro_maximo(Double.parseDouble(settings.getString("giro_maximo", String.valueOf(mainActivity.getGiro_maximo()))));
        mainActivity.setGiro_cuello(Double.parseDouble(settings.getString("giro_cuello", String.valueOf(mainActivity.getGiro_cuello()))));
    }

    public void GuardarPreferencias() {
        SharedPreferences settings = mainActivity.getPreferences(Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean("mostrar_fps", mainActivity.isMostrar_fps());
        editor.putBoolean("mostrar_debug", mainActivity.isMostrar_debug());
        editor.putString("velocidad_robot", String.valueOf(mainActivity.getVelocidad_robot()));
        editor.putString("giro_maximo", String.valueOf(mainActivity.getGiro_maximo()));
        editor.putString("giro_cuello", String.valueOf(mainActivity.getGiro_cuello()));

        editor.apply();
    }
}