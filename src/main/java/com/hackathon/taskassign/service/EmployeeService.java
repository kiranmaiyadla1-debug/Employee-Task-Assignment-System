package com.hackathon.taskassign.service;

import com.hackathon.taskassign.dto.EmployeeRequest;
import com.hackathon.taskassign.dto.EmployeeResponse;
import com.hackathon.taskassign.dto.WorkloadDto;
import com.hackathon.taskassign.entity.Employee;
import com.hackathon.taskassign.exception.DuplicateEmailException;
import com.hackathon.taskassign.exception.ResourceNotFoundException;
import com.hackathon.taskassign.repository.EmployeeRepository;
import com.hackathon.taskassign.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Employee CRUD plus live workload numbers. */
@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final WorkloadService workloadService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           TaskRepository taskRepository,
                           WorkloadService workloadService) {
        this.employeeRepository = employeeRepository;
        this.taskRepository = taskRepository;
        this.workloadService = workloadService;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> list(String search) {
        Map<Long, WorkloadDto> workloads = workloadService.getAllWorkloads().stream()
                .collect(Collectors.toMap(WorkloadDto::getEmployeeId, Function.identity()));
        String needle = search == null ? "" : search.trim().toLowerCase();
        return employeeRepository.findAllByOrderByNameAsc().stream()
                .filter(e -> needle.isEmpty()
                        || e.getName().toLowerCase().contains(needle)
                        || e.getEmail().toLowerCase().contains(needle)
                        || e.getDepartment().toLowerCase().contains(needle)
                        || e.getDesignation().toLowerCase().contains(needle))
                .map(e -> toResponse(e, workloads.get(e.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(Long id) {
        Employee employee = findEmployee(id);
        return toResponse(employee, workloadService.getWorkload(employee));
    }

    public EmployeeResponse create(EmployeeRequest request) {
        String email = request.getEmail().trim();
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }
        Employee employee = new Employee();
        apply(employee, request);
        Employee saved = employeeRepository.save(employee);
        return toResponse(saved, workloadService.getWorkload(saved));
    }

    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findEmployee(id);
        String email = request.getEmail().trim();
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateEmailException(email);
        }
        apply(employee, request);
        Employee saved = employeeRepository.save(employee);
        return toResponse(saved, workloadService.getWorkload(saved));
    }

    /**
     * Deletes the employee. Their tasks are NOT deleted: they simply become unassigned.
     *
     * @return how many tasks were unassigned
     */
    public int delete(Long id) {
        findEmployee(id);
        int unassigned = taskRepository.unassignAll(id);
        employeeRepository.deleteById(id);
        return unassigned;
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    private void apply(Employee employee, EmployeeRequest request) {
        employee.setName(request.getName().trim());
        employee.setEmail(request.getEmail().trim());
        employee.setDepartment(request.getDepartment().trim());
        employee.setDesignation(request.getDesignation().trim());
    }

    private EmployeeResponse toResponse(Employee e, WorkloadDto w) {
        long active = w == null ? 0 : w.getActiveCount();
        long completed = w == null ? 0 : w.getCompletedCount();
        return new EmployeeResponse(e.getId(), e.getName(), e.getEmail(), e.getDepartment(),
                e.getDesignation(), active, completed, workloadService.getMaxActiveTasks());
    }
}
