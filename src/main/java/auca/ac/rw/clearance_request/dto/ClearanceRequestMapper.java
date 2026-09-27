package auca.ac.rw.clearance_request.dto;

import auca.ac.rw.clearance_request.domain.ClearanceRequest;

public class ClearanceRequestMapper {

    private ClearanceRequestMapper() {}

    public static ClearanceRequestDTO toDTO(ClearanceRequest request) {
        return new ClearanceRequestDTO(
                request.getId(),
                request.getStudent().getId(),
                request.getDepartment().getId(),
                request.getStatus(),
                request.getRemarks(),
                request.getRequestDate()
        );
    }
}