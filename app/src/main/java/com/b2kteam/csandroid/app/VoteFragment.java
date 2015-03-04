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

public class VoteFragment extends Fragment implements AdapterView.OnItemClickListener {
    VoteInteraction interaction;

    public VoteFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ListView view = (ListView) activity.findViewById(R.id.listView);

//        ArrayAdapter<String> adapter = null;
//        if (params.getString("mode").contains("custom"))
//            adapter = new ArrayAdapter<String>(this,
//                    android.R.layout.simple_expandable_list_item_1, params.getStringArrayList("answers"));
//        else {
//            ArrayList<String> simple = new ArrayList<String>();
//            simple.add("No");
//            simple.add("Yes");
//            adapter = new ArrayAdapter<String>(this,
//                    android.R.layout.simple_expandable_list_item_1, simple);
//        }
//        view.setAdapter(adapter);
//        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                Intent returnIntent = new Intent();
//                returnIntent.putExtra("answer", i);
//                returnIntent.putExtra("uuid", params.getString("uuid"));
//                returnIntent.putExtra("mode", params.getString("mode"));
//                setResult(RESULT_OK, returnIntent);
//                finish();
//            }
//        });
//        TextView label = (TextView) findViewById(R.id.textView);
//        label.setText(params.getString("question"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_vote, container, false);
        ListView answerView = (ListView) view.findViewById(R.id.answerList);
        answerView.setAdapter(interaction.getAnswerListAdapter());
        answerView.setOnItemClickListener(this);
        return view;
    }

    @Override
    public void onAttach(Activity attachedActivity) {
        super.onAttach(attachedActivity);
        interaction = (VoteInteraction) attachedActivity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        interaction.selectAnswer(position);
    }
}

interface VoteInteraction {
    String getQuestion();
    ArrayAdapter<String> getAnswerListAdapter();
    void selectAnswer(int answer);
}
