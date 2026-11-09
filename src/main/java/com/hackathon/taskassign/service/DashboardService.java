package com.hackathon.taskassign.service;

import com.hackathon.taskassign.dto.DashboardDto;
import com.hackathon.taskassign.dto.WorkloadDto;
import com.hackathon.taskassign.entity.TaskStatus;
import com.hackathon.taskassign.repository.EmployeeRepository;
import com.hackathon.taskassign.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Builds the numbers shown on the dashboard. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final WorkloadService workloadService;
    private final TaskService taskService;

    public DashboardService(EmployeeRepository employeeRepository,
                            TaskRepository taskRepository,
                            WorkloadService workloadService,
                            TaskService taskService) {
        this.employeeRepository = employeeRepository;
        this.taskRepository = taskRepository;
        this.workloadService = workloadService;
        this.taskService = taskService;
    }

    public DashboardDto getDashboard() {
        List<WorkloadDto> workloads = workloadService.getAllWorkloads();
        long overloaded = workloads.stream().filter(WorkloadDto::isOverloaded).count();

        return new DashboardDto(
                employeeRepository.count(),
                taskRepository.count(),
                taskRepository.countByStatus(TaskStatus.PENDING),
                taskRepository.countByStatus(TaskStatus.IN_PROGRESS),
                taskRepository.countByStatus(TaskStatus.COMPLETED),
                overloaded,
                workloadService.getMaxActiveTasks(),
                workloads,
                taskService.recentAssignments(8));
    }
}
