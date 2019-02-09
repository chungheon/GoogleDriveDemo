package com.example.ishan.googledrivedemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroupChooser extends Activity {

    private ArrayList<String> list_of_groups = new ArrayList<>();
    private DatabaseReference rootRef;
    private ListView groupsList;
    private FirebaseUser current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chooser);
        rootRef = FirebaseDatabase.getInstance().getReference();
        groupsList = (ListView) findViewById(R.id.group_list);
        current = MainActivity.mAuth.getCurrentUser();
        RetrieveAndDisplayGroups();
    }

    private void RetrieveAndDisplayGroups(){
        DatabaseReference groupRef = rootRef.child("group_names").child(current.getUid());
        groupRef.addListenerForSingleValueEvent(new ValueEventListener(){
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
                    groupsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> a, View v, int position,
                                                long id) {
                            isOwner(list_of_groups.get(position));
                        }
                    });
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

    private void isOwner(String groupName){
        DatabaseReference groupRef = rootRef.child("group_names").child(current.getUid()).child(groupName).child("owner");
        groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String owner = (String) dataSnapshot.getValue();
                Log.d("Compare", owner + " compare " + current.getUid());
                if(current.getUid().equals(owner)){
                    Log.d("Test", "SUCCESS");
                    startFileTransferActivity(groupName, true);
                }else{
                    Log.d("Test", "Not owner");
                    startFileTransferActivity(groupName, false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("Test", "Failure2");
                startFileTransferActivity(groupName, false);
            }
        });
    }

    private void startFileTransferActivity(String groupName, boolean owner){
        Intent intent = new Intent(this, GroupSharing.class);
        intent.putExtra("group", groupName);
        intent.putExtra("isOwner", owner);
        setResult(RESULT_OK, intent);
        finish();
    }
}
