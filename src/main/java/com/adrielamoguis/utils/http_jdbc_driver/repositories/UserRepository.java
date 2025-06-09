package com.adrielamoguis.utils.http_jdbc_driver.repositories;

import com.adrielamoguis.utils.http_jdbc_driver.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
