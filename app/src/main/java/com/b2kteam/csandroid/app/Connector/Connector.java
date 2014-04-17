package com.b2kteam.csandroid.app.Connector;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
public class Connector implements Runnable {

    private Socket socket = null;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;
    private Thread listener = null;
    private final int readBufferSize = 1000;
    private volatile Handler commandHandler = null;
    private Handler resultHandler;

    public static final int REGISTRATION_ACTION = 0;
    public static final int CHANNEL_ACTION      = 1;
    public static final int CHANNEL_CLOSE_ACTION= 2;
    public static final int VOTE_ACTION         = 3;


    public Connector(Handler handler) {
        resultHandler = handler;
    }

    public Handler getCommandHandler() {
        return commandHandler;
    }

    private void doRegistrationRequest(Bundle serverInfo, Bundle userInfo) throws JSONException, IOException {
        //Close socket when connected
        if (listener != null)
            listener.interrupt();
        if (socket != null && socket.isConnected())
            socket.close();
        // open socket connection with server
        socket = new Socket(serverInfo.getString("address"), serverInfo.getInt("port"));
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
        listener = new Thread(new Listener());
        listener.start();

        JSONObject packet = new JSONObject();
        // set package type
        packet.put("request", "registration");
        // convert user info to JSON
        JSONObject user = new JSONObject();
        user.put("name", userInfo.getString("name"));
        user.put("company", userInfo.getString("company"));
        user.put("title", userInfo.getString("title"));

        packet.put("user", user);
        // send JSON packet over socket
        outputStream.write(packet.toString().getBytes("UTF-8"));
    }

    private void doChannelRequest() throws IOException, JSONException{
        // prepare request packet
        JSONObject packet = new JSONObject();
        packet.put("request", "channel");
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

    private void doVoteRequest(boolean type) throws JSONException, IOException {
        // prepare request packet
        JSONObject packet = new JSONObject();
        if (type)
            packet.put("request", "vote_yes");
        else
            packet.put("request", "vote_no");
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
            byte [] readBuffer = new byte[readBufferSize];

            while (!Thread.interrupted() && socket.isConnected()) {
                try {
                    if (inputStream.available() == 0)
                        continue;
                    // receive response
                    inputStream.read(readBuffer);
                    // parse response
                    String response = new String(readBuffer, "UTF-8");
                    JSONObject responseJson = new JSONObject(response);
                    // check response type
                    String requestType = responseJson.getString("request");
                    if (requestType.equals("registration")) {
                        emitResult(REGISTRATION_ACTION, response);
                    } else if (requestType.equals("channel")) {
                        emitResult(CHANNEL_ACTION, response);
                    } else if (requestType.equals("vote_yes")) {
                        emitResult(VOTE_ACTION, response);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run() {
        try {
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
                                doVoteRequest(data.getBoolean("type"));
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
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}