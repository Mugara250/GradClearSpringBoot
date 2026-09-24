package auca.ac.rw.academic.service;

import auca.ac.rw.academic.domain.Academic;

import java.util.List;

public interface AcademicService {
    public Academic register(Academic academic);
    public Academic update(Academic academic);
    public List<Academic> findAll();
    public Academic findById();
}
