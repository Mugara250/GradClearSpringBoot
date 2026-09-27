package auca.ac.rw.controller;

import auca.ac.rw.response.ApiResponse;
import auca.ac.rw.staff.domain.Staff;
import auca.ac.rw.staff.service.StaffService;
import auca.ac.rw.student.domain.Student;
import auca.ac.rw.student.service.StudentService;
import auca.ac.rw.user.domain.User;
import auca.ac.rw.user.domain.UserRole;
import auca.ac.rw.user.dto.LoginRequestDTO;
import auca.ac.rw.user.dto.UserDTO;
import auca.ac.rw.user.dto.UserMapper;
import auca.ac.rw.user.dto.UserRegistrationDTO;
import auca.ac.rw.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;
    private final StudentService studentService;
    private final StaffService staffService;

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<UserDTO>> findAll() {
        List<UserDTO> users = userService.findAll().stream()
                .map(UserMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Users retrieved successfully",
                users
        );
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UserDTO> findById(@PathVariable UUID id) {
        User user = new User();
        user.setId(id);
        User found = userService.findById(user);
        return ApiResponse.of(
                "User retrieved successfully",
                UserMapper.toDTO(found)
        );
    }

    // NOTE: accepts UserRegistrationDTO (carries the raw password) but always
    // returns UserDTO (never carries the password) — see each DTO's javadoc.
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserDTO> register(@RequestBody UserRegistrationDTO registrationDTO) {
        User user = buildEntityFromRegistrationDTO(registrationDTO);
        User saved = userService.register(user);
        return ApiResponse.of(
                "User created successfully",
                UserMapper.toDTO(saved)
        );
    }

    // NOTE: password is never changed here — UserService.update() doesn't
    // touch it, so this only re-links role/student/staff. Password changes
    // would need their own endpoint (re-hashing the new value).
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UserDTO> update(@PathVariable UUID id, @RequestBody UserRegistrationDTO registrationDTO) {
        User user = buildEntityFromRegistrationDTO(registrationDTO);
        user.setId(id);
        User updated = userService.update(user);
        return ApiResponse.of(
                "User updated successfully",
                UserMapper.toDTO(updated)
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        User user = new User();
        user.setId(id);
        userService.delete(user);
        return ApiResponse.of("User deleted successfully", null);
    }

    // loginId is the student's studentId (reg number) or the staff's staffId.
    // On success, starts a plain servlet-container session (no JWT here) so
    // logout below has something to invalidate.
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UserDTO> login(@RequestBody LoginRequestDTO loginRequest, HttpServletRequest httpRequest) {
        User user = userService.login(loginRequest.getLoginId(), loginRequest.getPassword());

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("userId", user.getId());
        session.setAttribute("role", user.getRole().name());

        return ApiResponse.of("Login successful", UserMapper.toDTO(user));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ApiResponse.of("Logout successful", null);
    }

    // resolves studentId/staffId -> actual, validated entities (throws 404 if missing),
    // and derives the required `username` column from whichever one is linked,
    // since UserRegistrationDTO doesn't ask the client for one separately.
    private User buildEntityFromRegistrationDTO(UserRegistrationDTO dto) {
        User user = new User();
        user.setPassword(dto.getPassword());
        user.setRole(dto.getRole());

        if (dto.getRole() == UserRole.STUDENT) {
            Student studentRef = new Student();
            studentRef.setId(dto.getStudentId());
            Student student = studentService.findById(studentRef);
            user.setStudent(student);
            user.setUsername(student.getStudentId());
        } else {
            Staff staffRef = new Staff();
            staffRef.setId(dto.getStaffId());
            Staff staff = staffService.findById(staffRef);
            user.setStaff(staff);
            user.setUsername(staff.getStaffId());
        }
        return user;
    }
}
