package com.hackathon.taskassign.dto;

/** Employee data returned to the UI / API, including live workload numbers. */
public class EmployeeResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final String department;
    private final String designation;
    private final long activeTasks;
    private final long completedTasks;
    private final int limit;

    public EmployeeResponse(Long id, String name, String email, String department, String designation,
                            long activeTasks, long completedTasks, int limit) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department;
        this.designation = designation;
        this.activeTasks = activeTasks;
        this.completedTasks = completedTasks;
        this.limit = limit;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public String getDesignation() { return designation; }
    public long getActiveTasks() { return activeTasks; }
    public long getCompletedTasks() { return completedTasks; }
    public long getTotalTasks() { return activeTasks + completedTasks; }
    public int getLimit() { return limit; }

    /** More than the allowed number of active tasks (only possible for pre-existing data). */
    public boolean isOverloaded() { return activeTasks > limit; }

    /** Exactly at the limit: no new task can be assigned. */
    public boolean isAtCapacity() { return activeTasks == limit; }
}
