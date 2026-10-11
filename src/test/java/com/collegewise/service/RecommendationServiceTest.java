package com.collegewise.service;

import com.collegewise.entity.*;
import com.collegewise.dto.ApiDtos.Recommendation;
import com.collegewise.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class RecommendationServiceTest {
    @Test void recommendationIsDeterministicAndUsesActualEvidence() {
        CollegeRepository colleges=mock(CollegeRepository.class);
        ProfileRepository profiles=mock(ProfileRepository.class);
        UserRepository users=mock(UserRepository.class);
        RecommendationHistoryRepository history=mock(RecommendationHistoryRepository.class);
        CutoffRepository cutoffs=mock(CutoffRepository.class);
        PlacementRepository placements=mock(PlacementRepository.class);
        User user=new User("A Student","a@example.com","hash",Role.STUDENT);
        StudentProfile profile=new StudentProfile(user);
        profile.update(null,90.0,90.0,"JEE Main",2025,18000,null,"OPEN",null,null,"Karnataka",null,200000.0,false,null);
        College match=new College("Bengaluru College","","Karnataka","Bengaluru","","","Public",2000,150000.0,50,false,"","");
        College other=new College("Other College","","Delhi","Delhi","","","Public",2000,300000.0,300,false,"","");
        setId(match,1L);setId(other,2L);
        when(users.findByEmailIgnoreCase("a@example.com")).thenReturn(Optional.of(user));
        when(profiles.findByUser(user)).thenReturn(Optional.of(profile));
        when(colleges.search(isNull(),isNull(),isNull(),isNull(),isNull(),any(Pageable.class))).thenReturn(new PageImpl<>(List.of(other,match)));
        when(cutoffs.findByCollegeIn(anyCollection())).thenReturn(List.of());
        when(placements.findByCollegeIn(anyCollection())).thenReturn(List.of());
        RecommendationService service=new RecommendationService(colleges,profiles,users,history,cutoffs,placements);
        var first=service.recommend("a@example.com");var second=service.recommend("a@example.com");
        assertEquals("Bengaluru College",first.get(0).collegeName());
        assertEquals(first,second);
        assertTrue(first.get(0).score()>=0&&first.get(0).score()<=100);
        assertEquals("INSUFFICIENT_DATA",first.get(0).admissionCompatibility());
        assertTrue(first.get(0).concerns().stream().anyMatch(c->c.startsWith("INSUFFICIENT_DATA")));
        assertTrue(first.get(0).score()>first.get(1).score());
    }
    @Test void matchingCutoffAndLatestPlacementContributeDocumentedFactors(){
        CollegeRepository colleges=mock(CollegeRepository.class);ProfileRepository profiles=mock(ProfileRepository.class);UserRepository users=mock(UserRepository.class);
        RecommendationHistoryRepository history=mock(RecommendationHistoryRepository.class);CutoffRepository cutoffs=mock(CutoffRepository.class);PlacementRepository placements=mock(PlacementRepository.class);
        User user=new User("A Student","b@example.com","hash",Role.STUDENT);StudentProfile profile=new StudentProfile(user);
        profile.update(null,90.0,90.0,"JEE Main",2025,18000,96.0,"OPEN","B.Tech","Computer Science","Karnataka",null,null,false,null);
        College college=new College("Tech College","","Karnataka","Bengaluru","","","Public",2000,150000.0,500,false,"","");setId(college,3L);
        Course course=new Course("B.Tech","");Branch branch=new Branch(college,"Computer Science","");
        Cutoff cutoff=new Cutoff(college,course,branch,"JEE Main",2025,"OPEN",25000,92.0);
        Placement placement=new Placement(college,course,2024,80,100,1_200_000.0,3_000_000.0);
        when(users.findByEmailIgnoreCase("b@example.com")).thenReturn(Optional.of(user));when(profiles.findByUser(user)).thenReturn(Optional.of(profile));
        when(colleges.search(isNull(),isNull(),isNull(),isNull(),isNull(),any(Pageable.class))).thenReturn(new PageImpl<>(List.of(college)));
        when(cutoffs.findByCollegeIn(anyCollection())).thenReturn(List.of(cutoff));when(placements.findByCollegeIn(anyCollection())).thenReturn(List.of(placement));
        Recommendation result=new RecommendationService(colleges,profiles,users,history,cutoffs,placements).recommend("b@example.com").get(0);
        assertEquals("MEETS_CUTOFF",result.admissionCompatibility());assertEquals(79,result.score());
        assertTrue(result.reasons().stream().anyMatch(x->x.contains("placement rate contributes 4")));
        assertTrue(result.reasons().stream().anyMatch(x->x.contains("average package contributes 2")));
    }
    private static void setId(College college,Long id){try{var field=College.class.getDeclaredField("id");field.setAccessible(true);field.set(college,id);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
