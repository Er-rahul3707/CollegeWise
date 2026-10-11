package com.collegewise.repository;
import com.collegewise.entity.*; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.data.jpa.repository.EntityGraph;
public interface FavoriteRepository extends JpaRepository<Favorite,Long>{@EntityGraph(attributePaths="college") List<Favorite> findByUser(User user); Optional<Favorite> findByUserAndCollege(User user,College college);void deleteByCollege(College college);}
