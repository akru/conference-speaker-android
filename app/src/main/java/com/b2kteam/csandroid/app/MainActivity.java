package com.b2kteam.csandroid.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import com.b2kteam.csandroid.app.Connector.Connector;
import com.b2kteam.csandroid.app.Connector.Discover;
import com.b2kteam.csandroid.app.Transmitter.Transmitter;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity implements VoteInteraction, ServerInteraction, MainInteraction {
    Thread discover          = null;
    Thread connector         = null;
    Thread transmitter       = null;
    Handler connectorCmd     = null;

    int                   currentServer     = -1;
    ArrayList<String>     serverNameList    = new ArrayList<String>();
    ArrayList<String>     serverUuidList    = new ArrayList<String>();
    ArrayAdapter<String>  serverListAdapter = null;

    String                question          = null;
    ArrayList<String>     answerList        = new ArrayList<String>();
    ArrayAdapter<String>  answerListAdapter = null;

    HashMap<String, Bundle> servers  = new HashMap<String, Bundle>();
    ArrayList<String>       votes    = new ArrayList<String>();
    Bundle                  userInfo = new Bundle();

    ConnectedState          state    = ConnectedState.DISCONNECTED;
    StateChangedListener    stateListener = null;

    ViewPager pager;

    // TODO: Static address usage (no DHCP)
    InetAddress getBroadcastAddress() {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        try {
            return InetAddress.getByAddress(quads);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    void loadPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        userInfo.putString("name",    preferences.getString("prefName", "Unknown"));
        userInfo.putString("company", preferences.getString("prefCompany", "Unknown"));
        userInfo.putString("title",   preferences.getString("prefTitle", "Unknown"));
    }

    void setupBar() {
        final ActionBar bar = getSupportActionBar();
        final FragmentAdapter fragmentAdapter =
                new FragmentAdapter(getSupportFragmentManager());
        pager = (ViewPager) findViewById(R.id.pager);
        final ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                pager.setCurrentItem(tab.getPosition());
            }
            @Override
            public void onTabUnselected(Tab tab, FragmentTransaction fragmentTransaction) {}
            @Override
            public void onTabReselected(Tab tab, FragmentTransaction fragmentTransaction) {}
        };

        bar.setLogo(R.drawable.ic_logo);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.addTab(bar.newTab().setText(R.string.action_main).setTabListener(tabListener));
        bar.addTab(bar.newTab().setText(R.string.action_vote).setTabListener(tabListener));
        bar.addTab(bar.newTab().setText(R.string.action_server).setTabListener(tabListener));

        pager.setAdapter(fragmentAdapter);
        pager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        bar.setSelectedNavigationItem(position);
                    }
                });
    }

    void startVote(Bundle vote) {

        pager.setCurrentItem(FragmentAdapter.VOTE_FRAGMENT, true);
    }

    void setState(ConnectedState s) {
        state = s;
        if (stateListener != null)
            stateListener.onStateChanged(s);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadPreferences();
        setupBar();

        if (connector == null) {
            Handler connectorHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Log.i("conH", "Msg reached");
                    Bundle data = msg.getData();
                    JSONObject response;
                    String result;
                    try {
                        response = new JSONObject(data.getString("response"));
                        result = response.getString("result");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return;
                    }
                    switch (data.getInt("action")) {
                        case Connector.REGISTRATION_ACTION:
                            if (result.contains("success"))
                                setState(ConnectedState.CONNECTED);
                            else
                                setState(ConnectedState.DISCONNECTED);
                            break;
                        case Connector.CHANNEL_ACTION:
                            if (result.contains("success")) {
                                try {
                                    JSONObject channel = response.getJSONObject("channel");
                                    String host = channel.getString("host");
                                    int port = channel.getInt("port");
                                    // Create transmitter thread
                                    transmitter = new Thread(new Transmitter(host, port));
                                    transmitter.start();
                                    // Update state
                                    setState(ConnectedState.VOICE);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    return;
                                }
                            }
                            else
                                setState(ConnectedState.CONNECTED);
                            break;
                        case Connector.CHANNEL_CLOSE_ACTION:
                            if (transmitter != null && !transmitter.isInterrupted())
                                transmitter.interrupt();
                            setState(ConnectedState.CONNECTED);
                            break;
                        case Connector.VOTE_ACTION:
                            // TODO: Alert
//                        if (result.contains("success"))
//                            toast(R.string.toast_vote_success);
//                        else
//                            toast(R.string.toast_vote_error);
                            break;
                    }
                }
            };

            // Create connector
            Connector c = new Connector(connectorHandler);
            // Create connector thread
            connector = new Thread(c);
            connector.start();
            // Waiting for command handler creation
            while (c.getCommandHandler() == null);
            connectorCmd = c.getCommandHandler();
        }

        // start discover thread when not started
        if (discover == null) {
            // List adapters
            serverListAdapter =
                    new ArrayAdapter<String>(this,
                            android.R.layout.simple_list_item_single_choice, serverNameList);
            answerListAdapter =
                    new ArrayAdapter<String>(this,
                            android.R.layout.simple_list_item_single_choice, answerList);
            // discover message handler
            Handler serverInfoHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    // Get server info
                    Bundle server = msg.getData();
                    String serverId = server.getString("uuid");
                    if (!servers.containsKey(serverId)) {
                        servers.put(serverId, server);
                        // Save current server UUID
                        String currentServerId= null;
                        if (currentServer != -1)
                            currentServerId = serverUuidList.get(currentServer);
                        // Update names & UUIDs
                        serverNameList.clear();
                        serverUuidList.clear();
                        for (Bundle srv : servers.values()) {
                            serverNameList.add(srv.getString("name"));
                            serverUuidList.add(srv.getString("uuid"));
                        }
                        serverListAdapter.notifyDataSetChanged();
                        // Recover current server
                        if (currentServerId != null)
                            currentServer = serverUuidList.indexOf(currentServerId);
                        // Connect when first server
                        if (currentServer == -1) {
                            selectServer(0);
                        }
                    }
                }
            };

            // Vote message handler
            Handler voteInfoHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Bundle vote = msg.getData();
                    if (!votes.contains(vote.getString("uuid"))) {
                        votes.add(vote.getString("uuid"));
                        startVote(vote);
                    }
                }
            };

            discover = new Thread(new Discover(serverInfoHandler, voteInfoHandler, getBroadcastAddress()));
            discover.start();
        }
    }

