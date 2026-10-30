package com.example.smartgymtracker;

import android.content.Intent;
import android.os.Bundle;
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

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        String storedPasswordHash = dbHelper.getPasswordHash(email);
        if (storedPasswordHash == null) {
            Toast.makeText(this, "User does not exist", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            boolean isValid = Password.checkPassword(password, storedPasswordHash);
            if (isValid) {
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //Switch to login page (intent)
    private void goToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}
