package com.ninelives.safepaws.repository;

import com.ninelives.safepaws.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Pass the email already trimmed and in lowercase, as it is stored. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
