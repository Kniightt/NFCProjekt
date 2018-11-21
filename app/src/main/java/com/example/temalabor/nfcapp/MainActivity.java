package com.example.temalabor.nfcapp;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.tech.Ndef;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.HashMap;
import java.util.Map;

import hu.bme.aut.myapplication.TokenClass;

public class MainActivity extends AppCompatActivity
        implements NfcAdapter.CreateNdefMessageCallback {

    private FirebaseAuth auth;
    NFCHelper nfcHelper;

    private AutoCompleteTextView emailView;
    private EditText passwordView;
    private TextView statusText;
    private TextView uidText;
    private Button btnGetToken;
    private TextView tvTokenCount;

    TokenClass.Token token = null;

    private FirebaseFunctions function;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        nfcHelper = new NFCHelper(this);
        statusText = findViewById(R.id.status);
        uidText = findViewById(R.id.uid);
        emailView = findViewById(R.id.email);
        tvTokenCount = findViewById(R.id.token_count);
        passwordView = findViewById(R.id.password);
        passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button signInButton = findViewById(R.id.sign_in_button);
        Button registerButton = findViewById(R.id.register_button);
        Button signOutButton = findViewById(R.id.sign_out_button);
        signInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        signOutButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });
        function = FirebaseFunctions.getInstance();
        nfcHelper.getAdapter().setNdefPushMessageCallback(this, this);
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        updateUI(currentUser);
    }

    public void updateUI(final FirebaseUser currentUser) {
        if (currentUser != null) {
            statusText.setText(getString(R.string.signed_in, currentUser.getEmail()));
            uidText.setText(getString(R.string.firebase_uid, currentUser.getUid()));

            btnGetToken = findViewById(R.id.btn_get_token);
            btnGetToken.setVisibility(View.VISIBLE);
            btnGetToken.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    getToken(currentUser.getUid()).addOnCompleteListener(new OnCompleteListener<Map<String, String>>() {
                        @Override
                        public void onComplete(@NonNull Task<Map<String, String>> task) {
                            if (!task.isSuccessful()) {
                                Exception e = task.getException();
                                if (e instanceof FirebaseFunctionsException) {
                                    FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                    FirebaseFunctionsException.Code code = ffe.getCode();
                                    Object details = ffe.getDetails();
                                }
                                Log.w("MainActivity", "Failure", e);
                                showSnackbar("An error occurred.");
                                return;
                            }
                            Map<String, String> result = task.getResult();
                            tvTokenCount.setText(result.get("count"));
                            token = TokenClass.Token.newBuilder()
                                    .setUsername(currentUser.getUid())
                                    .setCount(Integer.parseInt(tvTokenCount.getText().toString()))
                                    .build();
                        }
                    });
                }
            });

            findViewById(R.id.buttons).setVisibility(View.GONE);
            findViewById(R.id.email_login_form).setVisibility(View.GONE);
            findViewById(R.id.signed_in_buttons).setVisibility(View.VISIBLE);


        } else {
            statusText.setText(R.string.signed_out);
            uidText.setText(null);
            if (btnGetToken != null)
                btnGetToken.setVisibility(View.INVISIBLE);
            if (tvTokenCount != null) tvTokenCount.setText("");


            findViewById(R.id.buttons).setVisibility(View.VISIBLE);
            findViewById(R.id.email_login_form).setVisibility(View.VISIBLE);
            findViewById(R.id.signed_in_buttons).setVisibility(View.GONE);

            nfcHelper.getAdapter().setNdefPushMessage(null, this);
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    private Task<Map<String, String>> getToken(String userID) {
        Map<String, Object> data = new HashMap<>();
        data.put("userid", userID);


        return function.getHttpsCallable("getToken")
                .call(data).continueWith(new Continuation<HttpsCallableResult, Map<String, String>>() {
                    @Override
                    public Map<String, String> then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        Map<String, String> result = (Map<String, String>) task.getResult().getData();
                        return result;
                    }
                });
    }

    private void attemptRegister() {

        String email = emailView.getText().toString();
        String password = passwordView.getText().toString();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            updateUI(user);
                        } else {
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }

    private void attemptLogin() {

        String email = emailView.getText().toString();
        String password = passwordView.getText().toString();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            updateUI(user);
                        } else {
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }

    private void logout() {
        auth.signOut();
        updateUI(null);
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        if (token != null) {
            return nfcHelper.createTextMessage(token);

        }

        return null;
    }


    @Override
    protected void onResume() {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Parcelable[] receivedArray = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (receivedArray != null) {
                NdefMessage message = (NdefMessage) receivedArray[0];
                NdefRecord[] records = message.getRecords();
                try {
                    TokenClass.Token token = TokenClass.Token.parseFrom(records[0].getPayload());
                    uidText.setText(token.getUsername());
                    tvTokenCount.setText(Integer.toString(token.getCount()));
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            }

        }
        super.onResume();
    }
}