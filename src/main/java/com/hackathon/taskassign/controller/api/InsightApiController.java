package com.hackathon.taskassign.controller.api;

import com.hackathon.taskassign.dto.DashboardDto;
import com.hackathon.taskassign.dto.WorkloadDto;
import com.hackathon.taskassign.service.DashboardService;
import com.hackathon.taskassign.service.WorkloadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only REST endpoints: workload and dashboard statistics. */
@RestController
@RequestMapping("/api")
public class InsightApiController {

    private final WorkloadService workloadService;
    private final DashboardService dashboardService;

    public InsightApiController(WorkloadService workloadService, DashboardService dashboardService) {
        this.workloadService = workloadService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/workload")
    public List<WorkloadDto> workload() {
        return workloadService.getAllWorkloads();
    }

    @GetMapping("/workload/overloaded")
    public List<WorkloadDto> overloaded() {
        return workloadService.getOverloadedEmployees();
    }

    @GetMapping("/dashboard")
    public DashboardDto dashboard() {
        return dashboardService.getDashboard();
    }
}
