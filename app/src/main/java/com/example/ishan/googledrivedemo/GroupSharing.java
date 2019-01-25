package com.example.ishan.googledrivedemo;

import android.app.Activity;
import android.content.Intent;
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
    private String fileExt;
    private long numberOfGroups;
    private Button groupAddBtn;
    private Button addBtn;
    private Button uploadFile;
    private Button userGroup;
    private TextView groupNum;
    private SecretKeySpec secret;
    private String groupName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_sharing);
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
        addBtn.setVisibility(View.GONE);

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
                String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
                File localFile = new File(baseDir + "/Encrypted.mp4");
                File decrypted = new File(baseDir + "/Decrypted.mp4");

                decryptFile(localFile, decrypted);
            }
        });

        uploadFile.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                selectFile();
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
                fileRef = fbStore.getReference().child("test." + fileExtension);
                File encrypted = new File(file.getParent() + "/Encrypt." + fileExtension);
                encryptFile(file, encrypted);
                uploadFile(encrypted);
            }
        });
        fileChooser.showDialog();
    }


    private void downloadFile(){

        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();

        fileRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                fileExt= storageMetadata.getContentType();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Uh-oh, an error occurred!
            }
        });

        File localFile = new File(baseDir + "/Encrypted." + fileExt);
        File decrypted = new File(baseDir + "/Decrypted." + fileExt);

        fileRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
                decryptFile(localFile, decrypted);
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

    private void uploadFile(File file){
        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

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
                downloadFile();
            }
        });
    }
}
