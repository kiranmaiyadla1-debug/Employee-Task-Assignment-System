package com.hackathon.taskassign.dto;

import com.hackathon.taskassign.entity.Priority;
import com.hackathon.taskassign.entity.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Task data returned to the UI / API (no lazy entities leak out of the service layer). */
public class TaskResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final Priority priority;
    private final LocalDate deadline;
    private final TaskStatus status;
    private final Long assigneeId;
    private final String assigneeName;
    private final LocalDateTime assignedAt;
    private final boolean overdue;

    public TaskResponse(Long id, String title, String description, Priority priority, LocalDate deadline,
                        TaskStatus status, Long assigneeId, String assigneeName, LocalDateTime assignedAt,
                        boolean overdue) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.deadline = deadline;
        this.status = status;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.assignedAt = assignedAt;
        this.overdue = overdue;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public LocalDate getDeadline() { return deadline; }
    public TaskStatus getStatus() { return status; }
    public Long getAssigneeId() { return assigneeId; }
    public String getAssigneeName() { return assigneeName; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public boolean isOverdue() { return overdue; }
    public boolean isActive() { return status != null && status.isActive(); }
    public boolean isAssigned() { return assigneeId != null; }
}
