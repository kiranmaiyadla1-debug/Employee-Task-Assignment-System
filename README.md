# Employee Task Assignment System (Java / Spring Boot)

A manager assigns tasks to employees and monitors how much work each person has.
The system enforces one rule above all: **an employee can have at most 5 active tasks.**

- Language: **Java 17+** (no Python, Flask, Node.js or React)
- Frameworks: Spring Boot 3.4, Spring MVC, Spring Data JPA / Hibernate, Spring REST, Thymeleaf
- Database: MySQL
- UI: Thymeleaf + Bootstrap 5 + HTML/CSS + a few lines of JavaScript
- Build: Maven

> **Status of this build.** All source files, tests and documentation were written together, but the
> project could **not be compiled or run** in the environment that generated it (no network access to
> download Maven dependencies). Run `mvn test` first (step 7 below) - it needs no MySQL - and if anything
> fails, the message will point at the exact line. Nothing in this README claims a test result that was not observed.

---

## 1. Project introduction

TaskFlow is a small admin web application. A manager can keep a list of employees, create tasks,
give tasks to employees, update progress, and see at a glance who is busy and who is overloaded.

## 2. Problem statement

A manager needs to assign tasks to employees and monitor how much work each person has.
Without a limit, one person can silently collect too many tasks. The system must stop new
over-assignment and must also show people who are *already* overloaded - without deleting anything.

## 3. Objectives

1. Manage employees (add, view, edit, delete).
2. Manage tasks (create, view, edit, delete) and assign them to employees.
3. Track progress: Pending, In Progress, Completed; view active and completed tasks separately.
4. Reject any assignment that would give an employee a 6th active task.
5. Highlight employees who already hold more than 5 active tasks (red) and never auto-delete their assignments.
6. Provide a dashboard with totals, workload per employee, status statistics and recent assignments.
7. Offer search/filter of tasks and task counts per employee (optional features).

## 4. Technology stack

| Part | Technology |
|---|---|
| Language | Java 17 or later |
| Framework | Spring Boot 3.4.1 |
| Web | Spring MVC controllers (pages) + `@RestController` (REST API) |
| Persistence | Spring Data JPA, Hibernate |
| Templates | Thymeleaf |
| Styling | Bootstrap 5.3 and Bootstrap Icons (loaded from a CDN), own `style.css` |
| Database | MySQL 8 (H2 in memory, tests only) |
| Tests | JUnit 5, Mockito, Spring MockMvc |
| Build | Maven |

Authentication was **not** implemented (the brief makes the login page optional), so there is no login page.

## 5. System architecture

```
Browser ──► Controller layer ──► Service layer ──► Repository layer ──► MySQL
            (web + api)          (business rules)  (Spring Data JPA)
                 │                    │
                 └── DTO layer ◄──────┘        Exception layer handles errors for both
```

| Package (`com.hackathon.taskassign`) | Purpose |
|---|---|
| `controller.web` | Thymeleaf page controllers (`/`, `/employees`, `/tasks`, `/assignments`, `/workload`) |
| `controller.api` | JSON REST controllers under `/api` |
| `service` | Business logic: `WorkloadService`, `TaskService`, `EmployeeService`, `DashboardService` |
| `repository` | `EmployeeRepository`, `TaskRepository` |
| `entity` | `Employee`, `Task`, enums `TaskStatus`, `Priority` |
| `dto` | Request/response objects so entities never reach the UI |
| `exception` | Custom exceptions and the two error handlers (web and API) |
| `config` | `WebConfig` (CORS for the API), `DataInitializer` (demo data) |

## 6. Java backend workflow (assigning a task)

1. The manager submits the form on **Task Assignment** (or calls `POST /api/tasks/{id}/assign`).
2. `AssignmentController` passes the task id and employee id to `TaskService.assign(...)`.
3. `TaskService` opens a database transaction and loads the employee row **with a lock** (`SELECT ... FOR UPDATE`).
4. If the task is active, `WorkloadService.assertCanTakeNewActiveTask(...)` counts the employee's other active tasks.
5. If the count is already 5 or more, it throws `WorkloadLimitExceededException`; the transaction rolls back and nothing is saved.
6. Otherwise the task is saved with the new assignee and the time of assignment.
7. The controller shows either a green success message or the red error message.

## 7. Spring Boot explanation

Spring Boot starts an embedded web server (Tomcat), scans the `com.hackathon.taskassign` package for classes marked
`@Controller`, `@Service`, `@Repository`, etc., creates them and injects them into each other through constructors
(dependency injection). `application.properties` holds the settings (database, port, the limit of 5).
`TaskAssignmentApplication.main()` is the only thing you start.

## 8. Controller, Service and Repository

- **Controller** - receives the browser/API request, validates input (`@Valid`), calls a service, chooses the page or JSON to return. No business rules here.
- **Service** - the "brain". Contains the 5-task rule, the status rules and `@Transactional` boundaries.
- **Repository** - an interface; Spring Data JPA writes the SQL from method names such as `countByAssigneeIdAndStatusNot(...)`.

## 9. Entity relationships

