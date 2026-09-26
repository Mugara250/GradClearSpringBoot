package auca.ac.rw.controller;

import auca.ac.rw.academic.domain.Academic;
import auca.ac.rw.academic.service.AcademicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/academic")
public class AcademicController {

    private final AcademicService academicService;

    @GetMapping("/search-all")
    @ResponseStatus(HttpStatus.OK)
    public List<Academic> findAll() {
        return academicService.findAll();
    }
}
