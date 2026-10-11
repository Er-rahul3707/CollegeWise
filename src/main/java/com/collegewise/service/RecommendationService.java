package com.collegewise.service;

import com.collegewise.dto.ApiDtos.Recommendation;
import com.collegewise.entity.*;
import com.collegewise.repository.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {
    private final CollegeRepository colleges;
    private final ProfileRepository profiles;
    private final UserRepository users;
    private final RecommendationHistoryRepository history;
    private final CutoffRepository cutoffs;
    private final PlacementRepository placements;

    public RecommendationService(CollegeRepository colleges,ProfileRepository profiles,UserRepository users,
                                 RecommendationHistoryRepository history,CutoffRepository cutoffs,
                                 PlacementRepository placements) {
        this.colleges=colleges;this.profiles=profiles;this.users=users;this.history=history;
        this.cutoffs=cutoffs;this.placements=placements;
    }

    @Transactional
    public List<Recommendation> recommend(String email) {
        User user=users.findByEmailIgnoreCase(email).orElseThrow();
        StudentProfile profile=profiles.findByUser(user).orElseGet(()->new StudentProfile(user));
        List<College> candidates=colleges.search(null,null,null,null,null,PageRequest.of(0,1000)).getContent();
        if(candidates.isEmpty())return List.of();
        Map<Long,List<Cutoff>> cutoffMap=cutoffs.findByCollegeIn(candidates).stream()
                .collect(Collectors.groupingBy(x->x.getCollege().getId()));
        Map<Long,List<Placement>> placementMap=placements.findByCollegeIn(candidates).stream()
                .collect(Collectors.groupingBy(x->x.getCollege().getId()));
        List<Recommendation> result=candidates.stream().map(c->score(c,profile,
                        cutoffMap.getOrDefault(c.getId(),List.of()),placementMap.getOrDefault(c.getId(),List.of())))
                .sorted(Comparator.comparingInt(Recommendation::score).reversed().thenComparing(Recommendation::collegeName))
                .limit(20).toList();
        Map<Long,College> byId=candidates.stream().collect(Collectors.toMap(College::getId,c->c));
        history.saveAll(result.stream().map(r->new RecommendationHistory(user,byId.get(r.collegeId()),r.score())).toList());
        return result;
    }

    private Recommendation score(College college,StudentProfile profile,List<Cutoff> allCutoffs,List<Placement> placementRows) {
        int score=0;
        List<String> reasons=new ArrayList<>();
        List<String> concerns=new ArrayList<>();

        if(profile.getMaximumBudget()==null||college.getFees()==null){score+=12;reasons.add("budget fit is unknown");concerns.add("Budget or published fees are missing.");}
        else if(college.getFees()<=profile.getMaximumBudget()){score+=25;reasons.add("published fees are within your budget");}
        else concerns.add("Published fees exceed your maximum budget.");

        if(profile.getPreferredState()!=null){if(profile.getPreferredState().equalsIgnoreCase(college.getState())){score+=15;reasons.add("matches your preferred state");}else concerns.add("College is outside your preferred state.");}
        if(profile.getPreferredCity()!=null){if(profile.getPreferredCity().equalsIgnoreCase(college.getCity())){score+=10;reasons.add("matches your preferred city");}else concerns.add("College is outside your preferred city.");}

        if(Boolean.TRUE.equals(profile.getHostelRequired())){if(college.isHostelAvailable()){score+=10;reasons.add("hostel is listed as available");}else concerns.add("Hostel availability does not meet your preference.");}
        else score+=5;

        if(college.getRanking()!=null){int rankingPoints=Math.max(0,Math.min(10,11-college.getRanking()/50));score+=rankingPoints;reasons.add("published ranking contributes "+rankingPoints+" points");}
        else concerns.add("No ranking is available.");

        List<Cutoff> relevant=allCutoffs.stream()
                .filter(c->profile.getEntranceExam()!=null&&c.getEntranceExam().equalsIgnoreCase(profile.getEntranceExam()))
                .filter(c->profile.getExamYear()==null||Objects.equals(c.getExamYear(),profile.getExamYear()))
                .filter(c->profile.getCategory()==null||profile.getCategory().isBlank()||(c.getCategory()!=null&&c.getCategory().equalsIgnoreCase(profile.getCategory())))
                .filter(c->profile.getPreferredCourse()==null||(c.getCourse()!=null&&c.getCourse().getName().equalsIgnoreCase(profile.getPreferredCourse())))
                .filter(c->profile.getPreferredBranch()==null||(c.getBranch()!=null&&c.getBranch().getName().equalsIgnoreCase(profile.getPreferredBranch())))
                .sorted(Comparator.comparing(Cutoff::getExamYear,Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Cutoff::getId,Comparator.nullsLast(Comparator.naturalOrder()))).toList();
        boolean courseMatched=profile.getPreferredCourse()!=null&&relevant.stream().anyMatch(c->c.getCourse()!=null&&c.getCourse().getName().equalsIgnoreCase(profile.getPreferredCourse()));
        boolean branchMatched=profile.getPreferredBranch()!=null&&relevant.stream().anyMatch(c->c.getBranch()!=null&&c.getBranch().getName().equalsIgnoreCase(profile.getPreferredBranch()));
        if(courseMatched){score+=10;reasons.add("preferred course matches a published cutoff record");}
        else if(profile.getPreferredCourse()!=null)concerns.add("No cutoff record confirms the preferred course at this college.");
        if(branchMatched){score+=10;reasons.add("preferred branch matches a published cutoff record");}
        else if(profile.getPreferredBranch()!=null)concerns.add("No cutoff record confirms the preferred branch at this college.");

        String compatibility="INSUFFICIENT_DATA";
        Optional<Cutoff> applicable=relevant.stream().findFirst();
        if(applicable.isPresent()){
            Cutoff c=applicable.get();
            if(profile.getRank()!=null&&c.getClosingRank()!=null){
                boolean meets=profile.getRank()<=c.getClosingRank();compatibility=meets?"MEETS_CUTOFF":"BELOW_CUTOFF";
                if(meets){score+=20;reasons.add("rank meets the published closing-rank threshold");}else concerns.add("Rank is beyond the published closing-rank threshold.");
            }else if(profile.getPercentile()!=null&&c.getClosingPercentile()!=null){
                boolean meets=profile.getPercentile()>=c.getClosingPercentile();compatibility=meets?"MEETS_CUTOFF":"BELOW_CUTOFF";
                if(meets){score+=20;reasons.add("percentile meets the published cutoff threshold");}else concerns.add("Percentile is below the published cutoff threshold.");
            }else concerns.add("Cutoff record exists, but your rank or percentile cannot be compared.");
        }else concerns.add("INSUFFICIENT_DATA: no matching cutoff record is available.");

        Optional<Placement> latest=placementRows.stream().max(Comparator.comparing(Placement::getYear,Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Placement::getId,Comparator.nullsFirst(Comparator.naturalOrder())));
        if(latest.isPresent()&&latest.get().getStudentsEligible()!=null&&latest.get().getStudentsEligible()>0){
            Placement p=latest.get();double rate=Math.max(0,Math.min(1,(double)p.getStudentsPlaced()/p.getStudentsEligible()));
            int ratePoints=(int)Math.round(rate*5);int packagePoints=p.getAveragePackage()==null?0:(int)Math.round(Math.max(0,Math.min(1,p.getAveragePackage()/3_000_000d))*5);
            score+=ratePoints+packagePoints;reasons.add("latest placement rate contributes "+ratePoints+" points");
            if(p.getAveragePackage()!=null)reasons.add("latest average package contributes "+packagePoints+" points");
        }else concerns.add("No placement outcome data is available.");

        return new Recommendation(college.getId(),college.getName(),Math.max(0,Math.min(100,score)),compatibility,
                reasons.isEmpty()?"No profile factors matched yet.":String.join("; ",reasons),List.copyOf(reasons),List.copyOf(concerns));
    }
}
