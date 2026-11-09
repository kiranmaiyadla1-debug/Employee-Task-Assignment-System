package com.hackathon.taskassign.controller.web;

import com.hackathon.taskassign.dto.TaskResponse;
import com.hackathon.taskassign.exception.BusinessException;
import com.hackathon.taskassign.service.EmployeeService;
import com.hackathon.taskassign.service.TaskService;
import com.hackathon.taskassign.service.WorkloadService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Task assignment page: assign a task to an employee (workload limit is enforced in the service). */
@Controller
@RequestMapping("/assignments")
public class AssignmentController {

    private final TaskService taskService;
    private final EmployeeService employeeService;
    private final WorkloadService workloadService;

    public AssignmentController(TaskService taskService, EmployeeService employeeService,
                                WorkloadService workloadService) {
        this.taskService = taskService;
        this.employeeService = employeeService;
        this.workloadService = workloadService;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("activeTasks", taskService.search(null, null, null, null, "active"));
        model.addAttribute("unassigned", taskService.findUnassignedActive());
        model.addAttribute("employees", employeeService.list(null));
        model.addAttribute("recent", taskService.recentAssignments(8));
        model.addAttribute("maxActive", workloadService.getMaxActiveTasks());
        return "assignments";
    }

    @PostMapping
    public String assign(@RequestParam Long taskId, @RequestParam Long employeeId,
                         RedirectAttributes redirect) {
        try {
            TaskResponse saved = taskService.assign(taskId, employeeId);
            long active = workloadService.countActiveTasks(employeeId);
            redirect.addFlashAttribute("success", "Task '" + saved.getTitle() + "' is assigned to "
                    + saved.getAssigneeName() + ". " + saved.getAssigneeName() + " now has " + active
                    + " of " + workloadService.getMaxActiveTasks() + " active tasks.");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/assignments";
    }

    @PostMapping("/{taskId}/unassign")
    public String unassign(@PathVariable Long taskId, RedirectAttributes redirect) {
        TaskResponse saved = taskService.unassign(taskId);
        redirect.addFlashAttribute("success", "Task '" + saved.getTitle() + "' is now unassigned.");
        return "redirect:/assignments";
    }
}
