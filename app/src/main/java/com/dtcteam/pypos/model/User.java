package com.dtcteam.pypos.model;

public class User {
    private String id;
    private String email;
    private String role;
    private String username;
    private String fullName;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public boolean isAdmin() {
        return "admin".equals(role);
    }
}
