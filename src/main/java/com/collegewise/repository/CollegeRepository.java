package com.collegewise.repository;
import com.collegewise.entity.College; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
public interface CollegeRepository extends JpaRepository<College,Long>{
 java.util.Optional<College> findByNameIgnoreCase(String name);
 @Query("select c from College c where (:q is null or lower(c.name) like lower(concat('%',:q,'%')) or lower(c.city) like lower(concat('%',:q,'%')) or lower(c.state) like lower(concat('%',:q,'%'))) and (:state is null or lower(c.state)=lower(:state)) and (:city is null or lower(c.city)=lower(:city)) and (:maxFees is null or c.fees<=:maxFees) and (:hostel is null or c.hostelAvailable=:hostel)")
 Page<College> search(@Param("q") String q,@Param("state") String state,@Param("city") String city,@Param("maxFees") Double maxFees,@Param("hostel") Boolean hostel,Pageable pageable);
}
