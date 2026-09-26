package auca.ac.rw.controller;

import auca.ac.rw.academic.domain.Academic;
import auca.ac.rw.academic.domain.AcademicLevel;
import auca.ac.rw.academic.service.AcademicService;
import auca.ac.rw.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/academic")
public class AcademicController {

    private final AcademicService academicService;

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<Academic>> findAll() {
        return ApiResponse.of(
                "Academics retrieved successfully",
                academicService.findAll()
        );
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Academic> findById(@PathVariable UUID id) {
        Academic academic = new Academic();
        academic.setId(id);
        return ApiResponse.of(
                "Academic entry retrieved successfully",
                academicService.findById(academic)
        );
    }

    @GetMapping("/children/{parentId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<Academic>> findChildren(@PathVariable UUID parentId) {
        return ApiResponse.of(
                "Children retrieved successfully",
                academicService.findChildren(parentId)
        );
    }

    @GetMapping("/level/{level}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<Academic>> findByLevel(@PathVariable AcademicLevel level) {
        return ApiResponse.of(
                "Academic entries retrieved successfully",
                academicService.findByLevel(level)
        );
    }

    @GetMapping("/programs")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<Academic>> findAllPrograms() {
        return ApiResponse.of(
                "Programs retrieved successfully",
                academicService.findAllPrograms()
        );
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Academic> register(@RequestBody Academic academic) {
        return ApiResponse.of(
                "Academic entry created successfully",
                academicService.register(academic)
        );
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Academic> update(@PathVariable UUID id, @RequestBody Academic academic) {
        academic.setId(id);
        return ApiResponse.of(
                "Academic entry updated successfully",
                academicService.update(academic)
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
}
