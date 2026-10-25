package com.example.smartgymtracker;

public class User {
    private int userID;
    private String userEmail;
    private String userPasswordHash;

    public int getUserID() { return userID; }
    public String getUserEmail() { return userEmail; }

    public String getUserPasswordHash() { return userPasswordHash; }

    public User(int userID, String email, String passwordHash){
        this.userID = userID;
        this.userEmail = email;
        this.userPasswordHash = passwordHash;
    }

    public User(String email, String passwordHash){
        this.userEmail = email;
        this.userPasswordHash = passwordHash;
    }
}
