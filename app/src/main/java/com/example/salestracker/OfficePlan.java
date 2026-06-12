package com.example.salestracker;

public class OfficePlan {
    private String category;
    private double target;
    private double fact;
    private String unit;

    public OfficePlan(String category, double target, double fact, String unit) {
        this.category = category;
        this.target = target;
        this.fact = fact;
        this.unit = unit;
    }

    public String getCategory() { return category; }
    public double getTarget() { return target; }
    public double getFact() { return fact; }
    public String getUnit() { return unit; }

    public int getPercent() {
        return target > 0 ? (int) Math.round(fact / target * 100) : 0;
    }
}