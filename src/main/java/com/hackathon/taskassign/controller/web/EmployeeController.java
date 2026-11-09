package com.hackathon.taskassign.controller.web;

import com.hackathon.taskassign.dto.EmployeeRequest;
import com.hackathon.taskassign.dto.EmployeeResponse;
import com.hackathon.taskassign.exception.BusinessException;
import com.hackathon.taskassign.service.EmployeeService;
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

/** Employee management pages: list, add, edit, delete. */
@Controller
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final WorkloadService workloadService;

    public EmployeeController(EmployeeService employeeService, WorkloadService workloadService) {
        this.employeeService = employeeService;
        this.workloadService = workloadService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("employees", employeeService.list(q));
        model.addAttribute("q", q);
        model.addAttribute("maxActive", workloadService.getMaxActiveTasks());
        return "employees/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("employee", new EmployeeRequest());
        model.addAttribute("employeeId", null);
        return "employees/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        EmployeeResponse existing = employeeService.get(id);
        EmployeeRequest form = new EmployeeRequest();
        form.setName(existing.getName());
        form.setEmail(existing.getEmail());
        form.setDepartment(existing.getDepartment());
        form.setDesignation(existing.getDesignation());
        model.addAttribute("employee", form);
        model.addAttribute("employeeId", id);
        return "employees/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("employee") EmployeeRequest employee,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        model.addAttribute("employeeId", null);
        if (result.hasErrors()) {
            return "employees/form";
        }
        try {
            EmployeeResponse saved = employeeService.create(employee);
            redirect.addFlashAttribute("success", "Employee '" + saved.getName() + "' was added.");
            return "redirect:/employees";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "employees/form";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("employee") EmployeeRequest employee,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        model.addAttribute("employeeId", id);
        if (result.hasErrors()) {
            return "employees/form";
        }
        try {
            EmployeeResponse saved = employeeService.update(id, employee);
            redirect.addFlashAttribute("success", "Employee '" + saved.getName() + "' was updated.");
            return "redirect:/employees";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "employees/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        EmployeeResponse existing = employeeService.get(id);
        int unassigned = employeeService.delete(id);
        String message = "Employee '" + existing.getName() + "' was deleted.";
        if (unassigned > 0) {
            message += " " + unassigned + " task(s) were kept and are now unassigned.";
        }
        redirect.addFlashAttribute("success", message);
        return "redirect:/employees";
    }
}
