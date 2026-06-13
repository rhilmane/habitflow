package com.example.myapplication.models;

public class User {
    public long id;
    public String name;
    public String email;
    public String passwordHash;
    public String avatarUrl;
    public long createdAt;
    public String securityQuestion;
    public String securityAnswer;

    public User() {
    }

    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = System.currentTimeMillis();
    }
}
