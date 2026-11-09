package com.hackathon.taskassign.dto;

import java.util.List;

/** Everything the dashboard page needs. */
public class DashboardDto {

    private final long totalEmployees;
    private final long totalTasks;
    private final long pendingTasks;
    private final long inProgressTasks;
    private final long completedTasks;
    private final long overloadedEmployees;
    private final int maxActiveTasks;
    private final List<WorkloadDto> workloads;
    private final List<TaskResponse> recentAssignments;

    public DashboardDto(long totalEmployees, long totalTasks, long pendingTasks, long inProgressTasks,
                        long completedTasks, long overloadedEmployees, int maxActiveTasks,
                        List<WorkloadDto> workloads, List<TaskResponse> recentAssignments) {
        this.totalEmployees = totalEmployees;
        this.totalTasks = totalTasks;
        this.pendingTasks = pendingTasks;
        this.inProgressTasks = inProgressTasks;
        this.completedTasks = completedTasks;
        this.overloadedEmployees = overloadedEmployees;
        this.maxActiveTasks = maxActiveTasks;
        this.workloads = workloads;
        this.recentAssignments = recentAssignments;
    }

    public long getTotalEmployees() { return totalEmployees; }
    public long getTotalTasks() { return totalTasks; }
    public long getPendingTasks() { return pendingTasks; }
    public long getInProgressTasks() { return inProgressTasks; }
    public long getCompletedTasks() { return completedTasks; }
    public long getActiveTasks() { return pendingTasks + inProgressTasks; }
    public long getOverloadedEmployees() { return overloadedEmployees; }
    public int getMaxActiveTasks() { return maxActiveTasks; }
    public List<WorkloadDto> getWorkloads() { return workloads; }
    public List<TaskResponse> getRecentAssignments() { return recentAssignments; }

    public int getPendingPercent() { return percent(pendingTasks); }
    public int getInProgressPercent() { return percent(inProgressTasks); }
    public int getCompletedPercent() { return percent(completedTasks); }

    private int percent(long part) {
        return totalTasks == 0 ? 0 : (int) Math.round(part * 100.0 / totalTasks);
    }
}
