package com.example.oauththesis;

import static android.content.ContentValues.TAG;

import androidx.biometric.BiometricPrompt;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    GoogleSignInClient mGoogleSignInClient;
    private BiometricPrompt.CryptoObject myCrypto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);

        Button decryptButton = findViewById(R.id.decrypt);
        BioSecurity bioSecurity = new BioSecurity(MainActivity.this);

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bioSecurity.encrypt(new CryptoObjectListener() {
                    @Override
                    public void available(BiometricPrompt.CryptoObject cryptoObject) {
                        myCrypto=cryptoObject;
                        signIn();
//                        String plan_string = "This is my string";
//                        try {
//                            encryptedInfo = cryptoObject.getCipher().doFinal(
//                                    plan_string.getBytes(Charset.defaultCharset()));
//                        } catch (BadPaddingException e) {
//                            e.printStackTrace();
//                        } catch (IllegalBlockSizeException e) {
//                            e.printStackTrace();
//                        }
//                        Log.d("MY_APP_TAG", "Encrypted information: " +
//                                Arrays.toString(encryptedInfo));
                    }
                });
            }
        });
    }
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
            if (acct != null) {
                String personName = acct.getDisplayName();
                String personGivenName = acct.getGivenName();
                String personFamilyName = acct.getFamilyName();
                String personEmail = acct.getEmail();
                String personId = acct.getId();
                Uri personPhoto = acct.getPhotoUrl();

                byte[] pEmail = new byte[0];
                byte[] pName = new byte[0];
                byte[] pGivenName = new byte[0];
                byte[] pFamilyName = new byte[0];
                byte[] pID = new byte[0];

                try {
                    pEmail = myCrypto.getCipher().doFinal(
                            personEmail.getBytes(Charset.defaultCharset()));
                    pName = myCrypto.getCipher().doFinal(
                            personName.getBytes(Charset.defaultCharset()));
                    pGivenName = myCrypto.getCipher().doFinal(
                            personGivenName.getBytes(Charset.defaultCharset()));
                    pFamilyName = myCrypto.getCipher().doFinal(
                            personFamilyName.getBytes(Charset.defaultCharset()));
                    pID = myCrypto.getCipher().doFinal(
                            personId.getBytes(Charset.defaultCharset()));

                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
                Users users=new Users(pName.toString(), pGivenName.toString(), pFamilyName.toString(), pEmail.toString(), pID.toString(),personPhoto);
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                // Add a new document with a generated ID
                CollectionReference cities = db.collection("users");
                cities.document(users.personId).set(users);
                DocumentReference docRef = db.collection("users").document(personId);
                docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Log.d("Data", "DocumentSnapshot data: " + document.getData());
                            } else {
                                Log.d("No document", "No such document");
                            }
                        } else {
                            Log.d("failed to get", "get failed with ", task.getException());
                        }
                    }
                });

                //   abc =myRef.child(personId).get();
                Toast.makeText(this, "User email:"+personEmail, Toast.LENGTH_SHORT).show();
            }
//            startActivity(new Intent(MainActivity.this, SecondActivity.class));
            // Signed in successfully, show authenticated UI.
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.d("signInResult:failed code=", e.toString());
        }
    }
}