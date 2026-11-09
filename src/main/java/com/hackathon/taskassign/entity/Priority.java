package com.hackathon.taskassign.entity;

/** Task priority. */
public enum Priority {
    LOW("Low", "bg-secondary"),
    MEDIUM("Medium", "bg-info text-dark"),
    HIGH("High", "bg-danger");

    private final String label;
    private final String cssClass;

    Priority(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String getLabel() { return label; }

    public String getCssClass() { return cssClass; }
}
