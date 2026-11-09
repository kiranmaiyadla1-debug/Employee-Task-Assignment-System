package com.hackathon.taskassign.service;

import com.hackathon.taskassign.dto.TaskRequest;
import com.hackathon.taskassign.dto.TaskResponse;
import com.hackathon.taskassign.entity.Employee;
import com.hackathon.taskassign.entity.Priority;
import com.hackathon.taskassign.entity.Task;
import com.hackathon.taskassign.entity.TaskStatus;
import com.hackathon.taskassign.exception.ResourceNotFoundException;
import com.hackathon.taskassign.repository.EmployeeRepository;
import com.hackathon.taskassign.repository.TaskRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Task CRUD, assignment and progress updates.
 *
 * WORKLOAD RULE (see {@link WorkloadService}): before a task that is ACTIVE is given to an employee,
 * we check that the employee has fewer than 5 other active tasks. The check runs only when the
 * employee's active count would actually go UP:
 *   - a new active task is created for the employee,
 *   - an active task is moved to a different employee,
 *   - a Completed task assigned to the employee is re-opened (Pending / In Progress).
 * Editing a task that the employee already holds is always allowed, so an employee who is
 * already overloaded from old data is never blocked from finishing or editing their tasks.
 */
@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkloadService workloadService;

    public TaskService(TaskRepository taskRepository,
                       EmployeeRepository employeeRepository,
                       WorkloadService workloadService) {
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
        this.workloadService = workloadService;
    }

    // ------------------------------------------------------------------ queries

    /**
     * Search / filter tasks. Every argument is optional.
     *
     * @param view "active" (Pending + In Progress), "completed", or anything else for all tasks
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> search(String keyword, TaskStatus status, Priority priority,
                                     Long employeeId, String view) {
        String needle = keyword == null ? "" : keyword.trim().toLowerCase();
        return taskRepository.findAllWithAssignee().stream()
                .filter(t -> needle.isEmpty()
                        || t.getTitle().toLowerCase().contains(needle)
                        || (t.getDescription() != null && t.getDescription().toLowerCase().contains(needle))
                        || (t.getAssignee() != null && t.getAssignee().getName().toLowerCase().contains(needle)))
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> employeeId == null
                        || (t.getAssignee() != null && employeeId.equals(t.getAssignee().getId())))
                .filter(t -> "active".equalsIgnoreCase(view) ? t.isActive()
                        : "completed".equalsIgnoreCase(view) ? !t.isActive() : true)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return toResponse(findTask(id));
    }

    /** Active tasks that nobody owns yet. */
    @Transactional(readOnly = true)
    public List<TaskResponse> findUnassignedActive() {
        return taskRepository.findAllWithAssignee().stream()
                .filter(t -> t.getAssignee() == null && t.isActive())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> recentAssignments(int limit) {
        return taskRepository.findByAssigneeIsNotNullOrderByAssignedAtDescIdDesc(PageRequest.of(0, limit))
                .stream().map(this::toResponse).toList();
    }

    // ------------------------------------------------------------------ commands

    public TaskResponse create(TaskRequest request) {
        Task task = new Task();
        copyFields(task, request);

        if (request.getAssigneeId() != null) {
            Employee employee = lockEmployee(request.getAssigneeId());
            if (task.isActive()) {
                workloadService.assertCanTakeNewActiveTask(employee, null);
            }
            task.setAssignee(employee);
            task.setAssignedAt(LocalDateTime.now());
        }
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse update(Long id, TaskRequest request) {
        Task task = findTask(id);
        Long oldAssigneeId = assigneeIdOf(task);
        boolean wasActive = task.isActive();
        Long newAssigneeId = request.getAssigneeId();

        Employee newAssignee = null;
        if (newAssigneeId != null) {
            newAssignee = lockEmployee(newAssigneeId);
            boolean willBeActive = request.getStatus().isActive();
            boolean countGoesUp = willBeActive && (!wasActive || !newAssigneeId.equals(oldAssigneeId));
            if (countGoesUp) {
                workloadService.assertCanTakeNewActiveTask(newAssignee, task.getId());
            }
        }

        copyFields(task, request);
        if (!Objects.equals(oldAssigneeId, newAssigneeId)) {
            task.setAssignee(newAssignee);
            task.setAssignedAt(newAssignee == null ? null : LocalDateTime.now());
        }
        return toResponse(taskRepository.save(task));
    }

    public void delete(Long id) {
        taskRepository.delete(findTask(id));
    }

    /** Assign (or re-assign) a task to an employee. Enforces the 5-active-task limit. */
    public TaskResponse assign(Long taskId, Long employeeId) {
        Task task = findTask(taskId);
        Employee employee = lockEmployee(employeeId);

        if (!employee.getId().equals(assigneeIdOf(task))) {
            if (task.isActive()) {
                workloadService.assertCanTakeNewActiveTask(employee, task.getId());
            }
            task.setAssignee(employee);
            task.setAssignedAt(LocalDateTime.now());
        }
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse unassign(Long taskId) {
        Task task = findTask(taskId);
        task.setAssignee(null);
        task.setAssignedAt(null);
        return toResponse(taskRepository.save(task));
    }

    /** Change progress. Re-opening a Completed task counts as a new active task for its owner. */
    public TaskResponse updateStatus(Long taskId, TaskStatus newStatus) {
        Task task = findTask(taskId);
        boolean wasActive = task.isActive();

        if (task.getAssignee() != null && !wasActive && newStatus.isActive()) {
            Employee employee = lockEmployee(task.getAssignee().getId());
            workloadService.assertCanTakeNewActiveTask(employee, task.getId());
        }
        task.setStatus(newStatus);
        return toResponse(taskRepository.save(task));
    }

    // ------------------------------------------------------------------ helpers

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
    }

    private Employee lockEmployee(Long id) {
        return employeeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    private Long assigneeIdOf(Task task) {
        return task.getAssignee() == null ? null : task.getAssignee().getId();
    }

    private void copyFields(Task task, TaskRequest request) {
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        task.setPriority(request.getPriority());
        task.setDeadline(request.getDeadline());
        task.setStatus(request.getStatus());
    }

    private TaskResponse toResponse(Task t) {
        Employee a = t.getAssignee();
        boolean overdue = t.isActive() && t.getDeadline() != null && t.getDeadline().isBefore(LocalDate.now());
        return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getPriority(), t.getDeadline(),
                t.getStatus(), a == null ? null : a.getId(), a == null ? null : a.getName(),
                t.getAssignedAt(), overdue);
    }
}
