package hackthis.team.spartapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVInstallation;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.PushService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilterActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    ArrayList<Club> clubs;
    AVQuery<AVObject> query;

    View.OnClickListener CANCEL = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };
    View.OnClickListener CONFIRM = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            save();
        }
    };

    ListView root;

    boolean failed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        Log.d("filter_activity_log","filter activity created");
        query = new AVQuery<>("Clubs");
        clubs = new ArrayList<>(50);
        query.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> list, AVException e) {
                if ( e == null){
                    for(int i = 0; i < list.size(); i++){
                        clubs.add(new Club(list.get(i).getString("name")));
                    }

                    SharedPreferences sp = getSharedPreferences("clubs", Context.MODE_PRIVATE);
                    Set<String> subscribed_names = sp.getStringSet("subscribed",null);
                    int subscribedCount = 0;
                    if(subscribed_names == null){
                        for(int i = 0; i < clubs.size(); i++){
                            if(clubs.get(i).name.equals("Student Council") ||
                                    clubs.get(i).name.equals("{Hack,THIS}")){
                                clubs.get(i).checked = true;
                                Club temp = clubs.get(subscribedCount);
                                clubs.set(subscribedCount, clubs.get(i));
                                clubs.set(i, temp);
                                i--;
                                subscribedCount++;
                            }
                        }
                    }
                    else{
                        List<String> subscribed = new ArrayList<>(subscribed_names);
                        List<Club> selected_temp = new ArrayList<>(10);
                        for(int i = 0; i < clubs.size(); i++){
                            for(int j = 0; j < subscribed.size(); j++) {
                                if (clubs.get(i).name.equals(subscribed.get(j))) {
                                    clubs.get(i).checked = true;
                                    selected_temp.add(clubs.get(i));
                                    clubs.remove(i);
                                    i--;
                                    break;
                                }
                            }
                        }
                        for(int i = 0; i < selected_temp.size(); i++)
                            clubs.add(0,selected_temp.get(i));
                    }
                    failed = false;
                    update();
                }
                else{
                    failed = true;
                    error();
                }
            }
        });

        Button cancel = findViewById(R.id.filter_cancel);
        cancel.setOnClickListener(CANCEL);

        Button confirm = findViewById(R.id.filter_confirm);
        confirm.setOnClickListener(CONFIRM);

    }

    public void update(){
        Log.d("filter_activity_log","update called");
        ListView root = (ListView) findViewById(R.id.filter_list);
        final ClubAdapter MyAdapter = new ClubAdapter(FilterActivity.this, root, clubs);
        root.setAdapter(MyAdapter);

        Log.d("filter_activity", clubs.toString());

    }

    public void error(){
        Toast t = Toast.makeText(this, "An error has occurred, please connect to the internet", Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER,0,0);
        t.show();
    }

    public void save(){
        if(failed) { finish(); return;}

            ListView root = (ListView) findViewById(R.id.filter_list);
            ArrayList<String> sub = new ArrayList<>(50);

            Log.d("filter_activity", clubs.toString());

            for (int i = 0; i < clubs.size(); i++) {
                if (clubs.get(i).checked) {
                    sub.add(clubs.get(i).name);
                    PushService.subscribe(this, clubs.get(i).getCorrespondingChannel(), MainActivity.class);
                }
                else{
                    PushService.unsubscribe(this, clubs.get(i).getCorrespondingChannel());
                }
            }

            SharedPreferences sp = getSharedPreferences("clubs", Context.MODE_PRIVATE);
            sp.edit().putStringSet("subscribed", new HashSet<>(sub)).apply();

        AVInstallation.getCurrentInstallation().saveInBackground();

        finish();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        clubs.get(position).checked = ! clubs.get(position).checked;
    }
}