```
Employee 1 ──────────── * Task
 (employees)               (tasks.employee_id → employees.id, nullable)
```

- `Task.assignee` is `@ManyToOne` (owning side, holds the foreign key). `NULL` means unassigned.
- `Employee.tasks` is `@OneToMany(mappedBy = "assignee")` (read-only side).
- Deleting an employee does **not** delete tasks: they become unassigned (`ON DELETE SET NULL` and an explicit update in `EmployeeService`).

## 10. Database design

**employees**: `id` (PK, auto), `name`, `email` (unique), `department`, `designation`

**tasks**: `id` (PK, auto), `title`, `description`, `priority` (LOW/MEDIUM/HIGH), `deadline`, `status`
(PENDING/IN_PROGRESS/COMPLETED), `assigned_at`, `created_at`, `employee_id` (FK → employees.id)

There is an index on `(employee_id, status)` because the workload count uses exactly those two columns.
Files: `database/schema.sql` (DDL) and `database/sample_data.sql` (demo rows). Normally you do not need
them: Hibernate creates the tables and `DataInitializer` inserts the demo data on first start.

## 11. API explanation

See **[docs/API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md)** for every endpoint, example requests and the error format.
Short version: `/api/employees`, `/api/tasks`, `/api/tasks/{id}/assign`, `/api/tasks/{id}/status`, `/api/workload`, `/api/dashboard`.
A rejected assignment returns **HTTP 409** with a clear message.

## 12. Five-task workload validation logic

*Active* = status **Pending** or **In Progress**. *Completed* never counts. The check lives in
`WorkloadService.assertCanTakeNewActiveTask` and is called by `TaskService` whenever an employee's active count
would **go up**:

| Action | Checked? |
|---|---|
| Create an active task already assigned to an employee | Yes |
| Assign an unassigned active task / move an active task to another employee | Yes (the task itself is excluded from the count) |
| Re-open a Completed task (back to Pending/In Progress) | Yes |
| Assign or create a Completed task | No (it is not active) |
| Complete a task, edit text/priority/deadline, Pending → In Progress | No (count does not go up) |

Other details:
- The count is a database query every time (`count where employee = ? and status <> COMPLETED`), so workload is
  automatically **recalculated** after every assignment or status change - there is no stored counter that could get out of date.
- The employee row is locked during the check, so two managers cannot both slip in a 5th/6th task at the same moment.
- The limit is configurable: `app.workload.max-active-tasks=5`.
- The browser sees the message *"Cannot assign task: NAME already has 5 active tasks (maximum allowed is 5). Complete an existing task or choose another employee."*

## 13. Overload detection logic

*Overloaded* = **more than** 5 active tasks (so exactly 5 is "At limit / Full", not overloaded). Overload can only come from
data that existed before the rule (imported or old records), because the service refuses to create it.

- `WorkloadService.getOverloadedEmployees()` groups all tasks by employee and status in one query and flags active > 5.
- Overloaded employees are shown **in red** with their active count (`7 / 5`, "Overloaded +2") on the Dashboard, Employees, Task Assignment and Workload pages.
- Nothing is ever deleted or moved automatically. An overloaded employee can still finish or edit existing tasks; only *new* assignments to them are refused until they are back under the limit.
- Demo data: **Aarav Sharma** has 7 active tasks (overloaded) and **Priya Reddy** has exactly 5 (full).

## 14. Frontend workflow

| Page | URL | What you can do |
|---|---|---|
| Dashboard | `/` | Totals, overloaded alert, workload bars, status statistics, recent assignments |
| Employees | `/employees` | Search; add, edit, delete; see active/completed counts |
| Tasks | `/tasks` | Tabs All / Active / Completed; filter by text, status, priority, employee; change status from the drop-down; edit; delete |
| Task form | `/tasks/new`, `/tasks/{id}/edit` | Title, description, priority, deadline, status, assignee |
| Task Assignment | `/assignments` | Assign any active task to an employee, quick-assign unassigned tasks, unassign |
| Workload Monitor | `/workload` | Pending/In Progress/Completed per employee, filters: all / overloaded / at limit / free slots |

Forms are normal HTML posts handled by Spring MVC controllers (no front-end framework). The little JavaScript in
`static/js/app.js` only confirms deletes, auto-submits the status drop-down, and hides success messages.
Bootstrap is loaded from a CDN, so the **browser** needs internet access to show the styling and icons.

---

## 15. Installation and execution

### Step 1 - Install the JDK (17 or later)
- **Windows / macOS:** download *Eclipse Temurin JDK 17 (or 21)* from <https://adoptium.net> and run the installer.
- **Ubuntu/Debian:** `sudo apt update && sudo apt install openjdk-17-jdk`
- Check: open a new terminal and run `java -version` and `javac -version` (both must show 17 or higher).

### Step 2 - Install Maven (optional)
IntelliJ IDEA and VS Code (Java extensions) include Maven. For the command line, install it from <https://maven.apache.org/download.cgi>
(or `sudo apt install maven`, `brew install maven`) and check with `mvn -version`.

