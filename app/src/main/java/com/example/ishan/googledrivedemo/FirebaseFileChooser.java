package com.example.ishan.googledrivedemo;

import android.app.Activity;
import android.app.Dialog;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FirebaseFileChooser {
    private static final String PARENT_DIR = "..";

    private final Activity activity;
    private ListView list;
    private Dialog dialog;
    private FirebaseDatabase dBase;
    private String file;
    private String uid;
    private String groupName;
    private List<String> list_of_files;

    // filter on file extension
    private String extension = null;
    public void setExtension(String extension) {
        this.extension = (extension == null) ? null :
                extension.toLowerCase();
    }

    // file selection event handling
    public interface FileSelectedListener {
        void fileSelected(String fileRef);
    }

    public FirebaseFileChooser setFileListener(FileSelectedListener fileListener) {
        this.fileListener = fileListener;
        return this;
    }
    private FileSelectedListener fileListener;

    public FirebaseFileChooser(Activity activity, String uid, String groupName) {
        this.activity = activity;
        this.uid = uid;
        this.groupName = groupName;
        dialog = new Dialog(activity);
        list = new ListView(activity);
        list_of_files = new ArrayList<>();
        dBase = FirebaseDatabase.getInstance();
        populateList();
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int which, long id) {
                String fileChosen = (String) list.getItemAtPosition(which);

                if (fileListener != null) {
                    fileListener.fileSelected(fileChosen);
                }
                dialog.dismiss();
            }
        });
        dialog.setContentView(list);
        dialog.getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
    }

    private void populateList(){
        DatabaseReference fileRef = dBase.getReference().child("storage").child("groups").child(groupName).child(this.uid);
        fileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> set = new ArrayList<>();
                Iterator iterator = dataSnapshot.getChildren().iterator();

                while(iterator.hasNext())
                {
                    set.add(((DataSnapshot)iterator.next()).getKey());
                }
                if(set.isEmpty()){
                    dialog.dismiss();
                }else{
                    list_of_files.clear();
                    list_of_files.addAll(set);
                    ArrayAdapter<String> itemsAdapter;
                    list.setAdapter(new ArrayAdapter(activity,
                            android.R.layout.simple_list_item_1, list_of_files) {
                        @Override public View getView(int pos, View view, ViewGroup parent) {
                            view = super.getView(pos, view, parent);
                            ((TextView) view).setSingleLine(true);
                            return view;
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    public void showDialog() {
        dialog.show();
    }
}