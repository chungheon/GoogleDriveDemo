package com.example.ishan.googledrivedemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Base64;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
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

import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.bouncycastle.jce.provider.JDKPSSSigner;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import static com.google.common.io.Files.getFileExtension;

public class GroupSharing extends Activity {

    private final String TAG = "GroupSharing";
    private final int REQUEST_CREATEGROUP= 1;
    private final int REQUEST_ADDUSER = 2;
    private final int REQUEST_SELECTGROUP = 3;
    private final int REQUEST_KICKUSER = 4;
    private DatabaseReference rootRef;
    private FirebaseStorage fbStore;
    private FirebaseUser currentUser;
    private UploadTask uploadTask;
    private TextView tv;
    private StorageReference fileRef;
    private String fileExt;
    private long numberOfGroups;
    private Button kickUser;
    private Button groupAddBtn;
    private Button addBtn;
    private Button uploadFile;
    private Button userGroup;
    private Button downloadFile;
    private Button uploadKey;
    private Button logOutBtn;
    private TextView groupNum;
    private SecretKeySpec secret;
    private String groupName;
    private ArrayList<String> groupMembers;
    private ArrayList<Pair<String, Pair<Key, Certificate>>> keys;
    private boolean groupSelected = false;
    private ArrayList<Pair<String,String>> userNames;
    private boolean isOwner = false;
    private String nameUser;
    private ArrayList<String> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_sharing);
        Intent intent = getIntent();
        initWork();
        exqListener();
    }

    private void initWork(){
        rootRef = FirebaseDatabase.getInstance().getReference();
        currentUser = MainActivity.mAuth.getCurrentUser();
        fbStore = FirebaseStorage.getInstance();
        tv = (TextView) findViewById(R.id.display);
        groupAddBtn = (Button) findViewById(R.id.createGroup);
        addBtn = (Button) findViewById(R.id.addUser);
        uploadFile = (Button) findViewById(R.id.fileUpload);
        groupNum = (TextView) findViewById(R.id.displayID);
        userGroup = (Button) findViewById(R.id.viewGroups);
        downloadFile = (Button) findViewById(R.id.fileDownload);
        uploadKey = (Button) findViewById(R.id.keyUpload);
        kickUser = (Button) findViewById(R.id.kickUser);
        addBtn.setVisibility(View.VISIBLE);
        logOutBtn = (Button) findViewById(R.id.logOut);
        groupMembers = new ArrayList<String>();
        userNames = new ArrayList<>();
        users = new ArrayList<>();
        addBtn.setEnabled(false);
        kickUser.setEnabled(false);
        uploadFile.setEnabled(false);
        groupName = "Group Not Selected";
        tv.setText("Group Not Selected");
        getUserName();
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
                if(groupSelected) {
                    startAddUserActivity();
                }else{
                    Toast.makeText(GroupSharing.this, "Please select a group first", Toast.LENGTH_SHORT).show();
                }
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

        kickUser.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(groupSelected){
                    startKickMemberActivity();
                }else{
                    Toast.makeText(GroupSharing.this, "Please select a group first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        uploadKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadNewKey();
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

        logOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void getUserName(){
        rootRef.child("users").child(currentUser.getUid()).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                nameUser = (String) dataSnapshot.getValue();
                groupAddBtn.setEnabled(true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                finish();
            }
        });
    }

    private void getNames(){
        DatabaseReference groupRef = rootRef.child("users");
        groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                users.clear();
                userNames.clear();
                Iterator iterator = dataSnapshot.getChildren().iterator();
                while(iterator.hasNext()){
                    DataSnapshot userInfo = (DataSnapshot) iterator.next();
                    String uid = userInfo.getKey();
                    users.add(uid);
                    Iterator iterator1 = userInfo.getChildren().iterator();
                    while(iterator1.hasNext()){
                        DataSnapshot info = (DataSnapshot) iterator1.next();
                        if(info.getKey().equals("name")){
                            userNames.add(new Pair<String, String>((String) info.getValue(), uid));
                            if(userNames.size() == users.size()){
                                addBtn.setEnabled(true);
                                kickUser.setEnabled(true);
                            }
                        }
                    }
                    tv.setText(tv.getText());
                }
                //getMemberName();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(GroupSharing.this,"Error", Toast.LENGTH_SHORT);
                tv.setText("Connection unstable");
            }
        });
    }

    private void getMemberName(){
        for(String uid: users){
            DatabaseReference userRef = rootRef.child("users").child(uid).child("name");
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String name = dataSnapshot.getKey();
                    userNames.add(new Pair<String, String>(name, uid));
                    if(userNames.size() == users.size()){
                        addBtn.setEnabled(true);
                        kickUser.setEnabled(true);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(GroupSharing.this,"Error", Toast.LENGTH_SHORT);
                    tv.setText("Connection unstable");
                }
            });
        }

    }

    private void getGroupMembersList(){
        DatabaseReference usersRef = rootRef.child("groups").child(groupName).child("members");
        keys = new ArrayList<>();
        usersRef.addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> set = new ArrayList<>();
                Iterator iterator = dataSnapshot.getChildren().iterator();

                while(iterator.hasNext())
                {
                    DataSnapshot data = (DataSnapshot) iterator.next();
                    set.add(data.getKey());
                }

                if(set.isEmpty()){
                    groupMembers.addAll(set);
                }else{
                    groupMembers.clear();
                    groupMembers.addAll(set);
                }
                keys.clear();
                for(String member: groupMembers){
                    getPublicKey(member);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(GroupSharing.this,"ERROR - please try again", Toast.LENGTH_SHORT);
                tv.setText("Group Not Selected");
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case REQUEST_ADDUSER:
                if(resultCode == RESULT_OK) {
                    tv.setText("User Added to group");
                    Bundle args = data.getExtras();
                    String uid  = args.getString("groupmembers");
                    uploadFile.setEnabled(false);
                    getGroupMembersList();
                }else{
                    tv.setText("Unable to add user");
                }
                break;
                case REQUEST_CREATEGROUP:
                    if(resultCode == RESULT_OK) {
                        tv.setText("Group Created/n" + groupName);
                    }else{
                        tv.setText("Unabled to create group");
                    }
                    break;
                case REQUEST_SELECTGROUP:
                    if(resultCode == RESULT_OK){
                        Bundle args = data.getExtras();
                        this.groupName = args.getString("group");
                        this.groupSelected = true;
                        isOwner = args.getBoolean("isOwner", false);
                        addBtn.setEnabled(false);
                        kickUser.setEnabled(false);
                        uploadFile.setEnabled(false);
                        tv.setText(groupName);
                        if(isOwner){
                            addBtn.setVisibility(View.VISIBLE);
                            kickUser.setVisibility(View.VISIBLE);
                        }else{
                            addBtn.setVisibility(View.GONE);
                            kickUser.setVisibility(View.GONE);
                        }
                        getNames();
                        getGroupMembersList();
                    }else{
                        tv.setText("Unable to select group");
                    }
                    break;
            case REQUEST_KICKUSER:
                    if(resultCode == RESULT_OK){
                        Bundle args = data.getExtras();
                        tv.setText("Member has been removed");
                        uploadFile.setEnabled(false);
                        getGroupMembersList();
                    }else{
                        tv.setText("Unable to remove member");
                    }
        }
    }

    public void createKeys() throws NoSuchProviderException,
            NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, UnrecoverableEntryException {
        Calendar start = new GregorianCalendar();
        Calendar end = new GregorianCalendar();
        end.add(Calendar.YEAR, 1);
        String mAlias = MainActivity.mAuth.getCurrentUser().getUid();
        KeyPairGenerator kpGenerator = KeyPairGenerator
                .getInstance("RSA");
        AlgorithmParameterSpec spec;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

            spec = new KeyPairGeneratorSpec.Builder(this)
                    .setAlias(mAlias)
                    .setSubject(new X500Principal("CN=" + mAlias))
                    .setSerialNumber(BigInteger.valueOf(1337))
                    .setStartDate(start.getTime())
                    .setEndDate(end.getTime())
                    .build();


        } else {
            spec = new KeyGenParameterSpec.Builder(mAlias, KeyProperties.PURPOSE_SIGN)
                    .setCertificateSubject(new X500Principal("CN=" + mAlias))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .setCertificateSerialNumber(BigInteger.valueOf(1337))
                    .setCertificateNotBefore(start.getTime())
                    .setCertificateNotAfter(end.getTime())
                    .build();
        }


        KeyPair kp = kpGenerator.generateKeyPair();

        try {
            Certificate cert = SelfSignedCertificateGeneration.selfSign(kp,"CN=" + mAlias );
            KeyStore ks = null;
            try {
                ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(null);
                Certificate[] certChain = new Certificate[1];
                certChain[0] = cert;
                ks.setKeyEntry("PublicKey", kp.getPublic(), null, certChain);
                storePrivate(kp, certChain);

                String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();

                File keystore =  new File(baseDir + "/Download/test.jks");

                FileOutputStream out = new FileOutputStream(keystore);

                ks.store(out, "password".toCharArray());

                Log.e(TAG, "Keystore file created");

                out.close();

            } catch (FileNotFoundException e) {
                Log.e(TAG, "File Not Found" + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "IOException" + e.getMessage());
            } catch (KeyStoreException e) {
                Log.e(TAG, "KeyStoreException" + e.getMessage());
            } catch (CertificateException e) {
                Log.e(TAG, "CertificateException" + e.getMessage());
            }
        } catch (OperatorCreationException e) {
            e.printStackTrace();
        }
        // END_INCLUDE(create_spec)

    }

    private void storePrivate(KeyPair keyPair, Certificate[] certChain) throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        ks.setKeyEntry(currentUser.getUid(), keyPair.getPrivate(), null, certChain);
    }

    private void uploadNewKey(){
        try {
            createKeys();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            tv.setText("No Such Algorithm");
        } catch (KeyStoreException e) {
            tv.setText("KeyStore Not Found");
        } catch (IOException e) {
            tv.setText("ERROR, FILE NOT FOUND");
        } catch (CertificateException e) {
            tv.setText("ERROR, Certificate Not VALID");
        } catch (UnrecoverableEntryException e) {
            tv.setText("Key Not Found");
        }
        uploadKeyFile();
    }

    private void uploadKeyFile(){
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        File keystore =  new File(baseDir + "/Download/test.jks");
        InputStream stream = null;
        try {
            stream = new FileInputStream(keystore);
        } catch (FileNotFoundException e) {
            tv.setText("File Not Found");
        }
        String userID = MainActivity.mAuth.getCurrentUser().getUid();
        fileRef = fbStore.getReference().child("publickeys").child(userID);
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(".jks")
                .build();

        uploadTask = fileRef.putStream(stream, metadata);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                tv.setText("Failed to set new key");
                keystore.delete();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
                tv.setText("New key set");
                keystore.delete();
            }
        });
    }


    private void startCreateGroupActivity(){
        Intent intent = new Intent(this, CreateGroup.class);
        intent.putExtra("username", nameUser);
        startActivityForResult(intent, REQUEST_CREATEGROUP);
    }

    private void startGroupChoosingActivity(){
        Intent intent = new Intent(this, GroupChooser.class);
        startActivityForResult(intent, REQUEST_SELECTGROUP);
    }

    private void startAddUserActivity(){
        Intent intent = new Intent (this, AddUser.class);
        intent.putExtra("group", groupName);
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> uid = new ArrayList<>();
        for(Pair<String, String> user: userNames){
            if(!inGroup(user.second)){
                names.add(user.first);
                uid.add(user.second);
            }
        }
        intent.putExtra("usernames", names);
        intent.putExtra("groupmembers", uid);
        startActivityForResult(intent, REQUEST_ADDUSER);
    }

    private boolean inGroup(String user){
        for(String uid: groupMembers){
            if(user.equals(uid)){
                return true;
            }
        }
        return false;
    }

    public void startKickMemberActivity(){
        Intent intent = new Intent(GroupSharing.this, KickMember.class);
        intent.putExtra("group", groupName);
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> uid = new ArrayList<>();
        for(Pair<String, String> user: userNames){
            tv.setText(tv.getText() + " " + groupMembers.size());
            if(inGroup(user.second) && !user.second.equals(currentUser.getUid())){
                names.add(user.first);
                uid.add(user.second);
            }
        }
        intent.putExtra("usernames", names);
        intent.putExtra("groupmembers", uid);
        startActivityForResult(intent, REQUEST_KICKUSER);
    }


    private void selectFile(){
        FileChooser fileChooser = new FileChooser(this);

        fileChooser.setFileListener(new FileChooser.FileSelectedListener() {
            @Override
            public void fileSelected(final File file) {
                String fileExtension = getFileExtension(file.getPath());
                fileExt = fileExtension;
                String fileName = getFileName(file);
                fileRef = fbStore.getReference().child(MainActivity.mAuth.getCurrentUser().getUid()).child(fileName);
                File encrypted = new File(file.getParent() + "/UPLOAD");
                encryptFile(file, encrypted);
                String hash = HashFile.hashFile(file);
                uploadFile(encrypted, fileName, groupMembers.size()-1, hash);
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
                String fileName = storageMetadata.getName();
                String signedHash = storageMetadata.getCustomMetadata("signedhash");
                String secretKey = storageMetadata.getCustomMetadata("secretkey");
                String hash = storageMetadata.getCustomMetadata("hash");
                String sender = storageMetadata.getCustomMetadata("sender");
                boolean signatureVerified = false;
                Certificate cert = null;
                for(Pair<String, Pair<Key, Certificate>> userKey: keys){
                    if(userKey.first.equals(sender)){
                        cert = userKey.second.second;
                    }
                }
                try {
                    Signature s = Signature.getInstance("SHA256withRSA");
                    if(cert != null){
                        s.initVerify(cert);
                        s.update(hash.getBytes());
                        byte[] signature = Base64.decode(signedHash, Base64.DEFAULT);
                        signatureVerified = s.verify(signature);
                    }
                } catch (NoSuchAlgorithmException e) {
                    Toast.makeText(GroupSharing.this, "Unable to verify signature", Toast.LENGTH_SHORT).show();
                    return;
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (SignatureException e) {
                    e.printStackTrace();
                }
                if(signatureVerified){
                    File localFile = new File(baseDir + "/Encrypted." + fileExt);
                    File decrypted = new File(baseDir + "/" + fileName + "." + fileExt);


                    try {
                        SecretKeySpec secretKeySpec = getSecretKey(secretKey);
                        fileRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                // Local temp file has been created
                                decryptFile(localFile, decrypted, secretKeySpec);
                                String decryptHash = HashFile.hashFile(decrypted);
                                if(verifyHash(hash, decryptHash)){
                                    localFile.delete();
                                    tv.setText("Downloaded file");
                                    recordDownload(fileName + "." + fileExt);
                                }else{
                                    localFile.delete();
                                    decrypted.delete();
                                    tv.setText("Hash of file is different, file is dangerous and has been deleted");
                                    recordDownload(fileName + "." + fileExt + " (Dangerous)");
                                }

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

                    } catch (KeyStoreException e) {
                        tv.setText("KeyStoreException");
                    } catch (CertificateException e) {
                        tv.setText("CertificateException");
                    } catch (NoSuchAlgorithmException e) {
                        tv.setText("NoSuchAlgorithmException");
                    } catch (IOException e) {
                        tv.setText("IOException");
                    } catch (UnrecoverableKeyException e) {
                        tv.setText("UnrecoverableKeyException");
                    } catch (InvalidKeyException e) {
                        tv.setText("InvalidKeyException");
                    } catch (NoSuchPaddingException e) {
                        tv.setText("NoSuchPaddingException");
                    } catch (BadPaddingException e) {
                        tv.setText("BadPaddingException");
                    } catch (IllegalBlockSizeException e) {
                        tv.setText("IllegalBlockSizeException");
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                tv.setText(exception.getMessage());
            }
        });
    }

    private boolean verifyHash(String hash, String decryptHash){
        if(hash.equals(decryptHash)){
            return true;
        }
        return false;
    }

    private SecretKeySpec getSecretKey(String encrypted) throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException, UnrecoverableKeyException, InvalidKeyException,
            NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        PrivateKey key = (PrivateKey) ks.getKey(currentUser.getUid(), null);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedText = Base64.decode(encrypted, 0);
        SecretKeySpec keyAES = new SecretKeySpec(cipher.doFinal(decryptedText), "AES");
        return keyAES;
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

            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256); // for example
            SecretKey secretKey = keyGen.generateKey();
            secret = new SecretKeySpec(secretKey.getEncoded(), "AES");
            /*AES for future reference
            String password="Hello";
            //you can give whatever you want for password. This is for testing purpose
            SecureRandom random = new SecureRandom();
            byte bytes[] = new byte[10];
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
            secret = new SecretKeySpec(secretKey.getEncoded(), "AES");*/

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

    private void getPublicKey(String user){
        StorageReference userKey = fbStore.getReference().child("publickeys/"+user);
        String baseDir = Environment.getExternalStorageDirectory().getPath();
            userKey.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                @Override
                public void onSuccess(StorageMetadata storageMetadata) {
                    String extType = storageMetadata.getContentType();
                    File tempFile = new File(baseDir + "/" + user + "keystore.jks");
                    final long ONE_MEGABYTE = 1024 * 1024;
                    userKey.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            Log.d(TAG, "KEY UPLOADED FROM FB " + keys.size());
                            try {
                                FileOutputStream os = new FileOutputStream(tempFile);
                                os.write(bytes);
                                os.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            addKeyToKeyArray(tempFile, user);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                                tempFile.delete();
                                Log.d(TAG, "FAILED TO DOWNLOAD " + e.getMessage());
                        }
                    });
                }

            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.d(TAG, "FAILED TO FIND FILE " + keys.size());
                }
            });
    }

    private void addKeyToKeyArray(File file, String user){
        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream is = new FileInputStream(file);
            ks.load(is, "password".toCharArray());
            Key key = ks.getKey("PublicKey", null);
            Certificate cert = ks.getCertificate("PublicKey");
            Pair<String, Pair<Key, Certificate>> userKey = new Pair<>(user, new Pair<>(key, cert));
            keys.add(userKey);
            Log.d(TAG, "KEY ADDED TO ARRAY " + keys.size());
            if(keys.size() == groupMembers.size()){
                uploadFile.setEnabled(true);
            }
            file.delete();
        } catch (KeyStoreException e) {
            Log.e(TAG, "NO SUCH KEYSTORE: BKS");
        } catch (CertificateException e) {
            Log.e(TAG, "Certificate Exception");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "NoSuchAlgorithmException");
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException");
        } catch (IOException e) {
            Log.e(TAG, "IOException");
        } catch (UnrecoverableKeyException e) {
            Log.e(TAG, "UnrecoverableKeyException" + user);
        }
    }

    private void uploadFile(File file, String fileName,int count, String hash){
        if(count == -1){
            recordUpload(currentUser.getUid(), fileName);
            tv.setText("Successfully Uploaded");
            file.delete();
            return;
        }
        String signedHash = signHash(hash);
        if(!signedHash.equals("Error")){
            String member = groupMembers.get(count);
                Key key = getKey(member);
                String encryptedKey = encryptKey(secret, key);
                if(!encryptedKey.equals("Failed")){
                    fileRef = fbStore.getReference().child(member).child(fileName);
                    fileRef.getDownloadUrl().addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            InputStream stream = null;
                            try {
                                stream = new FileInputStream(file);StorageMetadata metadata = new StorageMetadata.Builder()
                                        .setContentType(fileExt)
                                        .setCustomMetadata("hash", hash)
                                        .setCustomMetadata("signedhash", signedHash)
                                        .setCustomMetadata("secretkey", encryptedKey)
                                        .setCustomMetadata("sender", currentUser.getUid())
                                        .setCustomMetadata("receiver", member)
                                        .build();

                                uploadTask = fileRef.putStream(stream, metadata);
                                uploadTask.addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        tv.setText("Failure to upload " + member);
                                    }
                                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        rootRef.child("storage").child("groups").child(groupName).child(member).child(fileName).setValue("");
                                        uploadFile(file, fileName, count-1, hash);
                                    }
                                });

                            } catch (FileNotFoundException fileException) {
                                tv.setText(fileException.getMessage());
                            }
                        }
                    }).addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Random r = new Random();
                            uploadFile(file, fileName + r.nextInt(), count, hash);
                        }
                    });
                }else{
                    tv.setText("Unable to encrypt key");
                }
        }else{
            tv.setText("Unable to sign hash");
        }
    }

    private Key getKey(String user){
        for(Pair<String, Pair<Key, Certificate>> userKey: keys){
            if(userKey.first.equals(user)){
                return userKey.second.first;
            }
        }
        return null;
    }

    private String encryptKey(SecretKeySpec secret, Key key){
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, (PublicKey) key);
            byte[] encryptedText = cipher.doFinal(secret.getEncoded());
            String result = Base64.encodeToString(encryptedText, Base64.DEFAULT);
            return result;
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return "Failed";
    }

    private void recordDownload(String fileName){
        DatabaseReference userRef = rootRef.child("users").child(currentUser.getUid()).child("noDownloads");
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (snapshot.getValue() != null) {
                        try {
                            Log.e("TAG", "" + snapshot.getValue()); // your name values you will get here
                            long noDl = (long) snapshot.getValue();
                            noDl++;
                            userRef.setValue(noDl);
                            Date currentTime = Calendar.getInstance().getTime();
                            DatabaseReference ref2 = rootRef.child("log").child(currentUser.getUid()).child(currentTime.toString());
                            ref2.setValue("Download: " + fileName);
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage());
                            return;
                        }
                    } else {
                        Log.e("TAG", " it's null.");
                        userRef.setValue(1);
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    return;
                }

                return;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG,  "Its Null");
                return;
            }
        });
        return;
    }

    private void recordUpload(String sender, String fileName){
        DatabaseReference userRef = rootRef.child("users").child(sender).child("noUploads");
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (snapshot.getValue() != null) {
                        try {
                            Log.e("TAG", "" + snapshot.getValue()); // your name values you will get here
                            long noUpload = (long) snapshot.getValue();
                            noUpload++;
                            userRef.setValue(noUpload);
                            Date currentTime = Calendar.getInstance().getTime();
                            DatabaseReference ref2 = rootRef.child("log").child(currentUser.getUid()).child(currentTime.toString());
                            ref2.setValue("Upload: " + fileName);
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage());
                            return;
                        }
                    } else {
                        Log.e("TAG", " it's null.");
                        userRef.setValue(1);
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    return;
                }

                return;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG,  "Its Null");
                return;
            }
        });

        return;
    }

    private String signHash(String data){
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            Key pk = ks.getKey(currentUser.getUid(), null);
            Signature s = Signature.getInstance("SHA256withRSA");

            s.initSign((PrivateKey) pk);

            byte[] unsignedData = data.getBytes();
            s.update(unsignedData);
            byte[] signature = s.sign();

            return Base64.encodeToString(signature, Base64.DEFAULT);
        } catch (KeyStoreException e) {
            tv.setText("KeyStore Not Found");
        } catch (CertificateException e) {
            tv.setText("Certificate Exception");
        } catch (NoSuchAlgorithmException e) {
            tv.setText("No such Algorithmn Exception");
        } catch (IOException e) {
            tv.setText("IO Exception");
        } catch (UnrecoverableKeyException e) {
            tv.setText("No such key found");
        } catch (SignatureException e) {
            tv.setText("Signature Invalid");
        } catch (InvalidKeyException e) {
            Log.e(TAG, e.getMessage());
            tv.setText("Invalid Key SignHash");
        }
        return "Error";
    }

}
