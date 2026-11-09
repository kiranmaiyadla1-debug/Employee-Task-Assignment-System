package com.hackathon.taskassign.service;

import com.hackathon.taskassign.dto.TaskRequest;
import com.hackathon.taskassign.dto.TaskResponse;
import com.hackathon.taskassign.entity.Employee;
import com.hackathon.taskassign.entity.Priority;
import com.hackathon.taskassign.entity.Task;
import com.hackathon.taskassign.entity.TaskStatus;
import com.hackathon.taskassign.exception.ResourceNotFoundException;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests (Mockito) for the workload rule inside TaskService - no database involved. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    private TaskService taskService;
    private Employee asha;

    @BeforeEach
    void setUp() {
        WorkloadService workloadService = new WorkloadService(taskRepository, employeeRepository, 5);
        taskService = new TaskService(taskRepository, employeeRepository, workloadService);

        asha = employee(1L, "Asha");
        when(employeeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(asha));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- Scenario 3/4: assigning tasks, up to 5 active -------------------------------------

    @Test
    void createTask_assignedToEmployeeWithFourActiveTasks_isAccepted() {
        activeTasksOfEmployeeIs(4);

        TaskResponse response = taskService.create(request(TaskStatus.PENDING, 1L));

        assertEquals(1L, response.getAssigneeId());
        assertEquals("Asha", response.getAssigneeName());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_unassigned_neverChecksWorkload() {
        TaskResponse response = taskService.create(request(TaskStatus.PENDING, null));

        assertEquals(null, response.getAssigneeId());
        verify(taskRepository, never()).countByAssigneeIdAndStatusNot(anyLong(), any());
    }

    // ---- Scenario 5: the 6th active task is rejected --------------------------------------

    @Test
    void createTask_whenEmployeeAlreadyHasFiveActiveTasks_isRejected() {
        activeTasksOfEmployeeIs(5);

        WorkloadLimitExceededException ex = assertThrows(WorkloadLimitExceededException.class,
                () -> taskService.create(request(TaskStatus.IN_PROGRESS, 1L)));

        assertTrue(ex.getMessage().contains("Asha"));
        assertTrue(ex.getMessage().contains("5"));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void assign_unassignedActiveTaskToEmployeeAtLimit_isRejected() {
        Task task = task(10L, TaskStatus.PENDING, null);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        activeTasksExcludingTaskIs(5);

        assertThrows(WorkloadLimitExceededException.class, () -> taskService.assign(10L, 1L));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void assign_unassignedActiveTaskToEmployeeBelowLimit_isAccepted() {
        Task task = task(10L, TaskStatus.PENDING, null);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        activeTasksExcludingTaskIs(4);

        TaskResponse response = taskService.assign(10L, 1L);

        assertEquals(1L, response.getAssigneeId());
    }

    @Test
    void assign_completedTaskToEmployeeAtLimit_isAllowedBecauseCompletedDoesNotCount() {
        Task task = task(10L, TaskStatus.COMPLETED, null);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        activeTasksExcludingTaskIs(5);

        TaskResponse response = taskService.assign(10L, 1L);

        assertEquals(1L, response.getAssigneeId());
    }

    @Test
    void assign_missingEmployee_throwsNotFound() {
        Task task = task(10L, TaskStatus.PENDING, null);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThrows(ResourceNotFoundException.class, () -> taskService.assign(10L, 99L));
    }

    // ---- Scenario 6/7: completing a task frees a slot --------------------------------------

    @Test
    void updateStatus_toCompleted_isAlwaysAllowedEvenWhenEmployeeIsOverloaded() {
        Task task = task(10L, TaskStatus.IN_PROGRESS, asha);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        activeTasksExcludingTaskIs(7);

        TaskResponse response = taskService.updateStatus(10L, TaskStatus.COMPLETED);

        assertEquals(TaskStatus.COMPLETED, response.getStatus());
    }

    @Test
    void updateStatus_reopeningCompletedTaskWhenEmployeeIsFull_isRejected() {
        Task task = task(10L, TaskStatus.COMPLETED, asha);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        activeTasksExcludingTaskIs(5);

        assertThrows(WorkloadLimitExceededException.class,
                () -> taskService.updateStatus(10L, TaskStatus.IN_PROGRESS));
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
    }

    @Test
    void updateStatus_pendingToInProgress_doesNotChangeWorkloadSoNoCheck() {
        Task task = task(10L, TaskStatus.PENDING, asha);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.updateStatus(10L, TaskStatus.IN_PROGRESS);

        assertEquals(TaskStatus.IN_PROGRESS, response.getStatus());
        verify(taskRepository, never()).countByAssigneeIdAndStatusNotAndIdNot(anyLong(), any(), anyLong());
    }

    // ---- Scenario 8: existing overload never blocks editing --------------------------------

    @Test
    void update_taskOfOverloadedEmployeeKeepingSameAssignee_isAllowed() {
        Task task = task(10L, TaskStatus.PENDING, asha);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        activeTasksExcludingTaskIs(7);

        TaskRequest edit = request(TaskStatus.IN_PROGRESS, 1L);
        edit.setTitle("Renamed task");
        TaskResponse response = taskService.update(10L, edit);

        assertEquals("Renamed task", response.getTitle());
        assertEquals(1L, response.getAssigneeId());
    }

    @Test
    void update_movingActiveTaskToAnotherFullEmployee_isRejected() {
        Employee ravi = employee(2L, "Ravi");
        when(employeeRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(ravi));
        Task task = task(10L, TaskStatus.PENDING, asha);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.countByAssigneeIdAndStatusNotAndIdNot(eq(2L), eq(TaskStatus.COMPLETED), eq(10L)))
                .thenReturn(5L);

        assertThrows(WorkloadLimitExceededException.class,
                () -> taskService.update(10L, request(TaskStatus.PENDING, 2L)));
        assertEquals(asha, task.getAssignee());
    }

    @Test
    void update_removingAssignee_clearsAssignment() {
        Task task = task(10L, TaskStatus.PENDING, asha);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.update(10L, request(TaskStatus.PENDING, null));

        assertEquals(null, response.getAssigneeId());
    }

    // ---- Scenario 9: delete ----------------------------------------------------------------

    @Test
    void delete_existingTask_removesIt() {
        Task task = task(10L, TaskStatus.PENDING, asha);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        taskService.delete(10L);

        verify(taskRepository).delete(task);
    }

    @Test
    void delete_missingTask_throwsNotFound() {
        when(taskRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.delete(404L));
    }

    // ---- helpers ---------------------------------------------------------------------------

    private void activeTasksOfEmployeeIs(long count) {
        when(taskRepository.countByAssigneeIdAndStatusNot(1L, TaskStatus.COMPLETED)).thenReturn(count);
    }

    private void activeTasksExcludingTaskIs(long count) {
        when(taskRepository.countByAssigneeIdAndStatusNotAndIdNot(eq(1L), eq(TaskStatus.COMPLETED), anyLong()))
                .thenReturn(count);
    }

    private Employee employee(Long id, String name) {
        Employee e = new Employee(name, name.toLowerCase() + "@example.com", "Engineering", "Developer");
        e.setId(id);
        return e;
    }

    private Task task(Long id, TaskStatus status, Employee assignee) {
        Task t = new Task();
        t.setId(id);
        t.setTitle("Task " + id);
        t.setPriority(Priority.MEDIUM);
        t.setDeadline(LocalDate.now().plusDays(3));
        t.setStatus(status);
        t.setAssignee(assignee);
        return t;
    }

    private TaskRequest request(TaskStatus status, Long assigneeId) {
        TaskRequest r = new TaskRequest();
        r.setTitle("New task");
        r.setDescription("desc");
        r.setPriority(Priority.HIGH);
        r.setDeadline(LocalDate.now().plusDays(5));
        r.setStatus(status);
        r.setAssigneeId(assigneeId);
        return r;
    }
}
