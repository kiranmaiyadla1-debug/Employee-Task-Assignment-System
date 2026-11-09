package com.hackathon.taskassign.entity;

/** Progress of a task. PENDING and IN_PROGRESS are "active"; COMPLETED is not. */
public enum TaskStatus {
    PENDING("Pending", "bg-warning text-dark"),
    IN_PROGRESS("In Progress", "bg-primary"),
    COMPLETED("Completed", "bg-success");

    private final String label;
    private final String cssClass;

    TaskStatus(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String getLabel() { return label; }

    public String getCssClass() { return cssClass; }

    /** Active tasks count towards an employee's workload limit. */
    public boolean isActive() { return this != COMPLETED; }
}
