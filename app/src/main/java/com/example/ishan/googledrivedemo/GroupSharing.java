package com.example.ishan.googledrivedemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.Drive;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static com.google.common.io.Files.getFileExtension;

public class GroupSharing extends Activity {

    private final String TAG = "GroupSharing";
    private DatabaseReference rootRef;
    private FirebaseStorage fbStore;
    private UploadTask uploadTask;
    private TextView tv;
    private StorageReference fileRef;
    private String fileExt;
    private long numberOfGroups;
    private Button groupAddBtn;
    private Button addBtn;
    private Button uploadFile;
    private Button userGroup;
    private Button downloadFile;
    private TextView groupNum;
    private SecretKeySpec secret;
    private String groupName;
    private boolean groupSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_sharing);
        Intent intent = getIntent();
        groupName = intent.getStringExtra("group");
        if(!groupName.equals("Group Not Selected")){
            groupSelected = true;
        }
        initWork();
        exqListener();
    }

    private void initWork(){
        rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("Groups");
        fbStore = FirebaseStorage.getInstance();
        tv = (TextView) findViewById(R.id.debug);
        groupAddBtn = (Button) findViewById(R.id.createGroup);
        addBtn = (Button) findViewById(R.id.addUser);
        uploadFile = (Button) findViewById(R.id.fileUpload);
        groupNum = (TextView) findViewById(R.id.displayID);
        userGroup = (Button) findViewById(R.id.viewGroups);
        downloadFile = (Button) findViewById(R.id.fileDownload);
        addBtn.setVisibility(View.GONE);
        tv.setText(groupName);

        byte[] ivBytes;
        String password="Hello";
        /*you can give whatever you want for password. This is for testing purpose*/
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[6];
        random.nextBytes(bytes);
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

    private void exqListener(){
        groupAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCreateGroupActivity();
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
                if(groupSelected){
                    selectFile();
                }else{
                    Toast.makeText(GroupSharing.this, "Please select a group first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        downloadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(groupSelected){
                    selectDownloadFile();
                }else{
                    Toast.makeText(GroupSharing.this, "Please select a group first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        userGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGroupChoosingActivity();
            }
        });
    }

    private void startCreateGroupActivity(){
        Intent intent = new Intent(this, CreateGroup.class);
        startActivity(intent);
    }

    private void startGroupChoosingActivity(){
        Intent intent = new Intent(this, GroupChooser.class);
        startActivity(intent);
    }

    private void selectFile(){
        FileChooser fileChooser = new FileChooser(this);

        fileChooser.setFileListener(new FileChooser.FileSelectedListener() {
            @Override
            public void fileSelected(final File file) {
                String fileExtension = getFileExtension(file.getPath());
                fileExt = fileExtension;
                String fileName = getFileName(file);
                fileRef = fbStore.getReference().child(MainActivity.mAuth.getCurrentUser().getUid().toString()).child(fileName);
                File encrypted = new File(file.getParent() +  "/UPLOADFILE");
                encryptFile(file, encrypted);
                uploadFile(encrypted, fileName);
            }
        });
        fileChooser.showDialog();
    }

    private void selectDownloadFile(){
        FirebaseFileChooser fileChooser = new FirebaseFileChooser(this, MainActivity.mAuth.getCurrentUser().getUid().toString(), groupName);

        fileChooser.setFileListener(new FirebaseFileChooser.FileSelectedListener() {
            @Override
            public void fileSelected(final String fileName) {
                String fileExtension = getFileExtension(fileName);
                fileExt = fileExtension;
                fileRef = fbStore.getReference().child(MainActivity.mAuth.getCurrentUser().getUid().toString()).child(fileName);
                downloadFile();
            }
        });
        fileChooser.showDialog();
    }

    private String getFileName(File file){
        String[] fileName = file.getName().split("\\.");
        return fileName[0];
    }


    private void downloadFile(){

        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();

        fileRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                fileExt = storageMetadata.getContentType();

                File localFile = new File(baseDir + "/Encrypted." + fileExt);
                File decrypted = new File(baseDir + "/Decrypted." + fileExt);

                fileRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        // Local temp file has been created
                        decryptFile(localFile, decrypted, secret);
                        localFile.delete();
                        tv.setText("Downloaded file");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                        localFile.delete();
                        decrypted.delete();
                        tv.setText("Failed to download");
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Uh-oh, an error occurred!
            }
        });


    }


    private void decryptFile(File encrypted, File decrypted, SecretKey secretKey) {
        try {
            FileInputStream inputStream = new FileInputStream(encrypted);
            byte[] inputBytes = new byte[(int) encrypted.length()];
            inputStream.read(inputBytes);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            FileOutputStream os = new FileOutputStream(decrypted);
            CipherOutputStream outputStream = new CipherOutputStream(os, cipher);
            outputStream.write(inputBytes);

            tv.setText("File decrypted");

            inputStream.close();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException | InvalidKeyException e) {
            e.printStackTrace();
            decrypted.delete();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            decrypted.delete();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }finally {
            //encrypted.delete();
        }
    }

    private void encryptFile(File file, File encrypted) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            byte[] inputBytes = new byte[(int) file.length()];
            inputStream.read(inputBytes);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            FileOutputStream os = new FileOutputStream(encrypted);
            CipherOutputStream outputStream = new CipherOutputStream(os, cipher);

            outputStream.write(inputBytes);
            /*byte[] input = new byte[64000];

            int bytesRead;
            while ((bytesRead = inputStream.read(input, 0, 64000)) != -1)
            {
                byte[] output = cipher.update(input, 0, bytesRead);
                if (output != null) outputStream.write(output);
            }*/

            inputStream.close();
            outputStream.close();
        } catch (InvalidKeyException e) {
        } catch (NoSuchAlgorithmException e) {
        } catch (NoSuchPaddingException e) {
        } catch (IOException e) {
        }
    }

    private void uploadFile(File file, String fileName){
        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        rootRef.child("storage").child("groups").child(groupName).child(MainActivity.mAuth.getCurrentUser().getUid().toString()).child(fileName).setValue("");
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(fileExt)
                .build();

        uploadTask = fileRef.putStream(stream, metadata);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                tv.setText("Failure");
                file.delete();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...

                tv.setText("Success");
                file.delete();
            }
        });
    }
}
