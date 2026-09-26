package auca.ac.rw.academic.service;

import auca.ac.rw.academic.domain.Academic;
import auca.ac.rw.academic.domain.AcademicLevel;

import java.util.List;
import java.util.UUID;

public interface AcademicService {
    Academic register(Academic academic);
    Academic update(Academic academic);
    List<Academic> findAll();
    Academic findById(Academic academic);
    void delete(Academic academic);

    List<Academic> findChildren(UUID parentId);
    List<Academic> findByLevel(AcademicLevel level);
    List<Academic> findAllPrograms();
}
