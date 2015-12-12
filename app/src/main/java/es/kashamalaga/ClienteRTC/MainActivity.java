package es.kashamalaga.ClienteRTC;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.splunk.mint.Mint;

import org.json.JSONException;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoRenderer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;

import static android.app.AlertDialog.Builder;

public class MainActivity extends Activity implements WebRTCCliente.RTCListener {
    private final static int VIDEO_CALL_SENT = 666;
    private final JoystickControl joystickControl = new JoystickControl(this);
    private final NuevaConexion nuevaConexion = new NuevaConexion(this);
    private final Preferencias preferencias = new Preferencias(this);
    private VideoStreamsView vsvRenderizadoStream;
    private WebRTCCliente client;
    private String mSocketAddress;
    private static boolean factoryStaticInitialized;
    private String callerId,callerId2;
    private String ip,puerto;
    final static String TAG = MainActivity.class.getCanonicalName();
    private long startTime;
    private boolean cargaweb=false;
    // Controles Display
    private Joysticks joystick;
    private Joysticks joystick2;

    // Opciones
    private TextView FPSTextView;
    private TextView angleTextView;
    private TextView powerTextView;
    private TextView directionTextView;
    private TextView DatosEnviotextView;

    private TextView BotonMostrarOcultarJoystick;
    private Switch switchFPS;
    private Switch switchDebug;
    private TextView GirotextView;
    private SeekBar seekGiroMaximo;
    private TextView CuellotextView;
    private SeekBar seekCuello;
    private TextView VelocidadtextView;
    private SeekBar seekVelocidad;

    // Variables de Velocidad y Angulo de Cuello
    public double velocidad_robot; // Quiere decir valor/1000 = 1m/s para trabajar con enteros
    public double giro_cuello;     // Hacemos esto porque seekBar solo trabaja con Int
    public double giro_maximo; // Controlamos el multiplicador para asistir al  giro
    public double progresValue;
    double progress = progresValue;

    private String respuesta;
    public static boolean conectado_servidor=false;
    public static boolean nueva_conexion=false;
    public boolean mostrar_joysticks=false;
    public  boolean mostrar_preferencias = false;
    public boolean mostrar_fps=false;
    public boolean mostrar_debug=false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        setContentView(R.layout.activity_main); // Cargamos plantilla Main


        CargarLibreriaNativaWebRTC();


        FPSTextView = (TextView) findViewById(R.id.FPSTextView);
        FPSTextView.setTypeface(null, Typeface.BOLD);

        switchFPS = (Switch) findViewById(R.id.switchFPS); /// Declaramos las opciones disponibles
        switchDebug = (Switch) findViewById(R.id.switchDebug);

        seekGiroMaximo = (SeekBar) findViewById(R.id.seekGiroMaximo);
        seekGiroMaximo.setProgress(seekGiroMaximo.getMax()/2);

        seekCuello = (SeekBar) findViewById(R.id.seekCuello);
        seekVelocidad= (SeekBar) findViewById(R.id.seekVelocidad);

        angleTextView = (TextView) findViewById(R.id.angleTextView);
        powerTextView = (TextView) findViewById(R.id.powerTextView);
        directionTextView = (TextView) findViewById(R.id.directionTextView);

        DatosEnviotextView = (TextView) findViewById(R.id.DatosEnviotextView);
        DatosEnviotextView.setTypeface(null, Typeface.BOLD);

        CuellotextView= (TextView) findViewById(R.id.CuellotextView);
        VelocidadtextView= (TextView) findViewById(R.id.VelocidadtextView);
        GirotextView= (TextView) findViewById(R.id.GirotextView);


        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if(conexionInternet(getApplicationContext()))
        {
            // Funcion para cargar desde una URL la IP y session
            cargarWeb2();
           // limpiarurl(); // Funcion para limpiar los datos recuperados de la web
            if(cargaweb) {
                limpiarurl2();
            }
        }
        else{
            Toast.makeText(getApplicationContext(), "No tienes conexion a internet", Toast.LENGTH_SHORT).show();

        }
       // clienteWebRTC();
        preferencias.CargarPreferencias(); // Cargar preferencias de Opciones

