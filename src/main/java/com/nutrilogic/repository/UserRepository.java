package com.nutrilogic.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nutrilogic.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    // Custom query method
    User findByUsername(String username);
}