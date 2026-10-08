package com.timemanagement.service;

import com.timemanagement.dao.UserDAO;
import com.timemanagement.exception.AuthenticationException;
import com.timemanagement.exception.DatabaseOperationException;
import com.timemanagement.model.User;

import java.util.Optional;

/**
 * Service managing user authentication and active session.
 */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User login(String email, String password) throws AuthenticationException, DatabaseOperationException {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new AuthenticationException("Email and password must not be empty.");
        }

        Optional<User> userOpt = userDAO.authenticate(email.trim(), password);
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Invalid email or password. Please try again.");
        }

        User user = userOpt.get();
        SessionManager.getInstance().login(user);
        return user;
    }

    public void logout() {
        SessionManager.getInstance().logout();
    }

    public User getCurrentUser() {
        return SessionManager.getInstance().getCurrentUser();
    }
}