      /* Thread.setDefaultUncaughtExceptionHandler(
             new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread t, Throwable e) {
                    e.printStackTrace();
                        System.exit(-1);
                   }
               });*/



        Mint.disableNetworkMonitoring();
        Mint.initAndStartSession(this, "9e84fc32"); // Lanzamos el servicio de Debugging Mint


        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action))
        {
            final List<String> segments = intent.getData().getPathSegments();
            callerId = segments.get(0);
        }

        try {

            switchFPS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean isChecked) {
                    if (isChecked) {
                        Log.i(TAG, "Switch is currently ON");
                        FPSTextView.setVisibility(View.VISIBLE);
                        mostrar_fps = true;

                    } else {
                        Log.i(TAG, "Switch is currently OFF");
                        FPSTextView.setVisibility(View.INVISIBLE);
                        mostrar_fps = false;
                    }

                }
            });

            switchDebug.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean isChecked) {
                    if (isChecked) {
                        Log.i(TAG, "Switch is currently ON");
                        angleTextView.setVisibility(View.VISIBLE);
                        powerTextView.setVisibility(View.VISIBLE);
                        directionTextView.setVisibility(View.VISIBLE);
                        DatosEnviotextView.setVisibility(View.VISIBLE);

                        mostrar_debug = true;

                    } else {
                        Log.i(TAG, "Switch is currently OFF");
                        angleTextView.setVisibility(View.INVISIBLE);
                        powerTextView.setVisibility(View.INVISIBLE);
                        directionTextView.setVisibility(View.INVISIBLE);
                        DatosEnviotextView.setVisibility(View.INVISIBLE);

                        mostrar_debug = false;
                    }

                }
            });

            seekVelocidad.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override

                public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                    progress = progresValue;
                    //  Toast.makeText(getApplicationContext(), "Changing seekbar's progress", Toast.LENGTH_SHORT).show();
                    velocidad_robot = progress;
                }

                @Override

                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
                }

                @Override

                public void onStopTrackingTouch(SeekBar seekBar) {
                    Toast.makeText(getApplicationContext(), "Velocidad actual: " + String.format("%.2f", progress / 1000) + " m/s", Toast.LENGTH_SHORT).show();

                }
            });

            seekCuello.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override

                public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                    progress = progresValue;
                    giro_cuello = progress;
                }

                @Override

                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
                }

                @Override

                public void onStopTrackingTouch(SeekBar seekBar) {
                    Toast.makeText(getApplicationContext(), "Giro Actual Cuello: " + String.format("%.2f", progress / 1000) + " °", Toast.LENGTH_SHORT).show();
                }
            });

            seekGiroMaximo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override

                public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                    progress = progresValue;
                    if (progress <1000)
                        giro_maximo=1000;
                    else
                    giro_maximo = progress;
                }

                @Override

                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
                }

                @Override

                public void onStopTrackingTouch(SeekBar seekBar) {
                    Toast.makeText(getApplicationContext(), "Multiplicador Asistente al Giro: " + " x"+ String.format("%.2f", giro_maximo / 1000), Toast.LENGTH_SHORT).show();
                }
            });
        }
        catch (Exception e)
        {
            Log.i(TAG, "Error seekX OnCreate: " + e.getMessage());
        }

    } // Fin del OnCreate

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
       // setProgressBarIndeterminateVisibility(false);
        joystick = (Joysticks) findViewById(R.id.joystick);  // Declaramos y ocultamos Joysticks
        joystick.setVisibility(View.INVISIBLE);
        joystick2 = (Joysticks) findViewById(R.id.joystick2);
        joystick2.setVisibility(View.INVISIBLE);

        vsvRenderizadoStream = (VideoStreamsView) findViewById(R.id.vsvRenderizadoStream); //Declaramos  y ocultamos render de Streaming
        vsvRenderizadoStream.setVisibility(View.INVISIBLE);

        if (mostrar_fps)
        {
            FPSTextView.setVisibility(View.VISIBLE);

        }
        if (mostrar_debug) {
            angleTextView.setVisibility(View.VISIBLE);
            powerTextView.setVisibility(View.VISIBLE);
            directionTextView.setVisibility(View.VISIBLE);
            DatosEnviotextView.setVisibility(View.VISIBLE);

        }

        vsvRenderizadoStream.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
              //  mGesDetect.onTouchEvent(event);
               //// Log.i(TAG, "SwJIJIJIJIJION");
              //  preferencias(); // Mostramos u Ocultamos las Prefencias avanzadas

