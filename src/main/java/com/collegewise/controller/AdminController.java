package com.collegewise.controller;

import com.collegewise.dto.ApiDtos.*;
import com.collegewise.entity.*;
import com.collegewise.repository.*;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin")
@Transactional
public class AdminController {
    private final UserRepository users;
    private final CollegeRepository colleges;
    private final CourseRepository courses;
    private final BranchRepository branches;
    private final CutoffRepository cutoffs;
    private final PlacementRepository placements;
    private final FavoriteRepository favorites;
    private final RecommendationHistoryRepository recommendationHistory;

    public AdminController(UserRepository users, CollegeRepository colleges, CourseRepository courses,
                           BranchRepository branches, CutoffRepository cutoffs, PlacementRepository placements,
                           FavoriteRepository favorites,RecommendationHistoryRepository recommendationHistory) {
        this.users=users; this.colleges=colleges; this.courses=courses; this.branches=branches;
        this.cutoffs=cutoffs; this.placements=placements;this.favorites=favorites;this.recommendationHistory=recommendationHistory;
    }

    @GetMapping("/dashboard")
    Map<String,Long> dashboard() {
        return Map.of("students",users.countByRole(Role.STUDENT),"colleges",colleges.count(),
                "courses",courses.count(),"branches",branches.count(),"cutoffs",cutoffs.count(),
                "placements",placements.count(),"favorites",favorites.count(),"recommendations",recommendationHistory.count());
    }

