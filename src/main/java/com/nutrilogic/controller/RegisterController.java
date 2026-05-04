package com.nutrilogic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
// IMPORT THIS: Fixes "RedirectAttributes cannot be resolved"
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.nutrilogic.model.User;
import com.nutrilogic.repository.UserRepository;

@Controller
public class RegisterController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/register")
    public String showRegisterForm(User user) {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(User user, RedirectAttributes redirectAttributes) {
        // 1. Check if user already exists (Optional but good practice)
        if (userRepository.findByUsername(user.getUsername()) != null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Username already taken!");
            return "redirect:/register";
        }

        // 2. Encode the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // 3. Set default role/enabled status if needed
        user.setEnabled(true);

        // 4. Save using userRepository (This fixes the "userService" error)
        userRepository.save(user);
        
        // 5. Add the success message for the login page
        redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
        
        return "redirect:/login"; 
    }
}