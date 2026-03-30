package org.code.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.code.api.domain.models.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserRepository extends JpaRepository<User,UUID> {

    Optional<User> findbyEmail(String email);

    boolean verifyEmail(String email);
    
}