    @GetMapping("/courses") Object listCourses(){return courses.findAll().stream().map(CourseResponse::from).toList();}
    @PostMapping("/courses") @ResponseStatus(HttpStatus.CREATED)
    Object createCourse(@RequestBody Map<String,String> r){return CourseResponse.from(courses.save(new Course(text(r,"name",100,true),text(r,"description",500,false))));}
    @PutMapping("/courses/{id}") Object updateCourse(@PathVariable Long id,@RequestBody Map<String,String> r){
        Course c=courses.findById(id).orElseThrow(()->notFound("Course"));c.updateFrom(new Course(text(r,"name",100,true),text(r,"description",500,false)));return CourseResponse.from(c);
    }
    @DeleteMapping("/courses/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteCourse(@PathVariable Long id){Course c=courses.findById(id).orElseThrow(()->notFound("Course"));cutoffs.deleteByCourse(c);placements.deleteByCourse(c);courses.delete(c);}

    @GetMapping("/branches") Object listBranches(){return branches.findAll().stream().map(BranchResponse::from).toList();}
    @PostMapping("/branches") @ResponseStatus(HttpStatus.CREATED)
    Object createBranch(@RequestBody Map<String,String> r){return BranchResponse.from(branches.save(branchFrom(r)));}
    @PutMapping("/branches/{id}") Object updateBranch(@PathVariable Long id,@RequestBody Map<String,String> r){
        Branch b=branches.findById(id).orElseThrow(()->notFound("Branch"));b.updateFrom(branchFrom(r));return BranchResponse.from(b);
    }
    @DeleteMapping("/branches/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteBranch(@PathVariable Long id){Branch b=branches.findById(id).orElseThrow(()->notFound("Branch"));cutoffs.deleteByBranch(b);branches.delete(b);}

    @GetMapping("/cutoffs") Object listCutoffs(){return cutoffs.findAll().stream().map(CutoffResponse::from).toList();}
    @PostMapping("/cutoffs") @ResponseStatus(HttpStatus.CREATED) Object createCutoff(@RequestBody Map<String,String> r){return CutoffResponse.from(cutoffs.save(cutoffFrom(r)));}
    @PutMapping("/cutoffs/{id}") Object updateCutoff(@PathVariable Long id,@RequestBody Map<String,String> r){
        Cutoff c=cutoffs.findById(id).orElseThrow(()->notFound("Cutoff"));c.updateFrom(cutoffFrom(r));return CutoffResponse.from(c);
    }
    @DeleteMapping("/cutoffs/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteCutoff(@PathVariable Long id){cutoffs.deleteById(id);}

    @GetMapping("/placements") Object listPlacements(){return placements.findAll().stream().map(PlacementResponse::from).toList();}
    @PostMapping("/placements") @ResponseStatus(HttpStatus.CREATED) Object createPlacement(@RequestBody Map<String,String> r){return PlacementResponse.from(placements.save(placementFrom(r)));}
    @PutMapping("/placements/{id}") Object updatePlacement(@PathVariable Long id,@RequestBody Map<String,String> r){
        Placement p=placements.findById(id).orElseThrow(()->notFound("Placement"));p.updateFrom(placementFrom(r));return PlacementResponse.from(p);
    }
    @DeleteMapping("/placements/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deletePlacement(@PathVariable Long id){placements.deleteById(id);}

    @PostMapping("/colleges") @ResponseStatus(HttpStatus.CREATED)
    Object createCollege(@Valid @RequestBody CollegeRequest r){return CollegeResponse.from(colleges.save(collegeFrom(r)));}
    @PutMapping("/colleges/{id}") Object updateCollege(@PathVariable Long id,@Valid @RequestBody CollegeRequest r){
        College c=colleges.findById(id).orElseThrow(()->notFound("College"));c.updateFrom(collegeFrom(r));return CollegeResponse.from(c);
    }
    @DeleteMapping("/colleges/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteCollege(@PathVariable Long id){
        College c=colleges.findById(id).orElseThrow(()->notFound("College"));cutoffs.deleteByCollege(c);placements.deleteByCollege(c);
        branches.deleteByCollege(c);favorites.deleteByCollege(c);recommendationHistory.deleteByCollege(c);colleges.delete(c);
    }

    @GetMapping("/users") PageResponse<UserResponse> listUsers(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size){
        if(size<1||size>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Page size must be between 1 and 100");
        return PageResponse.from(users.findAll(PageRequest.of(Math.max(0,page),size)).map(UserResponse::from));
    }
    @PatchMapping("/users/{id}/active") UserResponse updateUserActive(@PathVariable Long id,@RequestParam boolean active,Authentication authentication){
        User user=users.findById(id).orElseThrow(()->notFound("User"));
        if(user.getEmail().equalsIgnoreCase(authentication.getName()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"You cannot deactivate your own account");
        user.setActive(active);return UserResponse.from(users.save(user));
    }

    private Branch branchFrom(Map<String,String> r){College college=colleges.findById(Long.valueOf(required(r,"collegeId"))).orElseThrow(()->notFound("College"));return new Branch(college,text(r,"name",100,true),text(r,"description",500,false));}
    private Cutoff cutoffFrom(Map<String,String> r){College college=colleges.findById(Long.valueOf(required(r,"collegeId"))).orElseThrow(()->notFound("College"));Course course=optionalCourse(r);Branch branch=optionalBranch(r);if(branch!=null&&!branch.getCollege().getId().equals(college.getId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Branch must belong to the selected college");String exam=text(r,"entranceExam",50,true);int year=Integer.valueOf(required(r,"examYear"));if(year<2000||year>2100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"examYear must be between 2000 and 2100");Integer rank=integer(r.get("closingRank"));Double percentile=decimal(r.get("closingPercentile"));if(rank!=null&&rank<1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"closingRank must be positive");if(percentile!=null&&(percentile<0||percentile>100))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"closingPercentile must be between 0 and 100");if(rank==null&&percentile==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A closing rank or percentile is required");return new Cutoff(college,course,branch,exam,year,text(r,"category",40,false),rank,percentile);}
    private Placement placementFrom(Map<String,String> r){College college=colleges.findById(Long.valueOf(required(r,"collegeId"))).orElseThrow(()->notFound("College"));int year=Integer.valueOf(required(r,"year")),placed=Integer.valueOf(required(r,"studentsPlaced")),eligible=Integer.valueOf(required(r,"studentsEligible"));double average=Double.valueOf(required(r,"averagePackage")),highest=Double.valueOf(required(r,"highestPackage"));if(year<2000||year>2100||eligible<1||placed<0||placed>eligible||average<0||highest<average)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Placement year, totals, and package values are inconsistent");return new Placement(college,optionalCourse(r),year,placed,eligible,average,highest);}
    private Course optionalCourse(Map<String,String> r){return r.get("courseId")==null||r.get("courseId").isBlank()?null:courses.findById(Long.valueOf(r.get("courseId"))).orElseThrow(()->notFound("Course"));}
    private Branch optionalBranch(Map<String,String> r){return r.get("branchId")==null||r.get("branchId").isBlank()?null:branches.findById(Long.valueOf(r.get("branchId"))).orElseThrow(()->notFound("Branch"));}
    private College collegeFrom(CollegeRequest r){return new College(r.name(),r.description(),r.state(),r.city(),r.address(),r.website(),r.collegeType(),r.establishedYear(),r.fees(),r.ranking(),Boolean.TRUE.equals(r.hostelAvailable()),r.accreditation(),r.imageUrl());}
    private String required(Map<String,String> r,String key){String value=r.get(key);if(value==null||value.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,key+" is required");return value.trim();}
    private String text(Map<String,String> r,String key,int max,boolean required){String value=r.get(key);if(value==null||value.isBlank()){if(required)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,key+" is required");return null;}value=value.trim();if(value.length()>max)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,key+" must be at most "+max+" characters");return value;}
    private Integer integer(String s){return s==null||s.isBlank()?null:Integer.valueOf(s);}
    private Double decimal(String s){return s==null||s.isBlank()?null:Double.valueOf(s);}
    private ResponseStatusException notFound(String kind){return new ResponseStatusException(HttpStatus.NOT_FOUND,kind+" not found");}
}
