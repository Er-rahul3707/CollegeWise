package com.collegewise.repository;
import com.collegewise.entity.User; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<User,Long>{Optional<User> findByEmailIgnoreCase(String email); boolean existsByEmailIgnoreCase(String email);long countByRole(com.collegewise.entity.Role role);}
