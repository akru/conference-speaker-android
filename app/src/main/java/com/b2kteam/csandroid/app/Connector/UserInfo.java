package com.b2kteam.csandroid.app.Connector;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by akru on 12.03.14.
 */
public class UserInfo {
    public UserInfo(String name)
    {
        Name = name;
    }

    public String getName() {
        return Name;
    }

    public static UserInfo fromJson(String json) throws JSONException {
        JSONObject user = new JSONObject(json);
        String name = user.getString("name");
        return new UserInfo(name);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject output = new JSONObject();
        output.put("name", Name);
        return output;
    }

    private String Name;
}
