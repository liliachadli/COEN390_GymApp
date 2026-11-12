package com.example.smartgymtracker;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
    protected FirebaseAuth auth;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Connecting variables to the XML components
        emailLoginInput = findViewById(R.id.emailLoginInput);
        passwordLoginInput = findViewById(R.id.passwordLoginInput);
        loginButton = findViewById(R.id.loginButton);

        // Initialize Firebase auth
        auth = FirebaseAuth.getInstance();

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

        // Check if user exists in the database
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if(task.isSuccessful()){
                FirebaseUser user = auth.getCurrentUser();

                if(user != null){
                    user.reload().addOnCompleteListener(reloadTask -> {
                        if(reloadTask.isSuccessful()){
                            if(user.isEmailVerified()){
                                // Clear the workout history and session
                                WorkoutHistory.clearHistory();
                                WorkoutSession.reset();

                                Toast.makeText(this, "Login successful", Toast.LENGTH_LONG).show();
                                goToDashboard();
                            } else{
                                Toast.makeText(this, "Please verify your email", Toast.LENGTH_LONG).show();
                                auth.signOut();
                            }
                        } else{
                            Toast.makeText(this, "Failed to check email verification", Toast.LENGTH_LONG).show();
                            auth.signOut();
                        }
                    });
                } else {
                    Toast.makeText(this, "Failed to retrieve user information", Toast.LENGTH_LONG).show();
                    auth.signOut();
                }
            }
            else{
                Toast.makeText(this, "Login failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Switch to register page
    private void goToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    // Switch to main page
    private void goToDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}
