package io.github.adrielamoguis.utils.http_jdbc_driver.repositories;

import io.github.adrielamoguis.utils.http_jdbc_driver.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
