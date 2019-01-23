package com.example.ishan.googledrivedemo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static com.google.common.io.Files.getFileExtension;

public class GroupSharing extends Activity {

    private DatabaseReference rootRef;
    private FirebaseStorage fbStore;
    private UploadTask uploadTask;
    private TextView tv;
    private StorageReference fileRef;
    private long numberOfGroups;
    private Button groupAddBtn;
    private Button addBtn;
    private Button uploadFile;
    private EditText groupNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_sharing);
        rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("Groups");
        fbStore = FirebaseStorage.getInstance();
        tv = (TextView) findViewById(R.id.debug);
        groupAddBtn = (Button) findViewById(R.id.createGroup);
        groupAddBtn.setEnabled(false);
        addBtn = (Button) findViewById(R.id.addUser);
        uploadFile = (Button) findViewById(R.id.fileUpload);
        groupNum = (EditText) findViewById(R.id.groupNumber);
        calculateGroup();
        exqListener();
    }

    public void exqListener(){
        groupAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rootRef.child("Groups").child("Group_" + numberOfGroups).child("users").child(MainActivity.mAuth.getCurrentUser().getUid()).setValue("a");
            }
        });

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        uploadFile.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                selectFile();
            }
        });
    }

    public void calculateGroup(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Groups");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                    numberOfGroups = dataSnapshot.getChildrenCount();
                    tv.setText("Number of groups: " + numberOfGroups);
                    groupAddBtn.setEnabled(true);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                tv.setText("Unable to access");
            }
        });

    }

    public void selectFile(){
        FileChooser fileChooser = new FileChooser(this);

        fileChooser.setFileListener(new FileChooser.FileSelectedListener() {
            @Override
            public void fileSelected(final File file) {
                String fileExtension = getFileExtension(file.getPath());
                fileRef = fbStore.getReference().child("test." + fileExtension);
                uploadFile(file);
            }
        });
        fileChooser.showDialog();
    }

    public void uploadFile(File file){
        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        uploadTask = fileRef.putStream(stream);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                tv.setText("Failure");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...

                tv.setText("Success");
            }
        });
    }
}
