package com.nutrilogic.controller;

import com.nutrilogic.model.User;
import com.nutrilogic.repository.HealthProfileRepository;
import com.nutrilogic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthProfileRepository healthProfileRepository;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model,
                                  @RequestParam(required = false) String search) {

        List<User> users;

        // Search/Filter users by username
        if (search != null && !search.trim().isEmpty()) {
            users = userRepository.findAll().stream()
                    .filter(u -> u.getUsername().toLowerCase()
                    .contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        } else {
            users = userRepository.findAll();
        }

        model.addAttribute("users", users);
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalBmiRecords", healthProfileRepository.count());
        model.addAttribute("search", search);

        return "admin/dashboard";
    }

    // Delete user + all their health records
    @GetMapping("/deleteUser/{id}")
    public String deleteUser(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {

        userRepository.findById(id).ifPresent(user -> {
            // Delete health profiles first to avoid foreign key error
            healthProfileRepository.deleteAll(
                healthProfileRepository.findByUser(user)
            );
            userRepository.delete(user);
        });

        redirectAttributes.addFlashAttribute("successMessage",
                "User deleted successfully.");
        return "redirect:/admin/dashboard";
    }
}