package auca.ac.rw.user.service;

import auca.ac.rw.exception.InvalidCredentialsException;
import auca.ac.rw.exception.ResourceNotFoundException;
import auca.ac.rw.user.domain.User;
import auca.ac.rw.user.domain.UserRole;
import auca.ac.rw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserServiceImplementation implements UserService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(User user) {
        validateRoleConsistency(user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public User update(User user) {
        User found = findById(user);
        found.setUsername(user.getUsername());
        found.setRole(user.getRole());
        found.setStudent(user.getStudent());
        found.setStaff(user.getStaff());
        validateRoleConsistency(found);
        return userRepository.save(found);
    }

    @Override
    public void delete(User user) {
        userRepository.delete(user);
    }

    @Override
    public User findById(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(()->new ResourceNotFoundException("User with id " + user.getId() + " not found"));
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User login(String loginId, String rawPassword) {
        User user = userRepository.findByStudent_StudentId(loginId)
                .or(()-> userRepository.findByStaff_StaffId(loginId))
                .orElseThrow(()-> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        return user;
    }

    // A STUDENT user must be linked to a Student (and not a Staff), and a
    // DEPARTMENT_OFFICER/ADMIN user must be linked to a Staff (and not a Student).
    // Catches an inconsistent account before it ever reaches login().
    private void validateRoleConsistency(User user) {
        if (user.getRole() == UserRole.STUDENT) {
            if (user.getStudent() == null) {
                throw new IllegalStateException("A STUDENT user must be linked to a Student");
            }
            if (user.getStaff() != null) {
                throw new IllegalStateException("A STUDENT user must not be linked to Staff");
            }
        } else {
            if (user.getStaff() == null) {
                throw new IllegalStateException(user.getRole() + " user must be linked to Staff");
            }
            if (user.getStudent() != null) {
                throw new IllegalStateException(user.getRole() + " user must not be linked to a Student");
            }
        }
    }
}
