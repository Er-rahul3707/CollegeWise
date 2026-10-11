package com.collegewise.repository;
import com.collegewise.entity.Course; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
public interface CourseRepository extends JpaRepository<Course,Long>{Optional<Course> findByNameIgnoreCase(String name);}
