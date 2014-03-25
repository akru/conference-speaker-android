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

    private Socket socket;
    private final int readBufferSize = 1000;

    public void setServer(ServerInfo serverInfo) throws IOException {
        // open socket connection with server
        socket = new Socket(serverInfo.getAddress(), serverInfo.getPort());
    }

    public boolean isConnected() {
        if (socket != null)
            return socket.isConnected();
        else
            return false;
    }

    public JSONObject doRegistrationRequest(UserInfo userInfo) throws JSONException, IOException {
        // return when not connected
        if (!socket.isConnected())
            throw new IOException("Not connected");

        JSONObject packet = new JSONObject();
        // set package type
        packet.put("request", "registration");
        // convert user info to JSON
        packet.put("user", userInfo.toJson());
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
        // return response as JSON object
        return new JSONObject(responseJson);
    }

    public JSONObject doChannelRequest() throws JSONException, IOException {
            // prepare request packet
            JSONObject packet = new JSONObject();
            packet.put("request", "channel");
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
            // return response as JSON object
            return new JSONObject(responseJson);
    }
}
