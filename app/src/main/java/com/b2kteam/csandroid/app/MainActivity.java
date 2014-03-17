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

import java.net.SocketException;
import java.net.UnknownHostException;

import com.b2kteam.csandroid.app.Connector.Discover;
import com.b2kteam.csandroid.app.Transmitter.TransmitterStatus;


public class MainActivity extends ActionBarActivity {

    private Handler transmitterStatus;
    private Handler connectorStatus;
    private Handler discoverHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        // transmitter status handler
        transmitterStatus = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch ((TransmitterStatus) msg.obj) {
                    case CONNECTED:
                        break;
                    case RECORDING:
                        break;
                    case ERROR:
                        break;
                }
                Log.i("TRANSMITTER", msg.obj.toString());
            }
        };

        // connector status handler
        connectorStatus = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.i("CONNECTOR", msg.obj.toString());
            }
        };

        // discover message handler
        discoverHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.i("DISCOVER", msg.obj.toString());
            }
        };

        // start discover thread
        try {
            new Thread(new Discover(discoverHandler)).start();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
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
