package com.hackathon.taskassign.config;

import com.hackathon.taskassign.entity.Employee;
import com.hackathon.taskassign.entity.Priority;
import com.hackathon.taskassign.entity.Task;
import com.hackathon.taskassign.entity.TaskStatus;
import com.hackathon.taskassign.repository.EmployeeRepository;
import com.hackathon.taskassign.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.hackathon.taskassign.entity.Priority.HIGH;
import static com.hackathon.taskassign.entity.Priority.LOW;
import static com.hackathon.taskassign.entity.Priority.MEDIUM;
import static com.hackathon.taskassign.entity.TaskStatus.COMPLETED;
import static com.hackathon.taskassign.entity.TaskStatus.IN_PROGRESS;
import static com.hackathon.taskassign.entity.TaskStatus.PENDING;

/**
 * Inserts demo data the first time the application starts on an empty database.
 * Disable with app.seed-sample-data=false.
 *
 * The data is written straight through the repositories (not through TaskService) on purpose:
 * it contains one employee (Aarav Sharma) with 7 active tasks to demonstrate "existing overload"
 * detection, which the normal assignment flow would never allow.
 * The same data is available as plain SQL in database/sample_data.sql.
 */
@Component
@ConditionalOnProperty(name = "app.seed-sample-data", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;

    public DataInitializer(EmployeeRepository employeeRepository, TaskRepository taskRepository) {
        this.employeeRepository = employeeRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (employeeRepository.count() > 0) {
            return;
        }
        Employee aarav = employeeRepository.save(new Employee("Aarav Sharma", "aarav.sharma@example.com", "Engineering", "Senior Developer"));
        Employee priya = employeeRepository.save(new Employee("Priya Reddy", "priya.reddy@example.com", "Marketing", "Marketing Manager"));
        Employee rohan = employeeRepository.save(new Employee("Rohan Mehta", "rohan.mehta@example.com", "Operations", "Operations Lead"));
        Employee sneha = employeeRepository.save(new Employee("Sneha Iyer", "sneha.iyer@example.com", "Human Resources", "HR Executive"));
        employeeRepository.save(new Employee("Karthik Rao", "karthik.rao@example.com", "Engineering", "QA Engineer"));

        List<Task> tasks = new ArrayList<>();
        int hours = 1;

        // Aarav: 7 ACTIVE tasks -> overloaded (more than 5)
        tasks.add(task("Design database migration plan", "Plan schema changes for the next release", HIGH, 3, IN_PROGRESS, aarav, hours++));
        tasks.add(task("Refactor authentication module", "Split auth into smaller services", HIGH, 5, IN_PROGRESS, aarav, hours++));
        tasks.add(task("Write API documentation", "Document all public REST endpoints", MEDIUM, 7, PENDING, aarav, hours++));
        tasks.add(task("Fix payment gateway timeout", "Customers see timeouts at checkout", HIGH, 2, IN_PROGRESS, aarav, hours++));
        tasks.add(task("Set up CI pipeline", "Build and test on every commit", MEDIUM, 10, PENDING, aarav, hours++));
        tasks.add(task("Code review for release 2.3", "Review open pull requests", LOW, 4, PENDING, aarav, hours++));
        tasks.add(task("Optimize report queries", "Slow monthly reports need indexes", MEDIUM, 8, PENDING, aarav, hours++));

        // Priya: exactly 5 ACTIVE tasks -> at capacity (next assignment is rejected)
        tasks.add(task("Prepare Q4 marketing report", "Campaign results and spend", MEDIUM, 6, IN_PROGRESS, priya, hours++));
        tasks.add(task("Update brand guidelines", "Refresh logo usage and colours", LOW, 12, PENDING, priya, hours++));
        tasks.add(task("Plan product launch event", "Venue, guests and schedule", HIGH, 9, IN_PROGRESS, priya, hours++));
        tasks.add(task("Social media campaign", "Two-week campaign calendar", MEDIUM, 5, PENDING, priya, hours++));
        tasks.add(task("Customer survey analysis", "Summarise survey answers", MEDIUM, 11, PENDING, priya, hours++));

        // Rohan: 3 active + 1 completed
        tasks.add(task("Onboard new vendors", "Collect documents and contracts", MEDIUM, 6, PENDING, rohan, hours++));
        tasks.add(task("Negotiate office lease", "Renewal terms for the main office", HIGH, 4, IN_PROGRESS, rohan, hours++));
        tasks.add(task("Audit expense reports", "Check last quarter claims", LOW, 14, PENDING, rohan, hours++));
        tasks.add(task("Renew software licences", "Renewals due this month", MEDIUM, -2, COMPLETED, rohan, hours++));

        // Sneha: 1 active + 2 completed
        tasks.add(task("Screen interview candidates", "Shortlist for the developer role", HIGH, 3, IN_PROGRESS, sneha, hours++));
        tasks.add(task("Update HR policy handbook", "Add the new leave policy", LOW, -5, COMPLETED, sneha, hours++));
        tasks.add(task("Organise team workshop", "Half-day planning workshop", MEDIUM, -1, COMPLETED, sneha, hours++));

        // Unassigned tasks
        tasks.add(task("Migrate legacy reports to new BI tool", "Move 12 old reports", MEDIUM, 15, PENDING, null, 0));
        tasks.add(task("Create onboarding checklist", "One page for new joiners", LOW, 20, PENDING, null, 0));

        taskRepository.saveAll(tasks);
        log.info("Sample data loaded: 5 employees and {} tasks (Aarav Sharma is intentionally overloaded).", tasks.size());
    }

    private Task task(String title, String description, Priority priority, int deadlineInDays,
                      TaskStatus status, Employee employee, int assignedHoursAgo) {
        Task t = new Task();
        t.setTitle(title);
        t.setDescription(description);
        t.setPriority(priority);
        t.setDeadline(LocalDate.now().plusDays(deadlineInDays));
        t.setStatus(status);
        t.setAssignee(employee);
        t.setAssignedAt(employee == null ? null : LocalDateTime.now().minusHours(assignedHoursAgo));
        return t;
    }
}
