package auca.ac.rw.course.domain;

import auca.ac.rw.academic.domain.Academic;
import auca.ac.rw.base.BaseEntity;
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
@Table(name = "courses")
public class Course extends BaseEntity {

    @Column(name = "course_code", nullable = false, unique = true)
    private String courseCode;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "credit_hours", nullable = false)
    private int creditHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_scope", nullable = false)
    private CourseScope courseScope;

    @ManyToMany
    @JoinTable(name = "course_academic",
            joinColumns = @JoinColumn(name = "course_id"),
    inverseJoinColumns = @JoinColumn(name = "academic_id"))
    private Set<Academic> academics = new HashSet<>();
}
