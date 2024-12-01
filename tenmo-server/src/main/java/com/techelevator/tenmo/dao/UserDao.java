package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.model.RegisterUserDto;
import com.techelevator.tenmo.model.User;

import java.util.List;

public interface UserDao {

    List<User> getUsers();

    List<User> getUsersExcludingCurrent(int currentUserId);


    User getUserById(int id);

    User getUserByUsername(String username);

    User createUser(RegisterUserDto user);
}
