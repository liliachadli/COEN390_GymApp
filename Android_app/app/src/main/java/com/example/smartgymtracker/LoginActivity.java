package com.example.smartgymtracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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

    }

    //Switch to login page (intent)
    private void goToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}
