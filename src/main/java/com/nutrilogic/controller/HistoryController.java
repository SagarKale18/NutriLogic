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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HistoryController {

    @Autowired
    private HealthProfileRepository healthProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/history")
    public String showHistory(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return "redirect:/login";

        User user = userRepository.findByUsername(authentication.getName());
        if (user == null) return "redirect:/login";

        // ── Ascending order for chart (oldest → newest) ──────────────────────
        List<HealthProfile> historyList = healthProfileRepository
                .findByUserOrderByCreatedAtAsc(user);

        // ── Chart labels — formatted date strings ─────────────────────────────
        List<String> chartLabels = historyList.stream()
            .map(p -> p.getCreatedAt() != null
                    ? p.getCreatedAt().toLocalDate().toString()
                    : "Recent")
            .collect(Collectors.toList());

        // ── BMI chart data ────────────────────────────────────────────────────
        List<Double> chartData = historyList.stream()
            .map(HealthProfile::getBmi)
            .collect(Collectors.toList());

        // ── Risk Score chart data ─────────────────────────────────────────────
        List<Integer> riskChartData = historyList.stream()
            .map(p -> (Integer)(p.getHealthRiskScore() > 0 ? p.getHealthRiskScore() : 0))
            .collect(Collectors.toList());

        // ── Table list — newest first (reverse of chart order) ────────────────
        List<HealthProfile> historyForTable = new ArrayList<>(historyList);
        Collections.reverse(historyForTable);

        model.addAttribute("user",          user);
        model.addAttribute("history",       historyForTable);  // table = newest first
        model.addAttribute("username",      user.getUsername());
        model.addAttribute("chartLabels",   chartLabels);       // chart = oldest first
        model.addAttribute("chartData",     chartData);
        model.addAttribute("riskChartData", riskChartData);

        return "history";
    }

    @GetMapping("/delete/{id}")
    public String deleteProfile(@PathVariable Long id, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            healthProfileRepository.deleteById(id);
        }
        return "redirect:/history";
    }
}