package auca.ac.rw.security;

import auca.ac.rw.TestcontainersConfiguration;
import auca.ac.rw.academic.domain.Academic;
import auca.ac.rw.academic.domain.AcademicLevel;
import auca.ac.rw.academic.repository.AcademicRepository;
import auca.ac.rw.clearance_request.domain.ClearanceRequest;
import auca.ac.rw.clearance_request.domain.ClearanceStatus;
import auca.ac.rw.clearance_request.repository.ClearanceRequestRepository;
import auca.ac.rw.department.domain.Department;
import auca.ac.rw.department.repository.DepartmentRepository;
import auca.ac.rw.staff.domain.Staff;
import auca.ac.rw.staff.repository.StaffRepository;
import auca.ac.rw.student.domain.Student;
import auca.ac.rw.student.repository.StudentRepository;
import auca.ac.rw.user.domain.User;
import auca.ac.rw.user.domain.UserRole;
import auca.ac.rw.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks of the JWT + RBAC setup through the real filter chain,
 * controllers and a throwaway Postgres. Every test builds its own data with
 * random suffixes, so tests don't depend on each other or on ordering.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityIntegrationTest {

    private static final String PASSWORD = "Secret#123";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UserRepository userRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired StaffRepository staffRepository;
    @Autowired AcademicRepository academicRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired ClearanceRequestRepository clearanceRequestRepository;

    private Department library;
    private Student student;
    private User studentUser;
    private User otherStudentUser;
    private User libraryOfficer;
    private User financeOfficer;
    private User admin;

    @BeforeEach
    void setUp() {
        String s = UUID.randomUUID().toString().substring(0, 8);

        Academic academicDept = new Academic();
        academicDept.setName("Software Engineering " + s);
        academicDept.setLevel(AcademicLevel.DEPARTMENT);
        academicDept.setRequiredCredits(0);
        academicDept = academicRepository.save(academicDept);

        library = department("Library " + s);
        Department finance = department("Finance " + s);

        student = student("STU-" + s, academicDept);
        studentUser = studentUser(student);
        otherStudentUser = studentUser(student("STU2-" + s, academicDept));

        libraryOfficer = staffUser("LIB-" + s, UserRole.DEPARTMENT_OFFICER, library);
        financeOfficer = staffUser("FIN-" + s, UserRole.DEPARTMENT_OFFICER, finance);
        admin = staffUser("ADM-" + s, UserRole.ADMIN);
    }

    // (a) no token on a protected endpoint -> 401 JSON, not an HTML page
    @Test
    void unauthenticatedRequestToProtectedEndpointIs401() throws Exception {
        mockMvc.perform(get("/api/v1/department"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void malformedTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/department").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    // (b) STUDENT on ADMIN-only endpoints -> 403 (method-level and URL-level rules)
    @Test
    void studentTokenIsForbiddenOnAdminOnlyEndpoints() throws Exception {
        String token = tokenFor(studentUser);

        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(delete("/api/v1/department/" + library.getId()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(admin))))
                .andExpect(status().isOk());
    }

    // (c) local login -> JWT -> that JWT authenticates later requests
    @Test
    void loginReturnsTokenThatAuthenticatesSubsequentRequests() throws Exception {
        String body = mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"" + student.getStudentId() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(studentUser.getId().toString()))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.tokenExpiresAt").exists())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.data.token");
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/v1/user/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(studentUser.getId().toString()))
                .andExpect(jsonPath("$.data.studentId").value(student.getId().toString()))
                .andExpect(jsonPath("$.data.token").doesNotExist());
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"" + student.getStudentId() + "\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCanReadOnlyTheirOwnRecords() throws Exception {
        String token = tokenFor(studentUser);

        mockMvc.perform(get("/api/v1/transcript/student/" + student.getId()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/student/" + student.getId()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        UUID otherStudentId = otherStudentUser.getStudent().getId();
        mockMvc.perform(get("/api/v1/transcript/student/" + otherStudentId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/clearance-request/student/" + otherStudentId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyTheAddressedDepartmentsOfficerCanReviewARequest() throws Exception {
        ClearanceRequest request = clearanceRequest(ClearanceStatus.PENDING);
        String approve = "{\"status\":\"APPROVED\",\"remarks\":\"ok\"}";

        mockMvc.perform(put("/api/v1/clearance-request/" + request.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(studentUser)))
                        .contentType(MediaType.APPLICATION_JSON).content(approve))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/clearance-request/" + request.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(financeOfficer)))
                        .contentType(MediaType.APPLICATION_JSON).content(approve))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/clearance-request/" + request.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(libraryOfficer)))
                        .contentType(MediaType.APPLICATION_JSON).content(approve))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    // (d) step-0 guard still holds behind the new auth: an APPROVED row is final,
    // even for a caller who is allowed to review it
    @Test
    void approvedRequestCannotBeChangedEvenByAuthorizedReviewer() throws Exception {
        ClearanceRequest request = clearanceRequest(ClearanceStatus.APPROVED);

        for (User reviewer : new User[]{libraryOfficer, admin}) {
            mockMvc.perform(put("/api/v1/clearance-request/" + request.getId() + "/status")
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(reviewer)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"PENDING\",\"remarks\":\"reopen\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("already-approved")));
        }
        assertThat(clearanceRequestRepository.findById(request.getId()).orElseThrow().getStatus())
                .isEqualTo(ClearanceStatus.APPROVED);
    }

    @Test
    void rejectedRequestCanStillBeReopened() throws Exception {
        ClearanceRequest request = clearanceRequest(ClearanceStatus.REJECTED);

        mockMvc.perform(put("/api/v1/clearance-request/" + request.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(libraryOfficer)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\",\"remarks\":\"resubmitted\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void logoutRevokesTheToken() throws Exception {
        String token = tokenFor(studentUser);

        mockMvc.perform(post("/api/v1/user/logout").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/user/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    // --- fixtures ---------------------------------------------------------

    private String tokenFor(User user) {
        return jwtService.issue(user).token();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private Department department(String name) {
        Department department = new Department();
        department.setName(name);
        return departmentRepository.save(department);
    }

    private Student student(String studentId, Academic academic) {
        Student s = new Student();
        s.setStudentId(studentId);
        s.setFirstName("Test");
        s.setLastName(studentId);
        s.setEmail(studentId.toLowerCase() + "@auca.ac.rw");
        s.setPhoneNumber(studentId);
        s.setAcademic(academic);
        return studentRepository.save(s);
    }

    private User studentUser(Student s) {
        User user = new User();
        user.setUsername(s.getStudentId());
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(UserRole.STUDENT);
        user.setStudent(s);
        return userRepository.save(user);
    }

    private User staffUser(String staffId, UserRole role, Department... departments) {
        Staff staff = new Staff();
        staff.setStaffId(staffId);
        staff.setFirstName("Test");
        staff.setLastName(staffId);
        staff.setDepartments(new HashSet<>(Set.of(departments)));
        staff = staffRepository.save(staff);

        User user = new User();
        user.setUsername(staffId);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setStaff(staff);
        return userRepository.save(user);
    }

    private ClearanceRequest clearanceRequest(ClearanceStatus status) {
        ClearanceRequest request = new ClearanceRequest();
        request.setStudent(student);
        request.setDepartment(library);
        request.setStatus(status);
        return clearanceRequestRepository.save(request);
    }
}
