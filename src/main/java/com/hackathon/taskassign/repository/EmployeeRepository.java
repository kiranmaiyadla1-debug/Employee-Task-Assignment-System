package com.hackathon.taskassign.repository;

import com.hackathon.taskassign.entity.Employee;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Employee> findAllByOrderByNameAsc();

    /**
     * Loads the employee row with a database lock (SELECT ... FOR UPDATE).
     * Two managers assigning tasks to the same employee at the same moment are
     * therefore processed one after the other, so the 5-task limit cannot be bypassed.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.id = :id")
    Optional<Employee> findByIdForUpdate(@Param("id") Long id);
}
