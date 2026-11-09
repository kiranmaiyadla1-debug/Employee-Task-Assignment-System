package com.hackathon.taskassign.service;

import com.hackathon.taskassign.dto.EmployeeRequest;
import com.hackathon.taskassign.dto.EmployeeResponse;
import com.hackathon.taskassign.dto.WorkloadDto;
import com.hackathon.taskassign.entity.Employee;
import com.hackathon.taskassign.exception.DuplicateEmailException;
import com.hackathon.taskassign.exception.ResourceNotFoundException;
import com.hackathon.taskassign.repository.EmployeeRepository;
import com.hackathon.taskassign.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for employee add / edit / delete. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private WorkloadService workloadService;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeeRepository, taskRepository, workloadService);
        when(workloadService.getMaxActiveTasks()).thenReturn(5);
        when(workloadService.getWorkload(any(Employee.class))).thenReturn(
                new WorkloadDto(1L, "Asha", "asha@example.com", "IT", "Dev", 1, 1, 0, 5));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(1L);
            }
            return e;
        });
    }

    // ---- Scenario 1: add an employee ----

    @Test
    void create_validEmployee_isSavedAndTrimmed() {
        when(employeeRepository.existsByEmailIgnoreCase("asha@example.com")).thenReturn(false);

        EmployeeResponse response = employeeService.create(request("  Asha  ", " asha@example.com ", "IT", "Developer"));

        assertEquals("Asha", response.getName());
        assertEquals("asha@example.com", response.getEmail());
        assertEquals(2, response.getActiveTasks());
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void create_duplicateEmail_isRejected() {
        when(employeeRepository.existsByEmailIgnoreCase("asha@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class,
                () -> employeeService.create(request("Asha", "asha@example.com", "IT", "Developer")));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    // ---- Scenario 9: edit and delete ----

    @Test
    void update_changesDetails() {
        Employee existing = new Employee("Asha", "asha@example.com", "IT", "Developer");
        existing.setId(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.existsByEmailIgnoreCaseAndIdNot("asha@example.com", 1L)).thenReturn(false);

        EmployeeResponse response = employeeService.update(1L, request("Asha K", "asha@example.com", "Finance", "Lead"));

        assertEquals("Asha K", response.getName());
        assertEquals("Finance", response.getDepartment());
        assertEquals("Lead", response.getDesignation());
    }

    @Test
    void update_emailUsedByAnotherEmployee_isRejected() {
        Employee existing = new Employee("Asha", "asha@example.com", "IT", "Developer");
        existing.setId(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.existsByEmailIgnoreCaseAndIdNot("taken@example.com", 1L)).thenReturn(true);

        assertThrows(DuplicateEmailException.class,
                () -> employeeService.update(1L, request("Asha", "taken@example.com", "IT", "Developer")));
    }

    @Test
    void delete_keepsTasksButUnassignsThem() {
        Employee existing = new Employee("Asha", "asha@example.com", "IT", "Developer");
        existing.setId(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.unassignAll(1L)).thenReturn(3);

        int unassigned = employeeService.delete(1L);

        assertEquals(3, unassigned);
        verify(taskRepository).unassignAll(1L);
        verify(employeeRepository).deleteById(1L);
    }

    @Test
    void delete_missingEmployee_throwsNotFound() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.delete(99L));
        verify(employeeRepository, never()).deleteById(99L);
    }

    private EmployeeRequest request(String name, String email, String department, String designation) {
        EmployeeRequest r = new EmployeeRequest();
        r.setName(name);
        r.setEmail(email);
        r.setDepartment(department);
        r.setDesignation(designation);
        return r;
    }
}
