package com.b2kteam.csandroid.app.Connector;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by akru on 13.03.14.
 */
public class Connector implements Runnable {
    private Socket socket = null;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private Thread listener = new Thread(new Listener());
    private volatile Handler commandHandler = null;
    private Handler resultHandler;

    public static final int REGISTRATION_ACTION  = 0;
    public static final int CHANNEL_ACTION       = 1;
    public static final int CHANNEL_CLOSE_ACTION = 2;
    public static final int VOTE_ACTION          = 3;


    public Connector(Handler handler) {
        resultHandler = handler;
    }

    public Handler getCommandHandler() {
        return commandHandler;
    }

    private void doRegistrationRequest(Bundle serverInfo, Bundle userInfo) {
        //Close socket when connected
        listener.interrupt();
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket = new Socket();
        // open socket connection with server
        InetSocketAddress addr = new InetSocketAddress(serverInfo.getString("address"),
                                                       serverInfo.getInt("port"));
        try {
            socket.connect(addr, 10000);
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        listener = new Thread(new Listener());
        listener.start();
        while (!listener.isAlive());

        JSONObject packet = new JSONObject();
        try {
            // set package type
            packet.put("request", "registration");
            // convert user info to JSON
            JSONObject user = new JSONObject();
            user.put("name", userInfo.getString("name"));
            user.put("company", userInfo.getString("company"));
            user.put("title", userInfo.getString("title"));
            packet.put("user", user);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        // send JSON packet over socket
        try {
            outputStream.write(packet.toString().getBytes("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doChannelRequest() throws IOException, JSONException{
        // prepare request packet
        JSONObject packet = new JSONObject();
        packet.put("request", "channel_open");
        // send JSON packet over socket
        outputStream.write(packet.toString().getBytes("UTF-8"));
    }

    private void doChannelCloseRequest() throws JSONException, IOException {
        // prepare request packet
        JSONObject packet = new JSONObject();
        packet.put("request", "channel_close");
        // send JSON packet over socket
        outputStream.write(packet.toString().getBytes("UTF-8"));
    }

    private void doVoteRequest(String uuid, String mode, int answer) throws JSONException, IOException {
        // prepare request packet
        JSONObject packet = new JSONObject();
        packet.put("request", "vote");
        packet.put("uuid", uuid);
        if (mode.contains("simple"))
            packet.put("answer", answer != 0);
        else
            packet.put("answer", answer);
        // send JSON packet over socket
        outputStream.write(packet.toString().getBytes("UTF-8"));
    }

    private void emitResult(int action, String response) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt("action", action);
        data.putString("response", response);
        msg.setData(data);
        resultHandler.sendMessage(msg);
    }

    private class Listener implements Runnable {
        @Override
        public void run() {
            // allocate receive buffer
            byte [] readBuffer = new byte[1000];

            while (!socket.isClosed() && !Thread.interrupted()) {
                // receive response
                try {
                    inputStream.read(readBuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                // parse response
                try {
                    String response = new String(readBuffer, "UTF-8");
                    JSONObject responseJson = new JSONObject(response);
                    // check response type
                    String requestType = responseJson.getString("request");

                    if (requestType.equals("registration")) {
                        emitResult(REGISTRATION_ACTION, response);
                    } else if (requestType.equals("channel_open")) {
                        emitResult(CHANNEL_ACTION, response);
                    } else if (requestType.equals("channel_close")) {
                        emitResult(CHANNEL_CLOSE_ACTION, response);
                    } else if (requestType.equals("vote")) {
                        emitResult(VOTE_ACTION, response);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        commandHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle data = msg.getData();
                int action = data.getInt("action");
                try {
                    switch (action) {
                        case REGISTRATION_ACTION:
                            doRegistrationRequest(
                                    data.getBundle("server"),
                                    data.getBundle("user"));
                            break;
                        case CHANNEL_ACTION:
                            doChannelRequest();
                            break;
                        case CHANNEL_CLOSE_ACTION:
                            doChannelCloseRequest();
                            break;
                        case VOTE_ACTION:
                            doVoteRequest(
                                    data.getString("uuid"),
                                    data.getString("mode"),
                                    data.getInt("answer"));
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        };
        Looper.loop();
    }
}