package com.hackathon.taskassign.controller.api;

import com.hackathon.taskassign.dto.EmployeeRequest;
import com.hackathon.taskassign.dto.EmployeeResponse;
import com.hackathon.taskassign.dto.WorkloadDto;
import com.hackathon.taskassign.service.EmployeeService;
import com.hackathon.taskassign.service.WorkloadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** REST API for employees: /api/employees */
@RestController
@RequestMapping("/api/employees")
public class EmployeeApiController {

    private final EmployeeService employeeService;
    private final WorkloadService workloadService;

    public EmployeeApiController(EmployeeService employeeService, WorkloadService workloadService) {
        this.employeeService = employeeService;
        this.workloadService = workloadService;
    }

    @GetMapping
    public List<EmployeeResponse> list(@RequestParam(required = false) String q) {
        return employeeService.list(q);
    }

    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable Long id) {
        return employeeService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody EmployeeRequest request) {
        return employeeService.create(request);
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return employeeService.update(id, request);
    }

    /** Deletes the employee; their tasks are kept and become unassigned. */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        int unassigned = employeeService.delete(id);
        return Map.of("deleted", true, "unassignedTasks", unassigned);
    }

    @GetMapping("/{id}/workload")
    public WorkloadDto workload(@PathVariable Long id) {
        employeeService.get(id); // 404 if the employee does not exist
        return workloadService.getAllWorkloads().stream()
                .filter(w -> w.getEmployeeId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
