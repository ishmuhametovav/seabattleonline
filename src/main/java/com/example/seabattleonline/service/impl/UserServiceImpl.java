package com.example.seabattleonline.service.impl;

import com.example.seabattleonline.domain.exception.ResourceNotFoundException;
import com.example.seabattleonline.domain.user.Role;
import com.example.seabattleonline.domain.user.User;
import com.example.seabattleonline.repository.UserRepository;
import com.example.seabattleonline.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public User create(final User user) {
        if (userRepository.findByName(user.getName()).isPresent()) {
            throw new IllegalStateException(
                    "User with name " + user.getName() + " already exists"
            );
        }
        if (!user.getPassword().equals(user.getPasswordConfirmation())) {
            throw new IllegalStateException("Passwords don't match");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.ROLE_USER);
        return userRepository.save(user);
    }

    @Transactional
    @Override
    public User getById(final Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + id + " not found")
                );
    }

    @Override
    public User getByName(final String name) {
        return userRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with name " + name + " not found")
                );
    }

    @Transactional
    @Override
    public User update(final User user) {
        if (userRepository.findById(user.getId()).isEmpty()) {
            throw new ResourceNotFoundException(
                    "User with id " + user.getId() + " not found"
            );
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    @Override
    public void deleteById(final Long id) {
        userRepository.deleteById(id);
    }
}
