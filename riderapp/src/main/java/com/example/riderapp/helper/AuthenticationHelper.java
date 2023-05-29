package com.example.riderapp.helper;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.riderapp.R;
import com.example.riderapp.activity.HomeActivity;
import com.example.riderapp.common.Common;
import com.example.riderapp.model.firebase.User;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.Objects;

import dmax.dialog.SpotsDialog;

public class AuthenticationHelper {
    private static final String TAG = AuthenticationHelper.class.getCanonicalName();
    private final AppCompatActivity mActivity;
    private final FirebaseAuth mFirebaseAuth;
    private final FirebaseDatabase mFirebaseDatabase;
    private final DatabaseReference mUsers;
    private final ConstraintLayout mRoot;

    public AuthenticationHelper(AppCompatActivity activity) {
        Log.d(TAG, "AuthenticationHelper()");
        mActivity = activity;
        mRoot = activity.findViewById(R.id.root);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUsers = mFirebaseDatabase.getReference(Common.user_rider_tbl);
        //if (mFirebaseAuth.getUid() != null) loginSuccess();
    }

    public void showLoginDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mActivity);
        alertDialog.setTitle(mActivity.getResources().getString(R.string.login));
        alertDialog.setMessage(mActivity.getResources().getString(R.string.fill_fields));

        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View login_layout = inflater.inflate(R.layout.layout_login, null);
        final MaterialEditText etEmail = login_layout.findViewById(R.id.etEmail);
        final MaterialEditText etPassword = login_layout.findViewById(R.id.etPassword);

        alertDialog.setView(login_layout);
        alertDialog.setPositiveButton(mActivity.getResources().getString(R.string.login),
                (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    if (TextUtils.isEmpty(etEmail.getText().toString())) {
                        Snackbar.make(mRoot,
                                mActivity.getResources().getString(R.string.enter_email),
                                Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(etPassword.getText().toString())) {
                        Snackbar.make(mRoot,
                                mActivity.getResources().getString(R.string.enter_password),
                                Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (etPassword.getText().toString().length() < 6) {
                        Snackbar.make(mRoot,
                                mActivity.getResources().getString(R.string.password_short),
                                Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    login(etEmail.getText().toString(), etPassword.getText().toString());
                });
        alertDialog.setNegativeButton(mActivity.getResources().getString(R.string.cancel),
                (dialogInterface, i) -> dialogInterface.dismiss());
        alertDialog.show();
    }

    private void login(String email, String password) {
        final android.app.AlertDialog waitingDialog =
                new SpotsDialog.Builder().setContext(mActivity).build();
        waitingDialog.show();
        mFirebaseAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener(
                authResult -> {
                    waitingDialog.dismiss();
                    goToMainActivity();
                }).addOnFailureListener(e -> {
            waitingDialog.dismiss();
            Snackbar.make(mRoot,
                    mActivity.getResources().getString(R.string.failed) + e.getMessage(),
                    Snackbar.LENGTH_SHORT).show();
        });
    }

    public void showSignInDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mActivity);
        alertDialog.setTitle(mActivity.getResources().getString(R.string.sign_in));
        alertDialog.setMessage(mActivity.getResources().getString(R.string.fill_fields));

        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View sing_in_layout = inflater.inflate(R.layout.layout_register, null);
        final MaterialEditText etEmail = sing_in_layout.findViewById(R.id.etEmail);
        final MaterialEditText etPassword = sing_in_layout.findViewById(R.id.etPassword);
        final MaterialEditText etName = sing_in_layout.findViewById(R.id.etName);
        final MaterialEditText etPhone = sing_in_layout.findViewById(R.id.etPhone);

        alertDialog.setView(sing_in_layout);
        alertDialog.setPositiveButton(mActivity.getResources().getString(R.string.register),
                (dialogInterface, i) -> {
                    dialogInterface.dismiss();

                    if (TextUtils.isEmpty(etEmail.getText().toString())) {
                        Snackbar.make(mRoot,
                                mActivity.getResources().getString(R.string.enter_email),
                                Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(etPassword.getText().toString())) {
                        Snackbar.make(mRoot,
                                mActivity.getResources().getString(R.string.enter_password),
                                Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (etPassword.getText().toString().length() < 6) {
                        Snackbar.make(mRoot,
                                mActivity.getResources().getString(R.string.password_short),
                                Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(etName.getText().toString())) {
                        Snackbar.make(mRoot,
                                mActivity.getResources().getString(R.string.enter_name),
                                Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(etPhone.getText().toString())) {
                        Snackbar.make(mRoot,
                                mActivity.getResources().getString(R.string.enter_phone),
                                Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    register(etEmail.getText().toString(), etPassword.getText().toString(),
                            etName.getText().toString(), etPhone.getText().toString());

                });

        alertDialog.setNegativeButton(mActivity.getResources().getString(R.string.cancel),
                (dialogInterface, i) -> dialogInterface.dismiss());
        alertDialog.show();
    }

    private void register(String email, String password, String name, String phone) {
        mFirebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    User user = new User();
                    user.setEmail(email);
                    user.setName(name);
                    user.setPassword(password);
                    user.setPhone(phone);

                    mUsers.child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                            .setValue(user)
                            .addOnSuccessListener(aVoid -> Snackbar.make(mRoot,
                                    mActivity.getResources().getString(R.string.registered),
                                    Snackbar.LENGTH_SHORT).show()).addOnFailureListener(e -> Snackbar.make(
                                    mRoot,
                                    mActivity.getResources().getString(R.string.failed) + e.getMessage(),
                                    Snackbar.LENGTH_SHORT).show());
                }).addOnFailureListener(e -> Snackbar.make(mRoot,
                        mActivity.getResources().getString(R.string.failed) + e.getMessage(),
                        Snackbar.LENGTH_SHORT).show());
    }
    private void goToMainActivity() {
        mActivity.startActivity(new Intent(mActivity, HomeActivity.class));
        mActivity.finish();
    }
}
