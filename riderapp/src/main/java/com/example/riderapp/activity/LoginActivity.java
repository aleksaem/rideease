package com.example.riderapp.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.riderapp.R;
import com.example.riderapp.helper.AuthenticationHelper;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getCanonicalName();
    private AuthenticationHelper mAuthenticationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuthenticationHelper = new AuthenticationHelper(this);

        Button mSignInButton = findViewById(R.id.btnSignin);
        Button mLoginButton = findViewById(R.id.btnLogin);
        mSignInButton.setOnClickListener(view -> mAuthenticationHelper.showSignInDialog());
        mLoginButton.setOnClickListener(view -> mAuthenticationHelper.showLoginDialog());
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}
