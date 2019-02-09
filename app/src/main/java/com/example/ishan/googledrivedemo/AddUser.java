package com.example.ishan.googledrivedemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AddUser extends Activity {

    private FirebaseUser current;
    private ArrayList<String> list_of_users = new ArrayList<>();
    private ArrayList<String> userNames = new ArrayList<>();
    private DatabaseReference rootRef;
    private ListView usersList;
    private String groupName;
    private ArrayList<String> groupMembers;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);
        Intent data = getIntent();
        Bundle args = data.getExtras();
        rootRef = FirebaseDatabase.getInstance().getReference();
        usersList = (ListView) findViewById(R.id.userList);
        current = MainActivity.mAuth.getCurrentUser();
        groupMembers = args.getStringArrayList("groupmembers");
        groupName = args.getString("group");
        RetrieveAndDisplayUsers();
    }

    private void RetrieveAndDisplayUsers(){
        DatabaseReference GroupRef = rootRef.child("users");
        GroupRef.addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                List<String> set = new ArrayList<>();
                List<String> names = new ArrayList<>();
                Iterator iterator = dataSnapshot.getChildren().iterator();

                while(iterator.hasNext())
                {
                    DataSnapshot data = (DataSnapshot) iterator.next();
                    if(!inGroup(data.getKey())){
                        set.add(data.getKey());
                        names.add((String) data.child("name").getValue());
                    }
                }

                if(set.isEmpty()){
                    Toast.makeText(AddUser.this,"There are no other users", Toast.LENGTH_SHORT);
                }else{
                    list_of_users.clear();
                    list_of_users.addAll(set);
                    userNames.clear();
                    userNames.addAll(names);
                    ArrayAdapter<String> itemsAdapter;
                    itemsAdapter = new ArrayAdapter<String>(AddUser.this, android.R.layout.simple_list_item_1, list_of_users);
                    usersList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                            addUser(position);
                        }
                    });
                    usersList.setAdapter(itemsAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AddUser.this,"Error", Toast.LENGTH_SHORT);
                AddUser.super.onBackPressed();
            }
        });
    }

    private void addUser(int position){
        String user = list_of_users.get(position);
        DatabaseReference addUserRef = rootRef.child("group_names").child(user).child(groupName).child("owner");
        addUserRef.setValue(current.getUid());
        DatabaseReference groupRef = rootRef.child("groups").child(groupName).child("members").child(user);
        groupRef.setValue("").addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Intent intent = new Intent(AddUser.this, GroupSharing.class);
                setResult(RESULT_OK, intent);
                finish();
            }

        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Intent intent = new Intent(AddUser.this, GroupSharing.class);
                setResult(RESULT_CANCELED, intent);
                finish();
            }
        });
    }

    private boolean inGroup(String user){
        for(String member: groupMembers){
            if(user.equals(member)){
                return true;
            }
        }
        return false;
    }
}
