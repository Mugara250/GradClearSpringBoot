package auca.ac.rw.transcript.repository;

import auca.ac.rw.transcript.domain.CourseStatus;
import auca.ac.rw.transcript.domain.Transcript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {
    List<Transcript> findTranscriptsByStudentId(UUID studentId);

    List<Transcript> findTranscriptsByStudentIdAndStatus(UUID studentId, CourseStatus status);

    boolean existsByStudentIdAndStatusIn(UUID studentId, Collection<CourseStatus> statuses);

}
