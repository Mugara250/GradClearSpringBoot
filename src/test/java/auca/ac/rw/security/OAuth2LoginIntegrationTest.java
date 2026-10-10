package auca.ac.rw.security;

import auca.ac.rw.TestcontainersConfiguration;
import auca.ac.rw.academic.domain.Academic;
import auca.ac.rw.academic.domain.AcademicLevel;
import auca.ac.rw.academic.repository.AcademicRepository;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Google sign-in with dummy client credentials. The real round-trip to Google
 * can't run in a test, so this checks the two ends we own: the redirect to
 * Google is wired up, and OAuth2LoginSuccessHandler turns a (simulated)
 * Google identity into the same JWT the local login issues — or refuses it.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-client-secret",
        "spring.security.oauth2.client.registration.google.scope[0]=openid",
        "spring.security.oauth2.client.registration.google.scope[1]=email",
        "spring.security.oauth2.client.registration.google.scope[2]=profile"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OAuth2LoginIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired OAuth2LoginSuccessHandler successHandler;
    @Autowired UserRepository userRepository;
    @Autowired StaffRepository staffRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired AcademicRepository academicRepository;

    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void authorizationEndpointRedirectsToGoogle() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("https://accounts.google.com/")));
    }

    // enabling oauth2Login must not turn API 401s into redirects to a login page
    @Test
    void apiStillAnswers401JsonWithOAuth2Enabled() throws Exception {
        mockMvc.perform(get("/api/v1/department"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void preLinkedStaffEmailGetsAWorkingJwt() throws Exception {
        String email = "officer-" + suffix + "@auca.ac.rw";
        User officer = staffUser(email);

        MockHttpServletResponse response = googleLogin(email, true);

        assertThat(response.getStatus()).isEqualTo(200);
        String token = JsonPath.read(response.getContentAsString(), "$.data.token");
        mockMvc.perform(get("/api/v1/user/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(officer.getId().toString()))
                .andExpect(jsonPath("$.data.role").value("DEPARTMENT_OFFICER"));
    }

    @Test
    void studentIsMatchedThroughTheirStudentRecordEmail() throws Exception {
        String email = "student-" + suffix + "@auca.ac.rw";
        User studentUser = studentUser(email);

        MockHttpServletResponse response = googleLogin(email.toUpperCase(), true);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(response.getContentAsString(), "$.data.id"))
                .isEqualTo(studentUser.getId().toString());
    }

    @Test
    void unknownEmailIsRefusedAndNoUserIsCreated() throws Exception {
        long usersBefore = userRepository.count();

        MockHttpServletResponse response = googleLogin("stranger-" + suffix + "@gmail.com", true);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("No GradClear account is linked");
        assertThat(userRepository.count()).isEqualTo(usersBefore);
    }

    @Test
    void unverifiedEmailIsRefused() throws Exception {
        String email = "unverified-" + suffix + "@auca.ac.rw";
        staffUser(email);

        assertThat(googleLogin(email, false).getStatus()).isEqualTo(401);
    }

    // --- fixtures ---------------------------------------------------------

    private MockHttpServletResponse googleLogin(String email, boolean verified) throws Exception {
        DefaultOAuth2User googleUser = new DefaultOAuth2User(
                AuthorityUtils.createAuthorityList("OAUTH2_USER"),
                Map.of("sub", "google-" + suffix, "email", email, "email_verified", verified),
                "sub");
        OAuth2AuthenticationToken authentication =
                new OAuth2AuthenticationToken(googleUser, googleUser.getAuthorities(), "google");

        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);
        return response;
    }

    private User staffUser(String email) {
        Staff staff = new Staff();
        staff.setStaffId("OFF-" + suffix);
        staff.setFirstName("Test");
        staff.setLastName("Officer");
        staff = staffRepository.save(staff);

        User user = new User();
        user.setUsername(staff.getStaffId());
        user.setPassword("unused");
        user.setRole(UserRole.DEPARTMENT_OFFICER);
        user.setStaff(staff);
        user.setEmail(email);
        return userRepository.save(user);
    }

    private User studentUser(String studentEmail) {
        Academic academic = new Academic();
        academic.setName("Dept " + suffix);
        academic.setLevel(AcademicLevel.DEPARTMENT);
        academic = academicRepository.save(academic);

        Student student = new Student();
        student.setStudentId("STU-" + suffix);
        student.setFirstName("Test");
        student.setLastName("Student");
        student.setEmail(studentEmail);
        student.setPhoneNumber("P-" + suffix);
        student.setAcademic(academic);
        student = studentRepository.save(student);

        User user = new User();
        user.setUsername(student.getStudentId());
        user.setPassword("unused");
        user.setRole(UserRole.STUDENT);
        user.setStudent(student);
        return userRepository.save(user);
    }
}
