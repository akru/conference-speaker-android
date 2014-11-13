package com.b2kteam.csandroid.app.Connector;

import android.app.AlertDialog;
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

    public Discover(Handler discoverHandler, InetAddress broadcastAddress) throws SocketException, UnknownHostException {
        // store discover handler
        handler = discoverHandler;
        // open UDP broadcast listener
        socket = new DatagramSocket(DISCOVER_PORT, broadcastAddress);
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
        JSONObject info = server.getJSONObject("info");
        data.putString("name", info.getString("name"));
        data.putString("address", info.getString("address"));
        data.putInt("port", info.getInt("port"));
        msg.setData(data);
        handler.sendMessage(msg);
    }

    private Handler handler;
    private DatagramSocket socket;
    private final int inputBufferSize = 1000;
}
