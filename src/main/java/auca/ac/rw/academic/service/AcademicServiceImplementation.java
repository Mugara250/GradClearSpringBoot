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
        if (academic.getLevel() == AcademicLevel.FACULTY && academic.getParent() != null) {
            throw new IllegalStateException("A FACULTY level entry must not have a parent");
        }
        if (academic.getLevel() != AcademicLevel.FACULTY && academic.getParent() == null) {
            throw new IllegalStateException(academic.getLevel() + " level entry must have a parent");
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
                .orElseThrow(()-> new RuntimeException("Object not found"));
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
    public List<Academic> findAllFaculties() {
        return academicRepository.findAcademicsByParentIsNull();
    }
}
