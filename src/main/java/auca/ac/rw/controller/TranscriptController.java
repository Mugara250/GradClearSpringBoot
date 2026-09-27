package auca.ac.rw.controller;

import auca.ac.rw.course.domain.Course;
import auca.ac.rw.course.service.CourseService;
import auca.ac.rw.response.ApiResponse;
import auca.ac.rw.student.domain.Student;
import auca.ac.rw.student.service.StudentService;
import auca.ac.rw.transcript.domain.Transcript;
import auca.ac.rw.transcript.dto.TranscriptDTO;
import auca.ac.rw.transcript.dto.TranscriptMapper;
import auca.ac.rw.transcript.service.TranscriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/transcript")
public class TranscriptController {

    private final TranscriptService transcriptService;
    private final StudentService studentService;
    private final CourseService courseService;

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<TranscriptDTO>> findAll() {
        List<TranscriptDTO> transcripts = transcriptService.findAll().stream()
                .map(TranscriptMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Transcripts retrieved successfully",
                transcripts
        );
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<TranscriptDTO> findById(@PathVariable UUID id) {
        Transcript transcript = new Transcript();
        transcript.setId(id);
        Transcript found = transcriptService.findById(transcript);
        return ApiResponse.of(
                "Transcript retrieved successfully",
                TranscriptMapper.toDTO(found)
        );
    }

    @GetMapping("/student/{studentId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<TranscriptDTO>> findByStudent(@PathVariable UUID studentId) {
        List<TranscriptDTO> transcripts = transcriptService.findByStudent(studentId).stream()
                .map(TranscriptMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Transcripts retrieved successfully",
                transcripts
        );
    }

    @GetMapping("/student/{studentId}/academically-clear")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Boolean> isStudentAcademicallyClear(@PathVariable UUID studentId) {
        boolean clear = transcriptService.isStudentAcademicallyClear(studentId);
        return ApiResponse.of("Academic clearance status retrieved successfully", clear);
    }

    @GetMapping("/student/{studentId}/total-passed-credits")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Integer> getTotalPassedCredits(@PathVariable UUID studentId) {
        int credits = transcriptService.getTotalPassedCredits(studentId);
        return ApiResponse.of("Total passed credits retrieved successfully", credits);
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TranscriptDTO> add(@RequestBody TranscriptDTO transcriptDTO) {
        Transcript transcript = buildEntityFromDTO(transcriptDTO);
        Transcript saved = transcriptService.add(transcript);
        return ApiResponse.of(
                "Transcript created successfully",
                TranscriptMapper.toDTO(saved)
        );
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<TranscriptDTO> update(@PathVariable UUID id, @RequestBody TranscriptDTO transcriptDTO) {
        Transcript transcript = buildEntityFromDTO(transcriptDTO);
        transcript.setId(id);
        Transcript updated = transcriptService.update(transcript);
        return ApiResponse.of(
                "Transcript updated successfully",
                TranscriptMapper.toDTO(updated)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        Transcript transcript = new Transcript();
        transcript.setId(id);
        transcriptService.delete(transcript);
        return ApiResponse.of("Transcript deleted successfully", null);
    }

    // resolves studentId/courseId -> actual, validated Student/Course (throws 404 if missing)
    private Transcript buildEntityFromDTO(TranscriptDTO dto) {
        Transcript transcript = new Transcript();
        transcript.setGrade(dto.getGrade());
        transcript.setStatus(dto.getStatus());
        transcript.setAcademicYear(dto.getAcademicYear());

        if (dto.getStudentId() != null) {
            Student studentRef = new Student();
            studentRef.setId(dto.getStudentId());
            transcript.setStudent(studentService.findById(studentRef));
        }
        if (dto.getCourseId() != null) {
            Course courseRef = new Course();
            courseRef.setId(dto.getCourseId());
            transcript.setCourse(courseService.findById(courseRef));
        }
        return transcript;
    }
}
