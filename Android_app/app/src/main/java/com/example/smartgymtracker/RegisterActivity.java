package com.example.smartgymtracker;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class RegisterActivity extends AppCompatActivity {

    protected EditText emailRegisterInput;
    protected EditText passwordRegisterInput;
    protected EditText passwordConfirmRegisterInput;
    protected Button createAccountButton;
    protected TextView loginLabelClick;
    protected DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //Connecting variables to xml components
        emailRegisterInput = findViewById(R.id.emailRegisterInput);
        passwordRegisterInput = findViewById(R.id.passwordRegisterInput);
        passwordConfirmRegisterInput = findViewById(R.id.passwordConfirmRegisterInput);
        createAccountButton = findViewById(R.id.createAccountButton);
        loginLabelClick = findViewById(R.id.loginLabelClick);

        // Initialize dbHelper
        dbHelper = new DatabaseHelper(this);

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

        // Check if entered email exists in the database
        boolean emailCheck = dbHelper.checkEmail(email);
        if (emailCheck){
            Toast.makeText(this, "Email already exists", Toast.LENGTH_LONG).show();
            return;
        }

        // Hash entered password

        String hashPassword;
        try{
            hashPassword = Password.hashPassword(password);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e){
            Toast.makeText(this, "Unable to process password", Toast.LENGTH_LONG).show();
            return;
        }

        // Inserting the user in the database
        User user = new User(email, hashPassword);
        boolean insertSuccess = dbHelper.insertUser(user);

        if (insertSuccess){
            Toast.makeText(this, "Account created successfully", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to create account", Toast.LENGTH_LONG).show();
        }




    }


}
