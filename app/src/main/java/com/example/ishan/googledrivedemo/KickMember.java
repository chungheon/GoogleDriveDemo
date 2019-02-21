package com.example.ishan.googledrivedemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import javax.xml.namespace.NamespaceContext;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class KickMember extends Activity {

    private ArrayList<String> userNames = new ArrayList<>();
    private ArrayList<String> groupMembers = new ArrayList<>();
    private DatabaseReference rootRef;
    private ListView memberList;
    private FirebaseUser current;
    private String groupName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kick_member);
        Intent data = getIntent();
        Bundle args = data.getExtras();
        userNames = args.getStringArrayList("usernames");
        groupMembers = args.getStringArrayList("groupmembers");
        groupName = args.getString("group");
        rootRef = FirebaseDatabase.getInstance().getReference();
        memberList = (ListView) findViewById(R.id.member_list);
        current = MainActivity.mAuth.getCurrentUser();
        RetrieveAndDisplayGroups();
    }

    private void RetrieveAndDisplayGroups(){
        ArrayAdapter<String> itemsAdapter;
        itemsAdapter = new ArrayAdapter<String>(KickMember.this, android.R.layout.simple_list_item_1, userNames);
        memberList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                kickMember(position);
            }
        });
        memberList.setAdapter(itemsAdapter);
    }

    private void kickMember(int position){
        String uid = groupMembers.get(position);
        DatabaseReference groupRef = rootRef.child("group_names").child(uid).child(groupName);
        groupRef.removeValue();
        groupRef = rootRef.child("groups").child(groupName).child("members").child(uid);
        groupRef.removeValue();
        userNames.remove(position);
        groupMembers.remove(position);
        startFileTransferActivity();
    }

    private void startFileTransferActivity(){
        Intent intent = new Intent(this, GroupSharing.class);
        setResult(RESULT_OK, intent);
        finish();
    }
}
