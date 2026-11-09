package com.hackathon.taskassign.dto;

import jakarta.validation.constraints.NotNull;

/** Body of POST /api/tasks/{id}/assign */
public class AssignRequest {

    @NotNull(message = "employeeId is required")
    private Long employeeId;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
}
