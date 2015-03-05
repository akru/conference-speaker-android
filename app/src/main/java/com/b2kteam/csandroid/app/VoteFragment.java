package com.b2kteam.csandroid.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

interface VoteHandler {
    void startVote(Bundle vote);
    void stopVote();
}

interface VoteInteraction {
    void setVoteHandler(VoteHandler handler);
    void selectAnswer(int answer);
}

public class VoteFragment extends Fragment implements AdapterView.OnItemClickListener, VoteHandler {
    VoteInteraction interaction = null;
    Bundle          voteBundle  = null;

    public VoteFragment() {
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
        return inflater.inflate(R.layout.fragment_vote, container, false);
    }

    @Override
    public void onAttach(Activity attachedActivity) {
        super.onAttach(attachedActivity);
        interaction = (VoteInteraction) attachedActivity;
        interaction.setVoteHandler(this);
    }

//    @Override
//    public void onDetach() {
//        super.onDetach();
//    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        interaction.selectAnswer(position);
        stopVote();
    }

    @Override
    public void startVote(Bundle vote) {
        voteBundle = vote;
        updateView();
    }

    @Override
    public void stopVote() {
        voteBundle = null;
        updateView();
    }

    void updateView() {
        if (getView() == null) return;
        if (voteBundle == null) {
            TextView label = (TextView) getView().findViewById(R.id.questionText);
            label.setText(R.string.vote_not_started);
            ListView view = (ListView) getView().findViewById(R.id.answerList);
            view.setAdapter(null);
            return;
        }

        ListView view = (ListView) getView().findViewById(R.id.answerList);

        ArrayList<String> answerList = new ArrayList<String>();
        if (voteBundle.getString("mode").equals("custom")) {
            answerList = voteBundle.getStringArrayList("answers");
        } else {
            answerList.add(getResources().getText(android.R.string.no).toString());
            answerList.add(getResources().getText(android.R.string.yes).toString());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_single_choice, answerList);
        view.setAdapter(adapter);
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                interaction.selectAnswer(i);
                stopVote();
            }
        });
        TextView label = (TextView) getView().findViewById(R.id.questionText);
        label.setText(voteBundle.getString("question"));
    }
}
