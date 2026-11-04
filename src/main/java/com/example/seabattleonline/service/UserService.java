package com.example.seabattleonline.service;

import com.example.seabattleonline.domain.user.User;

public interface UserService {
    User create(User user);

    User getById(Long id);

    User getByName(String name);

    User update(User user);

    void deleteById(Long id);
}
