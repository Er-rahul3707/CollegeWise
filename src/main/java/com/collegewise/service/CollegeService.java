package com.collegewise.service;

import com.collegewise.dto.ApiDtos.CollegeRequest;
import com.collegewise.entity.College;
import com.collegewise.repository.BranchRepository;
import com.collegewise.repository.CollegeRepository;
import com.collegewise.repository.CutoffRepository;
import com.collegewise.repository.FavoriteRepository;
import com.collegewise.repository.PlacementRepository;
import com.collegewise.repository.RecommendationHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CollegeService {
    private final CollegeRepository repository;
    private final BranchRepository branches;
    private final CutoffRepository cutoffs;
    private final PlacementRepository placements;
    private final FavoriteRepository favorites;
    private final RecommendationHistoryRepository recommendationHistory;

    public CollegeService(CollegeRepository repository, BranchRepository branches, CutoffRepository cutoffs,
                          PlacementRepository placements, FavoriteRepository favorites,
                          RecommendationHistoryRepository recommendationHistory) {
        this.repository = repository;
        this.branches = branches;
        this.cutoffs = cutoffs;
        this.placements = placements;
        this.favorites = favorites;
        this.recommendationHistory = recommendationHistory;
    }

    public Page<College> search(String query, String state, String city, Double fees, Boolean hostel,
                                int page, int size, String sort) {
        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100");
        }
        Sort ordering = Sort.by("name");
        if ("fees".equals(sort)) ordering = Sort.by("fees");
        if ("ranking".equals(sort)) ordering = Sort.by("ranking");
        return repository.search(blank(query), blank(state), blank(city), fees, hostel,
                PageRequest.of(Math.max(0, page), size, ordering));
    }

    public College get(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "College not found"));
    }

    public College save(CollegeRequest request) {
        return repository.save(toEntity(request));
    }

    public College update(Long id, CollegeRequest request) {
        College current = get(id);
        current.updateFrom(toEntity(request));
        return current;
    }

    public void delete(Long id) {
        College college = get(id);
        cutoffs.deleteByCollege(college);
        placements.deleteByCollege(college);
        branches.deleteByCollege(college);
        favorites.deleteByCollege(college);
        recommendationHistory.deleteByCollege(college);
        repository.delete(college);
    }

    private College toEntity(CollegeRequest request) {
        return new College(request.name(), request.description(), request.state(), request.city(), request.address(),
                request.website(), request.collegeType(), request.establishedYear(), request.fees(), request.ranking(),
                Boolean.TRUE.equals(request.hostelAvailable()), request.accreditation(), request.imageUrl());
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
