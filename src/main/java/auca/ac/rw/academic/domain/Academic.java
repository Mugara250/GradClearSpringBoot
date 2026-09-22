package auca.ac.rw.academic.domain;

import auca.ac.rw.base.BaseEntity;
import auca.ac.rw.course.domain.Course;
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
@Table(name = "academics")
public class Academic extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AcademicLevel academicLevel;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Academic parent;

    @OneToMany(mappedBy = "academic")
    private List<Course> courses = new ArrayList<>();
}
