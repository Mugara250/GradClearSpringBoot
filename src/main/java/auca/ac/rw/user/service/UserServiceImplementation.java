package auca.ac.rw.user.service;

import auca.ac.rw.user.domain.User;
import auca.ac.rw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserServiceImplementation implements UserService{

    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
    @Override
    public User register(User user) {
//        theUser.setPassword(passwordEncoder.encode(theUser.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public User update(User user) {
        User found = findById(user);
        found.setUsername(user.getUsername());
        found.setRole(user.getRole());
        found.setStudent(user.getStudent());
        found.setStaff(user.getStaff());
        return userRepository.save(found);
    }

    @Override
    public void delete(User user) {
        userRepository.delete(user);
    }

    @Override
    public User findById(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(()->new RuntimeException("User with id " + user.getId() + " not found"));
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User login(String loginId, String rawPassword) {
        User user = userRepository.findByStudent_StudentId(loginId)
                .or(()-> userRepository.findByStaff_StaffId(loginId))
                .orElseThrow(()-> new RuntimeException("Invalid credentials"));

//        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
//            throw new RuntimeException("Invalid credentials");
//        }
        return user;
    }
}
