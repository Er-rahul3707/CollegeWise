package com.collegewise.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

@Entity
@Table(name="users")
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=120) private String name;
    @Column(nullable=false, unique=true, length=190) private String email;
    @Column(nullable=false) private String password;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Role role=Role.STUDENT;
    @Column(nullable=false) private boolean active=true;
    @Column(nullable=false, updatable=false) private Instant createdAt=Instant.now();
    @Column(nullable=false) private Instant updatedAt=Instant.now();
    @PreUpdate void touch(){updatedAt=Instant.now();}
    protected User() {}
    public User(String name,String email,String password,Role role){this.name=name;this.email=email;this.password=password;this.role=role;}
    public Long getId(){return id;} public String getName(){return name;} public String getEmail(){return email;}
    @JsonIgnore public String getPassword(){return password;} public Role getRole(){return role;} public boolean isActive(){return active;}
    public Instant getCreatedAt(){return createdAt;} public void setActive(boolean active){this.active=active;}
}
