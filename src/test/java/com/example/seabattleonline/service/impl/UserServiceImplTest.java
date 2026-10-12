package com.example.seabattleonline.service.impl;

import com.example.seabattleonline.domain.exception.ResourceNotFoundException;
import com.example.seabattleonline.domain.user.Role;
import com.example.seabattleonline.domain.user.User;
import com.example.seabattleonline.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void create() {
        String password = "password";
        User user = new User();
        user.setName("test");
        user.setPassword(password);
        user.setPasswordConfirmation(password);
        Mockito.when(userRepository.findByName(user.getName()))
                .thenReturn(Optional.empty());
        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        User testUser = userService.create(user);
        Mockito.verify(passwordEncoder).encode(password);
        Mockito.verify(userRepository).save(testUser);
        Assertions.assertEquals(Role.ROLE_USER, testUser.getRole());
    }

    @Test
    void createWithExistingUser() {
        User user = new User();
        user.setName("test");
        user.setPassword("password");
        user.setPasswordConfirmation("password");
        Mockito.when(userRepository.findByName(user.getName()))
                .thenReturn(Optional.of(user));
        Assertions.assertThrows(IllegalStateException.class, () -> {
            userService.create(user);
        });
        Mockito.verify(userRepository, Mockito.never()).save(user);
    }

    @Test
    void getById() {
        User user = new User();
        Long id = 1L;
        user.setId(id);
        Mockito.when(userRepository.findById(id))
                .thenReturn(Optional.of(user));
        User result = userService.getById(id);
        Assertions.assertEquals(user, result);
    }

    @Test
    void getByIdWithNotExistingId() {
        Long id = 1L;
        Mockito.when(userRepository.findById(id))
                .thenReturn(Optional.empty());
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            userService.getById(id);
        });
    }

    @Test
    void getByName() {
        User user = new User();
        String name = "name";
        user.setName(name);
        Mockito.when(userRepository.findByName(name))
                .thenReturn(Optional.of(user));
        User result = userService.getByName(name);
        Assertions.assertEquals(user, result);
    }

    @Test
    void getByNameWithNotExistingName() {
        String name = "name";
        Mockito.when(userRepository.findByName(name))
                .thenReturn(Optional.empty());
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            userService.getByName(name);
        });
    }

    @Test
    void update() {
        Long id = 1L;
        String password = "password";
        User user = new User();
        user.setId(id);
        user.setPassword(password);
        Mockito.when(userRepository.findById(id))
                .thenReturn(Optional.of(user));
        User result = userService.update(user);
        Mockito.verify(passwordEncoder).encode(password);
        Mockito.verify(userRepository).save(user);
    }

    @Test
    void updateWithoutExistingUser() {
        Long id = 1L;
        User user = new User();
        user.setId(id);
        Mockito.when(userRepository.findById(id))
                .thenReturn(Optional.empty());
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            userService.update(user);
        });
    }

    @Test
    void deleteById() {
        Long id = 1L;
        userService.deleteById(id);
        Mockito.verify(userRepository).deleteById(id);
    }
}
