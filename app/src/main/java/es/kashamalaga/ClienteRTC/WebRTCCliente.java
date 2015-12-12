package es.kashamalaga.ClienteRTC;

import android.os.Handler;
import android.util.Log;

import com.koushikdutta.async.http.socketio.Acknowledge;
import com.koushikdutta.async.http.socketio.ConnectCallback;
import com.koushikdutta.async.http.socketio.EventCallback;
import com.koushikdutta.async.http.socketio.SocketIOClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.HashMap;
import java.util.LinkedList;


class WebRTCCliente {
  private final static int MAX_PEER = 10; // Permite 10 personas maximo conectadas a la vez
  private static boolean[] endPoints = new boolean[MAX_PEER];
  private PeerConnectionFactory factory;
  private static HashMap<String, Peer> peers = new HashMap<String, Peer>();
  private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<PeerConnection.IceServer>();
  private MediaConstraints pcConstraints = new MediaConstraints();
  private MediaStream lMS;
  private static RTCListener mListener;
  private SocketIOClient client;
  private final MessageHandler messageHandler = new MessageHandler();
  private final static String TAG = WebRTCCliente.class.getCanonicalName();
  private static String from;

    public interface RTCListener{
    void onCallReady(String callId);

    void onStatusChanged(String newStatus);
  //  void onLocalStream(MediaStream localStream);

    void onAddRemoteStream(MediaStream remoteStream, int endPoint);

    void onRemoveRemoteStream(MediaStream remoteStream, int endPoint);

  }

  private interface Command{
    void execute(String peerId, JSONObject payload) throws JSONException;
  }

  private class CreateOfferCommand implements Command{
    public void execute(String peerId, JSONObject payload) throws JSONException {
    	Log.d(TAG,"CreateOfferCommand");
      Peer peer = peers.get(peerId);
      peer.pc.createOffer(peer, pcConstraints);
    }
  }

  private class CreateAnswerCommand implements Command{
    public void execute(String peerId, JSONObject payload) throws JSONException {
    	Log.d(TAG,"CreateAnswerCommand");
      Peer peer = peers.get(peerId);
      SessionDescription sdp = new SessionDescription(
                                                      SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                                                      payload.getString("sdp")
                                                      );
      peer.pc.setRemoteDescription(peer, sdp);
      peer.pc.createAnswer(peer, pcConstraints);
    }
  }

  private class SetRemoteSDPCommand implements Command{
    public void execute(String peerId, JSONObject payload) throws JSONException {
    	Log.d(TAG,"SetRemoteSDPCommand");
      Peer peer = peers.get(peerId);
      SessionDescription sdp = new SessionDescription(
                                                      SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                                                      payload.getString("sdp")
                                                      );
      peer.pc.setRemoteDescription(peer, sdp);
    }
  }

  private class AddIceCandidateCommand implements Command{
    public void execute(String peerId, JSONObject payload) throws JSONException {
    	Log.d(TAG,"AddIceCandidateCommand");
      PeerConnection pc = peers.get(peerId).pc;
      if (pc.getRemoteDescription() != null) {
        IceCandidate candidate = new IceCandidate(
                                                  payload.getString("id"),
                                                  payload.getInt("label"),
                                                  payload.getString("candidate")
                                                  );
        pc.addIceCandidate(candidate);
      }
    }
  }


  public void sendMessage(String to, String type, JSONObject payload) throws JSONException {
    JSONObject message = new JSONObject();
    message.put("to", to);
    message.put("type", type);
    message.put("payload", payload);
    client.emit("message", new JSONArray().put(message));
  }

  private class MessageHandler implements EventCallback {
    private HashMap<String, Command> commandMap;

    public MessageHandler() {
      this.commandMap = new HashMap<String, Command>();
      commandMap.put("init", new CreateOfferCommand());
      //sleep(5000);  // MITICO ERROR PARA EL RECUERDO SON RETRASOS SANOS
      commandMap.put("offer", new CreateAnswerCommand());
      commandMap.put("answer", new SetRemoteSDPCommand());
      commandMap.put("candidate", new AddIceCandidateCommand());
    }

