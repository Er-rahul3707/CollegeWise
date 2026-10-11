package com.collegewise.repository;
import com.collegewise.entity.*; import java.util.List; import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.data.jpa.repository.EntityGraph;
public interface RecommendationHistoryRepository extends JpaRepository<RecommendationHistory,Long>{@EntityGraph(attributePaths="college") List<RecommendationHistory> findTop50ByUserOrderByCreatedAtDesc(User user);void deleteByCollege(College college);}
