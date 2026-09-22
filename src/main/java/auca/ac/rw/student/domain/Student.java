package auca.ac.rw.student.domain;

import auca.ac.rw.academic.domain.Academic;
import auca.ac.rw.base.BaseEntity;
import auca.ac.rw.transcript.domain.Transcript;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "students")
public class Student extends BaseEntity {
    @Column(name = "student_id", unique = true)
    private String studentId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(unique = true)
    private String email;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @ManyToOne
    @JoinColumn(name = "academic_id", nullable = false)
    private Academic academic;

    @OneToMany(mappedBy = "student")
    private List<Transcript> transcripts = new ArrayList<>();
}
