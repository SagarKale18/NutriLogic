package com.nutrilogic.model;

import java.util.List;

public class Recipe {

    private String name;
    private String description;
    private String imageName;
    private int calories;
    private int protein;
    private int carbs;
    private int fats;
    private int fiber;
    private List<String> ingredients;
    private List<String> instructions;
    private String conditionTag; // e.g. "Low GI", "Omega-3", "Low Sodium"

    // ── Default constructor (needed for RecipesController helper)
    public Recipe() {}

    // ── Full constructor (backward compatible — original 10-arg)
    public Recipe(String name, String description, String imageName,
                  int calories, int protein, int carbs, int fats,
                  List<String> ingredients, List<String> instructions) {
        this.name         = name;
        this.description  = description;
        this.imageName    = imageName;
        this.calories     = calories;
        this.protein      = protein;
        this.carbs        = carbs;
        this.fats         = fats;
        this.fiber        = 0;
        this.ingredients  = ingredients;
        this.instructions = instructions;
        this.conditionTag = null;
    }

    // ── Getters ──────────────────────────────────────────────────────

    public String getName()              { return name; }
    public String getDescription()       { return description; }
    public String getImageName()         { return imageName; }
    public int    getCalories()          { return calories; }
    public int    getProtein()           { return protein; }
    public int    getCarbs()             { return carbs; }
    public int    getFats()              { return fats; }
    public int    getFiber()             { return fiber; }
    public List<String> getIngredients() { return ingredients; }
    public List<String> getInstructions(){ return instructions; }
    public String getConditionTag()      { return conditionTag; }

    // ── Setters ──────────────────────────────────────────────────────

    public void setName(String name)                          { this.name = name; }
    public void setDescription(String description)            { this.description = description; }
    public void setImageName(String imageName)                { this.imageName = imageName; }
    public void setCalories(int calories)                     { this.calories = calories; }
    public void setProtein(int protein)                       { this.protein = protein; }
    public void setCarbs(int carbs)                           { this.carbs = carbs; }
    public void setFats(int fats)                             { this.fats = fats; }
    public void setFiber(int fiber)                           { this.fiber = fiber; }
    public void setIngredients(List<String> ingredients)      { this.ingredients = ingredients; }
    public void setInstructions(List<String> instructions)    { this.instructions = instructions; }
    public void setConditionTag(String conditionTag)          { this.conditionTag = conditionTag; }
}