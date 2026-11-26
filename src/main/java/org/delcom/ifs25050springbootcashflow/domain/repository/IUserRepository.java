package org.delcom.ifs25050springbootcashflow.domain.repository;

import org.delcom.ifs25050springbootcashflow.domain.entity.User;

import java.util.Optional;

public interface IUserRepository extends IRepository<User, Integer> {
    Optional<User> findByUsername(String username);

    User saveNew(String username, String passwordHash, String fullName, org.delcom.ifs25050springbootcashflow.domain.entity.Role role);
}