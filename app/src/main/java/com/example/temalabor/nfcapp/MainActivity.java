package com.example.temalabor.nfcapp;

import android.nfc.NdefMessage;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFunctions function;
    private FirebaseUser user;

    private AutoCompleteTextView emailView;
    private EditText passwordView;
    private TextView statusText;
    private TextView uidText;
    private TextView tokenText;

    NFCHelper nfcHelper;
    TokenClass.Token token = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        function = FirebaseFunctions.getInstance();
        nfcHelper = new NFCHelper(this);

        emailView = findViewById(R.id.email);
        passwordView = findViewById(R.id.password);
        statusText = findViewById(R.id.status);
        uidText = findViewById(R.id.uid);
        tokenText = findViewById(R.id.token);

        Button signInButton = findViewById(R.id.sign_in_button);
        Button registerButton = findViewById(R.id.register_button);
        Button signOutButton = findViewById(R.id.sign_out_button);
        Button btnGetToken = findViewById(R.id.btn_get_token);

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

        btnGetToken.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                getToken(user.getUid()).addOnCompleteListener(new OnCompleteListener<Map<String, Object>>() {
                    @Override
                    public void onComplete(@NonNull Task<Map<String, Object>> task) {
                        if (!task.isSuccessful()) {
                            showSnackbar("Task unsuccessful.");
                            return;
                        }
                        Map<String, Object> result = task.getResult();
                        String textToShow = "Token: " + result.get("token");
                        tokenText.setText(textToShow);
                        token = TokenClass.Token.newBuilder()
                                .setUid(user.getUid())
                                .setToken((String) result.get("token"))
                                .build();
                        NdefMessage message = nfcHelper.createTextMessage(token);
                        nfcHelper.getAdapter().setNdefPushMessage(message, MainActivity.this);
                    }
                });
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        user = auth.getCurrentUser();
        updateUI();
    }

    public void updateUI() {
        if (user != null) {
            statusText.setText(getString(R.string.user_email, user.getEmail()));
            uidText.setText(getString(R.string.uid, user.getUid()));

            findViewById(R.id.buttons).setVisibility(View.GONE);
            findViewById(R.id.email_login_form).setVisibility(View.GONE);
            findViewById(R.id.signed_in_buttons).setVisibility(View.VISIBLE);
        } else {
            statusText.setText(R.string.signed_out);
            uidText.setText(null);
            tokenText.setText(null);

            findViewById(R.id.buttons).setVisibility(View.VISIBLE);
            findViewById(R.id.email_login_form).setVisibility(View.VISIBLE);
            findViewById(R.id.signed_in_buttons).setVisibility(View.GONE);

            nfcHelper.getAdapter().setNdefPushMessage(null, this);
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    private Task<Map<String, Object>> getToken(String uid) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);

        return function.getHttpsCallable("getToken")
                .call(data).continueWith(new Continuation<HttpsCallableResult, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> then(@NonNull Task<HttpsCallableResult> task) {
                        Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
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
                            user = auth.getCurrentUser();
                            updateUI();
                        } else {
                            showSnackbar("Authentication failed.");
                            user = null;
                            updateUI();
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
                            user = auth.getCurrentUser();
                            updateUI();
                        } else {
                            showSnackbar("Authentication failed.");
                            user = null;
                            updateUI();
                        }
                    }
                });
    }

    private void logout() {
        auth.signOut();
        user = null;
        updateUI();
    }
}