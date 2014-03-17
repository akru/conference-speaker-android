package com.b2kteam.csandroid.app.Connector;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by akru on 12.03.14.
 */
public class ServerInfo {
    public ServerInfo(String name, String address, int port) {
        Name = name;
        Address = address;
        Port = port;
    }

    public static ServerInfo fromJson(String json) throws JSONException {
        JSONObject server = new JSONObject(json);

        String name = server.getString("name");
        String address = server.getString("address");
        int port = server.getInt("port");

        return new ServerInfo(name, address, port);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject server = new JSONObject();
        server.put("name", Name);
        server.put("address", Address);
        server.put("port", Port);
        return server;
    }

    public String getName()
    {
        return Name;
    }

    public String getAddress() {
        return Address;
    }

    public int getPort()
    {
        return Port;
    }

    private String Name;
    private String Address;
    private int Port;
}
