package com.xlzhen.ifavorites.model;

public class UserInfo {
    private String email;
    private String userId;

    public UserInfo() {
    }
    public UserInfo(String email, String userId) {
        this.email = email;
        this.userId = userId;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
