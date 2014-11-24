package com.b2kteam.csandroid.app.Connector;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by akru on 17.03.14.
 */
public class Discover implements Runnable {
    // Discover constants
    public static final int    DISCOVER_PORT    = 35000;

    public Discover(Handler serverInfoHandler,
                    Handler voteHandler,
                    InetAddress broadcastAddress) throws SocketException, UnknownHostException {
        // store handlers
        serverInfo = serverInfoHandler;
        voteInvite = voteHandler;
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
                JSONObject json = new JSONObject(new String(packet.getData(), "UTF-8"));
                // notice main thread about new server
                emitServerInfo(json);
                emitVoteInvite(json);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void emitServerInfo(JSONObject discoverMsg) throws JSONException{
        Message msg = new Message();
        Bundle data = new Bundle();

        JSONObject info = discoverMsg.getJSONObject("info");
        data.putString("name", info.getString("name"));
        data.putString("address", info.getString("address"));
        data.putInt("port", info.getInt("port"));
        msg.setData(data);
        serverInfo.sendMessage(msg);
    }

    private void emitVoteInvite(JSONObject discoverMsg) throws JSONException{
        Message msg = new Message();
        Bundle data = new Bundle();

        JSONObject vote = discoverMsg.getJSONObject("vote");
        data.putString("uuid", vote.getString("uuid"));
        data.putString("question", vote.getString("question"));
        data.putString("mode", vote.getString("mode"));
        if (vote.getString("mode").contains("custom")) {
            ArrayList<String> answers = new ArrayList<String>();
            JSONArray jsonAnswers = vote.getJSONArray("answers");
            for (int i = 0; i < jsonAnswers.length(); i++) {
                answers.add(jsonAnswers.getString(i));
            }
            data.putStringArrayList("answers", answers);
        }
        msg.setData(data);
        voteInvite.sendMessage(msg);
    }

    private Handler serverInfo;
    private Handler voteInvite;
    private DatagramSocket socket;
    private final int inputBufferSize = 1000;
}