### Step 3 - Install MySQL
- Download *MySQL Community Server 8* from <https://dev.mysql.com/downloads/mysql/> and run the installer (Windows/macOS), or `sudo apt install mysql-server` on Ubuntu.
- During setup choose a password for the `root` user and remember it. Keep the default port **3306**.
- Check the server is running: `mysql -u root -p` and enter the password, then type `exit`.
- You do **not** need to create the database - the app creates `task_assignment_db` automatically.
  (To create it yourself: `mysql -u root -p < database/schema.sql`.)

### Step 4 - Configure the database credentials
Open `src/main/resources/application.properties` and set your password:
```
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```
Or leave the file as is and set environment variables instead: `DB_USERNAME`, `DB_PASSWORD` (also `DB_HOST`, `DB_PORT`, `DB_NAME`).
`application.properties.example` in the project root shows the same settings.

### Step 5 - Import the project
- **IntelliJ IDEA:** *File → Open…* → select the project folder (the one containing `pom.xml`) → *Open as Project* → *Trust Project*. Wait for the Maven import to finish. Set *File → Project Structure → SDK* to JDK 17+.
- **VS Code:** install the *Extension Pack for Java* and *Spring Boot Extension Pack*, then *File → Open Folder…* and choose the project folder. Wait until "Java: Ready" appears.

### Step 6 - Download the Maven dependencies
IDE: Maven does this automatically (or right-click `pom.xml` → *Maven → Reload project*).
Command line, inside the project folder: `mvn clean install -DskipTests`

### Step 7 - Run the tests (no MySQL needed)
```
mvn test
```
This uses an in-memory H2 database. All tests should pass before you start the app.

### Step 8 - Start Spring Boot
- Command line: `mvn spring-boot:run`
- IntelliJ / VS Code: open `TaskAssignmentApplication.java` and click the green *Run* button.
- Wait for the line `Started TaskAssignmentApplication`. On the first start the tables are created and demo data is loaded.

### Step 9 - Open in the browser
Go to **<http://localhost:8080>**. To use another port, change `server.port` in `application.properties`.

### Troubleshooting
| Message | Fix |
|---|---|
| `Access denied for user 'root'` | Wrong password in `application.properties` / `DB_PASSWORD` |
| `Communications link failure` | MySQL is not running or not on port 3306 |
| `Port 8080 was already in use` | Change `server.port` or stop the other program |
| `invalid target release: 17` | Your JDK is older than 17 - install JDK 17+ and select it in the IDE |
| Page has no colours/icons | Browser has no internet (Bootstrap CDN) |
| Want an empty database | Set `app.seed-sample-data=false` (and drop `task_assignment_db` once if it already has data) |

---

## 16. Testing procedure

Automated: `mvn test`

| File | Type | What it checks |
|---|---|---|
| `service/TaskServiceTest` | JUnit + Mockito | 4th/5th assignment accepted, **6th rejected**, completed tasks don't count, re-open rejected when full, overloaded employee can still edit/complete, move to a full employee rejected, delete |
| `service/WorkloadServiceTest` | JUnit + Mockito | Active-count maths, **overload detection** (7 = overloaded, exactly 5 = not) |
| `service/EmployeeServiceTest` | JUnit + Mockito | Add, duplicate email, edit, delete (tasks kept & unassigned) |
| `service/DashboardServiceTest` | JUnit + Mockito | Dashboard totals and percentages |
| `TaskAssignmentIntegrationTest` | Spring Boot + H2 + MockMvc | The whole flow with a real database, every page renders, the web form and REST API reject the 6th task (HTTP 409) |

Mapping to the ten required scenarios: 1 add employee, 2 create task, 3 assign, 4 five active tasks, 5 reject the 6th,
6 complete one, 7 assign another → `fullWorkloadScenario`; 8 overload → `overloadedEmployeeIsDetected_andNothingIsDeleted`;
9 edit/delete → `editAndDeleteRecords`; 10 dashboard → `dashboardStatisticsAreCorrect`.

**Manual check in the browser** (with the demo data):
1. *Dashboard* → "Overloaded Employees" shows **1** and Aarav is red (7 / 5).
2. *Task Assignment* → choose task "Create onboarding checklist" and employee **Priya Reddy (5/5, FULL)** → red error message, nothing changes.
3. *Tasks* → on one of Priya's tasks change status to **Completed** → *Workload Monitor* shows Priya 4 / 5.
4. *Task Assignment* → assign the same task to Priya again → success, Priya is 5 / 5 again.
5. *Tasks* → *Active* and *Completed* tabs show the tasks separately; try the filters.
6. *Employees* → add, edit and delete an employee (its tasks stay, now unassigned).

## Project structure

```
Employee_Task_Assignment_System_Java/
├── pom.xml
├── README.md
├── application.properties.example
├── database/              schema.sql, sample_data.sql
├── docs/API_DOCUMENTATION.md
└── src/
    ├── main/java/com/hackathon/taskassign/   controller, service, repository, entity, dto, exception, config
    ├── main/resources/    application.properties, templates/ (Thymeleaf), static/css, static/js
    └── test/              unit tests, integration test, test application.properties (H2)
```
