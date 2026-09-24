package auca.ac.rw.user.domain;

import auca.ac.rw.base.BaseEntity;
import auca.ac.rw.staff.domain.Staff;
import auca.ac.rw.student.domain.Student;
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
@Table(name = "users")
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

    @OneToOne
    @JoinColumn(name = "staff_id")
    private Staff staff; // only set when role = DEPARTMENT_OFFICER

}