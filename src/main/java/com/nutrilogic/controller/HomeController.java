package com.nutrilogic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.nutrilogic.model.HealthProfile;
import com.nutrilogic.model.User;
import com.nutrilogic.repository.HealthProfileRepository;
import com.nutrilogic.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthProfileRepository healthProfileRepository;

    // ── HOME PAGE ────────────────────────────────────────────────────────────
    @GetMapping({"/", "/home"})
    public String home(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return "redirect:/login";

        User currentUser = userRepository.findByUsername(authentication.getName());

        // Auto-reset water count on new day (unchanged)
        String today = LocalDate.now().toString();
        if (currentUser != null && !today.equals(currentUser.getWaterDate())) {
            currentUser.setWaterCount(0);
            currentUser.setWaterDate(today);
            userRepository.save(currentUser);
        }

        // Fetch latest health profile
        HealthProfile latestProfile = healthProfileRepository
                .findTopByUserOrderByCreatedAtDesc(currentUser);

        model.addAttribute("user", currentUser);
        model.addAttribute("username", currentUser != null
                ? currentUser.getUsername() : authentication.getName());

        if (latestProfile != null) {
            // ── V1 fields (unchanged) ────────────────────────────────────────
            model.addAttribute("latestBmi",          latestProfile.getBmi());
            // ── V2 new fields ────────────────────────────────────────────────
            model.addAttribute("latestRiskScore",    latestProfile.getHealthRiskScore());
            model.addAttribute("latestCondition",    latestProfile.getMedicalCondition());
            model.addAttribute("latestSleepHours",   latestProfile.getSleepHours());
            model.addAttribute("latestBmr",          (int) latestProfile.getBmr());
            model.addAttribute("latestCalorieNeed",  latestProfile.getDailyCalorieNeed());
            model.addAttribute("latestIdealWeight",  latestProfile.getIdealWeight());
            model.addAttribute("latestWaterGoalMl",  latestProfile.getWaterGoalMl());
            model.addAttribute("latestWaterGoalGlasses",
                    Math.round(latestProfile.getWaterGoalMl() / 250.0));
            model.addAttribute("latestStressLevel",  latestProfile.getStressLevel());
            model.addAttribute("latestActivityLevel",latestProfile.getActivityLevel());
            // ── Risk level + color for NHRS card ────────────────────────────
            int score = latestProfile.getHealthRiskScore();
            if (score <= 30) {
                model.addAttribute("latestRiskLevel", "Low Risk");
                model.addAttribute("latestRiskColor", "success");
            } else if (score <= 60) {
                model.addAttribute("latestRiskLevel", "Moderate Risk");
                model.addAttribute("latestRiskColor", "warning");
            } else {
                model.addAttribute("latestRiskLevel", "High Risk");
                model.addAttribute("latestRiskColor", "danger");
            }
        } else {
            // No profile yet — safe defaults
            model.addAttribute("latestBmi",              "Not calculated");
            model.addAttribute("latestRiskScore",        0);
            model.addAttribute("latestCondition",        "None");
            model.addAttribute("latestSleepHours",       0);
            model.addAttribute("latestBmr",              0);
            model.addAttribute("latestCalorieNeed",      0);
            model.addAttribute("latestIdealWeight",      0);
            model.addAttribute("latestWaterGoalMl",      0);
            model.addAttribute("latestWaterGoalGlasses", 0);
            model.addAttribute("latestStressLevel",      "Low");
            model.addAttribute("latestActivityLevel",    "Sedentary");
            model.addAttribute("latestRiskLevel",        "");
            model.addAttribute("latestRiskColor",        "success");
        }

        return "home";
    }

    // ── ARTICLES PAGE ────────────────────────────────────────────────────────
    @GetMapping("/articles")
    public String showArticles() {
        return "articles";
    }

    // ── WATER UPDATE (unchanged) ─────────────────────────────────────────────
    @PostMapping("/updateWater")
    @ResponseBody
    public ResponseEntity<?> updateWater(@RequestParam int count, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userRepository.findByUsername(authentication.getName());
            if (user != null) {
                user.setWaterCount(count);
                user.setWaterDate(LocalDate.now().toString());
                userRepository.save(user);
                return ResponseEntity.ok("Success");
            }
        }
        return ResponseEntity.status(401).body("Unauthorized");
    }

    // ── HEALTH PROFILE FORM ──────────────────────────────────────────────────
    @GetMapping("/healthprofile")
    public String showCalculator(Model model) {
        model.addAttribute("healthProfile", new HealthProfile());
        return "healthprofile";
    }

    // ── SAVE HEALTH PROFILE ──────────────────────────────────────────────────
    @PostMapping("/save")
    public String saveHealthProfile(@ModelAttribute HealthProfile healthProfile,
                                     Authentication auth, Model model) {
        if (auth == null) return "redirect:/login";

        User user = userRepository.findByUsername(auth.getName());
        healthProfile.setUser(user);

        // Save water snapshot
        healthProfile.setWaterCount(user.getWaterCount());

        // ── 1. BMI ───────────────────────────────────────────────────────────
        double heightInMeters = healthProfile.getHeight() / 100.0;
        double bmiValue = healthProfile.getWeight() / (heightInMeters * heightInMeters);
        double roundedBmi = Math.round(bmiValue * 100.0) / 100.0;
        healthProfile.setBmi(roundedBmi);

        // ── 2. BMI CATEGORY (WHO standard boundaries) ─────────────────────
        String category;
        if      (roundedBmi < 18.5) category = "Underweight";
        else if (roundedBmi < 25.0) category = "Normal Weight";
        else if (roundedBmi < 30.0) category = "Overweight";
        else                        category = "Obese";

        // ── 3. BMR (Harris-Benedict) ─────────────────────────────────────────
        double bmr;
        String gender  = healthProfile.getGender();
        double weight  = healthProfile.getWeight();
        double height  = healthProfile.getHeight();
        int    age     = healthProfile.getAge();

        if ("Female".equals(gender)) {
            bmr = 447.6 + (9.2 * weight) + (3.1 * height) - (4.3 * age);
        } else {
            bmr = 88.36 + (13.4 * weight) + (4.8 * height) - (5.7 * age);
        }
        bmr = Math.round(bmr * 100.0) / 100.0;
        healthProfile.setBmr(bmr);

        // ── 4. DAILY CALORIE NEED ────────────────────────────────────────────
        String activityLevel = healthProfile.getActivityLevel();
        if (activityLevel == null) activityLevel = "Sedentary";
        double activityFactor;
        switch (activityLevel) {
            case "LightlyActive":    activityFactor = 1.375; break;
            case "ModeratelyActive": activityFactor = 1.55;  break;
            case "VeryActive":       activityFactor = 1.725; break;
            case "ExtremelyActive":  activityFactor = 1.9;   break;
            default:                 activityFactor = 1.2;   break;
        }
        int dailyCalorieNeed = (int) Math.round(bmr * activityFactor);
        healthProfile.setDailyCalorieNeed(dailyCalorieNeed);

        // ── 5. IDEAL WEIGHT (Devine Formula) ─────────────────────────────────
        double heightInInches = height / 2.54;
        double idealWeight = "Female".equals(gender)
                ? 45.5 + 2.3 * (heightInInches - 60)
                : 50.0 + 2.3 * (heightInInches - 60);
        idealWeight = Math.max(idealWeight, 30.0);
        idealWeight = Math.round(idealWeight * 100.0) / 100.0;
        healthProfile.setIdealWeight(idealWeight);

        // ── 6. WATER GOAL (Weight × 30 ml) ───────────────────────────────────
        int waterGoalMl = (int) Math.round(weight * 30);
        healthProfile.setWaterGoalMl(waterGoalMl);

        // ── 7. NHRS ALGORITHM ────────────────────────────────────────────────
        int bmiScore;
        if      (roundedBmi < 18.5) bmiScore = 30;
        else if (roundedBmi < 25.0) bmiScore = 0;
        else if (roundedBmi < 30.0) bmiScore = 40;
        else                        bmiScore = 70;

        int sleepScore;
        int sleepHours = healthProfile.getSleepHours();
        if      (sleepHours < 6)  sleepScore = 40;
        else if (sleepHours < 7)  sleepScore = 20;
        else if (sleepHours <= 9) sleepScore = 0;
        else                      sleepScore = 15;

        String condition = healthProfile.getMedicalCondition();
        if (condition == null) condition = "None";
        int conditionScore;
        switch (condition) {
            case "HeartDisease":  conditionScore = 50; break;
            case "Diabetes":      conditionScore = 35; break;
            case "BloodPressure": conditionScore = 25; break;
            case "Thyroid":
            case "PCOD":          conditionScore = 20; break;
            default:              conditionScore = 0;  break;
        }

        int ageScore;
        if      (age < 30) ageScore = 0;
        else if (age < 45) ageScore = 10;
        else if (age < 60) ageScore = 20;
        else               ageScore = 30;

        String stress = healthProfile.getStressLevel();
        int stressBonus = "High".equals(stress) ? 10 : "Medium".equals(stress) ? 5 : 0;

        int healthRiskScore = (int) Math.round(
            (bmiScore * 0.35) + (sleepScore * 0.20) +
            (conditionScore * 0.30) + (ageScore * 0.15) + stressBonus
        );
        healthRiskScore = Math.min(healthRiskScore, 100);
        healthProfile.setHealthRiskScore(healthRiskScore);

        String riskLevel;
        String riskColor;
        if      (healthRiskScore <= 30) { riskLevel = "Low Risk";      riskColor = "success"; }
        else if (healthRiskScore <= 60) { riskLevel = "Moderate Risk"; riskColor = "warning"; }
        else                            { riskLevel = "High Risk";     riskColor = "danger";  }

        // ── 8. SAVE TO DATABASE ──────────────────────────────────────────────
        healthProfileRepository.save(healthProfile);

        // ── 9. PASS ALL DATA TO result.html ─────────────────────────────────
        model.addAttribute("bmi",              roundedBmi);
        model.addAttribute("category",         category);
        model.addAttribute("username",         user.getUsername());
        model.addAttribute("bmr",              (int) bmr);
        model.addAttribute("idealWeight",      idealWeight);
        model.addAttribute("dailyCalorieNeed", dailyCalorieNeed);
        model.addAttribute("waterGoalMl",      waterGoalMl);
        model.addAttribute("waterGoalGlasses", Math.round(waterGoalMl / 250.0));
        model.addAttribute("healthRiskScore",  healthRiskScore);
        model.addAttribute("riskLevel",        riskLevel);
        model.addAttribute("riskColor",        riskColor);
        model.addAttribute("medicalCondition", condition);
        model.addAttribute("activityLevel",    activityLevel);
        model.addAttribute("sleepHours",       sleepHours);
        model.addAttribute("sleepQuality",     healthProfile.getSleepQuality());
        model.addAttribute("stressLevel",      stress);
        model.addAttribute("gender",           gender);
        model.addAttribute("weightDiff",
                Math.round((weight - idealWeight) * 100.0) / 100.0);

        return "result";
    }
}