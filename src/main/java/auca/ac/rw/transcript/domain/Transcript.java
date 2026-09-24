package auca.ac.rw.transcript.domain;

import auca.ac.rw.base.BaseEntity;
import auca.ac.rw.course.domain.Course;
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
@Table(name = "transcripts")
public class Transcript extends BaseEntity {

    @Column(nullable = false)
    private Double grade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status = CourseStatus.INCOMPLETE;

    @Column(nullable = false)
    private String academicYear;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
}
