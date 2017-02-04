package net.derohimat.firebaseassistance;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class MainActivity extends AppCompatActivity {

    //Auth
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    //Remote Config
    private static final String DISCOUNT_CONFIG_KEY = "discount";
    private static final String PRICE_CONFIG_KEY = "price";
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private int cacheExpiration = 43200;
    private long initialPrice, finalPrice;

    //Database
    private FirebaseDatabase mDatabase;
    private DatabaseReference mDbRef;

    //View
    private EditText inpEmail, inpPassword;
    private Button btnDaftar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inpEmail = (EditText) findViewById(R.id.inpEmail);
        inpPassword = (EditText) findViewById(R.id.inpPassword);

        btnDaftar = (Button) findViewById(R.id.btnDaftar);

        setUpFCM();
        setUpAuth();
        setUpRemoteConfig();
    }

    private void setUpFCM() {
        String token = FirebaseInstanceId.getInstance().getToken();
        assert token != null;
        if (!token.isEmpty()) {
            Log.i("TOKEN FCM:", "token ada");
        }
        FirebaseMessaging.getInstance().subscribeToTopic("news");

        // Log and toast
        String msg = getString(R.string.msg_token_fmt, token);
        Log.d("Message", msg);
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    private void setUpAuth() {
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(MainActivity.class.getName(), "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(MainActivity.class.getName(), "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };

        btnDaftar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = inpEmail.getText().toString();
                String password = inpPassword.getText().toString();

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                //  Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                                // If sign in fails, display a message to the user. If sign in succeeds
                                // the auth state listener will be notified and logic to handle the
                                // signed in user can be handled in the listener.
                                if (!task.isSuccessful()) {
                                    Toast.makeText(MainActivity.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    FirebaseUser user = task.getResult().getUser();
                                    String uid = user.getUid();
                                    updateUser(uid);
                                }

                                // ...
                            }
                        });
            }
        });

    }

    private void setUpRemoteConfig() {
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config);
        // cacheExpirationSeconds is set to cacheExpiration here, indicating that any previously
        // fetched and cached config would be considered expired because it would have been fetched
        // more than cacheExpiration seconds ago. Thus the next fetch would go to the server unless
        // throttling is in progress. The default expiration duration is 43200 (12 hours).
        // mFirebaseRemoteConfig.fetch(cacheExpiration)
        mFirebaseRemoteConfig.fetch()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(MainActivity.class.getName(), "Fetch Succeeded");
                            // Once the config is successfully fetched it must be activated before newly fetched
                            // values are returned.
                            mFirebaseRemoteConfig.activateFetched();
                        } else {
                            Log.d(MainActivity.class.getName(), "Fetch failed");
                        }
                        displayPrice();
                    }
                });
    }

    private void displayPrice() {
        initialPrice = mFirebaseRemoteConfig.getLong(PRICE_CONFIG_KEY);
        finalPrice = initialPrice - mFirebaseRemoteConfig.getLong(DISCOUNT_CONFIG_KEY);
    }

    private void updateUser(String uid) {
        // Write a message to the database
        mDatabase = FirebaseDatabase.getInstance();
        mDbRef = mDatabase.getReference("message");

        mDbRef.setValue("Hello, World!");
    }


    private void getUser() {
        // Read from the database
        mDbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                Log.d(MainActivity.class.getName(), "Value is: " + value);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(MainActivity.class.getName(), "Failed to read value.", error.toException());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
