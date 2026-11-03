package com.digimate.repository;

import com.digimate.entity.User;
import com.digimate.entity.enums.HelperStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    List<User> findByHelperStatus(HelperStatus status);

}
