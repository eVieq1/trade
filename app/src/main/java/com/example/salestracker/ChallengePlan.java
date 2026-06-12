package com.example.salestracker;

public class ChallengePlan {
    private String category;
    private String model;
    private double target;
    private double fact;
    private String unit;
    private int id;

    public ChallengePlan(String category, String model, double target, double fact, String unit) {
        this.category = category;
        this.model = model;
        this.target = target;
        this.fact = fact;
        this.unit = unit;
    }

    public ChallengePlan(String category, String model, double target, double fact, String unit, int id) {
        this.category = category;
        this.model = model;
        this.target = target;
        this.fact = fact;
        this.unit = unit;
        this.id = id;
    }

    public String getCategory() { return category; }
    public String getModel() { return model; }
    public double getTarget() { return target; }
    public double getFact() { return fact; }
    public String getUnit() { return unit; }
    public int getId() { return id; }

    public void setTarget(double target) { this.target = target; }
    public void setFact(double fact) { this.fact = fact; }

    public int getPercent() {
        return target > 0 ? (int) Math.round(fact / target * 100) : 0;
    }
}