package com.hackathon.taskassign.service;

import com.hackathon.taskassign.dto.DashboardDto;
import com.hackathon.taskassign.dto.WorkloadDto;
import com.hackathon.taskassign.entity.TaskStatus;
import com.hackathon.taskassign.repository.EmployeeRepository;
import com.hackathon.taskassign.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/** Scenario 10: dashboard statistics are calculated correctly. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private WorkloadService workloadService;
    @Mock
    private TaskService taskService;

    @Test
    void getDashboard_combinesCountsAndOverloadedEmployees() {
        when(employeeRepository.count()).thenReturn(3L);
        when(taskRepository.count()).thenReturn(20L);
        when(taskRepository.countByStatus(TaskStatus.PENDING)).thenReturn(8L);
        when(taskRepository.countByStatus(TaskStatus.IN_PROGRESS)).thenReturn(7L);
        when(taskRepository.countByStatus(TaskStatus.COMPLETED)).thenReturn(5L);
        when(workloadService.getMaxActiveTasks()).thenReturn(5);
        when(workloadService.getAllWorkloads()).thenReturn(List.of(
                new WorkloadDto(1L, "Aarav", "a@x.com", "IT", "Dev", 4, 3, 0, 5),   // 7 -> overloaded
                new WorkloadDto(2L, "Priya", "p@x.com", "HR", "Mgr", 5, 0, 1, 5),   // 5 -> full, not overloaded
                new WorkloadDto(3L, "Rohan", "r@x.com", "Ops", "Lead", 0, 1, 4, 5)));
        when(taskService.recentAssignments(8)).thenReturn(List.of());

        DashboardService service = new DashboardService(employeeRepository, taskRepository, workloadService, taskService);
        DashboardDto dashboard = service.getDashboard();

        assertEquals(3, dashboard.getTotalEmployees());
        assertEquals(20, dashboard.getTotalTasks());
        assertEquals(15, dashboard.getActiveTasks());
        assertEquals(5, dashboard.getCompletedTasks());
        assertEquals(1, dashboard.getOverloadedEmployees());
        assertEquals(40, dashboard.getPendingPercent());
        assertEquals(35, dashboard.getInProgressPercent());
        assertEquals(25, dashboard.getCompletedPercent());
        assertEquals(3, dashboard.getWorkloads().size());
    }
}
