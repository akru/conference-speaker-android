package com.b2kteam.csandroid.app.Connector;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by akru on 17.03.14.
 */
public class Discover implements Runnable {
    // Discover constants
    public static final int    DISCOVER_PORT    = 35000;
    public static final String DISCOVER_ADDRESS = "255.255.255.255";

    public Discover(Handler discoverHandler) throws SocketException, UnknownHostException {
        // store discover handler
        handler = discoverHandler;
        // open UDP broadcast listener
        socket = new DatagramSocket(DISCOVER_PORT, InetAddress.getByName(DISCOVER_ADDRESS));
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket packet =
                        new DatagramPacket(new byte[inputBufferSize], inputBufferSize);
                // receive broadcast packet
                socket.receive(packet);
                // convert binary data to string
                String json = new String(packet.getData(), "UTF-8");
                // notice main thread about new server
                emitServerInfo(json);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void emitServerInfo(String serverInfo) throws JSONException{
        Message msg = new Message();
        Bundle data = new Bundle();

        JSONObject server = new JSONObject(serverInfo);
        data.putString("name", server.getString("name"));
        data.putString("address", server.getString("address"));
        data.putInt("port", server.getInt("port"));
        msg.setData(data);
        handler.sendMessage(msg);
    }

    private Handler handler;
    private DatagramSocket socket;
    private final int inputBufferSize = 100;
}
