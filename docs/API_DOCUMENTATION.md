# REST API Documentation

Base URL: `http://localhost:8080`  ·  Format: JSON  ·  No authentication.

Enum values: `status` = `PENDING` | `IN_PROGRESS` | `COMPLETED`; `priority` = `LOW` | `MEDIUM` | `HIGH`.
Dates are `yyyy-MM-dd`.

## Employees

| Method | URL | Description | Success |
|---|---|---|---|
| GET | `/api/employees?q=text` | List employees (optional search) with live workload | 200 |
| GET | `/api/employees/{id}` | One employee | 200 |
| POST | `/api/employees` | Add an employee | 201 |
| PUT | `/api/employees/{id}` | Edit an employee | 200 |
| DELETE | `/api/employees/{id}` | Delete; their tasks are kept and become unassigned | 200 |
| GET | `/api/employees/{id}/workload` | Workload of one employee | 200 |

**Request body (POST / PUT)**
```json
{ "name": "Asha Nair", "email": "asha.nair@example.com", "department": "Engineering", "designation": "Developer" }
```
**Response**
```json
{ "id": 6, "name": "Asha Nair", "email": "asha.nair@example.com", "department": "Engineering",
  "designation": "Developer", "activeTasks": 0, "completedTasks": 0, "totalTasks": 0,
  "limit": 5, "overloaded": false, "atCapacity": false }
```
**Delete response:** `{ "deleted": true, "unassignedTasks": 3 }`

## Tasks

| Method | URL | Description | Success |
|---|---|---|---|
| GET | `/api/tasks` | List/filter. Query params (all optional): `q`, `status`, `priority`, `employeeId`, `view` (`all`/`active`/`completed`) | 200 |
| GET | `/api/tasks/{id}` | One task | 200 |
| POST | `/api/tasks` | Create (optionally assigned) | 201 |
| PUT | `/api/tasks/{id}` | Edit (including assignee and status) | 200 |
| DELETE | `/api/tasks/{id}` | Delete | 204 |
| POST | `/api/tasks/{id}/assign` | Assign / re-assign to an employee | 200 |
| DELETE | `/api/tasks/{id}/assignment` | Unassign | 200 |
| PATCH | `/api/tasks/{id}/status` | Change progress | 200 |

**Request body (POST / PUT)**
```json
{ "title": "Prepare report", "description": "Monthly numbers", "priority": "HIGH",
  "deadline": "2026-12-01", "status": "PENDING", "assigneeId": 2 }
```
`assigneeId` is optional (`null` = unassigned). `status` defaults to `PENDING`, `priority` to `MEDIUM`.

**Response**
```json
{ "id": 12, "title": "Prepare report", "description": "Monthly numbers", "priority": "HIGH",
  "deadline": "2026-12-01", "status": "PENDING", "assigneeId": 2, "assigneeName": "Priya Reddy",
  "assignedAt": "2026-11-09T11:30:00", "overdue": false, "active": true, "assigned": true }
```

**Assign:** `POST /api/tasks/12/assign` with `{ "employeeId": 3 }`
**Status:** `PATCH /api/tasks/12/status` with `{ "status": "COMPLETED" }`

## Workload and dashboard (read only)

| Method | URL | Description |
|---|---|---|
| GET | `/api/workload` | Workload of every employee |
| GET | `/api/workload/overloaded` | Only employees with MORE than 5 active tasks |
| GET | `/api/dashboard` | Totals, status counts, overloaded count, workloads, recent assignments |

Workload item:
```json
{ "employeeId": 1, "name": "Aarav Sharma", "pendingCount": 4, "inProgressCount": 3, "completedCount": 0,
  "activeCount": 7, "limit": 5, "overloaded": true, "atCapacity": false, "excess": 2,
  "remainingSlots": 0, "percent": 100 }
```

## Errors

Every error has the same shape:
```json
{ "timestamp": "2026-11-09T11:30:00", "status": 409, "error": "Conflict",
  "message": "Cannot assign task: Priya Reddy already has 5 active tasks (maximum allowed is 5). Complete an existing task or choose another employee.",
  "fieldErrors": null }
```

| HTTP status | When |
|---|---|
| 400 | Validation failed (`fieldErrors` lists each field), malformed JSON, bad enum value or date |
| 404 | Employee or task id does not exist |
| 409 | **Workload limit exceeded** (6th active task) or duplicate employee email |

## Examples (curl)

```bash
# list employees with workload
curl http://localhost:8080/api/employees

# try to give Priya (id 2, already at 5 active tasks) one more -> 409
curl -X POST http://localhost:8080/api/tasks/20/assign -H "Content-Type: application/json" -d '{"employeeId":2}'

# complete a task
curl -X PATCH http://localhost:8080/api/tasks/8/status -H "Content-Type: application/json" -d '{"status":"COMPLETED"}'

# who is overloaded?
curl http://localhost:8080/api/workload/overloaded
```
