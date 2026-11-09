package com.hackathon.taskassign.exception;

/** Thrown when an assignment would give an employee more active tasks than allowed. */
public class WorkloadLimitExceededException extends BusinessException {

    private final long activeTasks;
    private final int limit;

    public WorkloadLimitExceededException(String employeeName, long activeTasks, int limit) {
        super("Cannot assign task: " + employeeName + " already has " + activeTasks
                + " active tasks (maximum allowed is " + limit + "). "
                + "Complete an existing task or choose another employee.");
        this.activeTasks = activeTasks;
        this.limit = limit;
    }

    public long getActiveTasks() { return activeTasks; }

    public int getLimit() { return limit; }
}
