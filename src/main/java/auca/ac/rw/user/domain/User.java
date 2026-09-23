package rw.ac.auca.user.domain;

import auca.ac.rw.base.BaseEntity;
import auca.ac.rw.department.Department;
import auca.ac.rw.student.domain.Student;
import auca.ac.rw.user.domain.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String username;   // or email

    @Column(nullable = false)
    private String password;   // hashed, never plain text

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @OneToOne
    @JoinColumn(name = "student_id")
    private Student student;       // only set when role = STUDENT

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department; // only set when role = DEPARTMENT_OFFICER


}