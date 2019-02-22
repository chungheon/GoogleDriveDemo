package com.example.ishan.googledrivedemo;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseError;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends Activity {

    private static final String TAG = "GoogleDriveEncryption";
    private static final int REQUEST_CODE_SIGN_IN = 0;
    private static final String KEY_VERIFY_IN_PROGRESS = "key_verify_in_progress";
    private SecretKeySpec secret;
    private final int GROUP_SHARE_LOG_OUT = 3;

    public static FirebaseAuth mAuth;

    private TextView fileOutput;
    private EditText codeField;
    private EditText emailField;
    private EditText pwdField;
    private Button logIn;
    private Button sendCode;
    private Button resendCode;
    private Button logInEmail;
    private GoogleSignInClient googleSignInClient;
    private boolean doubleBackToExitPressedOnce = false;
    private TextView emailLabel;
    private TextView passLabel;

    private boolean mVerificationInProgress = false;
    private boolean mLinkInProgress = false;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        initWork();
        exqListener();
        if(isReadStoragePermissionGranted() && isWriteStoragePermissionGranted()){ }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            googleSignInClient.signOut();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }


    private void initWork() {
        this.fileOutput = (TextView) findViewById(R.id.textOutput);
        this.logIn = (Button) findViewById(R.id.logIn);
        this.googleSignInClient = buildGoogleSignInClient();
        this.codeField = (EditText) findViewById(R.id.code);
        this.sendCode = (Button) findViewById(R.id.sendCode);
        this.resendCode = (Button) findViewById(R.id.resendCode);
        this.logInEmail = (Button) findViewById(R.id.logInEmail);
        this.emailField = (EditText) findViewById(R.id.email);
        this.pwdField = (EditText) findViewById(R.id.pwd);
        this.passLabel = (TextView) findViewById(R.id.labelPass);
        this.emailLabel = (TextView) findViewById(R.id.labelEmail);

        this.sendCode.setVisibility(View.GONE);
        this.resendCode.setVisibility(View.GONE);
        this.codeField.setVisibility(View.GONE);


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
        this.secret = new SecretKeySpec(secretKey.getEncoded(), "AES");


        mAuth = FirebaseAuth.getInstance();

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:" + credential);
                // [START_EXCLUDE silent]
                mVerificationInProgress = false;
                mLinkInProgress = false;
                // [END_EXCLUDE]
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e);
                // [START_EXCLUDE silent]
                mVerificationInProgress = false;
                mLinkInProgress = false;
                // [END_EXCLUDE]

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    // [START_EXCLUDE]
                    fileOutput.setText("Invalid phone number.");
                    loggedIn();
                    // [END_EXCLUDE]
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // [START_EXCLUDE]
                    fileOutput.setText("Text Sent Quota exceeded");
                    // [END_EXCLUDE]
                }

                // Show a message and update the UI
                // [START_EXCLUDE]
                // [END_EXCLUDE]
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:" + verificationId);

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;
                fileOutput.setText("Code has been sent");
                verifyingPhone();
            }
        };
        // [END phone_auth_callbacks]
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        codeField.setText("");
        // [START start_phone_auth]
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                30,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
        // [END start_phone_auth]

        mVerificationInProgress = true;
    }

    private void linkPhoneNumberWithCode(String verificationId, String code){
        // [START verify_with_code]
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        // [END verify_with_code]

        mAuth.getCurrentUser().linkWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "linkWithCredential:success");
                            fileOutput.setText("Linked success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            loggedIn();
                            userLoggedIn();
                        } else {
                            Log.w(TAG, "linkWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Failed to link",
                                    Toast.LENGTH_SHORT).show();
                            loggedIn();
                        }
                    }
                });
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = task.getResult().getUser();
                            userLoggedIn();
                            loggedIn();
                            startGroupSharing();
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            fileOutput.setText("Invalid Code");
                            loggedOut();
                        }
                    }
                });
    }

    private void userLoggedIn(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String user = mAuth.getCurrentUser().getUid();
        DatabaseReference ref = database.getReference().child("users").child(user).child("nologins");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                try {
                    if (snapshot.getValue() != null) {
                        try {
                            Log.e("TAG", "" + snapshot.getValue());
                            long noLogins = (long) snapshot.getValue();
                            noLogins++;
                            ref.setValue(noLogins);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.e("TAG", " it's null.");
                        ref.setValue(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("TAG", " it's null.");
            }
        });
        Date currentTime = Calendar.getInstance().getTime();
        DatabaseReference ref2 = database.getReference().child("log").child(user).child(currentTime.toString());
        ref2.setValue("Logged-IN");
    }

    private void userLoggedOut(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String user = mAuth.getCurrentUser().getUid();
        DatabaseReference ref = database.getReference().child("users").child(user).child("nologouts");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                try {
                    if (snapshot.getValue() != null) {
                        try {
                            Log.e("TAG", "" + snapshot.getValue()); // your name values you will get here
                            long noLogins = (long) snapshot.getValue();
                            noLogins++;
                            ref.setValue(noLogins);
                            fileOutput.setText("Success");
                        } catch (Exception e) {
                            fileOutput.setText("Failure" + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        Log.e("TAG", " it's null.");
                        ref.setValue(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("TAG", " it's null.");
            }
        });
        Date currentTime = Calendar.getInstance().getTime();
        DatabaseReference ref2 = database.getReference().child("log").child(user).child(currentTime.toString());
        ref2.setValue("Logged-Out");
    }

    private void loggedIn(){
        logIn.setVisibility(View.GONE);
        logInEmail.setVisibility(View.GONE);
        emailField.setText("");
        emailField.setVisibility(View.GONE);
        pwdField.setText("");
        pwdField.setVisibility(View.GONE);
        codeField.setVisibility(View.GONE);
        sendCode.setVisibility(View.GONE);
        resendCode.setVisibility(View.GONE);
        emailLabel.setVisibility(View.GONE);
        passLabel.setVisibility(View.GONE);
    }

    private void loggedOut(){
        logIn.setVisibility(View.VISIBLE);
        logInEmail.setVisibility(View.VISIBLE);
        emailField.setVisibility(View.VISIBLE);
        pwdField.setVisibility(View.VISIBLE);
        emailLabel.setVisibility(View.VISIBLE);
        passLabel.setVisibility(View.VISIBLE);
        mLinkInProgress = false;
        fileOutput.setText("Logged Out");
    }

    private void linkPhone(){
        logIn.setVisibility(View.GONE);
        logInEmail.setVisibility(View.GONE);
        emailField.setText("");
        emailField.setVisibility(View.GONE);
        pwdField.setText("");
        pwdField.setVisibility(View.GONE);
        sendCode.setVisibility(View.GONE);
        resendCode.setVisibility(View.GONE);
    }

    private void resendVerificationCode(String phoneNumber,
                                        PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                30,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks,         // OnVerificationStateChangedCallbacks
                token);             // ForceResendingToken from callbacks
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_VERIFY_IN_PROGRESS, mVerificationInProgress);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mVerificationInProgress = savedInstanceState.getBoolean(KEY_VERIFY_IN_PROGRESS);
    }

    private void exqListener() {
        logIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });


        sendCode.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(mLinkInProgress){
                    linkPhoneNumberWithCode(mVerificationId, codeField.getText().toString());
                }else{
                    verifyPhoneNumberWithCode(mVerificationId, codeField.getText().toString());
                }
            }
        });

        resendCode.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                resendVerificationCode(mAuth.getCurrentUser().getPhoneNumber(), mResendToken);
            }
        });

        logInEmail.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(isNullOrEmpty(emailField.getText().toString()) || isNullOrEmpty(pwdField.getText().toString())) {
                    fileOutput.setText("Please enter your acct and password");
                }else{
                    firebaseAuthWithEmailPwd(emailField.getText().toString(), pwdField.getText().toString());
                }
            }
        });
    }

    /**
     * Start sign in activity.
     */
    private void signIn() {
        Log.i(TAG, "Start sign in");
        startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    /**
     * Build a Google SignIn client.
     */
    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
        return GoogleSignIn.getClient(this, signInOptions);
    }


    private static String getFileExtension(String fullName) {
        if (isNullOrEmpty(fullName)) {
            return null;
        }
        String fileName = new File(fullName).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    private static boolean isNullOrEmpty(String str) {
        if (str != null && !str.trim().isEmpty())
            return false;
        return true;
    }

    private boolean editTextIsEmpty(EditText etText) {
        if (etText.getText().toString().trim().length() > 0)
            return false;

        return true;
    }

    private void startGroupSharing(){
        Intent intent = new Intent(MainActivity.this, GroupSharing.class);
        startActivityForResult(intent, GROUP_SHARE_LOG_OUT);
    }

    private void firebaseAuthWithEmailPwd(String email, String password){
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            String phone = user.getPhoneNumber();
                            if(isNullOrEmpty(phone)){
                                loggedIn();
                                fileOutput.setText("Logged In");
                                mLinkInProgress = true;
                                startGroupSharing();
                                userLoggedIn();
                            }else{
                                verifyingPhone();
                                startPhoneNumberVerification(phone);
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            fileOutput.setText("Invalid user");
                        }

                        // ...
                    }
                });
    }

    private void verifyingPhone(){
        codeField.setVisibility(View.VISIBLE);
        sendCode.setVisibility(View.VISIBLE);
        resendCode.setVisibility(View.VISIBLE);
        emailField.setVisibility(View.GONE);
        pwdField.setVisibility(View.GONE);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            AuthResult a = task.getResult();
                            boolean newUser = a.getAdditionalUserInfo().isNewUser();
                            if(newUser){
                                mAuth.getCurrentUser().delete();
                                mAuth.signOut();
                                mAuth = FirebaseAuth.getInstance();
                                fileOutput.setText("Invalid User");
                            }else {
                                Log.d(TAG, "signInWithCredential:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                String phone = user.getPhoneNumber();
                                if (isNullOrEmpty(phone)) {
                                    loggedIn();
                                    fileOutput.setText("Logged In");
                                    mLinkInProgress = true;
                                    startGroupSharing();
                                    userLoggedIn();
                                } else {
                                    startPhoneNumberVerification(phone);
                                }
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            fileOutput.setText("Invalid User");
                        }
                    }
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
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account);
                    } catch (ApiException e) {
                        fileOutput.setText("FAILED APIEXCEPTION");
                    }
                } else {
                    fileOutput.setText("Failed to sign in");
                }
                break;
            case GROUP_SHARE_LOG_OUT:
                if(resultCode == RESULT_OK){
                    userLoggedOut();
                    loggedOut();
                    mAuth.signOut();
                }
        }
    }

    public boolean isReadStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted1");
                return true;
            } else {

                Log.v(TAG, "Permission is revoked1");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted1");
            return true;
        }
    }

    public boolean isWriteStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted2");
                return true;
            } else {

                Log.v(TAG, "Permission is revoked2");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted2");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 2:
                Log.d(TAG, "External storage2");
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
                    //resume tasks needing this permission
                } else {
                }
                break;

            case 3:
                Log.d(TAG, "External storage1");
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
                    //resume tasks needing this permission
                } else {
                }
                break;
        }
    }
}
