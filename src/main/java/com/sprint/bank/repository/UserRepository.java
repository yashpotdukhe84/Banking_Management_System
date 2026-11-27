package com.sprint.bank.repository;

import com.sprint.bank.shared.entity.User;
import com.sprint.bank.shared.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data JPA generates the SQL from the method names - always parameterised, so no SQL injection. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);
}
