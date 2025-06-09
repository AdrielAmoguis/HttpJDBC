package io.github.adrielamoguis.utils.http_jdbc_driver.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "users", schema = "public")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private long userId;

    @Column(name = "user_name")
    private String name;
}