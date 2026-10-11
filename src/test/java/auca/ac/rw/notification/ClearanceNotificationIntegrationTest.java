package auca.ac.rw.notification;

import auca.ac.rw.TestcontainersConfiguration;
import auca.ac.rw.academic.domain.Academic;
import auca.ac.rw.academic.domain.AcademicLevel;
import auca.ac.rw.academic.repository.AcademicRepository;
import auca.ac.rw.clearance_request.domain.ClearanceRequest;
import auca.ac.rw.clearance_request.repository.ClearanceRequestRepository;
import auca.ac.rw.department.domain.Department;
import auca.ac.rw.department.repository.DepartmentRepository;
import auca.ac.rw.security.JwtService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A clearance decision made through the API reaches the student by email and
 * SMS: API -> ClearanceEventPublisher -> RabbitMQ -> listeners -> MailHog
 * (real SMTP) / recording SmsSender. Real broker, real mail server.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ClearanceNotificationIntegrationTest.RecordingSms.class})
class ClearanceNotificationIntegrationTest {

    /** Replaces the logging SMS sender so tests can see what was "texted". */
    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingSms {
        static final List<String[]> SENT = new CopyOnWriteArrayList<>();

        @Bean
        @Primary
        SmsSender recordingSmsSender() {
            return (phone, text) -> SENT.add(new String[]{phone, text});
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired StaffRepository staffRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired AcademicRepository academicRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired ClearanceRequestRepository clearanceRequestRepository;

    @Value("${" + TestcontainersConfiguration.MAILHOG_API_PROPERTY + "}")
    String mailhogApi;

    private final HttpClient http = HttpClient.newHttpClient();
    private String suffix;
    private String adminToken;
    private Academic academicDepartment;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);

        Academic dept = new Academic();
        dept.setName("Notif Dept " + suffix);
        dept.setLevel(AcademicLevel.DEPARTMENT);
        academicDepartment = academicRepository.save(dept);

        Staff staff = new Staff();
        staff.setStaffId("NADM-" + suffix);
        staff.setFirstName("Admin");
        staff.setLastName(suffix);
        staff = staffRepository.save(staff);
        User admin = new User();
        admin.setUsername(staff.getStaffId());
        admin.setPassword("unused");
        admin.setRole(UserRole.ADMIN);
        admin.setStaff(staff);
        adminToken = jwtService.issue(userRepository.save(admin)).token();
    }

    @Test
    void rejectionIsEmailedAndTextedWithTheReason() throws Exception {
        String email = "reject-" + suffix + "@student.test";
        Student student = student(email, "0788" + suffix.substring(0, 6).replaceAll("[^0-9]", "1"));
        ClearanceRequest library = request(student, "Library " + suffix);
        request(student, "Finance " + suffix);

        decide(library, "REJECTED", "Two books not returned");

        String json = awaitEmailTo(email, 1);
        assertThat((String) JsonPath.read(json, "$.items[0].Content.Headers.Subject[0]"))
                .isEqualTo("GradClear: Library " + suffix + " rejected your clearance");
        String body = body(json, 0);
        assertThat(body).contains("Dear Test Student", "has rejected", "Reason: Two books not returned", "0 of 2 departments");

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(smsTo(student)).anyMatch(text -> text.contains("REJECTED") && text.contains("Two books not returned")));
    }

    @Test
    void lastApprovalTellsTheStudentTheyAreFullyCleared() throws Exception {
        String email = "approve-" + suffix + "@student.test";
        Student student = student(email, null);
        ClearanceRequest library = request(student, "Library " + suffix);
        ClearanceRequest finance = request(student, "Finance " + suffix);

        decide(library, "APPROVED", null);
        awaitEmailTo(email, 1);
        decide(finance, "APPROVED", "All fees paid");

        String json = awaitEmailTo(email, 2);
        List<String> subjects = JsonPath.read(json, "$.items[*].Content.Headers.Subject[0]");
        assertThat(subjects).contains(
                "GradClear: Library " + suffix + " approved your clearance",
                "GradClear: you are fully cleared for graduation");
    }

    @Test
    void studentWithoutContactDetailsIsSkippedAndTheDecisionStillSaves() throws Exception {
        Student student = student(null, null);
        ClearanceRequest library = request(student, "Library " + suffix);

        decide(library, "APPROVED", null);

        Thread.sleep(1500); // give the listeners time to (not) send
        assertThat(smsTo(student)).isEmpty();
        assertThat(clearanceRequestRepository.findById(library.getId()).orElseThrow().getStatus().name())
                .isEqualTo("APPROVED");
    }

    // --- helpers ----------------------------------------------------------

    private void decide(ClearanceRequest request, String status, String remarks) throws Exception {
        String body = "{\"status\":\"" + status + "\",\"remarks\":" + (remarks == null ? "null" : "\"" + remarks + "\"") + "}";
        mockMvc.perform(put("/api/v1/clearance-request/" + request.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private String awaitEmailTo(String email, int count) {
        String[] last = new String[1];
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(300)).untilAsserted(() -> {
            HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(
                    mailhogApi + "/api/v2/search?kind=to&query=" + URLEncoder.encode(email, StandardCharsets.UTF_8))).build(),
                    HttpResponse.BodyHandlers.ofString());
            last[0] = response.body();
            assertThat((Integer) JsonPath.read(last[0], "$.total")).isGreaterThanOrEqualTo(count);
        });
        return last[0];
    }

    // undo quoted-printable soft line breaks if the mail was encoded that way
    private static String body(String json, int index) {
        String raw = JsonPath.read(json, "$.items[" + index + "].Content.Body");
        return raw.replace("=\r\n", "").replace("\r\n", "\n");
    }

    private List<String> smsTo(Student student) {
        String phone = student.getPhoneNumber() == null ? null : "+250" + student.getPhoneNumber().substring(1);
        return RecordingSms.SENT.stream().filter(s -> s[0].equals(phone)).map(s -> s[1]).toList();
    }

    private Student student(String email, String phone) {
        Student s = new Student();
        s.setStudentId("NST-" + suffix);
        s.setFirstName("Test");
        s.setLastName("Student");
        s.setEmail(email);
        s.setPhoneNumber(phone);
        s.setAcademic(academicDepartment);
        return studentRepository.save(s);
    }

    private ClearanceRequest request(Student student, String departmentName) {
        Department d = new Department();
        d.setName(departmentName);
        d = departmentRepository.save(d);
        ClearanceRequest r = new ClearanceRequest();
        r.setStudent(student);
        r.setDepartment(d);
        return clearanceRequestRepository.save(r);
    }
}
