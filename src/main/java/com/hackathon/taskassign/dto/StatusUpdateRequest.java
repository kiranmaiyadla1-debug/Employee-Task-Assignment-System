package com.hackathon.taskassign.dto;

import com.hackathon.taskassign.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

/** Body of PATCH /api/tasks/{id}/status */
public class StatusUpdateRequest {

    @NotNull(message = "status is required (PENDING, IN_PROGRESS or COMPLETED)")
    private TaskStatus status;

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
}
