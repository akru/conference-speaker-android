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
    private final int readBufferSize = 1000;
    private volatile Handler commandHandler = null;
    private Handler resultHandler;

    public static final int REGISTRATION_ACTION = 0;
    public static final int CHANNEL_ACTION      = 1;

    public Connector(Handler handler) {
        resultHandler = handler;
    }

    public Handler getCommandHandler() {
        return commandHandler;
    }

    public boolean isConnected() {
        if (socket != null)
            return socket.isConnected();
        else
            return false;
    }

    public String doRegistrationRequest(Bundle serverInfo, Bundle userInfo) throws JSONException, IOException {
        // open socket connection with server
        socket = new Socket(serverInfo.getString("address"), serverInfo.getInt("port"));

        JSONObject packet = new JSONObject();
        // set package type
        packet.put("request", "registration");
        // convert user info to JSON
        JSONObject user = new JSONObject();
        user.put("name", userInfo.getString("name"));

        packet.put("user", user);
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
        return responseJson;
    }

    public String doChannelRequest() throws JSONException, IOException {
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
            return responseJson;
    }

    private void emitResult(int action, String response) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt("action", action);
        data.putString("response", response);
        msg.setData(data);
        resultHandler.sendMessage(msg);
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
                    switch (action) {
                        case REGISTRATION_ACTION:
                            try {
                                String result = doRegistrationRequest(
                                        data.getBundle("server"), data.getBundle("user"));
                                emitResult(REGISTRATION_ACTION, result);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case CHANNEL_ACTION:
                            try {
                                String result = doChannelRequest();
                                emitResult(CHANNEL_ACTION, result);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            };
            Looper.loop();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}

/*

    protected boolean startTransmit() {
        if (!connector.isConnected()) {
            toast("No server connected");
            return false;
        }

        try {
            JSONObject chan = connector.doChannelRequest();
            int port = chan.getJSONObject("channel").getInt("port");
            String host = chan.getJSONObject("channel").getString("host");

            Transmitter t = new Transmitter();
            t.setChannel(host, port);

            transmitter = new Thread(t);
            transmitter.start();

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    protected void stopTransmit() {
        if (transmitter.isAlive())
            transmitter.interrupt();
        toast("Translation terminated");
    }

    protected void connectToServer(final ServerInfo serverInfo) {
        setNotice(R.string.connecting);

        final Handler toastHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle msgData = msg.getData();
                toast(msgData.getString("toast"));
                if (msgData.getBoolean("result")) {
                    setNotice(serverInfo.getName());
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Message msg = new Message();
                    Bundle data = new Bundle();

                    connector.setServer(serverInfo);
                    JSONObject res = connector.doRegistrationRequest(userInfo);
                    String result = res.getString("result");

                    if (result == "error") {
                        data.putString("toast", res.getString("message"));
                        data.putBoolean("result", false);
                    }
                    else {
                        data.putString("toast","Registration successful on " + serverInfo.getName());
                        data.putBoolean("result", true);
                    }
                    msg.setData(data);
                    toastHandler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
 */