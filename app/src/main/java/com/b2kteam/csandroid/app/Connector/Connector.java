package com.b2kteam.csandroid.app.Connector;


import android.os.Handler;
import android.os.Message;

import java.io.InputStream;
import java.net.Socket;
import java.io.IOException;
import java.io.OutputStream;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by akru on 13.03.14.
 */
public class Connector {
    public Connector(ServerInfo serverInfo, Handler statusHandler) throws IOException {
        // open socket connection with server
        socket = new Socket(serverInfo.getAddress(), serverInfo.getPort());
        // store status handler
        handler = statusHandler;
        // down registered flag
        registered = false;
    }

    public void doRegistration(UserInfo userInfo) throws JSONException {
        JSONObject packet = new JSONObject();
        // set package type
        packet.put("type", "registration");
        // convert user info to JSON
        packet.put("user", userInfo.toJson());
        // start registration thread
        new Thread(new RegisterUser(packet.toString())).start();
    }

    public void doTransmitRequest() {
        // not registered user can not transmit data
        if (!registered) return;
        // start transmit request thread
        new Thread(new TransmitRequest()).start();
    }

    private void emitResult(JSONObject result) {
        Message msg = new Message();
        msg.obj = result;
        handler.sendMessage(msg);
    }

    private Socket socket;
    private Handler handler;
    private boolean registered;
    private final int readBufferSize = 1000;

    class TransmitRequest implements Runnable {

        @Override
        public void run() {
            try {
                // prepare request packet
                JSONObject packet = new JSONObject();
                packet.put("type", "transmit");
                // open out stream
                OutputStream os = socket.getOutputStream();
                // send JSON packet over socket
                os.write(packet.toString().getBytes("UTF-8"));
                // waiting for response
                InputStream is = socket.getInputStream();
                // allocate receive buffer
                byte [] readBuffer = new byte[readBufferSize];
                // receive response
                is.read(readBuffer);
                // parse response
                String responseJson = new String(readBuffer, "UTF-8");
                // emit response as JSON object
                emitResult(new JSONObject(responseJson));
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    class RegisterUser implements Runnable {
        private String packetJson;

        public RegisterUser(String requestJson) {
            packetJson = requestJson;
        }

        @Override
        public void run() {
            try {
                // open out stream
                OutputStream os = socket.getOutputStream();
                // send JSON packet over socket
                os.write(packetJson.getBytes("UTF-8"));
                // waiting for response
                InputStream is = socket.getInputStream();
                // allocate receive buffer
                byte [] readBuffer = new byte[readBufferSize];
                // receive response
                is.read(readBuffer);
                // parse response
                String responseJson = new String(readBuffer, "UTF-8");
                // emit response as JSON object
                emitResult(new JSONObject(responseJson));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
