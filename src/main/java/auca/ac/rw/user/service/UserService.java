package auca.ac.rw.user.service;

import auca.ac.rw.user.domain.User;

import java.util.List;

public interface UserService {
    User register(User user);
    User update(User user);
    void delete(User user);
    User findById(User user);
    List<User> findAll();

    User login(String loginId, String rawPassword);
}
