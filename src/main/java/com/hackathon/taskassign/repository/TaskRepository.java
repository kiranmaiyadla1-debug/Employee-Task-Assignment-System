package com.hackathon.taskassign.repository;

import com.hackathon.taskassign.entity.Task;
import com.hackathon.taskassign.entity.TaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    long countByStatus(TaskStatus status);

    long countByAssigneeIdAndStatus(Long employeeId, TaskStatus status);

    /** Active tasks of an employee = every task whose status is NOT Completed. */
    long countByAssigneeIdAndStatusNot(Long employeeId, TaskStatus status);

    /** Same as above but ignoring one task (used when that task is being edited/moved). */
    long countByAssigneeIdAndStatusNotAndIdNot(Long employeeId, TaskStatus status, Long taskId);

    @Query("select t from Task t left join fetch t.assignee order by t.id desc")
    List<Task> findAllWithAssignee();

    @EntityGraph(attributePaths = "assignee")
    List<Task> findByAssigneeIsNotNullOrderByAssignedAtDescIdDesc(Pageable pageable);

    /** Rows of [employeeId, status, count] - one query gives the whole workload picture. */
    @Query("select t.assignee.id, t.status, count(t) from Task t "
            + "where t.assignee is not null group by t.assignee.id, t.status")
    List<Object[]> countGroupedByAssigneeAndStatus();

    /** Used when an employee is deleted: tasks are kept but become unassigned. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Task t set t.assignee = null, t.assignedAt = null where t.assignee.id = :employeeId")
    int unassignAll(@Param("employeeId") Long employeeId);
}
