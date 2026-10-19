package com.collegewise;

import com.collegewise.entity.College;
import com.collegewise.entity.Branch;
import com.collegewise.entity.Course;
import com.collegewise.entity.Cutoff;
import com.collegewise.entity.Favorite;
import com.collegewise.entity.Placement;
import com.collegewise.entity.RecommendationHistory;
import com.collegewise.entity.Role;
import com.collegewise.entity.StudentProfile;
import com.collegewise.entity.User;
import com.collegewise.repository.CollegeRepository;
import com.collegewise.repository.BranchRepository;
import com.collegewise.repository.CourseRepository;
import com.collegewise.repository.CutoffRepository;
import com.collegewise.repository.FavoriteRepository;
import com.collegewise.repository.PlacementRepository;
import com.collegewise.repository.RecommendationHistoryRepository;
import com.collegewise.repository.ProfileRepository;
import com.collegewise.repository.UserRepository;
import com.collegewise.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiContractTest {
    private static final String TEST_SECRET = randomSecret();

    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> TEST_SECRET);
    }

    @Autowired MockMvc mvc;
    @Autowired CollegeRepository colleges;
    @Autowired BranchRepository branches;
    @Autowired CourseRepository courses;
    @Autowired CutoffRepository cutoffs;
    @Autowired UserRepository users;
    @Autowired ProfileRepository profiles;
    @Autowired FavoriteRepository favorites;
    @Autowired PlacementRepository placements;
    @Autowired RecommendationHistoryRepository history;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper mapper;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void adminLoginAuthenticatesRolesAndSetsCookieOnlyForAdministrators() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String adminEmail = "admin-login-" + suffix + "@example.test";
        String studentEmail = "student-login-" + suffix + "@example.test";
        String password = "test-only-" + suffix + "-Aa9!";
        users.saveAndFlush(new User("Contract Admin", adminEmail, passwordEncoder.encode(password), Role.ADMIN));
        users.saveAndFlush(new User("Contract Student", studentEmail, passwordEncoder.encode(password), Role.STUDENT));

        mvc.perform(get("/admin-login.html")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/admin/login").contentType("application/json")
                        .content("{\"email\":\"" + adminEmail + "\",\"password\":\"wrong-password-123\"}"))
                .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Set-Cookie"));
        mvc.perform(post("/api/auth/admin/login").contentType("application/json")
                        .content("{\"email\":\"" + studentEmail + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Set-Cookie"));

        String response = mvc.perform(post("/api/auth/admin/login").contentType("application/json")
                        .content("{\"email\":\"" + adminEmail + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Strict"))))
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(response).get("token").asText();
        mvc.perform(get("/admin.html").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void searchAppliesTextLocationBudgetAndHostelFilters() throws Exception {
        College match = college("Search Contract College", "Karnataka", "Mysuru", 150_000d, true);
        college("Search Contract College Annex", "Karnataka", "Bengaluru", 300_000d, false);

        mvc.perform(get("/api/colleges")
                        .param("q", "SEARCH CONTRACT")
                        .param("state", "karnataka")
                        .param("city", "Mysuru")
                        .param("maxFees", "200000")
                        .param("hostel", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(match.getId()));

        mvc.perform(get("/api/colleges").param("q", "Search Contract").param("maxFees", "100000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/colleges").param("q", "Search Contract").param("hostel", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void comparisonIsAuthenticatedAndLimitedToTwoOrThreeDistinctColleges() throws Exception {
        String token = studentToken();
        College one = college("Compare Contract One", "State A", "City A", 1d, true);
        College two = college("Compare Contract Two", "State B", "City B", 2d, false);
        College three = college("Compare Contract Three", "State C", "City C", 3d, true);
        College four = college("Compare Contract Four", "State D", "City D", 4d, false);

        mvc.perform(get("/api/students/compare").param("ids", one.getId().toString(), two.getId().toString()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/students/compare").param("ids", one.getId().toString(), two.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/students/compare").param("ids", one.getId().toString(), two.getId().toString(), three.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/students/compare").param("ids", one.getId().toString(), two.getId().toString(), three.getId().toString(), four.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void favoritesAreScopedToTheStudentAndRepeatedSavesDoNotDuplicate() throws Exception {
        String firstToken = studentToken();
        String secondToken = studentToken();
        College favorite = college("Favorite Contract College", "State", "City", 10d, true);

        mvc.perform(post("/api/students/favorites/" + favorite.getId()).header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk());
        mvc.perform(post("/api/students/favorites/" + favorite.getId()).header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/students/favorites").header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/students/favorites").header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentAndAdminPagesAreProtectedByTheServerRole() throws Exception {
        String studentToken = studentToken();
        String adminToken = tokenFor("contract-admin-" + UUID.randomUUID() + "@example.test", Role.ADMIN);

        mvc.perform(get("/dashboard.html")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin.html")).andExpect(status().isUnauthorized());
        mvc.perform(get("/dashboard.html").cookie(new Cookie("CW_AUTH", studentToken))).andExpect(status().isOk());
        mvc.perform(get("/admin.html").cookie(new Cookie("CW_AUTH", studentToken))).andExpect(status().isForbidden());
        mvc.perform(get("/admin.html").cookie(new Cookie("CW_AUTH", adminToken))).andExpect(status().isOk());
        mvc.perform(get("/dashboard.html").cookie(new Cookie("CW_AUTH", adminToken))).andExpect(status().isForbidden());
    }

    @Test
    void browserAuthenticationSetsAnHttpOnlyStrictCookie() throws Exception {
        String password = "contract-" + UUID.randomUUID() + "-Aa9!";
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content("{\"name\":\"Cookie Student\",\"email\":\"cookie-" + UUID.randomUUID() + "@example.test\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Strict"),
                        org.hamcrest.Matchers.containsString("Path=/"))));
    }

    @Test
    void logoutExpiresTheBrowserAuthenticationCookie() throws Exception {
        String password = "contract-" + UUID.randomUUID() + "-Aa9!";
        String email = "logout-" + UUID.randomUUID() + "@example.test";
        String setCookie = mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content("{\"name\":\"Logout Student\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Set-Cookie");
        org.junit.jupiter.api.Assertions.assertNotNull(setCookie);
        String value = setCookie.substring("CW_AUTH=".length(), setCookie.indexOf(';'));

        mvc.perform(post("/api/auth/logout").cookie(new Cookie("CW_AUTH", value)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Strict"),
                        org.hamcrest.Matchers.containsString("Max-Age=0"))));
    }

    @Test
    void recommendationWithoutApplicableCutoffIsMarkedInsufficientAndSavedToHistory() throws Exception {
        String email = "contract-" + UUID.randomUUID() + "@example.test";
        String token = tokenFor(email, Role.STUDENT);
        User student = users.findByEmailIgnoreCase(email).orElseThrow();
        College college = college("No Cutoff Contract College", "State", "City", 100_000d, true);
        StudentProfile profile = profiles.saveAndFlush(new StudentProfile(student));
        profile.update(null, 90d, 90d, "Unlisted entrance exam", 2026, 500, null,
                "Open", "B.Tech", null, null, null, 200_000d, false, null);

        JsonNode rows = mapper.readTree(mvc.perform(get("/api/students/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode recommendation = null;
        for (JsonNode row : rows) if (row.path("collegeId").asLong() == college.getId()) recommendation = row;
        org.junit.jupiter.api.Assertions.assertNotNull(recommendation);
        org.junit.jupiter.api.Assertions.assertEquals("INSUFFICIENT_DATA", recommendation.path("admissionCompatibility").asText());
        org.junit.jupiter.api.Assertions.assertTrue(recommendation.path("concerns").toString().contains("no matching cutoff"));
        mvc.perform(get("/api/students/recommendations/history").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].collegeId").exists());
    }

    @Test
    void profileAndCollegeFieldsAreValidatedBeforeTheyReachTheDatabase() throws Exception {
        String studentToken = studentToken();
        String adminToken = tokenFor("contract-admin-" + UUID.randomUUID() + "@example.test", Role.ADMIN);
        mvc.perform(put("/api/students/profile").header("Authorization", "Bearer " + studentToken)
                        .contentType("application/json")
                        .content("{\"careerInterest\":\"" + "x".repeat(121) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.careerInterest").exists());

        String tooLongName = "x".repeat(161);
        mvc.perform(post("/api/admin/colleges").header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"name\":\"" + tooLongName + "\",\"state\":\"State\",\"city\":\"City\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.name").exists());
        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.placements").exists())
                .andExpect(jsonPath("$.favorites").exists()).andExpect(jsonPath("$.recommendations").exists());
    }

    @Test
    void protectedCollegeDeleteRemovesRelatedCatalogueRecords() throws Exception {
        String adminToken = tokenFor("contract-admin-" + UUID.randomUUID() + "@example.test", Role.ADMIN);
        String studentEmail = "contract-" + UUID.randomUUID() + "@example.test";
        tokenFor(studentEmail, Role.STUDENT);
        User student = users.findByEmailIgnoreCase(studentEmail).orElseThrow();
        College target = college("Delete Contract College", "State", "City", 100_000d, true);
        Course course = courses.saveAndFlush(new Course("Delete Contract Course " + UUID.randomUUID(), "fixture"));
        Branch branch = branches.saveAndFlush(new Branch(target, "Delete Contract Branch", "fixture"));
        cutoffs.saveAndFlush(new Cutoff(target, course, branch, "Contract Exam", 2025, "Open", 1000, null));
        placements.saveAndFlush(new Placement(target, course, 2025, 8, 10, 500_000d, 900_000d));
        favorites.saveAndFlush(new Favorite(student, target));
        history.saveAndFlush(new RecommendationHistory(student, target, 70));

        mvc.perform(delete("/api/colleges/" + target.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(colleges.existsById(target.getId()));
        org.junit.jupiter.api.Assertions.assertTrue(branches.findByCollegeId(target.getId()).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(cutoffs.findByCollege(target).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(placements.findByCollegeIn(List.of(target)).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(favorites.findByUser(student).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(history.findTop50ByUserOrderByCreatedAtDesc(student).isEmpty());
    }

    private College college(String name, String state, String city, Double fees, boolean hostel) {
        return colleges.saveAndFlush(new College(name, "Test fixture; not verified catalogue data.", state, city,
                "", "", "Test", 2000, fees, 50, hostel, "", ""));
    }

    private String studentToken() {
        return tokenFor("contract-" + UUID.randomUUID() + "@example.test", Role.STUDENT);
    }

    private String tokenFor(String email, Role role) {
        User user = users.saveAndFlush(new User("Contract User", email, "test-only-not-a-credential", role));
        return jwt.issue(user.getEmail(), user.getRole().name());
    }

    private static String randomSecret() {
        byte[] value = new byte[48];
        new SecureRandom().nextBytes(value);
        return Base64.getEncoder().encodeToString(value);
    }
}
