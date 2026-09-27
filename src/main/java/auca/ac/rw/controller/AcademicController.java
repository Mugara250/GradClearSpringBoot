package auca.ac.rw.controller;

import auca.ac.rw.academic.domain.Academic;
import auca.ac.rw.academic.domain.AcademicLevel;
import auca.ac.rw.academic.dto.AcademicDTO;
import auca.ac.rw.academic.dto.AcademicMapper;
import auca.ac.rw.academic.service.AcademicService;
import auca.ac.rw.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/academic")
public class AcademicController {

    private final AcademicService academicService;

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<AcademicDTO>> findAll() {
        List<AcademicDTO> academics = academicService.findAll().stream()
                .map(AcademicMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Academics retrieved successfully",
                academics
        );
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AcademicDTO> findById(@PathVariable UUID id) {
        Academic academic = new Academic();
        academic.setId(id);
        Academic found = academicService.findById(academic);
        return ApiResponse.of(
                "Academic entry retrieved successfully",
                AcademicMapper.toDTO(found)
        );
    }

    @GetMapping("/children/{parentId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<AcademicDTO>> findChildren(@PathVariable UUID parentId) {
        List<AcademicDTO> children = academicService.findChildren(parentId).stream()
                .map(AcademicMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Children retrieved successfully",
                children
        );
    }

    @GetMapping("/level/{level}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<AcademicDTO>> findByLevel(@PathVariable AcademicLevel level) {
        List<AcademicDTO> results = academicService.findByLevel(level).stream()
                .map(AcademicMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Academic entries retrieved successfully",
                results
        );
    }

    @GetMapping("/programs")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<AcademicDTO>> findAllPrograms() {
        List<AcademicDTO> programs = academicService.findAllPrograms().stream()
                .map(AcademicMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Programs retrieved successfully",
                programs
        );
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AcademicDTO> register(@RequestBody AcademicDTO academicDTO) {
        Academic academic = buildEntityFromDTO(academicDTO);
        Academic saved = academicService.register(academic);

        return ApiResponse.of(
                "Academic entry created successfully",
                AcademicMapper.toDTO(saved)
        );
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AcademicDTO> update(@PathVariable UUID id, @RequestBody AcademicDTO academicDTO) {
        Academic academic = buildEntityFromDTO(academicDTO);
        academic.setId(id);
        Academic updated = academicService.update(academic);
        return ApiResponse.of(
                "Academic entry updated successfully",
                AcademicMapper.toDTO(updated)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        Academic academic = new Academic();
        academic.setId(id);
        academicService.delete(academic);
        return ApiResponse.of("Academic entry deleted successfully", null);
    }

    // resolves parentId -> an actual, validated Academic (throws 404 if it doesn't exist)
    private Academic buildEntityFromDTO(AcademicDTO dto) {
        Academic academic = new Academic();
        academic.setName(dto.getName());
        academic.setLevel(dto.getLevel());
        academic.setRequiredCredits(dto.getRequiredCredits());

        if (dto.getParentId() != null) {
            Academic parentRef = new Academic();
            parentRef.setId(dto.getParentId());
            academic.setParent(academicService.findById(parentRef));
        }
        return academic;
    }
}
