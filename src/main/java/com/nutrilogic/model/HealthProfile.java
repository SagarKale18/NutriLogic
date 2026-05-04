package com.nutrilogic.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_profiles")
public class HealthProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── V1 Fields ────────────────────────────────────────────────────────────
    @Min(10) @Max(100)
    private int age;

    @DecimalMin("20.0") @DecimalMax("250.0")
    private double weight;

    @DecimalMin("100.0") @DecimalMax("250.0")
    private double height;

    private String goal;
    private double bmi;

    // ── V2 New Fields (with safe defaults) ───────────────────────────────────
    private String gender           = "Male";
    private String medicalCondition = "None";
    private String activityLevel    = "Sedentary";
    private int    sleepHours       = 7;
    private String sleepQuality     = "Good";
    private String sleepTime        = "Before10pm";
    private String stressLevel      = "Low";
    private String mealPlan         = "Standard";

    // ── Calculated Fields ─────────────────────────────────────────────────────
    private double bmr;
    private int    dailyCalorieNeed;
    private double idealWeight;
    private int    waterGoalMl;
    private int    healthRiskScore;
    private int    waterCount;

    // ── Timestamp ─────────────────────────────────────────────────────────────
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Relationship ──────────────────────────────────────────────────────────
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                          { return id; }

    public int getAge()                          { return age; }
    public void setAge(int age)                  { this.age = age; }

    public double getWeight()                    { return weight; }
    public void setWeight(double weight)         { this.weight = weight; }

    public double getHeight()                    { return height; }
    public void setHeight(double height)         { this.height = height; }

    public String getGoal()                      { return goal; }
    public void setGoal(String goal)             { this.goal = goal; }

    public double getBmi()                       { return bmi; }
    public void setBmi(double bmi)               { this.bmi = bmi; }

    public String getGender()                    { return gender; }
    public void setGender(String gender)         { this.gender = gender; }

    public String getMedicalCondition()          { return medicalCondition; }
    public void setMedicalCondition(String c)    { this.medicalCondition = c; }

    public String getActivityLevel()             { return activityLevel; }
    public void setActivityLevel(String a)       { this.activityLevel = a; }

    public int getSleepHours()                   { return sleepHours; }
    public void setSleepHours(int s)             { this.sleepHours = s; }

    public String getSleepQuality()              { return sleepQuality; }
    public void setSleepQuality(String s)        { this.sleepQuality = s; }

    public String getSleepTime()                 { return sleepTime; }
    public void setSleepTime(String s)           { this.sleepTime = s; }

    public String getStressLevel()               { return stressLevel; }
    public void setStressLevel(String s)         { this.stressLevel = s; }

    public String getMealPlan()                  { return mealPlan; }
    public void setMealPlan(String m)            { this.mealPlan = m; }

    public double getBmr()                       { return bmr; }
    public void setBmr(double bmr)               { this.bmr = bmr; }

    public int getDailyCalorieNeed()             { return dailyCalorieNeed; }
    public void setDailyCalorieNeed(int d)       { this.dailyCalorieNeed = d; }

    public double getIdealWeight()               { return idealWeight; }
    public void setIdealWeight(double i)         { this.idealWeight = i; }

    public int getWaterGoalMl()                  { return waterGoalMl; }
    public void setWaterGoalMl(int w)            { this.waterGoalMl = w; }

    public int getHealthRiskScore()              { return healthRiskScore; }
    public void setHealthRiskScore(int h)        { this.healthRiskScore = h; }

    public int getWaterCount()                   { return waterCount; }
    public void setWaterCount(int w)             { this.waterCount = w; }

    public LocalDateTime getCreatedAt()          { return createdAt; }

    public User getUser()                        { return user; }
    public void setUser(User user)               { this.user = user; }
}