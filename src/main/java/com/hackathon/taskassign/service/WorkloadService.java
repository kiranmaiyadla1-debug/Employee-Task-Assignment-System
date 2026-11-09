package com.hackathon.taskassign.service;

import com.hackathon.taskassign.dto.WorkloadDto;
import com.hackathon.taskassign.entity.Employee;
import com.hackathon.taskassign.entity.TaskStatus;
import com.hackathon.taskassign.exception.WorkloadLimitExceededException;
import com.hackathon.taskassign.repository.EmployeeRepository;
import com.hackathon.taskassign.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core business rule of the system: an employee may have at most
 * {@code app.workload.max-active-tasks} (default 5) ACTIVE tasks.
 *
 * Active = Pending or In Progress. Completed tasks never count.
 *
 * Nothing is cached: every number is calculated from the database each time, so the
 * workload is automatically "recalculated" whenever an assignment or a status changes.
 */
@Service
@Transactional(readOnly = true)
public class WorkloadService {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final int maxActiveTasks;

    public WorkloadService(TaskRepository taskRepository,
                           EmployeeRepository employeeRepository,
                           @Value("${app.workload.max-active-tasks:5}") int maxActiveTasks) {
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
        this.maxActiveTasks = maxActiveTasks;
    }

    public int getMaxActiveTasks() {
        return maxActiveTasks;
    }

    /** Number of active (not Completed) tasks of one employee. */
    public long countActiveTasks(Long employeeId) {
        return taskRepository.countByAssigneeIdAndStatusNot(employeeId, TaskStatus.COMPLETED);
    }

    /**
     * Validation used before giving an employee one more ACTIVE task.
     *
     * @param excludeTaskId the task being assigned/edited (so it is not counted twice), or null for a new task
     * @throws WorkloadLimitExceededException when the employee already has the maximum number of active tasks
     */
    public void assertCanTakeNewActiveTask(Employee employee, Long excludeTaskId) {
        long active = (excludeTaskId == null)
                ? taskRepository.countByAssigneeIdAndStatusNot(employee.getId(), TaskStatus.COMPLETED)
                : taskRepository.countByAssigneeIdAndStatusNotAndIdNot(
                        employee.getId(), TaskStatus.COMPLETED, excludeTaskId);
        if (active >= maxActiveTasks) {
            throw new WorkloadLimitExceededException(employee.getName(), active, maxActiveTasks);
        }
    }

    /** Workload of a single employee. */
    public WorkloadDto getWorkload(Employee employee) {
        return new WorkloadDto(employee.getId(), employee.getName(), employee.getEmail(),
                employee.getDepartment(), employee.getDesignation(),
                taskRepository.countByAssigneeIdAndStatus(employee.getId(), TaskStatus.PENDING),
                taskRepository.countByAssigneeIdAndStatus(employee.getId(), TaskStatus.IN_PROGRESS),
                taskRepository.countByAssigneeIdAndStatus(employee.getId(), TaskStatus.COMPLETED),
                maxActiveTasks);
    }

    /** Workload of every employee (one grouped query for all counts). */
    public List<WorkloadDto> getAllWorkloads() {
        // counts[0]=pending, counts[1]=in progress, counts[2]=completed
        Map<Long, long[]> counts = new HashMap<>();
        for (Object[] row : taskRepository.countGroupedByAssigneeAndStatus()) {
            Long employeeId = (Long) row[0];
            TaskStatus status = (TaskStatus) row[1];
            long count = ((Number) row[2]).longValue();
            long[] c = counts.computeIfAbsent(employeeId, k -> new long[3]);
            switch (status) {
                case PENDING -> c[0] = count;
                case IN_PROGRESS -> c[1] = count;
                case COMPLETED -> c[2] = count;
            }
        }
        return employeeRepository.findAllByOrderByNameAsc().stream()
                .map(e -> {
                    long[] c = counts.getOrDefault(e.getId(), new long[3]);
                    return new WorkloadDto(e.getId(), e.getName(), e.getEmail(), e.getDepartment(),
                            e.getDesignation(), c[0], c[1], c[2], maxActiveTasks);
                })
                .toList();
    }

    /** Employees holding MORE than the limit (existing overload). Nothing is ever deleted automatically. */
    public List<WorkloadDto> getOverloadedEmployees() {
        return getAllWorkloads().stream().filter(WorkloadDto::isOverloaded).toList();
    }

    public long countOverloadedEmployees() {
        return getAllWorkloads().stream().filter(WorkloadDto::isOverloaded).count();
    }
}
