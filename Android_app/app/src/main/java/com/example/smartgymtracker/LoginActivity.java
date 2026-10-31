package com.example.smartgymtracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    protected EditText emailLoginInput;
    protected EditText passwordLoginInput;
    protected Button loginButton;
    protected TextView signupLabelClick;
    protected DatabaseHelper dbHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Connecting variables to the xml components
        emailLoginInput = findViewById(R.id.emailLoginInput);
        passwordLoginInput = findViewById(R.id.passwordLoginInput);
        loginButton = findViewById(R.id.loginButton);

        // Initialize dbHelper
        dbHelper = new DatabaseHelper(this);

        // Connecting the Login button to loginUser()
        loginButton.setOnClickListener(v -> loginUser());

        //Connection between register label and intent
        signupLabelClick = findViewById(R.id.signupLabelClick);
        signupLabelClick.setOnClickListener(v -> goToRegister());


    }

    private void loginUser(){
        String email = emailLoginInput.getText().toString().trim();
        String password = passwordLoginInput.getText().toString();

        // Check if email and password are filled
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_LONG).show();
            return;
        }

        // Check if entered email exists in the database
        String storedPasswordHash = dbHelper.getPasswordHash(email);
        if (storedPasswordHash == null) {
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_LONG).show();
            return;
        }

        // Verify the entered password
        try {
            boolean isValid = Password.checkPassword(password, storedPasswordHash);
            if (isValid) {
                Toast.makeText(this, "Login successful", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("LoginActivity", "Password verification failed", e);
            Toast.makeText(this, "Unable to verify password", Toast.LENGTH_LONG).show();
        }
    }

    //Switch to login page (intent)
    private void goToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}
