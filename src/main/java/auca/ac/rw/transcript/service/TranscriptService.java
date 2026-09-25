package auca.ac.rw.transcript.service;

import auca.ac.rw.transcript.domain.Transcript;

import java.util.List;
import java.util.UUID;

public interface TranscriptService {
    Transcript add(Transcript transcript);
    Transcript update(Transcript transcript);
    void delete(Transcript transcript);
    Transcript findById(Transcript transcript);
    List<Transcript> findAll();

    List<Transcript> findByStudent(UUID studentId);
    boolean isStudentAcademicallyClear(UUID studentId);
}
