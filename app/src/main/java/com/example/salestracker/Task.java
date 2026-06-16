package com.example.salestracker;

public class Task {
    private int id;
    private String title;
    private String deadline;
    private String priority;

    public Task(int id, String title, String deadline, String priority) {
        this.id = id;
        this.title = title;
        this.deadline = deadline;
        this.priority = priority;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDeadline() { return deadline; }
    public String getPriority() { return priority; }

    public void setPriority(String priority) { this.priority = priority; }
}