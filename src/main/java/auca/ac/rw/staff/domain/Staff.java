package auca.ac.rw.staff.domain;

import auca.ac.rw.base.BaseEntity;
import auca.ac.rw.department.domain.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Staff extends BaseEntity {

    @Column(name = "staff_id", nullable = false, unique = true)
    private String staffId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column
    private String position;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}