package com.b2kteam.csandroid.app;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.b2kteam.csandroid.app.R;

import java.util.ArrayList;

public class Voting extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voting);

        final Bundle params = getIntent().getExtras();

        ListView view = (ListView) findViewById(R.id.listView);

        ArrayAdapter<String> adapter = null;
        if (params.getString("mode").contains("custom"))
            adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_expandable_list_item_1, params.getStringArrayList("answers"));
        else {
            ArrayList<String> simple = new ArrayList<String>();
            simple.add("No");
            simple.add("Yes");
            adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_expandable_list_item_1, simple);
        }
        view.setAdapter(adapter);
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("answer", i);
                returnIntent.putExtra("uuid", params.getString("uuid"));
                returnIntent.putExtra("mode", params.getString("mode"));
                setResult(RESULT_OK, returnIntent);
                finish();
            }
        });
        TextView label = (TextView) findViewById(R.id.textView);
        label.setText(params.getString("question"));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.voting, menu);
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
}
