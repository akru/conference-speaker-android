package com.b2kteam.csandroid.app;

import android.os.Message;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.b2kteam.csandroid.app.Connector.Connector;
import com.b2kteam.csandroid.app.Connector.Discover;
import com.b2kteam.csandroid.app.Connector.ServerInfo;
import com.b2kteam.csandroid.app.Connector.UserInfo;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity {

    private Thread discover;
    private Connector connector;

    protected void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    protected void connectToServer(ServerInfo serverInfo) {
        try {
            connector = new Connector(serverInfo);

            UserInfo userInfo = new UserInfo("akru");
            JSONObject res = connector.doRegistrationRequest(userInfo);
            String result = res.getString("result");

            if (result == "error") {
                toast(res.getString("message"));
                return;
            }
            else
                toast("Registration successful");

            ToggleButton b = (ToggleButton) findViewById(R.id.record_button);
            b.setClickable(true);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        JSONObject chan = connector.doChannelRequest();
                        toast(chan.getString("result") +
                                " -> " + chan.getJSONObject("channel").getInt("port"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        // discover message handler
        Handler discoverHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                discover.interrupt();

                Bundle server = msg.getData();
                toast("New server: " + server.getString("name"));

                ServerInfo serverInfo = new ServerInfo(server.getString("name"),
                        server.getString("address"), server.getInt("port"));
                connectToServer(serverInfo);
            }
        };
        // start discover thread
        try {
            discover = new Thread(new Discover(discoverHandler));
            discover.start();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        toast("Discovering the server...");
    }

    public void onClick(View view) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_microphone, container, false);
            return rootView;
        }
    }
}
