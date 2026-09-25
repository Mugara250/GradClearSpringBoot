package auca.ac.rw.academic.repository;

import auca.ac.rw.academic.domain.Academic;
import auca.ac.rw.academic.domain.AcademicLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcademicRepository extends JpaRepository<Academic, UUID> {
    List<Academic> findAcademicsByParentId(UUID parentId);

    List<Academic> findAcademicsByLevel(AcademicLevel academicLevel);

    List<Academic> findAcademicsByParentIsNull();
}
