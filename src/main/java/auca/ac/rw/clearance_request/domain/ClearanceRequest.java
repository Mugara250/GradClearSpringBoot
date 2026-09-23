package auca.ac.rw.clearance_request.domain;

import auca.ac.rw.base.BaseEntity;
import auca.ac.rw.department.Department;
import auca.ac.rw.student.domain.Student;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clearance_requests")
public class ClearanceRequest extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClearanceStatus status;

    private String remarks;

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate = LocalDate.now();

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}
