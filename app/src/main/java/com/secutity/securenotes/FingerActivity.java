package com.secutity.securenotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import javax.crypto.KeyGenerator;

public class FingerActivity extends AppCompatActivity {

    EditText usernameET;
    Button fingerPrintBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finger);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("Login via Finger");

        usernameET = findViewById(R.id.etUsername);
        fingerPrintBtn = findViewById(R.id.btnFingerprint);

        fingerPrintBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBiometryAvailable()) {
                    authenticateWithFingerprint();
                }
            }
        });

        SQLiteDatabase.loadLibs(getApplicationContext());
        InitializeSQLCipher();


    }

    private void authenticateWithFingerprint() {
        if (isBiometryAvailable()) {
            showBiometricPrompt();
        } else {
            Toast.makeText(this, "Biometric authentication is not available on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void showBiometricPrompt() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate with your fingerprint")
                .setSubtitle("Place your finger on the sensor")
                .setNegativeButtonText("Cancel")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        String enteredUsername = usernameET.getText().toString().trim();

                        if (TextUtils.isEmpty(enteredUsername)) {
                            Toast.makeText(getApplicationContext(), "Please enter a username", Toast.LENGTH_SHORT).show();
                        } else {
                            SQLiteDatabase db = new SQliteHelper(getApplicationContext()).getReadableDatabase("password");
                            boolean userExists = new SQliteHelper(getApplicationContext()).checkUserExists(enteredUsername, db);

                            SharedPreferences preferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("current_username", enteredUsername);
                            editor.apply();

                            if (userExists) {
                                startActivity(new Intent(FingerActivity.this, TaskListActivity.class));
                                finish();
                            } else {
                                new SQliteHelper(getApplicationContext()).insertUserWithoutPassword(enteredUsername, db);
                                startActivity(new Intent(FingerActivity.this, TaskListActivity.class));
                                finish();
                            }
                        }
                    }

                        @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(FingerActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        biometricPrompt.authenticate(promptInfo);
    }

    private boolean isBiometryAvailable() {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (Exception e) {
            return false;
        }

        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException |
                 NoSuchProviderException e) {
            return false;
        }

        if (keyGenerator == null || keyStore == null) {
            return false;
        }

        try {
            keyStore.load(null);
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder("finger_key",
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                 | CertificateException | IOException e) {
            return false;
        }
        return true;

    }

    private void InitializeSQLCipher() {
        try {
            AES.generateYek();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}