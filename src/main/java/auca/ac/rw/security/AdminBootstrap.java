package auca.ac.rw.security;

import auca.ac.rw.staff.domain.Staff;
import auca.ac.rw.staff.repository.StaffRepository;
import auca.ac.rw.user.domain.User;
import auca.ac.rw.user.domain.UserRole;
import auca.ac.rw.user.repository.UserRepository;
import auca.ac.rw.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class AdminBootstrap.
 * Creating users is ADMIN-only, so an empty database would have no way in.
 * On startup, if the users table is empty AND an admin password has been
 * supplied (GRADCLEAR_ADMIN_PASSWORD), this creates one Staff record plus an
 * ADMIN user linked to it. It never runs once any user exists, and does
 * nothing when no password is configured.
 *
 * @version 1.0
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final UserService userService;

    @Value("${gradclear.bootstrap.admin-staff-id:STF-ADMIN-0001}")
    private String adminStaffId;

    @Value("${gradclear.bootstrap.admin-password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminPassword.isBlank() || userRepository.count() > 0) {
            return;
        }

        Staff staff = staffRepository.findStaffByStaffId(adminStaffId).orElseGet(() -> {
            Staff created = new Staff();
            created.setStaffId(adminStaffId);
            created.setFirstName("System");
            created.setLastName("Administrator");
            created.setPosition("Administrator");
            return staffRepository.save(created);
        });

        User admin = new User();
        admin.setUsername(staff.getStaffId());
        admin.setPassword(adminPassword);   // hashed by UserService.register()
        admin.setRole(UserRole.ADMIN);
        admin.setStaff(staff);
        userService.register(admin);

        log.warn("No users existed: bootstrapped ADMIN account with loginId '{}'. "
                + "Unset GRADCLEAR_ADMIN_PASSWORD now that it exists.", adminStaffId);
    }
}
