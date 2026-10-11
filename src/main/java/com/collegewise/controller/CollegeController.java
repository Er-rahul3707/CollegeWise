package com.collegewise.controller;

import com.collegewise.dto.ApiDtos.*;
import com.collegewise.entity.College;
import com.collegewise.entity.Course;
import com.collegewise.entity.Cutoff;
import com.collegewise.entity.Placement;
import com.collegewise.repository.BranchRepository;
import com.collegewise.repository.CutoffRepository;
import com.collegewise.repository.PlacementRepository;
import com.collegewise.service.CollegeService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/colleges")
public class CollegeController {
    private final CollegeService service;
    private final BranchRepository branches;
    private final CutoffRepository cutoffs;
    private final PlacementRepository placements;
    public CollegeController(CollegeService service,BranchRepository branches,CutoffRepository cutoffs,PlacementRepository placements){this.service=service;this.branches=branches;this.cutoffs=cutoffs;this.placements=placements;}
    @GetMapping PageResponse<CollegeResponse> search(@RequestParam(required=false)String q,@RequestParam(required=false)String state,
        @RequestParam(required=false)String city,@RequestParam(required=false)Double maxFees,@RequestParam(required=false)Boolean hostel,
        @RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="12")int size,@RequestParam(defaultValue="name")String sort){
        return PageResponse.from(service.search(q,state,city,maxFees,hostel,page,size,sort).map(CollegeResponse::from));
    }
    @GetMapping("/search") PageResponse<CollegeResponse> searchAlias(@RequestParam(required=false)String q,
        @RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="12")int size){
        return PageResponse.from(service.search(q,null,null,null,null,page,size,"name").map(CollegeResponse::from));
    }
    @GetMapping("/{id}/details") CollegeDetailsResponse details(@PathVariable Long id){
        College college=service.get(id);
        List<Cutoff> cutoffRows=cutoffs.findByCollegeIn(List.of(college));
        List<Placement> placementRows=placements.findByCollegeIn(List.of(college));
        Map<Long,Course> programmes=new LinkedHashMap<>();
        cutoffRows.stream().map(Cutoff::getCourse).filter(java.util.Objects::nonNull).forEach(c->programmes.putIfAbsent(c.getId(),c));
        placementRows.stream().map(Placement::getCourse).filter(java.util.Objects::nonNull).forEach(c->programmes.putIfAbsent(c.getId(),c));
        return new CollegeDetailsResponse(CollegeResponse.from(college),
                programmes.values().stream().map(CourseResponse::from).toList(),
                branches.findByCollegeId(college.getId()).stream().map(BranchResponse::from).toList(),
                cutoffRows.stream().map(CutoffResponse::from).toList(),
                placementRows.stream().map(PlacementResponse::from).toList());
    }
    @GetMapping("/{id}") CollegeResponse get(@PathVariable Long id){return CollegeResponse.from(service.get(id));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) CollegeResponse create(@Valid @RequestBody CollegeRequest request){return CollegeResponse.from(service.save(request));}
    @PutMapping("/{id}") CollegeResponse update(@PathVariable Long id,@Valid @RequestBody CollegeRequest request){return CollegeResponse.from(service.update(id,request));}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable Long id){service.delete(id);}
}
