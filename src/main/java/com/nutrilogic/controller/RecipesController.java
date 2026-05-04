package com.nutrilogic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.nutrilogic.model.HealthProfile;
import com.nutrilogic.model.Recipe;
import com.nutrilogic.model.User;
import com.nutrilogic.repository.HealthProfileRepository;
import com.nutrilogic.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

@Controller
public class RecipesController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthProfileRepository healthProfileRepository;

    // ─────────────────────────────────────────────────────────────────
    //  Inner class — represents one day's full meal plan
    // ─────────────────────────────────────────────────────────────────
    public static class DayPlan {
        public String dayLabel;
        public List<Recipe> breakfast = new ArrayList<>();
        public List<Recipe> lunch     = new ArrayList<>();
        public List<Recipe> dinner    = new ArrayList<>();
        public List<Recipe> snacks    = new ArrayList<>();
        public int totalCalories;
        public int totalProtein;
        public int totalCarbs;
        public int totalFats;
        public int totalFiber;

        public DayPlan(String label) { this.dayLabel = label; }

        public void calculate() {
            List<Recipe> all = new ArrayList<>();
            all.addAll(breakfast); all.addAll(lunch);
            all.addAll(dinner);    all.addAll(snacks);
            for (Recipe r : all) {
                totalCalories += r.getCalories();
                totalProtein  += r.getProtein();
                totalCarbs    += r.getCarbs();
                totalFats     += r.getFats();
                totalFiber    += r.getFiber();
            }
        }

        // getters for Thymeleaf
        public String getDayLabel()    { return dayLabel; }
        public List<Recipe> getBreakfast() { return breakfast; }
        public List<Recipe> getLunch()     { return lunch; }
        public List<Recipe> getDinner()    { return dinner; }
        public List<Recipe> getSnacks()    { return snacks; }
        public int getTotalCalories() { return totalCalories; }
        public int getTotalProtein()  { return totalProtein; }
        public int getTotalCarbs()    { return totalCarbs; }
        public int getTotalFats()     { return totalFats; }
        public int getTotalFiber()    { return totalFiber; }
    }

    // ─────────────────────────────────────────────────────────────────
    //  MAIN ENDPOINT
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/recipes")
    public String showRecipes(Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        User user = userRepository.findByUsername(auth.getName());
        HealthProfile profile = healthProfileRepository.findTopByUserOrderByCreatedAtDesc(user);

        String goal      = (profile != null && profile.getGoal()             != null) ? profile.getGoal()             : "Maintain";
        String condition = (profile != null && profile.getMedicalCondition() != null) ? profile.getMedicalCondition() : "None";
        String activity  = (profile != null && profile.getActivityLevel()    != null) ? profile.getActivityLevel()    : "Sedentary";
        int    sleep     = (profile != null) ? profile.getSleepHours() : 7;
        String stress    = (profile != null && profile.getStressLevel()      != null) ? profile.getStressLevel()      : "Low";

        // Build 7-day meal plan based on condition + goal
        LinkedHashMap<String, DayPlan> mealPlan = buildMealPlan(goal, condition, activity);

        model.addAttribute("mealPlan",   mealPlan);
        model.addAttribute("goal",       goal);
        model.addAttribute("condition",  condition.equals("None") ? null : condition);
        model.addAttribute("activity",   activity);
        model.addAttribute("sleepInfo",  sleep + " hrs");
        model.addAttribute("stressInfo", stress.equals("Low") ? null : stress + " Stress");

        return "recipes";
    }

    // ─────────────────────────────────────────────────────────────────
    //  BUILD 7-DAY MEAL PLAN
    // ─────────────────────────────────────────────────────────────────
    private LinkedHashMap<String, DayPlan> buildMealPlan(String goal, String condition, String activity) {
        LinkedHashMap<String, DayPlan> plan = new LinkedHashMap<>();

        String[] days   = {"day1","day2","day3","day4","day5","day6","day7"};
        String[] labels = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};

        for (int i = 0; i < 7; i++) {
            DayPlan dp = new DayPlan(labels[i]);
            dp.breakfast.addAll(getBreakfast(condition, goal, i));
            dp.lunch    .addAll(getLunch    (condition, goal, i));
            dp.dinner   .addAll(getDinner   (condition, goal, i));
            dp.snacks   .addAll(getSnacks   (condition, goal, i));
            dp.calculate();
            plan.put(days[i], dp);
        }
        return plan;
    }

    // ─────────────────────────────────────────────────────────────────
    //  BREAKFAST BANK  (7 entries, rotates by day index)
    // ─────────────────────────────────────────────────────────────────
    private List<Recipe> getBreakfast(String condition, String goal, int day) {

        // ── DIABETES ──
        if (condition.equals("Diabetes")) {
            Recipe[][] bank = {
                {r("Methi Paratha (1 pc)","Low GI whole wheat with fenugreek leaves","breakfast.jpg",180,6,28,5,0,
                   al("Whole wheat flour","Methi leaves","Ajwain","Ghee 1 tsp"),
                   al("Knead dough with methi","Roll thin parathas","Cook on tawa with minimal ghee"),"Low GI")},
                {r("Oats Porridge","Slow-release carbs, high fibre breakfast","breakfast.jpg",210,7,34,4,4,
                   al("Rolled oats","Low-fat milk","Chia seeds","Stevia"),
                   al("Boil milk","Add oats, cook 5 min","Top with chia seeds"),"Diabetic")},
                {r("Moong Dal Chilla","Protein-packed savoury pancake","breakfast.jpg",220,14,28,5,3,
                   al("Yellow moong dal","Green chilli","Ginger","Spinach"),
                   al("Soak and grind dal","Add spices","Spread on tawa and cook"),"High Protein")},
                {r("Upma with Veggies","Semolina with low-GI vegetables","breakfast.jpg",230,6,38,6,3,
                   al("Coarse semolina","Mixed vegetables","Mustard seeds","Curry leaves"),
                   al("Dry roast semolina","Temper mustard seeds","Add veggies and water, mix"),"Low GI")},
                {r("Besan Chilla","Chickpea flour pancake, protein rich","breakfast.jpg",200,12,22,6,3,
                   al("Besan","Onion","Tomato","Coriander"),
                   al("Make thin batter","Spread on tawa","Cook until golden"),"High Protein")},
                {r("Sprouted Moong Salad","Raw sprouted moong with lemon","breakfast.jpg",140,9,20,1,5,
                   al("Sprouted moong","Cucumber","Tomato","Lemon juice"),
                   al("Sprout moong overnight","Toss with chopped veggies","Add lemon and black salt"),"Diabetic")},
                {r("Daliya Porridge","Broken wheat — low GI complex carb","breakfast.jpg",200,6,36,3,5,
                   al("Broken wheat daliya","Low-fat milk","Cardamom","Jaggery 1 tsp"),
                   al("Boil daliya in milk","Add cardamom","Sweeten lightly with jaggery"),"Low GI")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        // ── BLOOD PRESSURE ──
        if (condition.equals("BloodPressure")) {
            Recipe[][] bank = {
                {r("Banana Oat Smoothie","Potassium rich, no added salt","breakfast.jpg",250,8,42,4,4,
                   al("Banana","Rolled oats","Low-fat milk","Flaxseeds"),
                   al("Blend banana and milk","Add oats and flaxseeds","Blend smooth"),"Low Sodium")},
                {r("Vegetable Poha","No-salt, lemon and turmeric poha","breakfast.jpg",220,5,38,6,3,
                   al("Poha","Peas","Turmeric","Lemon juice"),
                   al("Wash poha well","Sauté peas in minimal oil","Mix poha, steam 3 min"),"Low Sodium")},
                {r("Idli with Sambar","Fermented, gut-friendly, low sodium","breakfast.jpg",240,8,44,3,4,
                   al("Idli batter","Toor dal","Vegetables","Low sodium spices"),
                   al("Steam idlis 12 min","Prepare sambar without excess salt","Serve hot"),"DASH")},
                {r("Banana Nut Oatmeal","Magnesium + potassium combo","breakfast.jpg",270,8,44,7,5,
                   al("Oats","Banana","Walnuts","Honey"),
                   al("Cook oats in water","Mash banana in","Top with walnuts"),"Heart Healthy")},
                {r("Spinach Egg White Omlette","Low sodium high protein","breakfast.jpg",180,18,4,7,1,
                   al("Egg whites","Spinach","Capsicum","Black pepper only"),
                   al("Beat egg whites","Add veggies","Cook without added salt"),"Low Sodium")},
                {r("Ragi Porridge","Calcium + iron, blood pressure friendly","breakfast.jpg",200,5,38,2,3,
                   al("Ragi flour","Low-fat milk","Jaggery","Cardamom"),
                   al("Mix ragi in cold milk","Cook on low flame","Add jaggery and cardamom"),"DASH")},
                {r("Wheat Dosa with Coconut Chutney","Whole grain, no maida","breakfast.jpg",230,7,40,5,4,
                   al("Whole wheat flour","Curd","Mustard seeds","Green chilli"),
                   al("Mix batter","Spread thin on tawa","Serve with coconut chutney"),"Low Sodium")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        // ── HEART DISEASE ──
        if (condition.equals("HeartDisease")) {
            Recipe[][] bank = {
                {r("Flaxseed Oatmeal","Omega-3 rich heart-healthy breakfast","breakfast.jpg",240,8,38,7,6,
                   al("Rolled oats","Flaxseeds","Blueberries","Almond milk"),
                   al("Cook oats in almond milk","Stir in flaxseeds","Top with berries"),"Omega-3")},
                {r("Avocado Toast (Whole Grain)","Healthy fats, no trans fat","breakfast.jpg",280,8,30,14,6,
                   al("Whole grain bread","Avocado","Lemon","Black pepper"),
                   al("Toast bread","Mash avocado with lemon","Spread and serve"),"Heart Healthy")},
                {r("Walnut Banana Smoothie","CoQ10 + potassium combo","breakfast.jpg",270,9,40,10,4,
                   al("Banana","Walnuts","Oats","Low-fat milk"),
                   al("Blend all ingredients","Add ice if desired","Serve immediately"),"Omega-3")},
                {r("Besan Chilla","No cholesterol, plant protein","breakfast.jpg",200,12,22,6,3,
                   al("Besan","Tomato","Onion","Coriander"),
                   al("Thin batter, no excess oil","Spread on tawa","Cook both sides"),"Low Fat")},
                {r("Oats Upma with Flaxseeds","Soluble fibre lowers LDL cholesterol","breakfast.jpg",230,7,36,6,5,
                   al("Rolled oats","Mixed veggies","Mustard seeds","Flaxseeds"),
                   al("Dry roast oats","Sauté veggies","Combine and cook"),"Heart Healthy")},
                {r("Chia Pudding","Omega-3 and fibre rich overnight pudding","breakfast.jpg",200,7,24,8,8,
                   al("Chia seeds","Almond milk","Honey","Strawberries"),
                   al("Mix chia in milk","Refrigerate overnight","Top with fruits"),"Omega-3")},
                {r("Multigrain Dosa","Heart-friendly grains blend","breakfast.jpg",220,8,36,5,4,
                   al("Multigrain flour","Curd","Mustard seeds","Curry leaves"),
                   al("Prepare batter","Spread thin","Cook without ghee"),"Heart Healthy")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        // ── THYROID ──
        if (condition.equals("Thyroid")) {
            Recipe[][] bank = {
                {r("Eggs & Toast (Whole Grain)","Selenium + iodine rich breakfast","breakfast.jpg",280,18,28,10,2,
                   al("Eggs 2","Whole grain bread","Iodized salt","Black pepper"),
                   al("Scramble or poach eggs","Toast bread","Serve with minimal iodized salt"),"Iodine Rich")},
                {r("Yogurt & Fruit Bowl","Dairy iodine + antioxidants","breakfast.jpg",200,10,30,4,2,
                   al("Low-fat yogurt","Banana","Strawberries","Honey"),
                   al("Take yogurt in bowl","Slice fruits","Mix and top with honey"),"Iodine Rich")},
                {r("Oats with Brazil Nuts","Selenium powerhouse breakfast","breakfast.jpg",280,9,38,10,5,
                   al("Rolled oats","Brazil nuts 3 pieces","Banana","Milk"),
                   al("Cook oats in milk","Slice banana","Top with Brazil nuts"),"Selenium")},
                {r("Moong Dal Chilla","Plant protein, easy to digest","breakfast.jpg",220,14,28,5,3,
                   al("Yellow moong dal","Ginger","Green chilli","Iodized salt"),
                   al("Grind soaked dal","Add spices","Cook on tawa"),"High Protein")},
                {r("Ragi Porridge with Milk","Calcium + selenium rich","breakfast.jpg",200,6,36,3,3,
                   al("Ragi flour","Milk","Cardamom","Jaggery"),
                   al("Mix ragi in cold milk","Cook stirring","Sweeten lightly"),"Iodine Rich")},
                {r("Sunflower Seed Smoothie","Selenium + Vitamin E combo","breakfast.jpg",250,8,36,8,3,
                   al("Banana","Sunflower seeds","Milk","Honey"),
                   al("Blend all ingredients","Adjust consistency","Serve cold"),"Selenium")},
                {r("Paneer Bhurji (Light)","High protein, thyroid-friendly","breakfast.jpg",260,18,10,16,1,
                   al("Low-fat paneer","Onion","Tomato","Iodized salt"),
                   al("Crumble paneer","Sauté onion-tomato","Add paneer and cook"),"High Protein")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        // ── PCOD ──
        if (condition.equals("PCOD")) {
            Recipe[][] bank = {
                {r("Spearmint Green Tea + Chilla","Hormone balancing breakfast","breakfast.jpg",200,12,22,6,3,
                   al("Besan","Spinach","Spearmint tea","Flaxseeds"),
                   al("Brew spearmint tea","Make besan chilla","Add flaxseeds to batter"),"Anti-Inflammatory")},
                {r("Chia Berry Smoothie Bowl","Antioxidants + Omega-3","breakfast.jpg",220,7,34,7,8,
                   al("Chia seeds","Mixed berries","Almond milk","Honey"),
                   al("Soak chia overnight","Blend with milk","Top with berries"),"Omega-3")},
                {r("Turmeric Oats","Anti-inflammatory golden oats","breakfast.jpg",230,7,38,5,5,
                   al("Rolled oats","Turmeric 1/4 tsp","Ginger","Honey"),
                   al("Cook oats in water","Add turmeric and ginger","Sweeten with honey"),"Anti-Inflammatory")},
                {r("Flaxseed Roti + Sabzi","Hormonal balance with fibre","breakfast.jpg",250,9,36,7,6,
                   al("Whole wheat flour","Flaxseed powder","Mixed sabzi","Minimal oil"),
                   al("Add flaxseed to dough","Roll rotis","Cook on tawa"),"Hormonal Balance")},
                {r("Methi & Sprout Salad","Insulin sensitizing breakfast","breakfast.jpg",160,10,22,3,6,
                   al("Sprouted methi seeds","Cucumber","Tomato","Lemon"),
                   al("Sprout methi overnight","Toss with veggies","Add lemon and salt"),"Low GI")},
                {r("Daliya with Berries","Complex carb + antioxidants","breakfast.jpg",210,6,38,3,5,
                   al("Broken wheat","Blueberries","Almond milk","Cinnamon"),
                   al("Cook daliya in milk","Add cinnamon","Top with berries"),"Anti-Inflammatory")},
                {r("Moong Dal Dosa","Light protein breakfast","breakfast.jpg",210,13,28,4,3,
                   al("Yellow moong dal","Ginger","Green chilli","Iodized salt"),
                   al("Grind dal to batter","Rest 30 min","Spread and cook on tawa"),"High Protein")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        // ── GAIN MUSCLE ──
        if (goal.equals("Gain Muscle")) {
            Recipe[][] bank = {
                {r("Paneer Bhurji with Roti","30g protein morning meal","breakfast.jpg",480,30,45,18,4,
                   al("Paneer 150g","Eggs 2","Whole wheat roti","Onion Tomato"),
                   al("Scramble eggs with paneer","Make sabzi","Serve with roti"),"High Protein")},
                {r("Sattu Shake + Boiled Eggs","Traditional Indian mass gainer","breakfast.jpg",500,32,55,12,3,
                   al("Sattu 60g","Milk 300ml","Eggs 3 boiled","Jaggery"),
                   al("Blend sattu in milk","Boil eggs separately","Have together"),"Mass Gainer")},
                {r("Oats Egg White Bowl","Muscle building combo","breakfast.jpg",420,28,50,8,5,
                   al("Rolled oats","Egg whites 5","Banana","Peanut butter"),
                   al("Cook oats","Scramble egg whites","Top oats with PB and banana"),"High Protein")},
                {r("Chicken Egg Paratha","Protein-loaded whole wheat paratha","breakfast.jpg",550,38,46,18,4,
                   al("Chicken mince 100g","Eggs 2","Whole wheat flour","Spices"),
                   al("Knead dough","Stuff with chicken","Cook on tawa"),"Muscle Fuel")},
                {r("Greek Yogurt Parfait","Casein + whey combo","breakfast.jpg",380,24,48,8,3,
                   al("Greek yogurt 200g","Granola","Banana","Honey"),
                   al("Layer yogurt","Add granola","Top with banana and honey"),"High Protein")},
                {r("Besan Chilla + Paneer","Double protein breakfast","breakfast.jpg",460,28,36,16,4,
                   al("Besan","Paneer 100g","Onion Capsicum","Ghee"),
                   al("Make thick chilla batter","Stuff with paneer","Cook both sides"),"High Protein")},
                {r("Poha with Peanuts & Eggs","Carbs + protein morning fuel","breakfast.jpg",420,20,56,12,3,
                   al("Thick poha","Peanuts 30g","Eggs 2","Curry leaves"),
                   al("Wash poha","Sauté with peanuts","Scramble eggs separately"),"Muscle Fuel")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        // ── LOSE WEIGHT ──
        if (goal.equals("Lose Weight")) {
            Recipe[][] bank = {
                {r("Moong Dal Chilla (2 pcs)","High protein low calorie","breakfast.jpg",200,14,25,4,3,
                   al("Yellow moong dal","Spinach","Green chilli","Ginger"),
                   al("Soak and grind dal","Make thin batter","Cook on non-stick tawa"),"Low Cal")},
                {r("Oats with Berries","Fibre fills you up longer","breakfast.jpg",180,6,32,3,5,
                   al("Rolled oats","Mixed berries","Low-fat milk","Stevia"),
                   al("Cook oats in low-fat milk","Add berries","No sugar"),"Low Cal")},
                {r("Egg White Omlette","Fat-free protein breakfast","breakfast.jpg",140,16,4,4,0,
                   al("Egg whites 4","Spinach","Capsicum","Black pepper"),
                   al("Beat whites well","Add veggies","Cook without butter"),"Low Cal")},
                {r("Idli 2 + Sambar","Low fat traditional breakfast","breakfast.jpg",200,7,38,2,4,
                   al("Idli batter","Toor dal","Vegetables","Low sodium"),
                   al("Steam idlis","Prepare vegetable sambar","Serve hot"),"Low Cal")},
                {r("Daliya Upma","Filling whole grain breakfast","breakfast.jpg",190,5,34,4,5,
                   al("Broken wheat","Mixed veggies","Mustard seeds","Lemon"),
                   al("Roast daliya","Add tempered veggies","Cook with water"),"High Fibre")},
                {r("Cucumber Smoothie","Detox hydrating breakfast","breakfast.jpg",120,4,18,2,3,
                   al("Cucumber","Mint","Lemon","Low-fat yogurt"),
                   al("Blend cucumber","Add mint and lemon","Mix yogurt in"),"Detox")},
                {r("Besan Chilla + Mint Chutney","Guilt-free satisfying meal","breakfast.jpg",180,11,20,5,3,
                   al("Besan","Onion","Tomato","Coriander"),
                   al("Thin batter only","Cook without oil on non-stick","Serve with chutney"),"Low Cal")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        // ── MAINTAIN (default) ──
        Recipe[][] bank = {
            {r("Vegetable Poha","Light energizing start","breakfast.jpg",280,7,46,8,3,
               al("Poha","Peanuts","Onion","Turmeric"),
               al("Wash poha","Sauté with peanuts","Mix and steam 3 min"),null)},
            {r("Idli Sambar (3 pcs)","Classic balanced breakfast","breakfast.jpg",290,9,52,3,4,
               al("Idli batter","Sambar","Coconut chutney","Curry leaves"),
               al("Steam idlis 12 min","Heat sambar","Serve together"),null)},
            {r("Masala Omlette","Desi spiced egg protein","breakfast.jpg",260,17,10,16,1,
               al("Eggs 2","Green chilli","Onion","Coriander"),
               al("Beat eggs with spices","Pour on pan","Fold and serve"),null)},
            {r("Daliya Porridge","Wholesome broken wheat bowl","breakfast.jpg",260,7,44,5,5,
               al("Broken wheat","Milk","Jaggery","Cardamom"),
               al("Cook daliya in milk","Add jaggery","Serve warm"),null)},
            {r("Besan Chilla","Protein-rich savoury pancake","breakfast.jpg",240,13,24,8,3,
               al("Besan","Onion","Tomato","Coriander"),
               al("Make batter","Cook on tawa","Serve with chutney"),null)},
            {r("Methi Paratha","Iron rich hearty breakfast","breakfast.jpg",310,8,44,10,4,
               al("Whole wheat flour","Methi leaves","Ajwain","Ghee"),
               al("Knead dough with methi","Roll parathas","Cook on tawa"),null)},
            {r("Upma with Coconut","South Indian traditional breakfast","breakfast.jpg",300,6,48,8,3,
               al("Semolina","Grated coconut","Mustard seeds","Curry leaves"),
               al("Roast semolina","Make tadka","Combine and cook"),null)}
        };
        return Arrays.asList(bank[day % bank.length]);
    }

    // ─────────────────────────────────────────────────────────────────
    //  LUNCH BANK
    // ─────────────────────────────────────────────────────────────────
    private List<Recipe> getLunch(String condition, String goal, int day) {

        if (condition.equals("Diabetes")) {
            Recipe[][] bank = {
                {r("Brown Rice + Palak Dal","Low GI, iron-protein combo lunch","lunch.jpg",380,16,58,8,6,
                   al("Brown rice","Palak dal","Onion Tomato","Low oil"),
                   al("Cook brown rice","Prepare dal with palak","Serve with sabzi"),"Low GI")},
                {r("Jowar Roti + Moong Dal","Millet for blood sugar control","lunch.jpg",360,14,54,7,7,
                   al("Jowar flour","Moong dal","Bottle gourd","Minimal salt"),
                   al("Knead jowar dough","Cook rotis","Prepare dal"),"Low GI")},
                {r("Bajra Roti + Mixed Sabzi","Ancient grain diabetic lunch","lunch.jpg",370,12,56,8,8,
                   al("Bajra flour","Mixed vegetables","Curd","Ghee 1 tsp"),
                   al("Make bajra rotis","Prepare low-oil sabzi","Serve with curd"),"Low GI")},
                {r("Dal Khichdi (Brown Rice)","Comfort food, diabetic-friendly","lunch.jpg",350,14,54,7,5,
                   al("Brown rice","Moong dal","Turmeric","Ghee 1 tsp"),
                   al("Pressure cook together","Temper with ghee","Serve hot"),"Low GI")},
                {r("Ragi Roti + Methi Sabzi","Diabetic superfood combo","lunch.jpg",320,10,50,7,7,
                   al("Ragi flour","Methi leaves","Onion","Minimal oil"),
                   al("Knead ragi dough","Make sabzi","Serve together"),"Low GI")},
                {r("Rajma (Small Portion) + Jowar Roti","Protein fibre combo","lunch.jpg",380,16,60,6,10,
                   al("Rajma","Jowar roti","Onion Tomato","Low sodium"),
                   al("Soak and cook rajma","Make gravy","Serve with jowar roti"),"High Fibre")},
                {r("Chole + Brown Rice (Small)","Chickpea protein, slow digesting","lunch.jpg",390,16,58,8,9,
                   al("Chickpeas","Brown rice small portion","Onion Tomato","Low oil"),
                   al("Cook chickpeas with masala","Boil brown rice","Serve with salad"),"High Fibre")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (condition.equals("BloodPressure")) {
            Recipe[][] bank = {
                {r("Brown Rice + Palak Paneer (Low Fat)","DASH diet lunch","lunch.jpg",420,18,58,10,5,
                   al("Brown rice","Low-fat paneer","Palak","No excess salt"),
                   al("Cook brown rice","Make low-fat palak paneer","Serve with salad"),"DASH")},
                {r("Moong Dal + Bajra Roti","Potassium-rich DASH lunch","lunch.jpg",360,14,54,7,7,
                   al("Moong dal","Bajra roti","Garlic 2 cloves","No added salt"),
                   al("Cook dal with garlic","Make bajra rotis","Serve together"),"Low Sodium")},
                {r("Vegetable Brown Rice Bowl","Colourful DASH diet bowl","lunch.jpg",380,9,62,6,6,
                   al("Brown rice","Broccoli","Capsicum","Beans"),
                   al("Cook brown rice","Steam veggies","Toss together with lemon"),"DASH")},
                {r("Rajma + Jowar Roti","High potassium kidney beans","lunch.jpg",400,16,62,6,10,
                   al("Rajma","Jowar roti","Onion Tomato","No added salt"),
                   al("Cook rajma without excess salt","Make jowar rotis","Serve warm"),"High Potassium")},
                {r("Dal Tadka + Brown Rice","Heart-friendly dal","lunch.jpg",370,14,58,6,5,
                   al("Toor dal","Brown rice","Garlic","Minimal oil"),
                   al("Cook dal","Make light tadka with garlic","Serve with rice"),"DASH")},
                {r("Lauki Kofta Curry","Bottle gourd low-sodium curry","lunch.jpg",310,9,38,12,4,
                   al("Bottle gourd","Besan","Tomato","Low-fat curd"),
                   al("Make koftas from lauki","Prepare tomato gravy","Simmer together"),"Low Sodium")},
                {r("Mixed Vegetable Khichdi","One-pot nutritious DASH meal","lunch.jpg",350,12,56,7,6,
                   al("Rice","Moong dal","Mixed veggies","Minimal ghee"),
                   al("Pressure cook all together","Temper lightly","Serve hot"),"DASH")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (condition.equals("HeartDisease")) {
            Recipe[][] bank = {
                {r("Grilled Fish + Brown Rice","Omega-3 packed heart lunch","lunch.jpg",450,36,50,12,3,
                   al("Rohu or Pomfret","Brown rice","Lemon","Herbs"),
                   al("Marinate fish with herbs","Grill without frying","Serve with brown rice"),"Omega-3")},
                {r("Palak Dal + Jowar Roti","Iron + folate heart healthy","lunch.jpg",360,14,52,7,8,
                   al("Palak","Toor dal","Jowar roti","Garlic"),
                   al("Cook dal with palak","Make light garlic tadka","Serve with roti"),"Heart Healthy")},
                {r("Quinoa Vegetable Bowl","Complete protein, heart-friendly","lunch.jpg",380,14,54,9,6,
                   al("Quinoa","Mixed vegetables","Olive oil 1 tsp","Lemon"),
                   al("Cook quinoa","Sauté veggies","Toss together with lemon"),"Heart Healthy")},
                {r("Besan Kadhi + Brown Rice","No-cholesterol yogurt curry","lunch.jpg",340,11,52,8,3,
                   al("Low-fat curd","Besan","Brown rice","Curry leaves"),
                   al("Mix curd and besan","Cook kadhi","Serve with brown rice"),"Low Cholesterol")},
                {r("Walnut Chutney + Multigrain Roti + Dal","Omega-3 chutney side","lunch.jpg",400,14,56,12,6,
                   al("Walnuts","Toor dal","Multigrain roti","Onion Tomato"),
                   al("Make walnut chutney","Cook simple dal","Serve with roti"),"Omega-3")},
                {r("Salmon / Tuna Salad Bowl","Best Omega-3 heart lunch","lunch.jpg",400,32,22,16,3,
                   al("Canned tuna in water","Mixed greens","Cherry tomatoes","Olive oil"),
                   al("Drain tuna","Toss with salad","Dress with olive oil lemon"),"Omega-3")},
                {r("Moong Dal Soup + Brown Rice","Light heart-friendly meal","lunch.jpg",320,13,50,5,5,
                   al("Yellow moong dal","Brown rice","Ginger","Turmeric"),
                   al("Cook thin moong soup","Boil brown rice","Serve together"),"Heart Healthy")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (condition.equals("Thyroid")) {
            Recipe[][] bank = {
                {r("Egg Rice Bowl","Selenium + iodine packed lunch","lunch.jpg",420,24,52,12,3,
                   al("White rice","Eggs 2","Iodized salt","Vegetables"),
                   al("Cook rice","Fry eggs lightly","Combine with veggies"),"Iodine Rich")},
                {r("Fish Curry + Rice","Best iodine source meal","lunch.jpg",460,30,54,12,3,
                   al("Rohu fish","White rice","Onion Tomato","Iodized salt"),
                   al("Make light tomato fish curry","Boil rice","Serve together"),"Iodine Rich")},
                {r("Brazil Nut Dal + Roti","Selenium-rich lentil meal","lunch.jpg",380,16,52,8,6,
                   al("Toor dal","Brazil nuts 3","Whole wheat roti","Onion"),
                   al("Cook dal","Crush Brazil nuts as garnish","Serve with roti"),"Selenium")},
                {r("Paneer + Brown Rice","Protein and iodine combo","lunch.jpg",440,24,54,14,3,
                   al("Low-fat paneer","Brown rice","Onion Tomato","Iodized salt"),
                   al("Make light paneer sabzi","Cook brown rice","Serve together"),"High Protein")},
                {r("Curd Rice + Sabzi","Probiotic thyroid-friendly meal","lunch.jpg",360,12,54,8,3,
                   al("White rice","Low-fat curd","Mustard seeds","Curry leaves"),
                   al("Mix warm rice with curd","Temper with mustard seeds","Serve with sabzi"),"Iodine Rich")},
                {r("Sunflower Seeds Khichdi","Selenium + easy digestion","lunch.jpg",370,12,56,8,5,
                   al("Rice","Moong dal","Sunflower seeds 2 tbsp","Ghee"),
                   al("Pressure cook rice and dal","Add seeds as garnish","Light ghee tadka"),"Selenium")},
                {r("Eggs + Multigrain Roti","High protein thyroid support","lunch.jpg",400,22,42,14,5,
                   al("Eggs 3","Multigrain flour roti","Onion Tomato","Iodized salt"),
                   al("Scramble eggs","Make roti","Serve together with salad"),"Iodine Rich")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (condition.equals("PCOD")) {
            Recipe[][] bank = {
                {r("Quinoa Salad Bowl","Anti-inflammatory complete protein","lunch.jpg",380,14,52,9,6,
                   al("Quinoa","Cucumber","Cherry tomatoes","Olive oil"),
                   al("Cook quinoa","Cool and toss with veggies","Dress with olive oil"),"Anti-Inflammatory")},
                {r("Methi Roti + Moong Dal","Insulin sensitizing meal","lunch.jpg",360,14,52,8,8,
                   al("Whole wheat + methi flour","Moong dal","Garlic","Minimal oil"),
                   al("Knead methi dough","Cook dal with garlic","Serve together"),"Hormonal Balance")},
                {r("Flaxseed Roti + Mixed Sabzi","Omega-3 + fibre lunch","lunch.jpg",370,12,52,10,7,
                   al("Whole wheat flour","Flaxseed powder 2 tbsp","Mixed sabzi","Minimal oil"),
                   al("Add flaxseed to dough","Make rotis","Serve with sabzi"),"Omega-3")},
                {r("Brown Rice + Palak Dal","Low GI PCOD-friendly meal","lunch.jpg",380,15,58,7,6,
                   al("Brown rice","Palak dal","Garlic","Turmeric"),
                   al("Cook brown rice","Prepare palak dal","Serve together"),"Low GI")},
                {r("Rajma + Jowar Roti","Phytoestrogen balancing meal","lunch.jpg",400,16,60,7,10,
                   al("Rajma","Jowar roti","Onion Tomato","Low oil"),
                   al("Cook rajma","Make jowar rotis","Serve with salad"),"Hormonal Balance")},
                {r("Tofu Stir Fry + Brown Rice","Plant protein PCOD support","lunch.jpg",380,18,50,12,4,
                   al("Tofu","Brown rice","Mixed veggies","Soy sauce minimal"),
                   al("Stir fry tofu","Add veggies","Serve with brown rice"),"Anti-Inflammatory")},
                {r("Vegetable Daliya Khichdi","Complex carb hormone support","lunch.jpg",340,10,54,7,7,
                   al("Broken wheat","Mixed veggies","Moong dal","Ghee 1 tsp"),
                   al("Combine and pressure cook","Light ghee tadka","Serve hot"),"Low GI")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (goal.equals("Gain Muscle")) {
            Recipe[][] bank = {
                {r("Chicken Breast + Brown Rice + Dal","Muscle gaining power lunch","lunch.jpg",650,48,70,14,6,
                   al("Chicken breast 200g","Brown rice","Toor dal","Vegetables"),
                   al("Grill chicken","Cook brown rice","Prepare dal"),"Muscle Fuel")},
                {r("Paneer 200g + Whole Wheat Roti","Vegetarian muscle lunch","lunch.jpg",600,36,56,22,5,
                   al("Paneer 200g","Whole wheat roti 3","Onion Tomato","Spices"),
                   al("Make paneer masala","Cook rotis","Serve with dal"),"High Protein")},
                {r("Egg Curry + Rice","Protein + carb combo","lunch.jpg",580,36,62,16,3,
                   al("Eggs 4","White rice","Onion Tomato","Spices"),
                   al("Hard boil eggs","Make curry gravy","Serve with rice"),"Muscle Fuel")},
                {r("Chicken Biryani (Brown Rice)","Mass builder traditional meal","lunch.jpg",700,42,72,18,5,
                   al("Chicken 200g","Brown rice","Yogurt","Biryani spices"),
                   al("Marinate chicken","Layer with rice","Dum cook 20 min"),"Mass Gainer")},
                {r("Fish + Quinoa + Salad","Omega-3 muscle builder","lunch.jpg",560,40,50,16,5,
                   al("Rohu fish 200g","Quinoa","Mixed greens","Lemon"),
                   al("Grill fish","Cook quinoa","Serve with greens"),"High Protein")},
                {r("Chole + Brown Rice + Raita","Plant protein bulk meal","lunch.jpg",620,22,82,12,12,
                   al("Chickpeas","Brown rice","Curd raita","Onion Tomato"),
                   al("Cook chole","Boil rice","Make raita","Serve together"),"Plant Protein")},
                {r("Mutton / Soya Keema + Roti","Iron + protein combination","lunch.jpg",680,44,54,22,4,
                   al("Mutton mince 200g or Soya","Whole wheat roti","Onion Tomato","Spices"),
                   al("Cook keema masala","Make rotis","Serve hot"),"Muscle Fuel")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (goal.equals("Lose Weight")) {
            Recipe[][] bank = {
                {r("Jowar Roti + Dal + Salad","Low calorie complete lunch","lunch.jpg",320,12,48,7,7,
                   al("Jowar roti","Moong dal","Salad","Lemon"),
                   al("Make jowar rotis","Cook light dal","Serve with large salad"),"Low Cal")},
                {r("Vegetable Soup + 1 Roti","Filling low calorie option","lunch.jpg",240,7,34,5,6,
                   al("Mixed vegetables","Whole wheat roti 1","Herbs","Lemon"),
                   al("Make thick veggie soup","Cook one roti","Serve together"),"Low Cal")},
                {r("Palak Paneer (Low Fat) + Roti","Protein keeps hunger away","lunch.jpg",340,16,34,12,4,
                   al("Low-fat paneer 100g","Palak","Roti 2","Minimal oil"),
                   al("Make palak gravy","Add paneer","Serve with roti"),"Low Fat")},
                {r("Dal Soup + Brown Rice (Small)","Protein fibre filling lunch","lunch.jpg",310,12,48,5,6,
                   al("Toor dal","Brown rice small portion","Vegetables","Lemon"),
                   al("Cook light dal soup","Boil small rice portion","Serve with salad"),"Low Cal")},
                {r("Ragi Roti + Sabzi","Low GI weight loss roti","lunch.jpg",300,9,46,6,7,
                   al("Ragi flour","Mixed sabzi","Minimal oil","Buttermilk"),
                   al("Make ragi rotis","Prepare light sabzi","Serve with buttermilk"),"Low Cal")},
                {r("Mixed Sprout Salad Bowl","Detox filling salad","lunch.jpg",200,10,28,3,8,
                   al("Mixed sprouts","Cucumber Tomato","Lemon","Cumin powder"),
                   al("Toss sprouts with veggies","Add lemon dressing","Serve immediately"),"Detox")},
                {r("Egg White Curry + Brown Rice","Lean protein lunch","lunch.jpg",290,22,36,4,2,
                   al("Egg whites 5","Brown rice small","Onion Tomato","Minimal oil"),
                   al("Make egg white curry","Boil small rice portion","Serve with salad"),"Low Cal")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        // MAINTAIN default
        Recipe[][] bank = {
            {r("Dal Khichdi + Raita","Complete balanced Indian meal","lunch.jpg",420,14,62,9,5,
               al("Rice","Moong dal","Curd raita","Ghee 1 tsp"),
               al("Pressure cook together","Make raita","Temper and serve"),null)},
            {r("Rajma Chawal","North Indian protein classic","lunch.jpg",460,16,70,8,10,
               al("Rajma","White rice","Onion Tomato","Spices"),
               al("Cook rajma masala","Boil rice","Serve hot"),null)},
            {r("Paneer Sabzi + Roti","Protein balanced vegetarian lunch","lunch.jpg",480,22,50,18,4,
               al("Paneer 120g","Whole wheat roti","Onion Tomato","Spices"),
               al("Make paneer masala","Cook rotis","Serve with dal"),null)},
            {r("Chole + Rice + Salad","High protein legume lunch","lunch.jpg",480,18,72,8,10,
               al("Chickpeas","White rice","Salad","Onion Tomato"),
               al("Cook chole masala","Boil rice","Serve with salad"),null)},
            {r("Mixed Veg Pulao + Dal","One-pot nutritious meal","lunch.jpg",440,12,68,8,5,
               al("Basmati rice","Mixed vegetables","Moong dal","Ghee"),
               al("Cook pulao","Prepare simple dal","Serve together"),null)},
            {r("Egg Curry + Rice","Classic protein-rich lunch","lunch.jpg",460,22,60,14,2,
               al("Eggs 3","White rice","Onion Tomato","Spices"),
               al("Hard boil eggs","Make masala curry","Serve with rice"),null)},
            {r("Palak Dal + Jowar Roti","Iron + protein balanced lunch","lunch.jpg",400,15,58,8,8,
               al("Palak","Toor dal","Jowar roti","Garlic"),
               al("Cook dal with palak","Make jowar rotis","Serve together"),null)}
        };
        return Arrays.asList(bank[day % bank.length]);
    }

    // ─────────────────────────────────────────────────────────────────
    //  DINNER BANK
    // ─────────────────────────────────────────────────────────────────
    private List<Recipe> getDinner(String condition, String goal, int day) {

        if (condition.equals("Diabetes")) {
            Recipe[][] bank = {
                {r("Moong Dal Soup + 1 Ragi Roti","Light low GI dinner","dinner.jpg",280,12,40,6,7,
                   al("Yellow moong dal","Ragi flour","Garlic","Turmeric"),
                   al("Make thin moong soup","Knead ragi dough","Cook rotis"),"Low GI")},
                {r("Bitter Gourd Sabzi + Jowar Roti","Karela controls blood sugar","dinner.jpg",260,9,36,8,7,
                   al("Bitter gourd","Jowar roti","Onion","Minimal oil"),
                   al("Stir fry karela","Make jowar rotis","Serve together"),"Diabetic")},
                {r("Palak Soup + 1 Roti","Iron + alkaline light dinner","dinner.jpg",220,8,30,5,5,
                   al("Palak","Whole wheat roti 1","Garlic","Black pepper"),
                   al("Blend palak soup","Cook one roti","Serve lightly"),"Low Cal")},
                {r("Vegetable Daliya","Filling whole grain light dinner","dinner.jpg",270,8,42,5,6,
                   al("Broken wheat","Mixed vegetables","Mustard seeds","Low oil"),
                   al("Cook daliya with veggies","Light tempering","Serve hot"),"Low GI")},
                {r("Ragi Roti + Lauki Sabzi","Bottle gourd low carb dinner","dinner.jpg",250,8,36,6,5,
                   al("Ragi flour","Lauki (bottle gourd)","Onion","Minimal oil"),
                   al("Cook lauki sabzi lightly","Make ragi rotis","Serve together"),"Low GI")},
                {r("Sprouted Moong Soup","Enzyme-rich digestive dinner","dinner.jpg",180,10,24,2,6,
                   al("Sprouted moong","Ginger","Lemon","Cumin"),
                   al("Cook sprouts lightly","Add ginger-lemon","Serve as soup"),"Diabetic")},
                {r("Besan Kadhi + 1 Jowar Roti","Light yogurt curry dinner","dinner.jpg",280,10,36,8,3,
                   al("Low-fat curd","Besan","Jowar roti","Curry leaves"),
                   al("Cook light kadhi","Make jowar roti","Serve together"),"Low GI")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (condition.equals("BloodPressure")) {
            Recipe[][] bank = {
                {r("Vegetable Soup + 1 Multigrain Roti","Low sodium DASH dinner","dinner.jpg",260,7,36,6,5,
                   al("Mixed vegetables","Multigrain roti","Herbs","No salt"),
                   al("Make vegetable broth","Cook roti","Season with herbs only"),"Low Sodium")},
                {r("Moong Dal + Bajra Roti","Potassium-rich light dinner","dinner.jpg",300,13,44,6,7,
                   al("Moong dal","Bajra roti","Garlic 2 cloves","No excess salt"),
                   al("Cook thin dal","Make bajra roti","Serve with salad"),"Low Sodium")},
                {r("Lauki Sabzi + Roti","Bottle gourd is BP-friendly","dinner.jpg",260,7,36,7,4,
                   al("Lauki","Whole wheat roti","Onion","Minimal oil"),
                   al("Cook lauki lightly","Make roti","Serve with buttermilk"),"Low Sodium")},
                {r("Mixed Vegetable Khichdi","One-pot low sodium dinner","dinner.jpg",310,10,48,7,5,
                   al("Rice","Moong dal","Mixed veggies","Minimal ghee"),
                   al("Pressure cook all","Light ghee tadka","Serve hot"),"DASH")},
                {r("Palak Soup + Multigrain Toast","Iron + no-sodium dinner","dinner.jpg",220,8,28,5,5,
                   al("Palak","Multigrain bread 2 slices","Garlic","Black pepper only"),
                   al("Blend palak soup","Toast bread","Season with pepper only"),"Low Sodium")},
                {r("Curd Rice + Low Sodium Sabzi","Probiotic cooling dinner","dinner.jpg",300,10,46,7,2,
                   al("White rice","Low-fat curd","Mustard seeds","Curry leaves"),
                   al("Mix rice with curd","Light tempering","Serve cool"),"DASH")},
                {r("Toor Dal Soup + 1 Roti","Simple DASH-approved dinner","dinner.jpg",290,12,40,6,5,
                   al("Toor dal","Whole wheat roti","Tomato","Garlic"),
                   al("Cook thin dal soup","Make one roti","Serve together"),"DASH")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (condition.equals("HeartDisease")) {
            Recipe[][] bank = {
                {r("Grilled Fish + Steamed Veggies","Omega-3 heart dinner","dinner.jpg",380,30,20,14,5,
                   al("Fish fillet","Broccoli","Carrot","Olive oil herbs"),
                   al("Marinate fish","Grill without frying","Steam vegetables"),"Omega-3")},
                {r("Moong Dal Soup + Multigrain Roti","Light low cholesterol dinner","dinner.jpg",300,12,42,6,6,
                   al("Moong dal","Multigrain roti","Garlic","Turmeric"),
                   al("Cook thin soup","Make roti","Serve lightly"),"Low Cholesterol")},
                {r("Palak Paneer (Low Fat) + 1 Roti","Iron + protein heart dinner","dinner.jpg",320,16,30,12,5,
                   al("Low-fat paneer","Palak","Roti 1","Minimal oil"),
                   al("Make palak gravy without cream","Add paneer","Serve with roti"),"Heart Healthy")},
                {r("Walnut Dal Tadka + Brown Rice","Omega-3 rich dal dinner","dinner.jpg",380,15,52,12,5,
                   al("Toor dal","Brown rice small","Walnuts","Garlic"),
                   al("Cook dal","Add walnut pieces as garnish","Serve with rice"),"Omega-3")},
                {r("Besan Vegetable Soup","Cholesterol-free filling dinner","dinner.jpg",240,10,30,6,4,
                   al("Besan","Mixed vegetables","Ginger","Black pepper"),
                   al("Make besan broth","Add vegetables","Cook thick soup"),"Low Cholesterol")},
                {r("Oats Khichdi","Soluble fibre for LDL reduction","dinner.jpg",300,10,46,7,6,
                   al("Rolled oats","Moong dal","Vegetables","Ghee 1 tsp"),
                   al("Cook oats and dal together","Add vegetables","Serve warm"),"Heart Healthy")},
                {r("Steamed Idli + Sambar","Light fermented heart dinner","dinner.jpg",280,9,48,3,4,
                   al("Idli batter","Sambar dal","Vegetables","No excess oil"),
                   al("Steam idlis","Make light sambar","Serve hot"),"Heart Healthy")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (condition.equals("Thyroid")) {
            Recipe[][] bank = {
                {r("Fish Soup + Rice","Best iodine rich light dinner","dinner.jpg",360,24,44,8,3,
                   al("Fish 150g","White rice","Iodized salt","Vegetables"),
                   al("Make light fish soup","Boil small rice portion","Serve together"),"Iodine Rich")},
                {r("Egg Curry (Light) + Roti","Selenium protein dinner","dinner.jpg",380,22,40,14,2,
                   al("Eggs 2","Whole wheat roti","Onion Tomato","Iodized salt"),
                   al("Make light egg curry","Cook roti","Serve together"),"Iodine Rich")},
                {r("Paneer + Daliya","Protein + selenium combo","dinner.jpg",360,18,44,12,5,
                   al("Paneer 100g","Broken wheat","Vegetables","Iodized salt"),
                   al("Cook daliya","Make paneer sabzi","Serve together"),"High Protein")},
                {r("Moong Dal Soup + Roti","Easy digest thyroid dinner","dinner.jpg",280,11,38,5,5,
                   al("Moong dal","Whole wheat roti","Garlic","Iodized salt"),
                   al("Cook thin soup","Make roti","Season with iodized salt"),"Iodine Rich")},
                {r("Brazil Nut Veggie Soup","Selenium-dense warm dinner","dinner.jpg",260,8,30,10,5,
                   al("Mixed vegetables","Brazil nuts 3","Ginger","Black pepper"),
                   al("Make vegetable broth","Crush Brazil nuts in","Serve warm"),"Selenium")},
                {r("Idli + Sambar (Iodized)","Traditional iodine dinner","dinner.jpg",260,8,44,3,4,
                   al("Idli batter","Sambar","Iodized salt","Curry leaves"),
                   al("Steam idlis","Cook sambar with iodized salt","Serve hot"),"Iodine Rich")},
                {r("Curd Rice + Sabzi","Probiotic dairy iodine dinner","dinner.jpg",300,10,44,7,2,
                   al("White rice","Low-fat curd","Mustard seeds","Iodized salt"),
                   al("Mix rice with curd","Light tempering","Serve with sabzi"),"Iodine Rich")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (condition.equals("PCOD")) {
            Recipe[][] bank = {
                {r("Turmeric Moong Dal Soup + Roti","Anti-inflammatory light dinner","dinner.jpg",290,12,38,6,6,
                   al("Moong dal","Turmeric","Whole wheat roti","Ginger"),
                   al("Cook golden dal with turmeric","Make roti","Serve together"),"Anti-Inflammatory")},
                {r("Methi Roti + Raita","Hormone regulating dinner","dinner.jpg",300,11,38,9,6,
                   al("Whole wheat + methi flour","Low-fat curd raita","Cucumber","Cumin"),
                   al("Make methi rotis","Prepare raita","Serve together"),"Hormonal Balance")},
                {r("Stir Fried Tofu + Brown Rice","Plant protein PCOD dinner","dinner.jpg",340,17,46,10,4,
                   al("Tofu","Brown rice small","Mixed vegetables","Minimal soy sauce"),
                   al("Stir fry tofu and veggies","Cook small rice portion","Serve warm"),"Anti-Inflammatory")},
                {r("Ginger Lemon Dal Soup","Digestive anti-inflammatory dinner","dinner.jpg",240,10,30,5,5,
                   al("Moong dal","Fresh ginger","Lemon","Coriander"),
                   al("Cook thin dal","Add ginger and lemon","Serve as soup"),"Anti-Inflammatory")},
                {r("Bajra Roti + Lauki Sabzi","Complex carb hormone dinner","dinner.jpg",280,8,40,7,7,
                   al("Bajra flour","Lauki","Onion","Minimal oil"),
                   al("Make bajra rotis","Cook lauki lightly","Serve together"),"Low GI")},
                {r("Flaxseed Khichdi","Omega-3 one-pot PCOD dinner","dinner.jpg",320,11,48,8,6,
                   al("Rice","Moong dal","Flaxseeds 2 tbsp","Vegetables"),
                   al("Cook khichdi normally","Stir in flaxseeds","Serve warm"),"Omega-3")},
                {r("Palak Soup + Multigrain Roti","Iron for PCOD anaemia","dinner.jpg",260,9,32,6,6,
                   al("Palak","Multigrain roti","Garlic","Black pepper"),
                   al("Blend palak soup","Cook roti","Serve lightly"),"Anti-Inflammatory")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (goal.equals("Gain Muscle")) {
            Recipe[][] bank = {
                {r("Chicken + Sweet Potato","Post-gym muscle recovery dinner","dinner.jpg",520,42,54,10,6,
                   al("Chicken breast 180g","Sweet potato","Broccoli","Olive oil"),
                   al("Grill chicken","Bake sweet potato","Steam broccoli","Serve together"),"Muscle Recovery")},
                {r("Paneer Tikka + Brown Rice","Vegetarian muscle dinner","dinner.jpg",540,34,58,18,4,
                   al("Paneer 180g","Brown rice","Capsicum Onion","Yogurt marinade"),
                   al("Marinate paneer","Grill on tawa","Serve with brown rice"),"High Protein")},
                {r("Egg Bhurji + Roti (3)","Protein + carb night meal","dinner.jpg",500,30,54,18,4,
                   al("Eggs 4","Whole wheat roti 3","Onion Tomato","Spices"),
                   al("Scramble eggs with veggies","Cook rotis","Serve hot"),"Muscle Fuel")},
                {r("Fish Curry + Brown Rice","Omega-3 muscle builder","dinner.jpg",520,38,54,14,4,
                   al("Fish 200g","Brown rice","Onion Tomato","Coconut minimal"),
                   al("Make light fish curry","Cook brown rice","Serve together"),"High Protein")},
                {r("Soya Keema + Roti","Plant protein muscle dinner","dinner.jpg",480,32,52,14,6,
                   al("Soya granules","Whole wheat roti 3","Onion Tomato","Spices"),
                   al("Soak soya","Cook keema masala","Serve with rotis"),"Plant Protein")},
                {r("Chicken Dal Tadka + Rice","Double protein dinner","dinner.jpg",560,44,58,14,5,
                   al("Chicken 180g","Toor dal","White rice","Onion Tomato"),
                   al("Grill chicken separately","Cook dal tadka","Serve with rice"),"Muscle Fuel")},
                {r("Mutton / Paneer Curry + 3 Roti","Iron + protein mass builder","dinner.jpg",600,40,56,20,5,
                   al("Mutton 180g or Paneer 200g","Whole wheat roti 3","Onion Tomato","Spices"),
                   al("Cook curry","Make rotis","Serve hot"),"Mass Gainer")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (goal.equals("Lose Weight")) {
            Recipe[][] bank = {
                {r("Moong Dal Soup","Best fat loss dinner, light and filling","dinner.jpg",180,10,24,3,5,
                   al("Yellow moong dal","Ginger","Lemon","Cumin"),
                   al("Cook thin soup","Add ginger","Finish with lemon"),"Low Cal")},
                {r("Grilled Paneer Salad","Protein keeps you full at night","dinner.jpg",220,16,8,14,2,
                   al("Low-fat paneer 80g","Mixed greens","Cucumber","Lemon dressing"),
                   al("Grill paneer without oil","Toss with greens","Add lemon dressing"),"Low Cal")},
                {r("Vegetable Soup + 1 Roti","Fibre-rich low calorie dinner","dinner.jpg",200,6,28,4,6,
                   al("Mixed vegetables","Whole wheat roti 1","Herbs","Lemon"),
                   al("Make thick veggie soup","Cook one roti","Serve together"),"Low Cal")},
                {r("Idli 2 + Sambar","Light fermented weight loss dinner","dinner.jpg",200,7,36,2,4,
                   al("Idli batter","Vegetable sambar","No oil","Curry leaves"),
                   al("Steam idlis","Prepare light sambar","Serve hot"),"Low Cal")},
                {r("Egg White Bhurji + 1 Roti","15g protein, only 180 cal","dinner.jpg",200,16,20,4,1,
                   al("Egg whites 4","Whole wheat roti 1","Onion Capsicum","Minimal oil"),
                   al("Scramble egg whites with veggies","Cook one roti","Serve hot"),"Low Cal")},
                {r("Palak Soup","Iron-rich detox night soup","dinner.jpg",120,5,14,4,4,
                   al("Palak","Garlic","Black pepper","Lemon"),
                   al("Blend palak soup","Add garlic","Season with pepper and lemon"),"Detox")},
                {r("Ragi Roti + Curd","Calcium + probiotic night meal","dinner.jpg",220,8,32,5,4,
                   al("Ragi flour","Low-fat curd","Cucumber raita","Cumin"),
                   al("Make ragi rotis","Prepare raita","Serve together"),"Low Cal")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        // MAINTAIN default
        Recipe[][] bank = {
            {r("Dal Makhani + 2 Roti","Rich protein traditional dinner","dinner.jpg",440,16,58,14,7,
               al("Black dal","Whole wheat roti 2","Onion Tomato","Minimal butter"),
               al("Slow cook dal","Make rotis","Serve hot"),null)},
            {r("Palak Paneer + Roti","Iron + protein dinner","dinner.jpg",460,20,46,20,5,
               al("Paneer","Palak","Roti 2","Onion Tomato"),
               al("Make palak gravy","Add paneer","Serve with roti"),null)},
            {r("Chole + Rice + Raita","Complete protein dinner","dinner.jpg",480,17,70,8,9,
               al("Chickpeas","White rice","Raita","Onion Tomato"),
               al("Cook chole","Boil rice","Make raita","Serve"),null)},
            {r("Mixed Veg + Roti + Dal","Well-rounded traditional dinner","dinner.jpg",420,14,58,10,6,
               al("Mixed vegetables","Toor dal","Whole wheat roti","Spices"),
               al("Cook mixed sabzi","Prepare dal","Make rotis","Serve"),null)},
            {r("Egg Curry + 2 Roti","Protein-rich night meal","dinner.jpg",440,22,52,14,2,
               al("Eggs 3","Whole wheat roti 2","Onion Tomato","Spices"),
               al("Make egg curry","Cook rotis","Serve hot"),null)},
            {r("Rajma + Roti + Salad","High fibre satisfying dinner","dinner.jpg",460,16,64,8,10,
               al("Rajma","Whole wheat roti 2","Green salad","Lemon"),
               al("Cook rajma masala","Make rotis","Serve with salad"),null)},
            {r("Fish Curry + Rice","Omega-3 balanced dinner","dinner.jpg",460,28,54,12,3,
               al("Rohu fish","White rice","Onion Tomato","Coconut minimal"),
               al("Make light curry","Boil rice","Serve hot"),null)}
        };
        return Arrays.asList(bank[day % bank.length]);
    }

    // ─────────────────────────────────────────────────────────────────
    //  SNACKS BANK
    // ─────────────────────────────────────────────────────────────────
    private List<Recipe> getSnacks(String condition, String goal, int day) {

        if (condition.equals("Diabetes")) {
            Recipe[][] bank = {
                {r("Bitter Gourd Juice","Natural blood sugar reducer","snacks.jpg",30,1,6,0,1,
                   al("Bitter gourd","Lemon","Ginger"),al("Blend and strain","Add lemon","Drink immediately"),"Diabetic")},
                {r("Roasted Chana","Low GI high fibre snack","snacks.jpg",120,7,18,2,5,
                   al("Bengal gram","Black salt","Lemon"),al("Dry roast chana","Add salt and lemon","Serve"),"Low GI")},
                {r("Methi Seeds Water","Insulin sensitizing morning drink","snacks.jpg",15,1,3,0,1,
                   al("Methi seeds 1 tsp","Water"),al("Soak overnight","Drink water in morning","Eat seeds"),"Diabetic")},
                {r("Cucumber Sticks + Hummus","Low carb filling snack","snacks.jpg",110,5,14,4,3,
                   al("Cucumber","Homemade hummus","Lemon","Black pepper"),al("Slice cucumber","Dip in hummus","Serve fresh"),"Low GI")},
                {r("Sprouted Moong","Enzyme-rich raw snack","snacks.jpg",80,6,14,0,4,
                   al("Sprouted moong","Lemon","Black salt"),al("Sprout moong overnight","Add lemon","Serve fresh"),"Diabetic")},
                {r("Flaxseed Buttermilk","Probiotic omega-3 snack drink","snacks.jpg",70,4,8,2,2,
                   al("Low-fat buttermilk","Flaxseed powder 1 tsp","Cumin","Coriander"),al("Mix all","Stir well","Drink chilled"),"Diabetic")},
                {r("Roasted Pumpkin Seeds","Magnesium for insulin function","snacks.jpg",100,5,8,7,1,
                   al("Pumpkin seeds","Rock salt","Lemon"),al("Dry roast seeds","Add salt","Serve"),"Low GI")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (condition.equals("BloodPressure")) {
            Recipe[][] bank = {
                {r("Hibiscus Tea","Natural ACE inhibitor drink","snacks.jpg",10,0,2,0,0,
                   al("Dried hibiscus","Hot water","Honey"),al("Steep hibiscus 5 min","Add honey","Drink warm"),"BP Control")},
                {r("Banana + Walnuts","Potassium + omega-3 power snack","snacks.jpg",180,4,30,8,3,
                   al("Banana 1","Walnuts 4 pieces"),al("Eat together","Chew slowly","Stay hydrated"),"Low Sodium")},
                {r("Unsalted Roasted Makhana","Zero sodium snack","snacks.jpg",90,3,14,2,0,
                   al("Makhana","Ghee 1/2 tsp","Pepper only"),al("Roast in ghee","Add pepper only","No salt at all"),"Low Sodium")},
                {r("Coconut Water","Natural electrolyte BP drink","snacks.jpg",45,1,9,0,0,
                   al("Fresh coconut water"),al("Serve chilled","Drink slowly","Avoid packaged"),"Low Sodium")},
                {r("Flaxseed Crackers","Omega-3 low sodium snack","snacks.jpg",120,4,16,5,3,
                   al("Flaxseeds","Whole wheat flour","Minimal salt","Herbs"),al("Make dough","Roll thin","Bake until crisp"),"Low Sodium")},
                {r("Garlic Green Tea","Garlic lowers blood pressure naturally","snacks.jpg",15,0,3,0,0,
                   al("Green tea","Garlic 1 clove","Lemon","Honey"),al("Brew green tea","Crush garlic in","Add lemon and honey"),"BP Control")},
                {r("Pomegranate Seeds","Natural nitric oxide booster","snacks.jpg",80,1,18,0,3,
                   al("Fresh pomegranate"),al("Deseed fresh pomegranate","Eat as is","Drink juice occasionally"),"BP Control")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (condition.equals("HeartDisease")) {
            Recipe[][] bank = {
                {r("Walnut + Flaxseed Mix","Best cardiac snack combo","snacks.jpg",160,5,8,14,3,
                   al("Walnuts 4 pieces","Flaxseeds 1 tsp","Almonds 5"),al("Mix together","Eat slowly","Chew well"),"Omega-3")},
                {r("Green Tea + Dark Chocolate","Flavonoids for heart health","snacks.jpg",80,1,12,4,1,
                   al("Green tea","Dark chocolate 1 square (70%+)"),al("Brew green tea","Have with dark chocolate","Avoid milk chocolate"),"Heart Healthy")},
                {r("Chia Seed Lemonade","Omega-3 refreshing drink","snacks.jpg",60,2,10,2,4,
                   al("Chia seeds 1 tbsp","Lemon juice","Honey","Water"),al("Soak chia 15 min","Mix with lemonade","Stir and drink"),"Omega-3")},
                {r("Almond Milk Smoothie","LDL lowering healthy snack","snacks.jpg",150,5,22,6,3,
                   al("Almond milk","Banana","Flaxseeds","Honey"),al("Blend all","Serve cold","Drink immediately"),"Heart Healthy")},
                {r("Arjuna Bark Tea","Traditional Ayurvedic heart tonic","snacks.jpg",10,0,2,0,0,
                   al("Arjuna bark powder","Hot water","Honey"),al("Boil bark powder","Strain","Add honey"),"Heart Tonic")},
                {r("Roasted Pumpkin Seeds","CoQ10 supporting snack","snacks.jpg",100,5,8,7,1,
                   al("Pumpkin seeds","Rock salt","Lemon"),al("Dry roast","Add seasoning","Serve"),"Heart Healthy")},
                {r("Blueberry Smoothie","Anthocyanins reduce heart risk","snacks.jpg",130,4,24,2,3,
                   al("Blueberries","Low-fat yogurt","Honey","Water"),al("Blend all","Serve cold","Drink fresh"),"Omega-3")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (condition.equals("Thyroid")) {
            Recipe[][] bank = {
                {r("Brazil Nut + Walnut Mix","Best selenium + omega-3 snack","snacks.jpg",150,4,6,13,1,
                   al("Brazil nuts 3","Walnuts 4","Almonds 5"),al("Mix together","Eat fresh","Do not store long"),"Selenium")},
                {r("Yogurt + Fruits","Dairy iodine + vitamins","snacks.jpg",160,8,26,3,2,
                   al("Low-fat yogurt","Banana","Strawberries","Honey"),al("Take yogurt","Slice fruits","Mix together"),"Iodine Rich")},
                {r("Boiled Egg","Selenium + iodine single snack","snacks.jpg",80,6,0,5,0,
                   al("Egg 1","Iodized salt","Black pepper"),al("Hard boil egg","Season with iodized salt","Eat fresh"),"Iodine Rich")},
                {r("Sunflower Seed Trail Mix","Selenium + vitamin E snack","snacks.jpg",140,5,12,9,2,
                   al("Sunflower seeds","Almonds","Raisins"),al("Mix seeds and nuts","Add raisins","Portion 30g serving"),"Selenium")},
                {r("Makhana (Roasted)","Low cal thyroid-friendly snack","snacks.jpg",90,3,14,2,0,
                   al("Makhana","Ghee 1/2 tsp","Rock salt","Turmeric"),al("Roast in ghee","Add seasoning","Serve crisp"),"Low Cal")},
                {r("Banana Smoothie","Potassium + iodine combo snack","snacks.jpg",160,5,30,3,3,
                   al("Banana","Low-fat milk","Honey","Cardamom"),al("Blend with milk","Add honey","Serve cold"),"Iodine Rich")},
                {r("Coconut Water","Electrolytes support thyroid","snacks.jpg",45,1,9,0,0,
                   al("Fresh coconut water"),al("Drink fresh","Avoid packaged","One glass"),null)}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (condition.equals("PCOD")) {
            Recipe[][] bank = {
                {r("Spearmint Tea","Reduces androgen levels naturally","snacks.jpg",5,0,1,0,0,
                   al("Spearmint leaves","Hot water","Honey"),al("Steep 5 min","Add honey","Drink warm"),"Hormonal Balance")},
                {r("Flaxseed Lemonade","Omega-3 PCOD hormone support","snacks.jpg",50,1,8,2,3,
                   al("Flaxseed powder 1 tsp","Lemon juice","Honey","Water"),al("Mix all","Stir well","Drink immediately"),"Omega-3")},
                {r("Chia Seed Pudding","Protein + omega-3 snack","snacks.jpg",160,6,20,7,8,
                   al("Chia seeds 2 tbsp","Almond milk","Honey","Berries"),al("Soak chia in milk 4 hrs","Top with berries","Eat cold"),"Anti-Inflammatory")},
                {r("Mixed Nuts + Dark Chocolate","Anti-inflammatory power snack","snacks.jpg",170,5,14,12,2,
                   al("Walnuts","Almonds","Dark chocolate 1 square"),al("Mix nuts","Have with chocolate","30g serving only"),"Anti-Inflammatory")},
                {r("Cucumber Mint Water","Detox and PCOD support drink","snacks.jpg",10,0,2,0,0,
                   al("Cucumber","Mint leaves","Lemon","Water"),al("Slice cucumber","Add to water with mint","Refrigerate 1 hr","Drink"),"Detox")},
                {r("Roasted Pumpkin Seeds","Zinc for PCOD hormone balance","snacks.jpg",100,5,8,7,1,
                   al("Pumpkin seeds","Rock salt","Lemon"),al("Dry roast seeds","Season lightly","30g serving"),"Hormonal Balance")},
                {r("Turmeric Milk (Low Fat)","Anti-inflammatory golden milk","snacks.jpg",110,6,14,3,0,
                   al("Low-fat milk","Turmeric 1/4 tsp","Ginger","Honey"),al("Warm milk","Add turmeric and ginger","Add honey","Serve warm"),"Anti-Inflammatory")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (goal.equals("Gain Muscle")) {
            Recipe[][] bank = {
                {r("Sattu Shake","Traditional Indian protein shake","snacks.jpg",360,20,52,8,3,
                   al("Sattu 60g","Milk 300ml","Jaggery","Cardamom"),al("Add sattu to milk","Mix well","Add jaggery","Serve"),"Mass Gainer")},
                {r("Peanut Butter Banana Shake","500 cal muscle shake","snacks.jpg",480,18,62,18,5,
                   al("Banana 2","Peanut butter 2 tbsp","Milk 300ml","Oats"),al("Blend all","Thick consistency","Drink immediately"),"Mass Gainer")},
                {r("Boiled Eggs + Bread","Quick protein carb snack","snacks.jpg",320,20,32,12,2,
                   al("Eggs 3 boiled","Whole grain bread 2 slices","Black pepper","Butter minimal"),al("Boil eggs","Toast bread","Assemble"),"High Protein")},
                {r("Greek Yogurt + Granola","Casein protein between meals","snacks.jpg",300,16,38,8,2,
                   al("Greek yogurt 150g","Granola 30g","Honey","Almonds"),al("Take yogurt","Add granola","Drizzle honey","Eat"),"High Protein")},
                {r("Chana Chaat","Plant protein post-workout snack","snacks.jpg",240,12,36,5,8,
                   al("Boiled chana","Onion Tomato","Lemon","Chaat masala"),al("Boil chana","Toss with veggies","Add masala and lemon"),"Plant Protein")},
                {r("Paneer Cubes + Nuts","Casein + healthy fats snack","snacks.jpg",280,18,6,20,0,
                   al("Paneer 100g","Almonds 10","Walnuts 4","Black pepper"),al("Cut paneer","Mix with nuts","Season with pepper"),"High Protein")},
                {r("Whey + Banana (Post Workout)","Fastest muscle recovery snack","snacks.jpg",300,25,38,3,2,
                   al("Whey protein 1 scoop","Banana","Water or milk","Ice"),al("Blend all","Drink within 45 min post workout","Crucial timing"),"Muscle Recovery")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        if (goal.equals("Lose Weight")) {
            Recipe[][] bank = {
                {r("Green Tea","Boosts metabolism, zero calorie","snacks.jpg",5,0,1,0,0,
                   al("Green tea bag","Hot water","Lemon"),al("Steep 3 min","Add lemon","No sugar"),"Metabolism Boost")},
                {r("Cucumber + Carrot Sticks","Zero guilt raw snack","snacks.jpg",50,2,10,0,4,
                   al("Cucumber","Carrot","Lemon juice","Black pepper"),al("Slice veggies","Add lemon","Eat fresh"),"Low Cal")},
                {r("Buttermilk (Plain)","Probiotic zero-fat drink","snacks.jpg",40,3,5,1,0,
                   al("Low-fat curd","Water","Cumin","Coriander"),al("Blend curd with water","Add spices","Serve cold"),"Low Cal")},
                {r("Roasted Makhana","Filling low calorie snack","snacks.jpg",80,3,14,1,0,
                   al("Makhana","Ghee 1/2 tsp","Rock salt","Pepper"),al("Roast in ghee","Add seasoning","Serve crisp"),"Low Cal")},
                {r("Apple + Cinnamon","Pectin keeps hunger away","snacks.jpg",70,0,18,0,3,
                   al("Apple 1 medium","Cinnamon powder","Lemon"),al("Slice apple","Sprinkle cinnamon","Add lemon"),"Low Cal")},
                {r("Lemon Water + Chia","Detox metabolism booster","snacks.jpg",40,1,6,1,3,
                   al("Lemon juice","Chia seeds 1 tsp","Honey minimal","Water"),al("Mix all","Let chia soak 10 min","Drink"),"Detox")},
                {r("Sprouted Moong Chaat","Protein without calorie load","snacks.jpg",100,7,16,1,5,
                   al("Sprouted moong","Lemon","Black salt","Cumin"),al("Sprout moong","Toss with spices","Eat fresh"),"Low Cal")}
            };
            return Arrays.asList(bank[day % bank.length]);
        }

        // MAINTAIN default
        Recipe[][] bank = {
            {r("Roasted Chana + Peanuts","Protein fibre evening snack","snacks.jpg",150,8,18,6,4,
               al("Roasted chana","Peanuts","Black salt","Lemon"),al("Mix together","Add seasoning","Serve"),null)},
            {r("Buttermilk","Probiotic cooling drink","snacks.jpg",40,3,5,1,0,
               al("Curd","Water","Cumin","Mint"),al("Blend curd","Add spices","Serve cold"),null)},
            {r("Makhana","Light crispy protein snack","snacks.jpg",80,3,14,1,0,
               al("Makhana","Ghee","Salt","Pepper"),al("Roast in ghee","Season","Serve"),null)},
            {r("Fruit Salad","Vitamins and antioxidants","snacks.jpg",100,1,24,0,3,
               al("Seasonal fruits","Chaat masala","Lemon"),al("Dice fresh fruits","Add masala","Serve fresh"),null)},
            {r("Peanut Butter Banana","Energy filling snack","snacks.jpg",180,6,26,7,3,
               al("Banana","Peanut butter 1 tbsp"),al("Slice banana","Dip or spread PB","Eat fresh"),null)},
            {r("Green Tea + Almonds","Antioxidants + healthy fats","snacks.jpg",90,3,4,7,1,
               al("Green tea","Almonds 10 pieces"),al("Brew tea","Eat almonds alongside"),null)},
            {r("Coconut Water","Electrolyte refreshing drink","snacks.jpg",45,1,9,0,0,
               al("Fresh coconut water"),al("Drink chilled","Avoid packaged"),null)}
        };
        return Arrays.asList(bank[day % bank.length]);
    }

    // ─────────────────────────────────────────────────────────────────
    //  HELPER — create Recipe quickly
    // ─────────────────────────────────────────────────────────────────
    private Recipe r(String name, String desc, String img,
                     int cal, int protein, int carbs, int fat, int fiber,
                     List<String> ingredients, List<String> instructions, String tag) {
        Recipe re = new Recipe();
        re.setName(name);
        re.setDescription(desc);
        re.setImageName(img);
        re.setCalories(cal);
        re.setProtein(protein);
        re.setCarbs(carbs);
        re.setFats(fat);
        re.setFiber(fiber);
        re.setIngredients(ingredients);
        re.setInstructions(instructions);
        re.setConditionTag(tag);
        return re;
    }

    private List<String> al(String... items) {
        return Arrays.asList(items);
    }
}