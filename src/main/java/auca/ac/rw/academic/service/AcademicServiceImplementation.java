package auca.ac.rw.academic.service;

import auca.ac.rw.academic.domain.Academic;
import auca.ac.rw.academic.domain.AcademicLevel;
import auca.ac.rw.academic.repository.AcademicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AcademicServiceImplementation implements AcademicService{
    private final AcademicRepository academicRepository;


    @Override
    public Academic register(Academic academic) {
        switch (academic.getLevel()) {
            case PROGRAM -> {
                if (academic.getParent() != null) {
                    throw new IllegalStateException("A PROGRAM level entry must not have a parent");
                }
            }
            case FACULTY -> {
                if (academic.getParent() == null || academic.getParent().getLevel() != AcademicLevel.PROGRAM) {
                    throw new IllegalStateException("A FACULTY level entry must have a PROGRAM as its parent");
                }
            }
            case DEPARTMENT -> {
                if (academic.getParent() == null || academic.getParent().getLevel() != AcademicLevel.FACULTY) {
                    throw new IllegalStateException("A DEPARTMENT level entry must have a FACULTY as its parent");
                }
            }
        }
        return academicRepository.save(academic);
    }

    @Override
    public Academic update(Academic academic) {
        Academic found = findById(academic);
        found.setName(academic.getName());
        found.setLevel(academic.getLevel());
        found.setParent(academic.getParent());
        return academicRepository.save(found);
    }

    @Override
    public List<Academic> findAll() {
        return academicRepository.findAll();
    }

    @Override
    public Academic findById(Academic academic) {
        return academicRepository.findById(academic.getId())
                .orElseThrow(()-> new RuntimeException("Academic with id " + academic.getId() + " not found"));
    }

    @Override
    public void delete(Academic academic) {
        academicRepository.delete(academic);
    }

    @Override
    public List<Academic> findChildren(UUID parentId) {
        return academicRepository.findAcademicsByParentId(parentId);
    }

    @Override
    public List<Academic> findByLevel(AcademicLevel level) {
        return academicRepository.findAcademicsByLevel(level);
    }

    @Override
    public List<Academic> findAllPrograms() {
        return academicRepository.findAcademicsByParentIsNull();
    }
}
