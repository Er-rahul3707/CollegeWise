package com.collegewise.controller;

import com.collegewise.dto.ApiDtos.*;
import com.collegewise.entity.*;
import com.collegewise.repository.*;
import com.collegewise.service.RecommendationService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final UserRepository users; private final ProfileRepository profiles;
    private final RecommendationService recommendations; private final FavoriteRepository favorites;
    private final CollegeRepository colleges; private final RecommendationHistoryRepository history;
    public StudentController(UserRepository users,ProfileRepository profiles,RecommendationService recommendations,
                             FavoriteRepository favorites,CollegeRepository colleges,RecommendationHistoryRepository history){
        this.users=users;this.profiles=profiles;this.recommendations=recommendations;this.favorites=favorites;this.colleges=colleges;this.history=history;
    }
    private User current(Authentication authentication){return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();}
    private StudentProfile profile(User user){return profiles.findByUser(user).orElseGet(()->profiles.save(new StudentProfile(user)));}

    @GetMapping("/profile") ProfileResponse getProfile(Authentication a){return ProfileResponse.from(profile(current(a)));}
    @PutMapping("/profile") ProfileResponse updateProfile(@Valid @RequestBody ProfileUpdate r,Authentication a){
        StudentProfile p=profile(current(a));p.update(r.phone(),r.tenthPercentage(),r.twelfthPercentage(),r.entranceExam(),r.examYear(),r.rank(),r.percentile(),r.category(),r.preferredCourse(),r.preferredBranch(),r.preferredState(),r.preferredCity(),r.maximumBudget(),r.hostelRequired(),r.careerInterest());
        return ProfileResponse.from(profiles.save(p));
    }
    @GetMapping("/recommendations") List<Recommendation> recommend(Authentication a){return recommendations.recommend(a.getName());}
    @GetMapping("/recommendations/history") List<HistoryResponse> recommendationHistory(Authentication a){return history.findTop50ByUserOrderByCreatedAtDesc(current(a)).stream().map(HistoryResponse::from).toList();}
    @GetMapping("/dashboard") Map<String,Object> dashboard(Authentication a){User u=current(a);return Map.of("favoriteCount",favorites.findByUser(u).size(),"recentRecommendations",history.findTop50ByUserOrderByCreatedAtDesc(u).size(),"profileComplete",profiles.findByUser(u).map(p->p.getPreferredCourse()!=null&&p.getMaximumBudget()!=null).orElse(false));}
    @GetMapping("/career-guidance") Map<String,String> careerGuidance(Authentication a){StudentProfile p=profile(current(a));String interest=p.getCareerInterest()==null?"":p.getCareerInterest().toLowerCase(Locale.ROOT);String guidance=interest.contains("data")?"Explore statistics, programming, and data visualization. Compare data science, computer science, and mathematics programs.":interest.contains("design")?"Build a portfolio and compare product design, UX, and interaction design programs.":interest.contains("business")?"Compare business, economics, and management programs; look for internships and case competitions.":"Set your career interest in your profile. Compare course curricula, internships, and verified placement reports.";return Map.of("careerInterest",p.getCareerInterest()==null?"Not set":p.getCareerInterest(),"guidance",guidance);}
    @GetMapping("/favorites") List<CollegeResponse> favoriteList(Authentication a){return favorites.findByUser(current(a)).stream().map(Favorite::getCollege).map(CollegeResponse::from).toList();}
    @PostMapping("/favorites/{collegeId}") CollegeResponse addFavorite(@PathVariable Long collegeId,Authentication a){User u=current(a);College c=findCollege(collegeId);favorites.findByUserAndCollege(u,c).orElseGet(()->favorites.save(new Favorite(u,c)));return CollegeResponse.from(c);}
    @DeleteMapping("/favorites/{collegeId}") @ResponseStatus(HttpStatus.NO_CONTENT) void removeFavorite(@PathVariable Long collegeId,Authentication a){User u=current(a);College c=findCollege(collegeId);favorites.findByUserAndCollege(u,c).ifPresent(favorites::delete);}
    @GetMapping("/compare") List<CollegeResponse> compare(@RequestParam List<Long> ids){List<Long> distinct=ids.stream().distinct().toList();if(distinct.size()<2||distinct.size()>3)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Compare between 2 and 3 distinct colleges");return distinct.stream().map(this::findCollege).map(CollegeResponse::from).toList();}
    private College findCollege(Long id){return colleges.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"College not found: "+id));}
}
