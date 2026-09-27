package auca.ac.rw.transcript.dto;

import auca.ac.rw.transcript.domain.Transcript;

public class TranscriptMapper {

    private TranscriptMapper() {}

    public static TranscriptDTO toDTO(Transcript transcript) {
        return new TranscriptDTO(
                transcript.getId(),
                transcript.getGrade(),
                transcript.getStatus(),
                transcript.getAcademicYear(),
                transcript.getStudent().getId(),
                transcript.getCourse().getId()
        );
    }
}