package auca.ac.rw.staff.domain;

import auca.ac.rw.base.BaseEntity;
import auca.ac.rw.department.domain.Department;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

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

    @ManyToMany
    @JoinTable(name = "staff_department",
    joinColumns = @JoinColumn(name = "staff_id"),
    inverseJoinColumns = @JoinColumn(name = "department_id"))
    private Set<Department> departments = new HashSet<>();
}