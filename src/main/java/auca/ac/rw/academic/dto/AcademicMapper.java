package auca.ac.rw.academic.dto;

import auca.ac.rw.academic.domain.Academic;

public class AcademicMapper {

    private AcademicMapper() {}

    public static AcademicDTO toDTO(Academic academic) {
        return new AcademicDTO(
                academic.getId(),
                academic.getName(),
                academic.getLevel(),
                academic.getParent() != null ? academic.getParent().getId() : null,
                academic.getRequiredCredits()
        );
    }
}