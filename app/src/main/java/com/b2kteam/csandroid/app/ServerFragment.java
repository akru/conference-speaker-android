package com.b2kteam.csandroid.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ServerFragment extends Fragment implements AdapterView.OnItemClickListener {
    ServerInteraction interaction;

    public ServerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_server, container, false);

        ListView serverView = (ListView) view.findViewById(R.id.serverList);
        serverView.setAdapter(interaction.getServerListAdapter());
        serverView.setOnItemClickListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ListView serverView = (ListView) getView().findViewById(R.id.serverList);
        serverView.setItemChecked(interaction.getCurrentServer(), true);
    }

    @Override
    public void onAttach(Activity attachedActivity) {
        super.onAttach(attachedActivity);
        interaction = (ServerInteraction) attachedActivity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i("SF", String.valueOf(position));
        interaction.selectServer(position);
    }
}

interface ServerInteraction {
    ArrayAdapter<String> getServerListAdapter();
    int getCurrentServer();
    void selectServer(int server);
}