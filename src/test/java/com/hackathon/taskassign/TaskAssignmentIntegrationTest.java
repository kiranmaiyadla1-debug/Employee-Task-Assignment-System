package com.hackathon.taskassign;

import com.hackathon.taskassign.dto.DashboardDto;
import com.hackathon.taskassign.dto.EmployeeRequest;
import com.hackathon.taskassign.dto.EmployeeResponse;
import com.hackathon.taskassign.dto.TaskRequest;
import com.hackathon.taskassign.dto.TaskResponse;
import com.hackathon.taskassign.dto.WorkloadDto;
import com.hackathon.taskassign.entity.Employee;
import com.hackathon.taskassign.entity.Priority;
import com.hackathon.taskassign.entity.Task;
import com.hackathon.taskassign.entity.TaskStatus;
import com.hackathon.taskassign.exception.ResourceNotFoundException;
import com.hackathon.taskassign.exception.WorkloadLimitExceededException;
import com.hackathon.taskassign.repository.EmployeeRepository;
import com.hackathon.taskassign.repository.TaskRepository;
import com.hackathon.taskassign.service.DashboardService;
import com.hackathon.taskassign.service.EmployeeService;
import com.hackathon.taskassign.service.TaskService;
import com.hackathon.taskassign.service.WorkloadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests with the real Spring context, real services, real JPA and an in-memory H2
 * database. Each test is rolled back afterwards. Together with the Mockito unit tests they
 * cover the ten scenarios listed in the project brief.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskAssignmentIntegrationTest {

    @Autowired private EmployeeService employeeService;
    @Autowired private TaskService taskService;
    @Autowired private WorkloadService workloadService;
    @Autowired private DashboardService dashboardService;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private MockMvc mockMvc;

    // Scenarios 1-7 in one flow ------------------------------------------------------------

    @Test
    void fullWorkloadScenario() {
        // 1. add an employee
        EmployeeResponse asha = employeeService.create(employeeRequest("Asha", "asha@example.com"));
        assertEquals(0, asha.getActiveTasks());

        // 2 + 3 + 4. create tasks and assign five active tasks (mix of Pending / In Progress)
        TaskResponse first = null;
        for (int i = 1; i <= 5; i++) {
            TaskResponse t = taskService.create(taskRequest("Task " + i,
                    i % 2 == 0 ? TaskStatus.IN_PROGRESS : TaskStatus.PENDING, asha.getId()));
            if (i == 1) {
                first = t;
            }
        }
        assertEquals(5, workloadService.countActiveTasks(asha.getId()));

        // 5. the 6th active task is rejected, and nothing is saved
        long tasksBefore = taskRepository.count();
        WorkloadLimitExceededException ex = assertThrows(WorkloadLimitExceededException.class,
                () -> taskService.create(taskRequest("Task 6", TaskStatus.PENDING, asha.getId())));
        assertTrue(ex.getMessage().contains("already has 5 active tasks"));
        assertEquals(tasksBefore, taskRepository.count());

        // a Completed task does NOT count: assigning one is allowed even at the limit
        taskService.create(taskRequest("Old finished work", TaskStatus.COMPLETED, asha.getId()));
        assertEquals(5, workloadService.countActiveTasks(asha.getId()));

        // 6. complete one task -> workload is recalculated
        taskService.updateStatus(first.getId(), TaskStatus.COMPLETED);
        assertEquals(4, workloadService.countActiveTasks(asha.getId()));

        // 7. now another task can be assigned
        TaskResponse another = taskService.create(taskRequest("Task 7", TaskStatus.PENDING, asha.getId()));
        assertEquals(asha.getId(), another.getAssigneeId());
        assertEquals(5, workloadService.countActiveTasks(asha.getId()));

        // re-opening the completed task is now rejected (employee is full again)
        Long completedId = first.getId();
        assertThrows(WorkloadLimitExceededException.class,
                () -> taskService.updateStatus(completedId, TaskStatus.PENDING));
    }

    @Test
    void assigningExistingUnassignedTask_isCheckedAgainstLimit() {
        EmployeeResponse asha = employeeService.create(employeeRequest("Asha", "asha2@example.com"));
        for (int i = 1; i <= 5; i++) {
            taskService.create(taskRequest("T" + i, TaskStatus.PENDING, asha.getId()));
        }
        TaskResponse loose = taskService.create(taskRequest("Loose task", TaskStatus.PENDING, null));

        assertThrows(WorkloadLimitExceededException.class, () -> taskService.assign(loose.getId(), asha.getId()));
        assertFalse(taskService.get(loose.getId()).isAssigned());
    }

    // Scenario 8: existing overload ----------------------------------------------------------

    @Test
    void overloadedEmployeeIsDetected_andNothingIsDeleted() {
        Employee aarav = employeeRepository.save(new Employee("Aarav", "aarav@example.com", "IT", "Dev"));
        Employee priya = employeeRepository.save(new Employee("Priya", "priya@example.com", "HR", "Mgr"));
        for (int i = 0; i < 7; i++) {            // written directly = "old data" that bypasses validation
            taskRepository.save(rawTask("Old " + i, TaskStatus.PENDING, aarav));
        }
        for (int i = 0; i < 5; i++) {
            taskRepository.save(rawTask("Fine " + i, TaskStatus.PENDING, priya));
        }

        List<WorkloadDto> overloaded = workloadService.getOverloadedEmployees();

        assertEquals(1, overloaded.size());
        assertEquals("Aarav", overloaded.get(0).getName());
        assertEquals(7, overloaded.get(0).getActiveCount());
        assertEquals(12, taskRepository.count());                      // no assignments removed

        // new assignment is rejected, but existing tasks can still be completed
        TaskResponse extra = taskService.create(taskRequest("Extra", TaskStatus.PENDING, null));
        assertThrows(WorkloadLimitExceededException.class, () -> taskService.assign(extra.getId(), aarav.getId()));
        Task oneOfAarav = taskRepository.findAllWithAssignee().stream()
                .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(aarav.getId()))
                .findFirst().orElseThrow();
        taskService.updateStatus(oneOfAarav.getId(), TaskStatus.COMPLETED);
        assertEquals(6, workloadService.countActiveTasks(aarav.getId()));
    }

    // Scenario 9: edit and delete -----------------------------------------------------------

    @Test
    void editAndDeleteRecords() {
        EmployeeResponse asha = employeeService.create(employeeRequest("Asha", "asha3@example.com"));
        TaskResponse task = taskService.create(taskRequest("Draft", TaskStatus.PENDING, asha.getId()));

        EmployeeRequest edit = employeeRequest("Asha Kumar", "asha3@example.com");
        edit.setDepartment("Finance");
        assertEquals("Finance", employeeService.update(asha.getId(), edit).getDepartment());

        TaskRequest taskEdit = taskRequest("Final", TaskStatus.IN_PROGRESS, asha.getId());
        assertEquals("Final", taskService.update(task.getId(), taskEdit).getTitle());

        taskService.delete(task.getId());
        assertThrows(ResourceNotFoundException.class, () -> taskService.get(task.getId()));

        TaskResponse keep = taskService.create(taskRequest("Keep me", TaskStatus.PENDING, asha.getId()));
        assertEquals(1, employeeService.delete(asha.getId()));          // task is unassigned, not deleted
        assertFalse(taskService.get(keep.getId()).isAssigned());
    }

    // Scenario 10: dashboard statistics ------------------------------------------------------

    @Test
    void dashboardStatisticsAreCorrect() {
        EmployeeResponse asha = employeeService.create(employeeRequest("Asha", "asha4@example.com"));
        employeeService.create(employeeRequest("Ravi", "ravi@example.com"));
        taskService.create(taskRequest("A", TaskStatus.PENDING, asha.getId()));
        taskService.create(taskRequest("B", TaskStatus.IN_PROGRESS, asha.getId()));
        taskService.create(taskRequest("C", TaskStatus.COMPLETED, asha.getId()));
        taskService.create(taskRequest("D", TaskStatus.PENDING, null));

        DashboardDto d = dashboardService.getDashboard();

        assertEquals(2, d.getTotalEmployees());
        assertEquals(4, d.getTotalTasks());
        assertEquals(3, d.getActiveTasks());
        assertEquals(1, d.getCompletedTasks());
        assertEquals(2, d.getPendingTasks());
        assertEquals(1, d.getInProgressTasks());
        assertEquals(0, d.getOverloadedEmployees());
        assertEquals(3, d.getRecentAssignments().size());               // only assigned tasks
    }

    // Web layer: every page renders, and the rejection message reaches the screen --------------

    @Test
    void allPagesRender() throws Exception {
        employeeService.create(employeeRequest("Asha", "asha5@example.com"));
        for (String url : List.of("/", "/employees", "/employees/new", "/tasks", "/tasks/new",
                "/assignments", "/workload", "/workload?filter=overloaded", "/tasks?view=active")) {
            mockMvc.perform(get(url)).andExpect(status().isOk());
        }
        mockMvc.perform(get("/")).andExpect(content().string(org.hamcrest.Matchers.containsString("Dashboard")));
    }

    @Test
    void unknownEmployeeShowsFriendlyNotFoundPage() throws Exception {
        mockMvc.perform(get("/employees/99999/edit")).andExpect(status().isNotFound());
    }

    @Test
    void sixthAssignmentThroughTheWebFormShowsErrorMessage() throws Exception {
        EmployeeResponse asha = employeeService.create(employeeRequest("Asha", "asha6@example.com"));
        for (int i = 1; i <= 5; i++) {
            taskService.create(taskRequest("T" + i, TaskStatus.PENDING, asha.getId()));
        }
        TaskResponse loose = taskService.create(taskRequest("Loose", TaskStatus.PENDING, null));

        mockMvc.perform(post("/assignments")
                        .param("taskId", String.valueOf(loose.getId()))
                        .param("employeeId", String.valueOf(asha.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void restApiReturnsConflictWhenLimitIsExceeded() throws Exception {
        EmployeeResponse asha = employeeService.create(employeeRequest("Asha", "asha7@example.com"));
        for (int i = 1; i <= 5; i++) {
            taskService.create(taskRequest("T" + i, TaskStatus.PENDING, asha.getId()));
        }
        TaskResponse loose = taskService.create(taskRequest("Loose", TaskStatus.PENDING, null));

        mockMvc.perform(post("/api/tasks/" + loose.getId() + "/assign")
                        .contentType("application/json")
                        .content("{\"employeeId\":" + asha.getId() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already has 5 active tasks")));
    }

    // helpers ---------------------------------------------------------------------------------

    private EmployeeRequest employeeRequest(String name, String email) {
        EmployeeRequest r = new EmployeeRequest();
        r.setName(name);
        r.setEmail(email);
        r.setDepartment("Engineering");
        r.setDesignation("Developer");
        return r;
    }

    private TaskRequest taskRequest(String title, TaskStatus status, Long assigneeId) {
        TaskRequest r = new TaskRequest();
        r.setTitle(title);
        r.setDescription("Test task");
        r.setPriority(Priority.MEDIUM);
        r.setDeadline(LocalDate.now().plusDays(7));
        r.setStatus(status);
        r.setAssigneeId(assigneeId);
        return r;
    }

    private Task rawTask(String title, TaskStatus status, Employee employee) {
        Task t = new Task();
        t.setTitle(title);
        t.setPriority(Priority.LOW);
        t.setDeadline(LocalDate.now().plusDays(3));
        t.setStatus(status);
        t.setAssignee(employee);
        t.setAssignedAt(java.time.LocalDateTime.now());
        return t;
    }
}