    @Override
    public void onEvent(String s, JSONArray jsonArray, Acknowledge acknowledge) {
      try {
    	  Log.d(TAG,"MessageHandler.onEvent() "+ (s == null ? "nil" : s));
        if(s.equals("id")) {
          mListener.onCallReady(jsonArray.getString(0));
        } else {
          JSONObject json = jsonArray.getJSONObject(0);
           from = json.getString("from");
          String type = json.getString("type");
          JSONObject payload = null;
          if(!type.equals("init")) {
            payload = json.getJSONObject("payload");
          }

          // if peer is unknown, try to add him
          if(!peers.containsKey(from)) {
            // if MAX_PEER is reach, ignore the call
            int endPoint = findEndPoint();
            if(endPoint != MAX_PEER) {
              addPeer(from, endPoint);

              commandMap.get(type).execute(from, payload);
            }
          } else {
            commandMap.get(type).execute(from, payload);
          }
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  }

  private class Peer implements SdpObserver, PeerConnection.Observer{
    private PeerConnection pc;
    private String id;
    private int endPoint;

    @Override
    public void onCreateSuccess(final SessionDescription sdp) {
      try {
        JSONObject payload = new JSONObject();
        payload.put("type", sdp.type.canonicalForm());
        payload.put("sdp", sdp.description);
        sendMessage(id, sdp.type.canonicalForm(), payload);
        pc.setLocalDescription(Peer.this, sdp);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onSetSuccess() {}

    @Override
    public void onCreateFailure(String s) {}

    @Override
    public void onSetFailure(String s) {}

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {}

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
      if(iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
        removePeer(id);
        mListener.onStatusChanged("DESCONECTADO");
      }
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {}

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
      try {
        JSONObject payload = new JSONObject();
        payload.put("label", candidate.sdpMLineIndex);
        payload.put("id", candidate.sdpMid);
        payload.put("candidate", candidate.sdp);
        sendMessage(id, "candidate", payload);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onError() {}

    @Override
    public void onAddStream(MediaStream mediaStream) {
    	Log.d(TAG,"SE AÑADE UN CLIENTE onAddStream "+mediaStream.label());

      // remote streams are displayed from 1 to MAX_PEER (0 is localStream)
      mListener.onAddRemoteStream(mediaStream, endPoint+1);
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
      mListener.onRemoveRemoteStream(mediaStream, endPoint);

      removePeer(id);
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {}

    @Override
    public void onRenegotiationNeeded() {

    }

    public Peer(String id, int endPoint) {
    	Log.d(TAG, "NUEVO HAMIJO new Peer: " + id + " " + endPoint);
      this.pc = factory.createPeerConnection(iceServers, pcConstraints, this);
      this.id = id;
      this.endPoint = endPoint;

      pc.addStream(lMS, new MediaConstraints());

      mListener.onStatusChanged("Conectando al Servidor WebRTC");
        //abrir datachanel
    // createDataChannelToRegressionTestBug2302(pc);

    }
  }

  public WebRTCCliente(RTCListener listener, String host) {
    mListener = listener;
    factory = new PeerConnectionFactory();

    SocketIOClient.connect(host, new ConnectCallback() {

      @Override
      public void onConnectCompleted(Exception ex, SocketIOClient socket) {
        if (ex != null) {
          mListener.onStatusChanged("No se puedo conectar al Servidor WebSocket en la direccion: " + ex.getMessage());
          //Log.e(TAG,"WebRTCCliente connect failed: "+ex.getMessage());
          return;
        }
        mListener.onStatusChanged("Conectado al Servidor WebSocket.");

        //  Log.d(TAG, "Conectado al Servidor WebRTC.");
        MainActivity.conectado_servidor = true;
        client = socket;

        // specify which events you are interested in receiving
        client.addListener("id", messageHandler);
        client.addListener("message", messageHandler);
      }
    }, new Handler());

    iceServers.add(new PeerConnection.IceServer("stun:23.21.150.121"));
    iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

      //  Habilitar el uso de Datachannels 30.09/2014
       //  pcConstraints.optional.add(new MediaConstraints.KeyValuePair("RtpDataChannels", "true"));
      //
    pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
    pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

  }

  public void setCamera(String cameraFacing, String height, String width){
    MediaConstraints videoConstraints = new MediaConstraints();
    videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", height));
   videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", width));

    //VideoSource videoSource = factory.createVideoSource(getVideoCapturer(cameraFacing), videoConstraints);
    lMS = factory.createLocalMediaStream("ARDAMS");
   // lMS.addTrack(factory.createVideoTrack("ARDAMSv0", videoSource)); // Desactivar el envio de video del movil al Servidor
   // lMS.addTrack(factory.createAudioTrack("ARDAMSa0"));  // Desactivar el envio de audio del movil al Servidor
   // mListener.onLocalStream(lMS);
  }

  private int findEndPoint() {
    for(int i = 0; i < MAX_PEER; i++) {
      if(!endPoints[i]) return i;
    }
    return MAX_PEER;
  }

  public void start() throws JSONException {

      lMS = factory.createLocalMediaStream("ARDAMS");

      JSONObject message = new JSONObject();
      message.put("name", "prueba_android");
      client.emit("readyToStream", new JSONArray().put(message));

  }
    public void enviardato(String mensaje){
        try {
            JSONObject message = new JSONObject();
            message.put("name", mensaje);
            client.emit("mensaje", new JSONArray().put(message));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
  private void addPeer(String id, int endPoint) {
    Peer peer = new Peer(id, endPoint) {

      @Override
      public void onRenegotiationNeeded() {

      }
    };
    peers.put(id, peer);

    endPoints[endPoint] = true;
  }
  public static void desconectar(){
    if(MainActivity.conectado_servidor && MainActivity.nueva_conexion ) {
      try
        {
        removePeer(from);
       }
      catch (Exception e)
      {
        Log.i(TAG, "Error desconectando!: " + e.getMessage());

      }
      mListener.onStatusChanged("DESCONECTADO");
      MainActivity.conectado_servidor = false;
      MainActivity.nueva_conexion = false;
    }
    else
    {
     // mListener.onStatusChanged("No estás conectado, por lo tanto no puedes desconectarte"); //no usar sino esta inicializado antes
      Log.i(TAG,"No estás conectado, por lo tanto no puedes desconectarte");

    }
  }
  public static void removePeer(String id) {
    try {
      Peer peer = peers.get(id);
      peer.pc.close();
      peer.pc.dispose();
      peers.remove(peer.id);

      endPoints[peer.endPoint] = false;
    }  catch (Exception e)
    {
      Log.i(TAG, "Error en funcion removePeer: " + e.getMessage());

    }
  }

}
