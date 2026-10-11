package com.collegewise.entity;
import jakarta.persistence.*;
@Entity @Table(uniqueConstraints=@UniqueConstraint(columnNames={"user_id","college_id"}))
public class Favorite { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY,optional=false) private User user; @ManyToOne(fetch=FetchType.LAZY,optional=false) private College college; protected Favorite(){} public Favorite(User user,College college){this.user=user;this.college=college;} public Long getId(){return id;} public User getUser(){return user;} public College getCollege(){return college;} }
