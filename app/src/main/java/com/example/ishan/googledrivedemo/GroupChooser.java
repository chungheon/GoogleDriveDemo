package com.example.ishan.googledrivedemo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GroupChooser extends Activity {

    private ArrayList<String> list_of_groups = new ArrayList<>();
    private DatabaseReference rootRef;
    private ListView groupsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chooser);
        rootRef = FirebaseDatabase.getInstance().getReference();
        groupsList = (ListView) findViewById(R.id.group_list);
        RetrieveAndDisplayGroups();

    }

    private void RetrieveAndDisplayGroups(){
        DatabaseReference GroupRef = rootRef.child("group_names");
        GroupRef.addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                List<String> set = new ArrayList<>();
                Iterator iterator = dataSnapshot.getChildren().iterator();

                while(iterator.hasNext())
                {
                    set.add(((DataSnapshot)iterator.next()).getKey());
                }

                if(set.isEmpty()){
                    Toast.makeText(GroupChooser.this,"User is not in any group", Toast.LENGTH_SHORT);
                }else{
                    list_of_groups.clear();
                    list_of_groups.addAll(set);
                    ArrayAdapter<String> itemsAdapter;
                    itemsAdapter = new ArrayAdapter<String>(GroupChooser.this, android.R.layout.simple_list_item_1, list_of_groups);
                    groupsList.setAdapter(itemsAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(GroupChooser.this,"Error", Toast.LENGTH_SHORT);
                GroupChooser.super.onBackPressed();
            }
        });
    }
}