// MAIN
    @Override
    public ConnectedState getConnectedState() {
        return state;
    }

    @Override
    public void channelRequest() {
        // Create channel request
        Bundle req = new Bundle();
        req.putInt("action", Connector.CHANNEL_ACTION);
        Message reqMsg = new Message();
        reqMsg.setData(req);
        connectorCmd.sendMessage(reqMsg);
        setState(ConnectedState.HAND_UP);
    }

    @Override
    public void channelClose() {
        Bundle req = new Bundle();
        req.putInt("action", Connector.CHANNEL_CLOSE_ACTION);
        Message reqMsg = new Message();
        reqMsg.setData(req);
        connectorCmd.sendMessage(reqMsg);
        setState(ConnectedState.CONNECTED);
    }

    @Override
    public void setStateChangedListener(StateChangedListener listener) {
        stateListener = listener;
    }

// VOTE

    @Override
    public String getQuestion() {
        return question;
    }

    @Override
    public ArrayAdapter<String> getAnswerListAdapter() {
        return  answerListAdapter;
    }

    @Override
    public void selectAnswer(int answer) {
//                int answer = data.getIntExtra("answer", 0);
//                String uuid = data.getStringExtra("uuid");
//                String mode = data.getStringExtra("mode");
//                Bundle req = new Bundle();
//                req.putInt("answer", answer);
//                req.putString("mode", mode);
//                req.putString("uuid", uuid);
//                req.putInt("action", Connector.VOTE_ACTION);
//                Message msg = new Message();
//                msg.setData(req);
//                connectorCmd.sendMessage(msg);
    }

// SERVER

    @Override
    public ArrayAdapter<String> getServerListAdapter() {
        return serverListAdapter;
    }

    @Override
    public int getCurrentServer() {
        return currentServer;
    }

    @Override
    public void selectServer(int srv) {
        // Double select check
        if (srv == currentServer) return;
        currentServer = srv;

        // Create registration request
        Bundle req = new Bundle();
        req.putInt("action", Connector.REGISTRATION_ACTION);
        req.putBundle("server", servers.get(serverUuidList.get(srv)));
        req.putBundle("user", userInfo);
        // Prepare message
        Message reqMsg = new Message();
        reqMsg.setData(req);
        connectorCmd.sendMessage(reqMsg);
    }

// MENU

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, RESULT_OK);
                break;
        }
        return true;
    }
}
