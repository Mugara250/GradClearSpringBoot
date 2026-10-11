package auca.ac.rw.controller;

import auca.ac.rw.response.ApiResponse;
import auca.ac.rw.security.AuthenticatedUser;
import auca.ac.rw.security.AuthorizationService;
import auca.ac.rw.security.JwtService;
import auca.ac.rw.security.TokenBlocklist;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final JwtService jwtService;
    private final TokenBlocklist tokenBlocklist;
    private final AuthorizationService authorizationService;

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserDTO>> findAll() {
        List<UserDTO> users = userService.findAll().stream()
                .map(UserMapper::toDTO)
                .toList();
        return ApiResponse.of(
                "Users retrieved successfully",
                users
        );
    }

    // who the bearer of the token is — what a frontend calls on page load /
    // refresh to recover identity, since there is no server-side session
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UserDTO> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        User user = new User();
        user.setId(principal.userId());
        return ApiResponse.of(
                "Current user retrieved successfully",
                withCapabilities(UserMapper.toDTO(userService.findById(user)))
        );
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelfUser(#id)")
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
    // ADMIN-only: the body picks the role, so open registration would let
    // anyone mint an ADMIN account. The very first admin comes from
    // AdminBootstrap instead.
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        User user = new User();
        user.setId(id);
        userService.delete(user);
        return ApiResponse.of("User deleted successfully", null);
    }

    // loginId is the student's studentId (reg number) or the staff's staffId.
    // Credential checking stays in UserService.login(); on success this hands
    // back a signed JWT (UserDTO.token) instead of starting a session. The
    // client sends it as `Authorization: Bearer <token>` from then on.
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UserDTO> login(@RequestBody LoginRequestDTO loginRequest) {
        User user = userService.login(loginRequest.getLoginId(), loginRequest.getPassword());
        JwtService.IssuedToken issuedToken = jwtService.issue(user);
        return ApiResponse.of("Login successful", withCapabilities(UserMapper.toDTO(user, issuedToken)));
    }

    // what the current user may do beyond their role, for the UI to show
    // the right screens (the server still checks every request itself)
    private UserDTO withCapabilities(UserDTO dto) {
        dto.setRegistrar(authorizationService.isRegistrar(dto.getRole(), dto.getStaffId()));
        return dto;
    }

    // JWTs are self-contained, so "logging out" is mostly the client throwing
    // its token away. To make it also stop working server-side, the token's
    // id (jti) goes on TokenBlocklist until the token would have expired —
    // see TokenBlocklist for the in-memory trade-off.
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> logout(@AuthenticationPrincipal AuthenticatedUser principal) {
        tokenBlocklist.revoke(principal.tokenId(), principal.expiresAt());
        return ApiResponse.of("Logout successful", null);
    }

    // resolves studentId/staffId -> actual, validated entities (throws 404 if missing),
    // and derives the required `username` column from whichever one is linked,
    // since UserRegistrationDTO doesn't ask the client for one separately.
    private User buildEntityFromRegistrationDTO(UserRegistrationDTO dto) {
        User user = new User();
        user.setPassword(dto.getPassword());
        user.setRole(dto.getRole());
        user.setEmail(dto.getEmail() == null || dto.getEmail().isBlank() ? null : dto.getEmail().trim());

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
