package auca.ac.rw.department.service;

import auca.ac.rw.department.domain.Department;
import auca.ac.rw.department.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
//import java.util.Optional;

@RequiredArgsConstructor
@Service
public class DepartmentServiceImplementation implements DepartmentService{

    private final DepartmentRepository departmentRepository;

    @Override
    public Department register(Department department) {
        return departmentRepository.save(department);
    }

    @Override
    public Department update(Department department) {
        Department found = findById(department);
        found.setName(department.getName());
        found.setDescription(department.getDescription());
        return departmentRepository.save(found);
    }

    @Override
    public void delete(Department department) {
        departmentRepository.delete(department);
    }

    @Override
    public Department findById(Department department) {
        return departmentRepository.findById(department.getId())
                .orElseThrow(()-> new RuntimeException("Department not found"));
    }

    @Override
    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

//    @Override
//    public Optional<Department> findByName(String name) {
//        return Optional.empty();
//    }
}
