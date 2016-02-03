package es.kashamalaga.ClienteRTC;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class JoystickControl {
    private final MainActivity mainActivity;

    public JoystickControl(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    void pintarmando() {
        // findViewById(R.id.frameLayout1).setBackgroundColor(getResources().getColor(android.R.color.transparent));
        mainActivity.setBotonMostrarOcultarJoystick((TextView) mainActivity.findViewById(R.id.menu_habilitar_joystick));  // Boton del menu del Joystick

        if (!mainActivity.isMostrar_joysticks()) {
            mainActivity.getBotonMostrarOcultarJoystick().setText(R.string.ocultar_joystick);
            //setTitle(R.string.ocultar_joystick);
            mainActivity.setMostrar_joysticks(true);

            // TextView assam = (TextView) findViewById(R.id.menu_habilitar_joystick);
            // assam.setText(R.string.ocultar_joystick);

            /// Mostramos los Joysticks Ocultos
            mainActivity.getJoystick().setVisibility(View.VISIBLE);
            mainActivity.getJoystick2().setVisibility(View.VISIBLE);
            //

            mainActivity.getJoystick().setOnJoystickMoveListener(new Joysticks.OnJoystickMoveListener() {

                                                                     @Override
                                                                     public void onValueChanged(int angle, int power, int direction) {

                                                                         double x = Math.cos(angle);
                                                                         double y = Math.sin(angle);
                                                                         double length = Math.sqrt((x * x) + (y * y));
                                                                         x /= length;
                                                                         y /= length;
                                                                         float currSpeed = (float) ((mainActivity.getVelocidad_robot() / 1000) * (power / 100.0f));
                                                                         mainActivity.getAngleTextView().setText(" Angulo:  " + String.valueOf(angle) + " °");
                                                                         mainActivity.getPowerTextView().setText(" Velocidad: " + String.format("%.2f", currSpeed) + " al: " + String.valueOf(power) + " %");
                                                                         int speed;
                                                                         x *= currSpeed;
                                                                         y *= currSpeed;


                                                                         DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
                                                                         simbolos.setDecimalSeparator('.');
                                                                         String motion = "Motion " + new DecimalFormat("##.###", simbolos).format(currSpeed) + " " + new DecimalFormat("##.###", simbolos).format(angle);


                                                                         // Log.i("ActionBar", new DecimalFormat("##.##").format(x)+" "+ new DecimalFormat("##.##").format(y));
                                                                         try {
                                                                             switch (direction) {
                                                                                 case Joysticks.ADELANTE:
                                                                                     mainActivity.getDirectionTextView().setText(" Adelante"); // Horizontal 0%
                                                                                     mainActivity.getDatosEnviotextView().setText("Motion " + new DecimalFormat("##.##", simbolos).format(currSpeed) + "  " + "0.00"); // Horizontal 0%
                                                                                     if (MainActivity.conectado_servidor) {
                                                                                         mainActivity.getClient().enviardato("Motion " + new DecimalFormat("##.###", simbolos).format(currSpeed) + " " + "0.000");
                                                                                     }
                                                                                     break;
                                                                                 case Joysticks.ADELANTE_IZQUIERDA:                         // Adelante Izq
                                                                                     mainActivity.getDirectionTextView().setText(" Adelante_Izquierda"); //15%
                                                                                     mainActivity.getDatosEnviotextView().setText("Motion " + new DecimalFormat("##.##", simbolos).format(currSpeed-0.1) + "  " + new DecimalFormat("##.##", simbolos).format((power / 100.0f) * 0.23));
                                                                                     if (MainActivity.conectado_servidor) {
                                                                                         mainActivity.getClient().enviardato("Motion " + new DecimalFormat("##.###", simbolos).format(currSpeed-0.1) + " " + new DecimalFormat("##.##", simbolos).format((power / 100.0f) * 0.23));
                                                                                     }
                                                                                     break;
                                                                                 case Joysticks.ADELANTE_DERECHA:                         // Adelante Derecha
                                                                                     mainActivity.getDatosEnviotextView().setText("Motion " + new DecimalFormat("##.##", simbolos).format(currSpeed-0.1) + "  " + new DecimalFormat("##.##", simbolos).format((power / 100.0f) * -0.23));
                                                                                     mainActivity.getDirectionTextView().setText(" Adelante_Derecha");
                                                                                     if (MainActivity.conectado_servidor) {
                                                                                         mainActivity.getClient().enviardato("Motion " + new DecimalFormat("##.###", simbolos).format(currSpeed-0.1) + " " + new DecimalFormat("##.##", simbolos).format((power / 100.0f) * -0.23));
                                                                                     }

                                                                                     break;
                                                                                 case Joysticks.IZQUIERDA:                              // Izquierda
                                                                                     mainActivity.getDatosEnviotextView().setText("Motion " + new DecimalFormat("##.##", simbolos).format(0) + "  " + new DecimalFormat("##.##", simbolos).format(((power / 100.0f) * 0.47) * (mainActivity.getGiro_maximo() / 1000)));
                                                                                     mainActivity.getDirectionTextView().setText(" Izquierda"); //30%
                                                                                     if (MainActivity.conectado_servidor) {
                                                                                         mainActivity.getClient().enviardato("Motion " + new DecimalFormat("##.###", simbolos).format(0) + " " + new DecimalFormat("##.##", simbolos).format(((power / 100.0f) * 0.47) * (mainActivity.getGiro_maximo() / 1000)));
                                                                                     }

                                                                                     break;
                                                                                 case Joysticks.DERECHA:                            //Derecha
                                                                                     mainActivity.getDatosEnviotextView().setText("Motion " + new DecimalFormat("##.##", simbolos).format(0) + "  " + new DecimalFormat("##.##", simbolos).format(((power / 100.0f) * -0.47) * (mainActivity.getGiro_maximo() / 1000)));
                                                                                     mainActivity.getDirectionTextView().setText(" Derecha");
                                                                                     if (MainActivity.conectado_servidor) {
                                                                                         mainActivity.getClient().enviardato("Motion " + new DecimalFormat("##.###", simbolos).format(0) + " " + new DecimalFormat("##.##", simbolos).format(((power / 100.0f) * -0.47) * (mainActivity.getGiro_maximo() / 1000)));
                                                                                     }

                                                                                     break;
                                                                                 case Joysticks.ATRAS:
                                                                                     mainActivity.getDatosEnviotextView().setText("Motion " + new DecimalFormat("##.##", simbolos).format(-currSpeed) + "  " + "0.00");
                                                                                     mainActivity.getDirectionTextView().setText(" Atras");
                                                                                     if (MainActivity.conectado_servidor) {
                                                                                         mainActivity.getClient().enviardato("Motion " + new DecimalFormat("##.###", simbolos).format(-currSpeed) + " " + "0.000");
                                                                                     }

                                                                                     // client.enviardato("Motion "+String.format("%.3f",x)+" "+String.format("%.3f",y));
                                                                                     break;
                                                                                 case Joysticks.ATRAS_IZQUIERDA:                        //Atras Izq
                                                                                     mainActivity.getDatosEnviotextView().setText("Motion " + new DecimalFormat("##.##", simbolos).format(-currSpeed+0.1) + "  " + new DecimalFormat("##.##", simbolos).format((power / 100.0f) * 0.70));
                                                                                     mainActivity.getDirectionTextView().setText(" Atras_Izquierda"); //45%
                                                                                     if (MainActivity.conectado_servidor) {
                                                                                         mainActivity.getClient().enviardato("Motion " + new DecimalFormat("##.###", simbolos).format(-currSpeed+0.1) + " " + new DecimalFormat("##.##", simbolos).format((power / 100.0f) * 0.70));
                                                                                     }

                                                                                     break;
                                                                                 case Joysticks.ATRAS_DERECHA:                     //Atras Derecha
                                                                                     mainActivity.getDatosEnviotextView().setText("Motion " + new DecimalFormat("##.##", simbolos).format(-currSpeed+0.1) + "  " + new DecimalFormat("##.##", simbolos).format((power / 100.0f) * -0.70));
                                                                                     mainActivity.getDirectionTextView().setText(" Atras_Derecha");
                                                                                     if (MainActivity.conectado_servidor) {
                                                                                         mainActivity.getClient().enviardato("Motion " + new DecimalFormat("##.###", simbolos).format(-currSpeed+0.1) + " " + new DecimalFormat("##.##", simbolos).format((power / 100.0f) * -0.70));
                                                                                     }

                                                                                     break;

                                                                                 default:
                                                                                     mainActivity.getDirectionTextView().setText(" Centro");
                                                                                     mainActivity.getDatosEnviotextView().setText("Motion 0.00" + "  " + "0.00");
                                                                                     if (MainActivity.conectado_servidor) {
                                                                                         mainActivity.getClient().enviardato("Motion 0.000" + " " + "0.000");
                                                                                     }

                                                                             }// fin switch
                                                                         } catch ( Exception e ) {
                                                                             Log.i(MainActivity.TAG, "Error en Joystick 1: " + e.getMessage());
                                                                         }
                                                                     } // fin OverValueChanged
                                                                 } // fin OnJoystickMove
                    , Joysticks.DEFAULT_LOOP_INTERVAL);

            // /////////////
            ///////// Aqui se obtienen los datos de movimiento del segundo Joystick
            ////////////
            mainActivity.getJoystick2().setOnJoystickMoveListener(new Joysticks.OnJoystickMoveListener() {

                                                                      @Override
                                                                      public void onValueChanged(int angle, int power, int direction) {
                                                                          mainActivity.getFPSTextView().setText("FPS: " + new DecimalFormat("##.##").format(VideoStreamsView.fps));

                                                                          double x = Math.cos(angle);
                                                                          double y = Math.sin(angle);
                                                                          double length = Math.sqrt((x * x) + (y * y));
                                                                          x /= length;
                                                                          y /= length;
                                                                          int moveSpeed = 10;
                                                                          float currSpeed = moveSpeed * (power / 100.0f);
                                                                          int speed;
                                                                          x *= currSpeed;
                                                                          y *= currSpeed;
                                                                          // Log.i("ActionBar", new DecimalFormat("##.##").format(x)+" "+ new DecimalFormat("##.##").format(y));
                                                                          switch (direction) {
                                                                              case Joysticks.ADELANTE:
                                                                                  //   directionTextView.setText("Adelante");
                                                                                  mainActivity.getDatosEnviotextView().setText("Tilt + " + (mainActivity.getGiro_cuello() / 1000));
                                                                                  if (MainActivity.conectado_servidor) {
                                                                                      mainActivity.getClient().enviardato("Tilt + " + (mainActivity.getGiro_cuello() / 1000));
                                                                                  }
                                                                                  break;
                                                                              case Joysticks.ADELANTE_IZQUIERDA:
                                                                                  //  directionTextView.setText("Adelante_Izquierda");

                                                                                  break;
                                                                              case Joysticks.ADELANTE_DERECHA:
                                                                                  // directionTextView.setText("Adelante_Derecha");
                                                                                  break;
                                                                              case Joysticks.IZQUIERDA:
                                                                                  // directionTextView.setText("Izquierda");
                                                                                  break;
                                                                              case Joysticks.DERECHA:
                                                                                  // directionTextView.setText("Derecha");
                                                                                  break;
                                                                              case Joysticks.ATRAS:
                                                                                  mainActivity.getDatosEnviotextView().setText("Tilt - " + (mainActivity.getGiro_cuello() / 1000));
                                                                                  //  directionTextView.setText(String.valueOf(currSpeed)+" " + new DecimalFormat("##.##").format(x)+" "+ new DecimalFormat("##.##").format(y));
                                                                                  if (MainActivity.conectado_servidor) {
                                                                                      mainActivity.getClient().enviardato("Tilt - " + (mainActivity.getGiro_cuello() / 1000));
                                                                                  }
                                                                                  break;
                                                                              case Joysticks.ATRAS_IZQUIERDA:
                                                                                  //   directionTextView.setText(String.valueOf(currSpeed)+" " + new DecimalFormat("##.##").format(x)+" "+ new DecimalFormat("##.##").format(y));
                                                                                  break;
                                                                              case Joysticks.ATRAS_DERECHA:
                                                                                  //  directionTextView.setText(String.valueOf(currSpeed)+" " + new DecimalFormat("##.##").format(x)+" "+ new DecimalFormat("##.##").format(y));
                                                                                  break;
                                                                              default:
                                                                                  //   directionTextView.setText("Centro");
                                                                          }
                                                                      }
                                                                  }
                    , Joysticks.DEFAULT_LOOP_INTERVAL2);


        } else {
            ///// Si los joystick estaban mostrandose, ahora se ocultaran
            mainActivity.getJoystick().setVisibility(View.INVISIBLE);
            mainActivity.getJoystick2().setVisibility(View.INVISIBLE);
            //////
            mainActivity.getBotonMostrarOcultarJoystick().setText(R.string.mostrar_joystick);
            // setTitle(R.string.mostrar_joystick);
            mainActivity.setMostrar_joysticks(false);
        }

    }
}