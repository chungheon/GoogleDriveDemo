package com.example.ishan.googledrivedemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.MainThread;
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
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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
    private SecretKeySpec secret;

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

        byte[] ivBytes;
        String password="Hello";
        /*you can give whatever you want for password. This is for testing purpose*/
        SecureRandom random = new SecureRandom();
        byte bytes[] = {1,1,1,1,1,1};
        byte[] saltBytes = bytes;
        // Derive the key
        SecretKeyFactory factory = null;
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(),saltBytes,65556,256);
        SecretKey secretKey = null;
        try {
            secretKey = factory.generateSecret(spec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        secret = new SecretKeySpec(secretKey.getEncoded(), "AES");
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
                File encrypted = new File(file.getParent() + "/Encrypt." + fileExtension);
                encryptFile(file, encrypted);
                uploadFile(encrypted);
            }
        });
        fileChooser.showDialog();
    }


    public void downloadFile(){

        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
         File localFile = new File(baseDir + "/Encrypted.jpg");
         File decrypted = new File(baseDir + "/Decrypted.jpg");

        fileRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
                decryptFile(localFile, decrypted);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
    }


    private void decryptFile(File encrypted, File decrypted) {
        try {
            FileInputStream inputStream = new FileInputStream(encrypted);
            byte[] inputBytes = new byte[(int) encrypted.length()];
            inputStream.read(inputBytes);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secret);

            FileOutputStream os = new FileOutputStream(decrypted);
            CipherOutputStream outputStream = new CipherOutputStream(os, cipher);
            outputStream.write(inputBytes);

            inputStream.close();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    public void encryptFile(File file, File encrypted) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            byte[] inputBytes = new byte[(int) file.length()];
            inputStream.read(inputBytes);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            FileOutputStream os = new FileOutputStream(encrypted);
            CipherOutputStream outputStream = new CipherOutputStream(os, cipher);
            outputStream.write(inputBytes);

            inputStream.close();
            outputStream.close();
        } catch (InvalidKeyException e) {
        } catch (NoSuchAlgorithmException e) {
        } catch (NoSuchPaddingException e) {
        } catch (IOException e) {
        }
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
                downloadFile();
            }
        });
    }
}
