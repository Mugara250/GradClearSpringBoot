package auca.ac.rw.transcript.repository;

import auca.ac.rw.transcript.domain.CourseStatus;
import auca.ac.rw.transcript.domain.Transcript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {
    List<Transcript> findTranscriptsByStudentId(UUID studentId);

    List<Transcript> findTranscriptsByStudentIdAndStatus(UUID studentId, CourseStatus status);

    boolean existsByStudentIdAndStatusIn(UUID studentId, Collection<CourseStatus> statuses);

    @Query("SELECT COALESCE(SUM(t.course.creditHours), 0) FROM Transcript t " +
            "WHERE t.student.id = :studentId AND t.status = 'PASSED'")
    Integer sumPassedCreditsByStudentId(@Param("studentId") UUID studentId);
}
