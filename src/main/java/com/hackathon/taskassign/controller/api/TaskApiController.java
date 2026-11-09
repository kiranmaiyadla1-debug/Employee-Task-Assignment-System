package com.hackathon.taskassign.controller.api;

import com.hackathon.taskassign.dto.AssignRequest;
import com.hackathon.taskassign.dto.StatusUpdateRequest;
import com.hackathon.taskassign.dto.TaskRequest;
import com.hackathon.taskassign.dto.TaskResponse;
import com.hackathon.taskassign.entity.Priority;
import com.hackathon.taskassign.entity.TaskStatus;
import com.hackathon.taskassign.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** REST API for tasks: /api/tasks */
@RestController
@RequestMapping("/api/tasks")
public class TaskApiController {

    private final TaskService taskService;

    public TaskApiController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> list(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) TaskStatus status,
                                   @RequestParam(required = false) Priority priority,
                                   @RequestParam(required = false) Long employeeId,
                                   @RequestParam(defaultValue = "all") String view) {
        return taskService.search(q, status, priority, employeeId, view);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) {
        return taskService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskRequest request) {
        return taskService.create(request);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }

    /** Assign a task to an employee. Returns 409 Conflict when the employee already has 5 active tasks. */
    @PostMapping("/{id}/assign")
    public TaskResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        return taskService.assign(id, request.getEmployeeId());
    }

    @DeleteMapping("/{id}/assignment")
    public TaskResponse unassign(@PathVariable Long id) {
        return taskService.unassign(id);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return taskService.updateStatus(id, request.getStatus());
    }
}
