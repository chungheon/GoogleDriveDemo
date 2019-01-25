package com.example.ishan.googledrivedemo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

public class CreateGroup extends Activity {

    private Button addGroup;
    private TextView tv;
    private TextView message;
    private long numberOfGroups;
    private DatabaseReference rootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);
        initWork();
        exqListener();
    }

    private void initWork(){
        addGroup = (Button) findViewById(R.id.createGroup);
        tv = (TextView) findViewById(R.id.groupsNumber);
        message = (TextView) findViewById(R.id.message);
        addGroup.setEnabled(false);
        rootRef = FirebaseDatabase.getInstance().getReference();
        exqListener();
        calculateGroup();
    }

    private void exqListener(){
        addGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rootRef.child("groups").child("group_" + numberOfGroups).child("members").child(MainActivity.mAuth.getCurrentUser().getUid()).setValue("owner");
                rootRef.child("group_names").child("group_" + numberOfGroups).child("owner").setValue(MainActivity.mAuth.getCurrentUser().getUid(), new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        if(databaseError == null) {
                            message.setText("Group created");
                        }else{
                            message.setText("Unable to create group\n" + databaseError.getDetails());
                        }
                    }
                });
            }
        });
    }

    public void calculateGroup(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("group_names");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                numberOfGroups = dataSnapshot.getChildrenCount();
                tv.setText("Number of groups: " + numberOfGroups);
                addGroup.setEnabled(true);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                tv.setText("Unable to access");
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }
}
