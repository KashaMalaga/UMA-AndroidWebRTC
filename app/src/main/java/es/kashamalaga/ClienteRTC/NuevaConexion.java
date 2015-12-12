package es.kashamalaga.ClienteRTC;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class NuevaConexion {
    private final MainActivity mainActivity;

    public NuevaConexion(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    void nuevaconn() {

        AlertDialog.Builder alert = new AlertDialog.Builder(mainActivity);
        final EditText input = new EditText(mainActivity);
        alert.setView(input);
        // ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        //input.setText(clipboard.getText());

        ClipboardManager clipboard = (ClipboardManager) mainActivity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            ClipDescription description = clipboard.getPrimaryClipDescription();
            ClipData data = clipboard.getPrimaryClip();
            if (data != null && description != null && description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
                input.setText(String.valueOf(data.getItemAt(0).getText()));
        }
        // String out;
//        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                input.setText("canceled");
//            }
//        });

        // FPSTextView = (TextView) findViewById(R.id.FPSTextView);
        // FPSTextView.setVisibility(View.INVISIBLE);

        AlertDialog.Builder ok = alert.setNeutralButton("Conectar", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mainActivity.clienteWebRTC(); // Intentamos conectar el WebSocket

                String toSend;
                toSend = input.getText().toString();
                String dominio = toSend; // AQUI HE CAMBIADO ALGO RARO QUE NO RECUERDO PORQUE ESTABA ASI

                if (toSend.length() < 15) {
                    AlertDialog dlgAlert = new AlertDialog.Builder(mainActivity).create();
                    dlgAlert.setTitle("ClienteRTC Kashita");
                    dlgAlert.setMessage("La URL introducida o la sesion no es valida");

                    dlgAlert.setButton("OK", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {

                            // Toast.makeText(getApplicationContext(), "You clicked on OK", Toast.LENGTH_SHORT).show();
                            // return;
                        }
                    });

                    dlgAlert.show();
                } else if (toSend.length() == 20) {
                    try {
                        mainActivity.getVsvRenderizadoStream().invalidate();
                        mainActivity.setContentView(mainActivity.getVsvRenderizadoStream());
                        // clienteWebRTC();

                        mainActivity.answer(toSend);
                    } catch ( Exception e ) {
                        Log.i(MainActivity.TAG, "Error Answer Longitud 20: " + e.getMessage());
                        //Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    mainActivity.pintarFPS();
                } else {
                    int index = dominio.indexOf("://");

                    if (index != -1) {
                        try {
                            dominio = dominio.substring(index + 3);
                            System.out.println("URL = " + dominio);
                            int index2 = dominio.indexOf("/");
                            mainActivity.setCallerId(dominio.substring(index2 + 1, index2 + 21));
                            System.out.println("SESION =" + mainActivity.getCallerId());
                        } catch ( Exception e ) {
                            Log.i(MainActivity.TAG, "Error en funcion nuevaconn : " + e.getMessage());
                            //Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    index = dominio.indexOf('/');
                    if (index != -1) {
                        dominio = dominio.substring(0, index);
                    }
                    try {
                        dominio = dominio.replaceFirst("^www.*?\\.", "");
                        String urlmaspuerto = dominio;
                        mainActivity.setIp(urlmaspuerto.substring(0, urlmaspuerto.indexOf(':')));
                        mainActivity.setPuerto(urlmaspuerto.replaceAll(mainActivity.getIp() + ":", ""));
                        System.out.println("IP:" + mainActivity.getIp() + " Puerto:" + mainActivity.getPuerto());


                        mainActivity.setVsvRenderizadoStream((VideoStreamsView) mainActivity.findViewById(R.id.vsvRenderizadoStream));
                        mainActivity.getVsvRenderizadoStream().setVisibility(View.VISIBLE);

                        mainActivity.setmSocketAddress("http://" + mainActivity.getIp());
                        mainActivity.setmSocketAddress(mainActivity.getmSocketAddress() + (":" + mainActivity.getPuerto() + "/"));
                        //clienteWebRTC());
                        System.out.println("URL COMPLETA: " + mainActivity.getmSocketAddress());
                        System.out.println("jijiji callerId2: " + mainActivity.getCallerId2());

                        mainActivity.answer(mainActivity.getCallerId2());
                    } catch ( Exception e ) {
                        Log.i(MainActivity.TAG, "Error partiendo URL: " + e.getMessage());
                    }
                    mainActivity.pintarFPS();
                }
            }
        });
        alert.show();
    }
}