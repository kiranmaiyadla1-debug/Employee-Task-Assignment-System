package com.hackathon.taskassign.service;

import com.hackathon.taskassign.dto.WorkloadDto;
import com.hackathon.taskassign.entity.Employee;
import com.hackathon.taskassign.entity.TaskStatus;
import com.hackathon.taskassign.exception.WorkloadLimitExceededException;
import com.hackathon.taskassign.repository.EmployeeRepository;
import com.hackathon.taskassign.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/** Unit tests for the five-task rule and for overload detection. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkloadServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    private WorkloadService workloadService;

    @BeforeEach
    void setUp() {
        workloadService = new WorkloadService(taskRepository, employeeRepository, 5);
    }

    @Test
    void assertCanTakeNewActiveTask_withFourActive_passes() {
        when(taskRepository.countByAssigneeIdAndStatusNot(1L, TaskStatus.COMPLETED)).thenReturn(4L);

        assertDoesNotThrow(() -> workloadService.assertCanTakeNewActiveTask(employee(1L, "Asha"), null));
    }

    @Test
    void assertCanTakeNewActiveTask_withFiveActive_throwsWithClearMessage() {
        when(taskRepository.countByAssigneeIdAndStatusNot(1L, TaskStatus.COMPLETED)).thenReturn(5L);

        WorkloadLimitExceededException ex = assertThrows(WorkloadLimitExceededException.class,
                () -> workloadService.assertCanTakeNewActiveTask(employee(1L, "Asha"), null));

        assertEquals(5, ex.getActiveTasks());
        assertEquals(5, ex.getLimit());
        assertTrue(ex.getMessage().contains("Asha already has 5 active tasks"));
    }

    @Test
    void getAllWorkloads_completedTasksDoNotCountAsActive() {
        Employee asha = employee(1L, "Asha");
        when(employeeRepository.findAllByOrderByNameAsc()).thenReturn(List.of(asha));
        when(taskRepository.countGroupedByAssigneeAndStatus()).thenReturn(List.of(
                new Object[]{1L, TaskStatus.PENDING, 2L},
                new Object[]{1L, TaskStatus.IN_PROGRESS, 3L},
                new Object[]{1L, TaskStatus.COMPLETED, 9L}));

        WorkloadDto w = workloadService.getAllWorkloads().get(0);

        assertEquals(5, w.getActiveCount());
        assertEquals(9, w.getCompletedCount());
        assertTrue(w.isAtCapacity());
        assertFalse(w.isOverloaded());
        assertEquals(0, w.getRemainingSlots());
    }

    @Test
    void getOverloadedEmployees_returnsOnlyEmployeesWithMoreThanFiveActiveTasks() {
        Employee overloaded = employee(1L, "Aarav");
        Employee full = employee(2L, "Priya");
        Employee light = employee(3L, "Rohan");
        when(employeeRepository.findAllByOrderByNameAsc()).thenReturn(List.of(overloaded, full, light));
        when(taskRepository.countGroupedByAssigneeAndStatus()).thenReturn(List.of(
                new Object[]{1L, TaskStatus.PENDING, 4L},
                new Object[]{1L, TaskStatus.IN_PROGRESS, 3L},      // 7 active
                new Object[]{2L, TaskStatus.PENDING, 5L},          // exactly 5 -> not overloaded
                new Object[]{3L, TaskStatus.IN_PROGRESS, 1L}));

        List<WorkloadDto> result = workloadService.getOverloadedEmployees();

        assertEquals(1, result.size());
        assertEquals("Aarav", result.get(0).getName());
        assertEquals(7, result.get(0).getActiveCount());
        assertEquals(2, result.get(0).getExcess());
        assertEquals(100, result.get(0).getPercent());
        assertEquals(1, workloadService.countOverloadedEmployees());
    }

    @Test
    void getAllWorkloads_employeeWithoutTasks_hasZeroEverything() {
        when(employeeRepository.findAllByOrderByNameAsc()).thenReturn(List.of(employee(1L, "New Hire")));
        when(taskRepository.countGroupedByAssigneeAndStatus()).thenReturn(List.of());

        WorkloadDto w = workloadService.getAllWorkloads().get(0);

        assertEquals(0, w.getActiveCount());
        assertEquals(5, w.getRemainingSlots());
        assertEquals(0, w.getPercent());
    }

    private Employee employee(Long id, String name) {
        Employee e = new Employee(name, name.toLowerCase().replace(' ', '.') + "@example.com", "IT", "Dev");
        e.setId(id);
        return e;
    }
}
