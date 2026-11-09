package com.hackathon.taskassign.controller.web;

import com.hackathon.taskassign.dto.WorkloadDto;
import com.hackathon.taskassign.service.WorkloadService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** Workload monitoring page. Overloaded employees (more than 5 active tasks) are shown in red. */
@Controller
@RequestMapping("/workload")
public class WorkloadController {

    private final WorkloadService workloadService;

    public WorkloadController(WorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    @GetMapping
    public String page(@RequestParam(defaultValue = "all") String filter, Model model) {
        List<WorkloadDto> all = workloadService.getAllWorkloads();
        List<WorkloadDto> shown = switch (filter) {
            case "overloaded" -> all.stream().filter(WorkloadDto::isOverloaded).toList();
            case "full" -> all.stream().filter(WorkloadDto::isAtCapacity).toList();
            case "available" -> all.stream().filter(w -> w.getRemainingSlots() > 0).toList();
            default -> all;
        };
        model.addAttribute("workloads", shown);
        model.addAttribute("filter", filter);
        model.addAttribute("overloadedCount", all.stream().filter(WorkloadDto::isOverloaded).count());
        model.addAttribute("maxActive", workloadService.getMaxActiveTasks());
        return "workload";
    }
}
