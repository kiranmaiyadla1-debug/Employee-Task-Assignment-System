package com.hackathon.taskassign.dto;

/** Workload summary of one employee. Always calculated fresh from the database. */
public class WorkloadDto {

    private final Long employeeId;
    private final String name;
    private final String email;
    private final String department;
    private final String designation;
    private final long pendingCount;
    private final long inProgressCount;
    private final long completedCount;
    private final int limit;

    public WorkloadDto(Long employeeId, String name, String email, String department, String designation,
                       long pendingCount, long inProgressCount, long completedCount, int limit) {
        this.employeeId = employeeId;
        this.name = name;
        this.email = email;
        this.department = department;
        this.designation = designation;
        this.pendingCount = pendingCount;
        this.inProgressCount = inProgressCount;
        this.completedCount = completedCount;
        this.limit = limit;
    }

    public Long getEmployeeId() { return employeeId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public String getDesignation() { return designation; }
    public long getPendingCount() { return pendingCount; }
    public long getInProgressCount() { return inProgressCount; }
    public long getCompletedCount() { return completedCount; }
    public int getLimit() { return limit; }

    /** Active = Pending + In Progress (Completed tasks never count). */
    public long getActiveCount() { return pendingCount + inProgressCount; }

    /** Overloaded = MORE than the limit (e.g. 6 or 7 active tasks from old data). */
    public boolean isOverloaded() { return getActiveCount() > limit; }

    /** At capacity = exactly at the limit; the next assignment will be rejected. */
    public boolean isAtCapacity() { return getActiveCount() == limit; }

    /** How many tasks above the limit (0 when not overloaded). */
    public long getExcess() { return Math.max(0, getActiveCount() - limit); }

    public long getRemainingSlots() { return Math.max(0, limit - getActiveCount()); }

    /** Progress-bar width, capped at 100. */
    public int getPercent() {
        if (limit <= 0) return 0;
        return (int) Math.min(100, Math.round(getActiveCount() * 100.0 / limit));
    }
}
