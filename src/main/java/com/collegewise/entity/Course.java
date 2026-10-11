package com.collegewise.entity;
import jakarta.persistence.*;
@Entity @Table(uniqueConstraints=@UniqueConstraint(columnNames={"name"}))
public class Course { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,length=100) private String name; @Column(length=500) private String description; protected Course(){} public Course(String name,String description){this.name=name;this.description=description;} public Long getId(){return id;} public String getName(){return name;} public String getDescription(){return description;} public void updateFrom(Course c){this.name=c.name;this.description=c.description;} }
