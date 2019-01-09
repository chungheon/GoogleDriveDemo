package com.example.ishan.googledrivedemo;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.*;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class MainActivity extends Activity {

    private static final String TAG = "GoogleDriveEncryption";
    private static final int REQUEST_CODE_SIGN_IN = 0;
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_OPEN_ITEM = 5;
    private SecretKeySpec secret;

    private DriveClient mDriveClient;
    private DriveResourceClient mDriveResourceClient;
    private TaskCompletionSource<DriveId> mOpenItemTaskSource;
    private String retrieveExt = "";

    private TextView fileOutput;
    private Button logIn;
    private Button logOut;
    private Button upload;
    private Button download;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(isReadStoragePermissionGranted() && isWriteStoragePermissionGranted()){
            initWork();
            exqListener();
        }
    }

    private void initWork(){
        this.fileOutput = (TextView) findViewById(R.id.textOutput);
        this.logIn = (Button) findViewById(R.id.logIn);
        this.logOut = (Button) findViewById(R.id.logOut);
        this.upload = (Button) findViewById(R.id.uploadFile);
        this.download = (Button) findViewById(R.id.download);

        this.logOut.setEnabled(false);
        this.upload.setEnabled(false);
        this.download.setEnabled(false);

        final String password = "password";

        // Here the magic numbers
        final int pswdIterations = 65536;
        final int keySize = 128;

        final byte[] saltBytes = {0, 1, 2, 3, 4, 5, 6};

        SecretKeyFactory factory = null;
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, pswdIterations, keySize);
            SecretKey secretKey = factory.generateSecret(spec);
            this.secret = new SecretKeySpec(secretKey.getEncoded(), "AES");
            final KeyGenerator kg = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) { }
        catch (InvalidKeySpecException e) { }
    }

   private void exqListener(){
        logIn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        upload.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                uploadFile();
            }
        });

        download.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                pickTextFile();
            }
        });
    }

    /** Start sign in activity. */
    private void signIn() {
        Log.i(TAG, "Start sign in");
        GoogleSignInClient GoogleSignInClient = buildGoogleSignInClient();
        startActivityForResult(GoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    /** Build a Google SignIn client. */
    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .build();
        return GoogleSignIn.getClient(this, signInOptions);
    }



    private void uploadFile(){
        FileChooser fileChooser = new FileChooser(this);

        fileChooser.setFileListener(new FileChooser.FileSelectedListener() {
            @Override
            public void fileSelected(final File file) {
                String fileExtension = getFileExtension(file.getPath());
                if(fileExtension != null) {
                    byte[] encrypted = encryptFile(file);
                    if(encrypted == null){
                        Toast.makeText(MainActivity.this, "ERROR ENCRYPTING", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveFileToDrive(encrypted, fileExtension);
                }
            }
        });
        fileChooser.showDialog();
    }

    private static String getFileExtension(String fullName) {
        if(isNullOrEmpty(fullName)){
            return null;
        }
        String fileName = new File(fullName).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    private static boolean isNullOrEmpty(String str) {
        if(str != null && !str.trim().isEmpty())
            return false;
        return true;
    }

    private void encryptFile(File file, File encrypted){
        try {
            FileInputStream inputStream = new FileInputStream(file);
            byte[] inputBytes = new byte[(int) file.length()];
            inputStream.read(inputBytes);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, this.secret);
            //byte[] outputBytes = cipher.doFinal(inputBytes);
            FileOutputStream os = new FileOutputStream(encrypted);
            CipherOutputStream outputStream = new CipherOutputStream(os,cipher);
            outputStream.write(inputBytes);

            inputStream.close();
            outputStream.close();
        }catch (InvalidKeyException e){
            fileOutput.setText("InvalidKey");
        }catch (NoSuchAlgorithmException e){
            fileOutput.setText("NoSuchAlgorithm");
        }catch (NoSuchPaddingException e){
            fileOutput.setText("NoSuchPadding");
        }catch (IOException e){
            fileOutput.setText("IOException");
        }
    }

    private byte[] encryptFile(File file){
        try {
            FileInputStream inputStream = new FileInputStream(file);
            byte[] inputBytes = new byte[(int) file.length()];
            inputStream.read(inputBytes);
            byte[] outputByte = new byte[(int) file.length()];

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, this.secret);
            //byte[] outputBytes = cipher.doFinal(inputBytes);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            CipherOutputStream outputStream = new CipherOutputStream(os,cipher);
            InputStream is = new ByteArrayInputStream(inputBytes);
            byte[] buffer = new byte[1024];
            int len;
            while((len=is.read(buffer)) != -1){
                outputStream.write(buffer, 0, len);
            }

            outputByte = os.toByteArray();

            is.close();
            inputStream.close();
            outputStream.close();

            return outputByte;
        }catch (InvalidKeyException e){
            fileOutput.setText("InvalidKey");
        }catch (NoSuchAlgorithmException e){
            fileOutput.setText("NoSuchAlgorithm");
        }catch (NoSuchPaddingException e){
            fileOutput.setText("NoSuchPadding");
        }catch (IOException e){
            fileOutput.setText("IOException");
        }
        return null;
    }

    private void decryptFile(File encrypted, File decrypted){
        try {
            FileInputStream inputStream = new FileInputStream(encrypted);
            byte[] inputBytes = new byte[(int) encrypted.length()];
            inputStream.read(inputBytes);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secret);
            //byte[] outputBytes = cipher.doFinal(inputBytes);

            FileOutputStream os = new FileOutputStream(decrypted);
            CipherOutputStream outputStream = new CipherOutputStream(os,cipher);
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

    private void decryptFile(byte[] encrypted, File decrypted){
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secret);
            //byte[] outputBytes = cipher.doFinal(inputBytes);

            FileOutputStream os = new FileOutputStream(decrypted);
            CipherOutputStream outputStream = new CipherOutputStream(os,cipher);
            outputStream.write(encrypted);

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

    private void saveFileToDrive(byte[] encrypted, String ext) {
        // Start by creating a new contents, and setting a callback.
        Log.i(TAG, "Creating new contents.");

        mDriveResourceClient
                .createContents()
                .continueWithTask(
                        task -> createFileIntentSender(task.getResult(), encrypted, ext))
                .addOnFailureListener(
                        e -> Log.w(TAG, "Failed to create new contents.", e));
    }

    private Task<Void> createFileIntentSender(DriveContents driveContents, byte[] encrypted, String ext) {
        Log.i(TAG, "New contents created.");
        // Get an output stream for the contents.
        OutputStream outputStream = driveContents.getOutputStream();

        try {
                outputStream.write(encrypted);
        } catch (IOException e) {
            Log.w(TAG, "Unable to write file contents.", e);
        }

        // Create the initial metadata - MIME type and title.
        // Note that the user will be able to change the title later.
        MetadataChangeSet metadataChangeSet =
                new MetadataChangeSet.Builder()
                        .setMimeType("*/*")
                        .setTitle("Encrypt." + ext)
                        .build();
        // Set up options to configure and display the create file activity.
        CreateFileActivityOptions createFileActivityOptions =
                new CreateFileActivityOptions.Builder()
                        .setInitialMetadata(metadataChangeSet)
                        .setInitialDriveContents(driveContents)
                        .build();

        return mDriveClient
                .newCreateFileActivityIntentSender(createFileActivityOptions)
                .continueWith(
                        task -> {
                            startIntentSenderForResult(task.getResult(), REQUEST_CODE_CREATOR, null, 0, 0, 0);
                            return null;
                        });
    }

    protected Task<DriveId> pickTextFile() {
        OpenFileActivityOptions openOptions =
                new OpenFileActivityOptions.Builder()
                        .setSelectionFilter(Filters.eq(SearchableField.MIME_TYPE, "*/*"))
                        .setActivityTitle("decrypted")
                        .build();
        return pickItem(openOptions);
    }

    private Task<DriveId> pickItem(OpenFileActivityOptions openOptions) {
        mOpenItemTaskSource = new TaskCompletionSource<>();
        mDriveClient.newOpenFileActivityIntentSender(openOptions)
                .continueWith((Continuation<IntentSender, Void>) task -> {
                    startIntentSenderForResult(
                            task.getResult(), REQUEST_CODE_OPEN_ITEM, null, 0, 0, 0);
                    return null;
                });
        return mOpenItemTaskSource.getTask();
    }

    private void retrieveMetadata(final DriveFile file) {
        Task<Metadata> getMetadataTask = mDriveResourceClient.getMetadata(file);
        getMetadataTask
                .addOnSuccessListener(this,
                        metadata -> {
                            retrieveExt = getFileExtension(metadata.getTitle());
                        })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Unable to retrieve metadata", e);
                });
    }

    private void retrieveContents(DriveFile file) {
        // [START drive_android_open_file]
        Task<DriveContents> openFileTask =
                mDriveResourceClient.openFile(file, DriveFile.MODE_READ_ONLY);
        // [END drive_android_open_file]
        // [START drive_android_read_contents]
        openFileTask
                .continueWithTask(task -> {
                    DriveContents contents = task.getResult();
                    InputStream is = contents.getInputStream();
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;

                    while((len=is.read(buffer)) != -1){
                        os.write(buffer, 0, len);
                    }

                    byte bytes[] = os.toByteArray();
                    String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();

                    /*try (FileOutputStream stream = new FileOutputStream(baseDir + "/Encrypt." + retrieveExt)) {
                        stream.write(bytes);
                    }*/

                    File decrypt = new File(baseDir + "/Decrypt." + retrieveExt);
                    decryptFile(os.toByteArray(), decrypt);

                    fileOutput.setText("File saved as " + baseDir + "/Decrypt." + retrieveExt);

                    Task<Void> discardTask = mDriveResourceClient.discardContents(contents);
                    // [END drive_android_discard_contents]
                    return discardTask;
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    // [START_EXCLUDE]
                    Log.e(TAG, "Unable to read contents", e);

                    // [END_EXCLUDE]
                });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                Log.i(TAG, "Sign in request code");
                // Called after user is signed in.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Signed in successfully.");
                    // Use the last signed in account here since it already have a Drive scope.
                    mDriveClient = Drive.getDriveClient(this, GoogleSignIn.getLastSignedInAccount(this));
                    // Build a drive resource client.
                    mDriveResourceClient =
                            Drive.getDriveResourceClient(this, GoogleSignIn.getLastSignedInAccount(this));
                    // Start camera.
                    fileOutput.setText("Successfully Signed In");
                    this.logIn.setEnabled(false);
                    this.logOut.setEnabled(true);
                    this.upload.setEnabled(true);
                    this.download.setEnabled(true);
                }else{
                    fileOutput.setText("Failed to sign in");
                }
                break;
            case REQUEST_CODE_CREATOR:
                Log.i(TAG, "creator request code");
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    fileOutput.setText("Upload successful");
                }else{
                    fileOutput.setText("Upload unsuccessful");
                }
                break;
            case REQUEST_CODE_OPEN_ITEM:
                if (resultCode == RESULT_OK) {
                    DriveId driveId = data.getParcelableExtra(
                            OpenFileActivityOptions.EXTRA_RESPONSE_DRIVE_ID);
                    mOpenItemTaskSource.setResult(driveId);
                    retrieveMetadata(driveId.asDriveFile());
                    retrieveContents(driveId.asDriveFile());
                } else {
                    mOpenItemTaskSource.setException(new RuntimeException("Unable to open file"));
                }
                break;
        }
    }

    public boolean isReadStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted1");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked1");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted1");
            return true;
        }
    }

    public  boolean isWriteStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted2");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked2");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted2");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 2:
                Log.d(TAG, "External storage2");
                if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
                    //resume tasks needing this permission
                }else{
                }
                break;

            case 3:
                Log.d(TAG, "External storage1");
                if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
                    //resume tasks needing this permission
                }else{
                }
                break;
        }
    }
}
