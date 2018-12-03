package com.example.temalabor.nfcapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.temalabor.nfcapp.adapter.TokenAdapter;
import com.example.temalabor.nfcapp.data.TokenList;
import com.example.temalabor.nfcapp.utility.NFCHelper;
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
    private TextView emailText;
    private TextView uidText;

    NFCHelper nfcHelper;
    PendingIntent pendingIntent;
    IntentFilter[] intentFilters;

    private TokenAdapter tokenAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        function = FirebaseFunctions.getInstance();

        Intent nfcIntent = new Intent(this, getClass());
        nfcIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0, nfcIntent, 0);
        IntentFilter ndefIntentFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefIntentFilter.addDataType("text/plain");
            intentFilters = new IntentFilter[]{ndefIntentFilter};
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }

        nfcHelper = new NFCHelper(this, this);
        nfcHelper.pushMessage(null);

        emailView = findViewById(R.id.emailView);
        passwordView = findViewById(R.id.passwordView);
        emailText = findViewById(R.id.emailText);
        uidText = findViewById(R.id.uidText);

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
                getToken(user.getUid()).addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            showSnackbar("Task unsuccessful.");
                            return;
                        }
                        tokenAdapter.addItem(task.getResult());
                    }
                });
            }
        });

        initRecyclerView();
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.RecyclerView);
        tokenAdapter = new TokenAdapter(new TokenList(), this, nfcHelper);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(tokenAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        user = auth.getCurrentUser();
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcHelper.getAdapter().enableForegroundDispatch(this, pendingIntent,
                intentFilters, null);
    }

    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        nfcHelper.receiveMessage(intent);
    }

    public void updateUI() {
        if (user != null) {
            emailText.setText(getString(R.string.user_email, user.getEmail()));
            uidText.setText(getString(R.string.uid, user.getUid()));

            findViewById(R.id.buttons).setVisibility(View.GONE);
            findViewById(R.id.email_login_form).setVisibility(View.GONE);
            findViewById(R.id.signed_in_buttons).setVisibility(View.VISIBLE);
            findViewById(R.id.dataTexts).setVisibility(View.VISIBLE);
        } else {

            findViewById(R.id.buttons).setVisibility(View.VISIBLE);
            findViewById(R.id.email_login_form).setVisibility(View.VISIBLE);
            findViewById(R.id.signed_in_buttons).setVisibility(View.GONE);
            findViewById(R.id.dataTexts).setVisibility(View.GONE);
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    private Task<String> getToken(String uid) {
        Map<String, Object> data = new HashMap<>();
        data.put("userid", uid);
        data.put("useremail", user.getEmail().split("@")[0]);

        return function.getHttpsCallable("getCustomToken")
                .call(data).continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) {
                        String result = (String) task.getResult().getData();
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