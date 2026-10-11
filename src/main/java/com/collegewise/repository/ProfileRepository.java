package com.collegewise.repository;
import com.collegewise.entity.*; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
public interface ProfileRepository extends JpaRepository<StudentProfile,Long>{Optional<StudentProfile> findByUser(User user);}
