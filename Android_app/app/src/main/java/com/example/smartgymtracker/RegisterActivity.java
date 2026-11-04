package com.example.smartgymtracker;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseUser;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;
import java.util.HashMap;


public class RegisterActivity extends AppCompatActivity {

    protected EditText emailRegisterInput;
    protected EditText passwordRegisterInput;
    protected EditText passwordConfirmRegisterInput;
    protected Button createAccountButton;
    protected TextView loginLabelClick;
    protected FirebaseAuth auth;
    protected FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //Connecting variables to XML components
        emailRegisterInput = findViewById(R.id.emailRegisterInput);
        passwordRegisterInput = findViewById(R.id.passwordRegisterInput);
        passwordConfirmRegisterInput = findViewById(R.id.passwordConfirmRegisterInput);
        createAccountButton = findViewById(R.id.createAccountButton);
        loginLabelClick = findViewById(R.id.loginLabelClick);

        // Initialize Firebase Auth and database
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        // Connecting the Register button to registerUser()
        createAccountButton.setOnClickListener(v -> registerUser());

        //Connection between login label and intent
        loginLabelClick.setOnClickListener(v -> {finish();});

    }

    private void registerUser() {
        String email = emailRegisterInput.getText().toString().trim();
        String domain = email.substring(email.lastIndexOf("@") + 1);
        String password = passwordRegisterInput.getText().toString();
        String confirmPassword = passwordConfirmRegisterInput.getText().toString();

        // Check that all fields are filled
        if(email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()){
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_LONG).show();
            return;
        }

        // Check if email matches an appropriate email format
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_LONG).show();
            return;
        }

        // Check if the domain specifically matches an appropriate domain
        if( !domain.equals("gmail.com") &&
                !domain.equals("outlook.com") &&
                !domain.equals("hotmail.com") &&
                !domain.equals("yahoo.com") &&
                !domain.equals("icloud.com") &&
                !domain.equals("live.concordia.ca")){
            Toast.makeText(this, "Invalid email domain", Toast.LENGTH_LONG).show();
            return;
        }

        //Check if both passwords entered are the same
        if(!password.equals(confirmPassword)){
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_LONG).show();
            return;
        }

        // Create and store user accounts in the database
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if(task.isSuccessful()){
                FirebaseUser user = auth.getCurrentUser();
                if(user != null){
                    String uid = user.getUid();
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("email", email);
                    db.collection("users").document(uid).set(userData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Account created successfully", Toast.LENGTH_LONG).show();
                                auth.signOut();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to save user information", Toast.LENGTH_LONG).show();
                            });
                }
                else{
                    Toast.makeText(this, "Failed to retrieve user information", Toast.LENGTH_LONG).show();
                }

            } else {
                Toast.makeText(this, "Failed to create account", Toast.LENGTH_LONG).show();
            }
            });




    }


}
