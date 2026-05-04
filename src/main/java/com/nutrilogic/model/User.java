package com.nutrilogic.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String password;
    private boolean enabled = true;
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private int waterCount = 0;

    // NEW: stores the date of last water update — used for daily auto-reset
    private String waterDate = "";

    // ADD THIS FIELD: To store ROLE_USER or ROLE_ADMIN
    private String role = "ROLE_USER"; 
    // Getters and Setters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    public int getWaterCount() { return waterCount; }
    public void setWaterCount(int waterCount) { this.waterCount = waterCount; }

    // NEW: getter and setter for waterDate
    public String getWaterDate() { return waterDate; }
    public void setWaterDate(String waterDate) { this.waterDate = waterDate; }

    // ADD THESE: Getter and Setter for role
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}