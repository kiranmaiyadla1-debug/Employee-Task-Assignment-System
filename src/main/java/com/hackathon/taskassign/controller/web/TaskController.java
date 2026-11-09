package com.hackathon.taskassign.controller.web;

import com.hackathon.taskassign.dto.TaskRequest;
import com.hackathon.taskassign.dto.TaskResponse;
import com.hackathon.taskassign.entity.Priority;
import com.hackathon.taskassign.entity.TaskStatus;
import com.hackathon.taskassign.exception.BusinessException;
import com.hackathon.taskassign.service.EmployeeService;
import com.hackathon.taskassign.service.TaskService;
import com.hackathon.taskassign.service.WorkloadService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** Task management pages: list/filter, create, edit, delete, change status. */
@Controller
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;
    private final EmployeeService employeeService;
    private final WorkloadService workloadService;

    public TaskController(TaskService taskService, EmployeeService employeeService,
                          WorkloadService workloadService) {
        this.taskService = taskService;
        this.employeeService = employeeService;
        this.workloadService = workloadService;
    }

    /** Available in every template of this controller (drop-downs). */
    @ModelAttribute("priorities")
    public Priority[] priorities() {
        return Priority.values();
    }

    @ModelAttribute("statuses")
    public TaskStatus[] statuses() {
        return TaskStatus.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) TaskStatus status,
                       @RequestParam(required = false) Priority priority,
                       @RequestParam(required = false) Long employeeId,
                       @RequestParam(defaultValue = "all") String view,
                       Model model) {
        model.addAttribute("tasks", taskService.search(q, status, priority, employeeId, view));
        model.addAttribute("employees", employeeService.list(null));
        model.addAttribute("q", q);
        model.addAttribute("fStatus", status);
        model.addAttribute("fPriority", priority);
        model.addAttribute("fEmployeeId", employeeId);
        model.addAttribute("view", view);
        return "tasks/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long employeeId, Model model) {
        TaskRequest form = new TaskRequest();
        form.setDeadline(LocalDate.now().plusDays(7));
        form.setAssigneeId(employeeId);
        model.addAttribute("task", form);
        return formView(model, null);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        TaskResponse existing = taskService.get(id);
        TaskRequest form = new TaskRequest();
        form.setTitle(existing.getTitle());
        form.setDescription(existing.getDescription());
        form.setPriority(existing.getPriority());
        form.setDeadline(existing.getDeadline());
        form.setStatus(existing.getStatus());
        form.setAssigneeId(existing.getAssigneeId());
        model.addAttribute("task", form);
        return formView(model, id);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("task") TaskRequest task, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return formView(model, null);
        }
        try {
            TaskResponse saved = taskService.create(task);
            redirect.addFlashAttribute("success", "Task '" + saved.getTitle() + "' was created"
                    + (saved.isAssigned() ? " and assigned to " + saved.getAssigneeName() + "." : "."));
            return "redirect:/tasks";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return formView(model, null);
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("task") TaskRequest task,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return formView(model, id);
        }
        try {
            TaskResponse saved = taskService.update(id, task);
            redirect.addFlashAttribute("success", "Task '" + saved.getTitle() + "' was updated.");
            return "redirect:/tasks";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return formView(model, id);
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam TaskStatus status,
                               RedirectAttributes redirect) {
        try {
            TaskResponse saved = taskService.updateStatus(id, status);
            redirect.addFlashAttribute("success", "Task '" + saved.getTitle() + "' is now "
                    + saved.getStatus().getLabel() + ".");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tasks";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        TaskResponse existing = taskService.get(id);
        taskService.delete(id);
        redirect.addFlashAttribute("success", "Task '" + existing.getTitle() + "' was deleted.");
        return "redirect:/tasks";
    }

    private String formView(Model model, Long taskId) {
        model.addAttribute("taskId", taskId);
        model.addAttribute("employees", employeeService.list(null));
        model.addAttribute("maxActive", workloadService.getMaxActiveTasks());
        return "tasks/form";
    }
}