//constant for defining the time duration between the click that can be considered as double-tap
                 final int MAX_DURATION = 200;

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    startTime = System.currentTimeMillis();
                }
                else if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    if(System.currentTimeMillis() - startTime <= MAX_DURATION)
                    {
                        //DOUBLE TAP

                        if (Build.VERSION.SDK_INT >= 11) {
                            if (!mostrar_preferencias)
                                {
                                    getActionBar().show();


                                }
                            else
                                {
                                    getActionBar().hide();

                                   }
                            preferencias.preferencias();
                    }
                    }
                }
                return true;
            }


        });
    }

    private static void abortarSi(boolean condicion) {
        if (!condicion) {
            throw new RuntimeException("Fallo al inicializar las variables Globales");
        }
    }
    //public void onConfigurationChanged(Configuration newConfig)
    // {
    //   super.onConfigurationChanged(newConfig);
    //   setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    // }

    @Override
    public void onPause() {
        super.onPause();
        preferencias.GuardarPreferencias();
        vsvRenderizadoStream = (VideoStreamsView) findViewById(R.id.vsvRenderizadoStream);
        vsvRenderizadoStream.onPause();
        System.exit(-1);
    }

    @Override
    public void onResume() {
        super.onResume();
        preferencias.GuardarPreferencias();
        vsvRenderizadoStream = (VideoStreamsView) findViewById(R.id.vsvRenderizadoStream);
        vsvRenderizadoStream.onResume();

    }

    @Override
    public void onCallReady(String callId) {
        if (callerId == null) {
            //
            //
            //        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
            //        dlgAlert.setTitle("ClienteRTC Kashita");
            //        dlgAlert.setMessage("Esta aplicacion no puede lanzarse por si misma, necesita ser lanzada desde una URL");
            //        dlgAlert.setPositiveButton("OK",new DialogInterface.OnClickListener() {
            //            public void onClick(DialogInterface dialog, int whichButton) {
            //               // finish();


            // nuevaconn();  // LANZAR AUTOMATICAMENTE EL LOGIN SI HAY DATOS


            //            }
            //        });
            //        dlgAlert.setCancelable(true);
            //        dlgAlert.create().show();

            //call(callId);
        } else {
            // Log.i("LLAMAR", callerId);
            //    vsv.invalidate();
            // setContentView(vsv); // Mostrar Streaming
            vsvRenderizadoStream = (VideoStreamsView) findViewById(R.id.vsvRenderizadoStream);
            vsvRenderizadoStream.setVisibility(View.VISIBLE);

            answer(callerId);
            pintarFPS();
        }
    }

    void answer(String callerId)  {
        try {

            client.sendMessage(callerId, "init", null);
            nueva_conexion=true;
        }
        catch (JSONException e) {
            Log.i(TAG, "Error en Answer!");
            e.printStackTrace();
        }
        catch (Exception e)
        {
            Log.i(TAG, "Error en funcion answer2: " + e.getMessage());
           // Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        //     if (flag == 0)
        //    {
        try {
            startCam();
        } catch (Exception e)
        {
            Log.i(TAG, "Error en StartCam: " + e.getMessage());
          //  Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        //       flag=1;
        //  }
    }

    public void call(String callId) {
        Intent msg = new Intent(Intent.ACTION_SEND);
        msg.putExtra(Intent.EXTRA_TEXT, mSocketAddress + callId);
        msg.setType("text/plain");
        startActivityForResult(Intent.createChooser(msg, "Manda esta URL:"), VIDEO_CALL_SENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == VIDEO_CALL_SENT) {
            try {
                startCam();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    void startCam() throws JSONException {
        //setContentView(vsv);

        // Camera settings
      //  client.setCamera("front", "1280", "720");
        client.start();
    }

    @Override
    public void onStatusChanged(final String newStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,"Correcto!" + newStatus);
              //  Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // @Override
    // public void onLocalStream(MediaStream localStream) {
    //   localStream.videoTracks.get(0).addRenderer(new VideoRenderer(new VideoCallbacks(vsv, 0)));

    // }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
        //¡¡remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(new VideoCallbacks(vsv, endPoint)));
        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(new VideoCallbacks(vsvRenderizadoStream)));
        vsvRenderizadoStream.shouldDraw[endPoint] = false;
    }

    @Override
    public void onRemoveRemoteStream(MediaStream remoteStream, int endPoint) {
        remoteStream.videoTracks.get(0).dispose();
        vsvRenderizadoStream.shouldDraw[endPoint] = false;
    }

    // Implementation detail: bridge the VideoRenderer.Callbacks interface to the
    // VideoStreamsView implementation.
    private class VideoCallbacks implements VideoRenderer.Callbacks {
        private final VideoStreamsView view;
        private final int stream;

        public VideoCallbacks(VideoStreamsView view) {
            this.view = view;
            this.stream = 0;
        }

        @Override
        public void setSize(final int width, final int height) {
            view.queueEvent(new Runnable() {
                public void run() {
                    view.setSize(stream, width, height);
                }
            });
        }

        @Override
        public void renderFrame(VideoRenderer.I420Frame frame) {
            view.queueFrame(stream, frame);
        }
    }
    @Override
    public void onBackPressed() {
        if (mostrar_preferencias)
        {
            switchFPS.setVisibility(View.INVISIBLE);
            switchDebug.setVisibility(View.INVISIBLE);
            CuellotextView.setVisibility(View.INVISIBLE);
            GirotextView.setVisibility(View.INVISIBLE);
            seekGiroMaximo.setVisibility(View.INVISIBLE);
            seekCuello.setVisibility(View.INVISIBLE);
            VelocidadtextView.setVisibility(View.INVISIBLE);
            seekVelocidad.setVisibility(View.INVISIBLE);

            mostrar_preferencias = false;
        }
        else
        {
            preferencias.GuardarPreferencias(); // Guardamos que es gratis :P
            final AlertDialog show = new Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Salida")
                    .setMessage("¿Quieres cerrar la ventana?")
                    .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Mint.closeSession(MainActivity.this);
                            // wakeLock.release();
                            System.exit(-1);
                            //finish();
                        }
                    }).setNegativeButton("No", null).show();
        } // fin else
    } // fin pulsar atras
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_nuevo:
                if(!nueva_conexion) {
                    nuevaConexion.nuevaconn();
                }
                return true;
            case R.id.menu_habilitar_joystick:
                // findViewById(R.id.frameLayout1).setBackgroundColor(getResources().getColor(android.R.color.transparent));
                joystickControl.pintarmando(); // Mostramos u Ocultamos los Joysticks
                Log.i(TAG, "Mostrar mando!");
                return true;
            case R.id.menu_preferencias:
                preferencias.preferencias(); // Mostramos u Ocultamos las Prefencias avanzadas
                Log.i(TAG, "Preferencias!");
                return true;
            case R.id.menu_desconectar:
                Toast.makeText(getApplicationContext(), "Desconectando del servidor WebRTC", Toast.LENGTH_SHORT).show();
                WebRTCCliente.desconectar();
                Log.i(TAG, "Desconectar!");
                if (Build.VERSION.SDK_INT >= 11)
                        getActionBar().show();
                vsvRenderizadoStream.setVisibility(View.INVISIBLE);

                return true;
            case R.id.menu_acercade:
                Intent intentar = new Intent(this, CreditActivity.class);
                startActivity(intentar);
                Log.i(TAG, "Acerca de!");
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void pintarFPS() {
        Thread timer = new Thread() { //new thread
            public void run() {
                try {
                    do {
                        sleep(1000);
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                if(VideoStreamsView.fps>4.0)
                                {
                                    FPSTextView.setText("FPS: " + new DecimalFormat("##.##").format(VideoStreamsView.fps));
                                }

                            }
                        });
                    }
                    while (true);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        timer.start();
    }

    public static boolean conexionInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    public void CargarLibreriaNativaWebRTC() {
        final ProgressDialog ringProgressDialog = ProgressDialog.show(MainActivity.this, "Por favor espere ...", "Mientras que se descarga la informacion del servidor ...", true);
        ringProgressDialog.setCancelable(true);

        new MainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Here you should write your time consuming task...
                    // Let the progress ring for 10 seconds...
                    if (!factoryStaticInitialized)
                    {
                        abortarSi(PeerConnectionFactory.initializeAndroidGlobals(MainActivity.this, true, true));
                        factoryStaticInitialized = true;
                        //Thread.sleep(200);

                    }
                }
                catch (Exception e)
                {
                    Log.i(TAG, "Error en funcion Iniciar Libreria: " + e.getMessage());
                }
                ringProgressDialog.dismiss();
            }
        });
    }

    public void clienteWebRTC() {
        final ProgressDialog ringProgressDialog = ProgressDialog.show(MainActivity.this, "Por favor espere ...", "Mientras que se descarga la informacion del servidor ...", true);
        ringProgressDialog.setCancelable(true);

        new MainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try
                    {
                        if(!conectado_servidor)
                    client = new WebRTCCliente(MainActivity.this, mSocketAddress); // Lanzar sesion con los datos
                    }
                catch (Exception e)
                    {
                    Log.i(TAG, "No se pudo conectar con el cliente WebSocket: "+e.getMessage());
                     }
                ringProgressDialog.dismiss();
            }
        });
    }

    public void cargarWeb2() {
        final ProgressDialog ringProgressDialog = ProgressDialog.show(MainActivity.this, "Por favor espere ...", "Mientras que se descarga la informacion del servidor ...", true);
        ringProgressDialog.setCancelable(true);

        new MainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
    try {

        URL url = new URL("http://kasha-malaga.es/tfg.txt");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        InputStream in;
        try {
            urlConnection.setConnectTimeout(3000); // otro quebradero de cabeza esta tonteria
            urlConnection.setReadTimeout(3000);
            in = new BufferedInputStream(urlConnection.getInputStream());
            respuesta = readFully(in);
            cargaweb=true;
        } finally {
            urlConnection.disconnect();
        }
  //      Log.i(TAG, "WebRecuperada Respuesta: " + respuesta);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setText(respuesta);


    }
    catch (java.net.SocketTimeoutException e) {
        Toast.makeText(getApplicationContext(), "No se puede acceder al servidor para recuperar la URL..", Toast.LENGTH_SHORT).show();
    } catch (java.io.IOException e) {
        Log.i(TAG, "Error en funcion CargarWeb2: " + e.getMessage());
    }
    catch (Exception e)
    {
        Toast.makeText(getApplicationContext(), "No se puede acceder al servidor para recuperar la URL.."+e.getMessage(), Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Error en funcion CargarWeb2: " + e.getMessage());
    }
                ringProgressDialog.dismiss();
            }
        });
    }

    public String readFully(InputStream entityResponse) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = entityResponse.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toString();
    }

    public void limpiarurl2() {
        final ProgressDialog ringProgressDialog = ProgressDialog.show(MainActivity.this, "Por favor espere ...", "Mientras que se descarga la informacion del servidor ...", true);
        ringProgressDialog.setCancelable(true);

        new MainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
        try {
            int index = respuesta.indexOf("://");

            if (index != -1) {
                respuesta = respuesta.substring(index + 3);
                //System.out.println("URL = " + respuesta);
                int index2 = respuesta.indexOf("/");
                callerId2 = respuesta.substring(index2 + 1, index2 + 21);
               // System.out.println("SESION =" + callerId2);
            }

            index = respuesta.indexOf('/');
            if (index != -1) {
                respuesta = respuesta.substring(0, index);
            }
            respuesta = respuesta.replaceFirst("^www.*?\\.", "");
            String urlmaspuerto = respuesta;
            ip = urlmaspuerto.substring(0, urlmaspuerto.indexOf(':'));
            puerto = urlmaspuerto.replaceAll(ip + ":", "");
            //System.out.println("IP:" + ip + " Puerto:" + puerto);

            mSocketAddress = "http://" + ip;
            mSocketAddress += (":" + puerto + "/");
           // System.out.println(mSocketAddress);


        }catch (Exception e)
        {
            Log.i(TAG, "Error en funcion LimpiarURL: " + e.getMessage());
        }
                ringProgressDialog.dismiss();
            }
        });
    }

    public Joysticks getJoystick2() {
        return joystick2;
    }

    public TextView getPowerTextView() {
        return powerTextView;
    }

    public TextView getDatosEnviotextView() {
        return DatosEnviotextView;
    }

    public TextView getDirectionTextView() {
        return directionTextView;
    }

    public TextView getFPSTextView() {
        return FPSTextView;
    }

    public Joysticks getJoystick() {
        return joystick;
    }

    public TextView getBotonMostrarOcultarJoystick() {
        return BotonMostrarOcultarJoystick;
    }

    public TextView getAngleTextView() {
        return angleTextView;
    }

    public WebRTCCliente getClient() {
        return client;
    }

    public void setBotonMostrarOcultarJoystick(TextView botonMostrarOcultarJoystick) {
        BotonMostrarOcultarJoystick = botonMostrarOcultarJoystick;
    }

    public boolean isMostrar_joysticks() {
        return mostrar_joysticks;
    }

    public void setMostrar_joysticks(boolean mostrar_joysticks) {
        this.mostrar_joysticks = mostrar_joysticks;
    }

    public double getVelocidad_robot() {
        return velocidad_robot;
    }

    public double getGiro_maximo() {
        return giro_maximo;
    }

    public double getGiro_cuello() {
        return giro_cuello;
    }

    public String getCallerId() {
        return callerId;
    }

    public String getCallerId2() {
        return callerId2;
    }

    public VideoStreamsView getVsvRenderizadoStream() {
        return vsvRenderizadoStream;
    }

    public String getmSocketAddress() {
        return mSocketAddress;
    }

    public String getIp() {
        return ip;
    }

    public String getPuerto() {
        return puerto;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public void setVsvRenderizadoStream(VideoStreamsView vsvRenderizadoStream) {
        this.vsvRenderizadoStream = vsvRenderizadoStream;
    }

    public void setmSocketAddress(String mSocketAddress) {
        this.mSocketAddress = mSocketAddress;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPuerto(String puerto) {
        this.puerto = puerto;
    }

    public SeekBar getSeekCuello() {
        return seekCuello;
    }

    public Switch getSwitchFPS() {
        return switchFPS;
    }

    public TextView getCuellotextView() {
        return CuellotextView;
    }

    public Switch getSwitchDebug() {
        return switchDebug;
    }

    public TextView getVelocidadtextView() {
        return VelocidadtextView;
    }

    public SeekBar getSeekVelocidad() {
        return seekVelocidad;
    }

    public SeekBar getSeekGiroMaximo() {
        return seekGiroMaximo;
    }

    public TextView getGirotextView() {
        return GirotextView;
    }

    public void setMostrar_preferencias(boolean mostrar_preferencias) {
        this.mostrar_preferencias = mostrar_preferencias;
    }

    public boolean isMostrar_preferencias() {
        return mostrar_preferencias;
    }

    public boolean isMostrar_fps() {
        return mostrar_fps;
    }

    public boolean isMostrar_debug() {
        return mostrar_debug;
    }

    public void setMostrar_fps(boolean mostrar_fps) {
        this.mostrar_fps = mostrar_fps;
    }

    public void setMostrar_debug(boolean mostrar_debug) {
        this.mostrar_debug = mostrar_debug;
    }

    public void setVelocidad_robot(double velocidad_robot) {
        this.velocidad_robot = velocidad_robot;
    }

    public void setGiro_maximo(double giro_maximo) {
        this.giro_maximo = giro_maximo;
    }

    public void setGiro_cuello(double giro_cuello) {
        this.giro_cuello = giro_cuello;
    }
}






