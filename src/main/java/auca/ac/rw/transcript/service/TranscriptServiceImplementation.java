package auca.ac.rw.transcript.service;

import auca.ac.rw.transcript.domain.CourseStatus;
import auca.ac.rw.transcript.domain.Transcript;
import auca.ac.rw.transcript.repository.TranscriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TranscriptServiceImplementation implements TranscriptService{

    private final TranscriptRepository transcriptRepository;

    @Override
    public Transcript add(Transcript transcript) {
        return transcriptRepository.save(transcript);
    }

    @Override
    public Transcript update(Transcript transcript) {
        Transcript found = findById(transcript);
        found.setGrade(transcript.getGrade());
        found.setStatus(transcript.getStatus());
        found.setAcademicYear(transcript.getAcademicYear());
        return transcriptRepository.save(found);
    }

    @Override
    public void delete(Transcript transcript) {
        transcriptRepository.delete(transcript);
    }

    @Override
    public Transcript findById(Transcript transcript) {
        return transcriptRepository.findById(transcript.getId())
                .orElseThrow(()->new RuntimeException("Transcript with id " + transcript.getId() + " not found"));
    }

    @Override
    public List<Transcript> findAll() {
        return transcriptRepository.findAll();
    }

    @Override
    public List<Transcript> findByStudent(UUID studentId) {
        return transcriptRepository.findTranscriptsByStudentId(studentId);
    }

    @Override
    public boolean isStudentAcademicallyClear(UUID studentId) {
        boolean hasUnresolvedCourses = transcriptRepository.existsByStudentIdAndStatusIn(
                studentId, List.of(CourseStatus.FAILED, CourseStatus.INCOMPLETE));
        return !hasUnresolvedCourses;
    }
}
