package com.collegewise;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.collegewise.entity.College;
import com.collegewise.entity.Branch;
import com.collegewise.entity.Course;
import com.collegewise.entity.Cutoff;
import com.collegewise.entity.Placement;
import com.collegewise.repository.CollegeRepository;
import com.collegewise.repository.BranchRepository;
import com.collegewise.repository.CourseRepository;
import com.collegewise.repository.CutoffRepository;
import com.collegewise.repository.PlacementRepository;
import com.collegewise.repository.UserRepository;
import com.collegewise.entity.User;
import com.collegewise.entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {
    private static final String TEST_JWT_SECRET=randomSecret();
    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry){registry.add("app.jwt.secret",()->TEST_JWT_SECRET);}
    @Autowired MockMvc mvc;
    @Autowired CollegeRepository colleges;
    @Autowired BranchRepository branches;
    @Autowired CourseRepository courses;
    @Autowired CutoffRepository cutoffs;
    @Autowired PlacementRepository placements;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test void registrationProfileAndRoleAuthorizationWork() throws Exception {
        College college=colleges.save(new College("Integration College","test fixture","Karnataka","Mysuru","","","Public",2000,150000.0,75,true,"", ""));
        College comparisonCollege=colleges.save(new College("Second Comparison College","comparison fixture","Tamil Nadu","Chennai","","","Public",2001,200000.0,50,true,"", ""));
        Course course=courses.save(new Course("B.Tech","Engineering degree"));
        Branch branch=branches.save(new Branch(college,"Computer Science and Engineering","Computing"));
        cutoffs.save(new Cutoff(college,course,branch,"JEE Main",2025,"Open",15000,null));
        placements.save(new Placement(college,course,2025,90,100,900000.0,1800000.0));
        mvc.perform(get("/api/colleges")).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2));
        mvc.perform(get("/api/colleges/"+college.getId()+"/details"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.college.name").value("Integration College"))
                .andExpect(jsonPath("$.courses[0].name").value("B.Tech"))
                .andExpect(jsonPath("$.branches[0].name").value("Computer Science and Engineering"))
                .andExpect(jsonPath("$.cutoffs[0].closingRank").value(15000))
                .andExpect(jsonPath("$.placements[0].averagePackage").value(900000.0));
        String studentPassword=UUID.randomUUID()+"Aa9!";
        String response=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Student\",\"email\":\"integration@example.com\",\"password\":\""+studentPassword+"\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("STUDENT"))
                .andReturn().getResponse().getContentAsString();
        String token=com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(response).get("token").asText();
        String studentLogin=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"integration@example.com\",\"password\":\""+studentPassword+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andReturn().getResponse().getContentAsString();
        token=com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(studentLogin).get("token").asText();
        mvc.perform(get("/api/students/profile").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("integration@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(put("/api/students/profile").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenthPercentage\":91.5,\"twelfthPercentage\":94.0,\"entranceExam\":\"JEE Main\",\"examYear\":2025,\"rank\":12000,\"percentile\":98.5,\"category\":\"Open\",\"preferredCourse\":\"B.Tech\",\"preferredBranch\":\"Computer Science and Engineering\",\"preferredState\":\"Karnataka\",\"preferredCity\":\"Mysuru\",\"maximumBudget\":200000,\"hostelRequired\":true,\"careerInterest\":\"Data science\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.twelfthPercentage").value(94.0))
                .andExpect(jsonPath("$.entranceExam").value("JEE Main"))
                .andExpect(jsonPath("$.preferredBranch").value("Computer Science and Engineering"));
        mvc.perform(get("/api/students/dashboard").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.profileComplete").value(true));
        mvc.perform(get("/api/students/recommendations").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].collegeName").exists())
                .andExpect(jsonPath("$[0].admissionCompatibility").value("MEETS_CUTOFF"))
                .andExpect(jsonPath("$[0].reasons[0]").exists());
        mvc.perform(get("/api/students/recommendations/history").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].collegeName").exists());
        mvc.perform(get("/api/students/career-guidance").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.guidance").exists());
        mvc.perform(post("/api/students/favorites/"+college.getId()).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(college.getId()));
        mvc.perform(get("/api/students/favorites").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Integration College"));
        mvc.perform(delete("/api/students/favorites/"+college.getId()).header("Authorization","Bearer "+token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/students/favorites").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/students/compare").param("ids",college.getId().toString(),comparisonCollege.getId().toString())
                .header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/students/compare").param("ids","1","2","3","4").header("Authorization","Bearer "+token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/dashboard").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/colleges").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Weak Password\",\"email\":\"weak@example.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());

        String adminPassword=UUID.randomUUID()+"Bb8!";
        users.save(new User("Test Admin","admin@example.com",encoder.encode(adminPassword),Role.ADMIN));
        String adminResponse=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.com\",\"password\":\""+adminPassword+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();
        String adminToken=com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(adminResponse).get("token").asText();
        mvc.perform(get("/api/admin/dashboard").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
        String created=mvc.perform(post("/api/admin/colleges").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"CRUD College\",\"state\":\"Gujarat\",\"city\":\"Surat\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long createdId=com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(created).get("id").asLong();
        mvc.perform(put("/api/admin/colleges/"+createdId).header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"CRUD College Updated\",\"state\":\"Gujarat\",\"city\":\"Surat\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("CRUD College Updated"));
        String courseJson=mvc.perform(post("/api/admin/courses").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"CRUD course\",\"description\":\"test\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long courseId=com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(courseJson).get("id").asLong();
        mvc.perform(put("/api/admin/courses/"+courseId).header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"CRUD course updated\",\"description\":\"test\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("CRUD course updated"));
        String branchJson=mvc.perform(post("/api/admin/branches").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"collegeId\":\""+createdId+"\",\"name\":\"CRUD branch\",\"description\":\"test\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long branchId=com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(branchJson).get("id").asLong();
        mvc.perform(put("/api/admin/branches/"+branchId).header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"collegeId\":\""+createdId+"\",\"name\":\"CRUD branch updated\",\"description\":\"test\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("CRUD branch updated"));
        String cutoffJson=mvc.perform(post("/api/admin/cutoffs").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"collegeId\":\""+createdId+"\",\"courseId\":\""+courseId+"\",\"branchId\":\""+branchId+"\",\"entranceExam\":\"JEE Main\",\"examYear\":\"2025\",\"category\":\"Open\",\"closingRank\":\"10000\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long cutoffId=com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(cutoffJson).get("id").asLong();
        mvc.perform(put("/api/admin/cutoffs/"+cutoffId).header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"collegeId\":\""+createdId+"\",\"courseId\":\""+courseId+"\",\"branchId\":\""+branchId+"\",\"entranceExam\":\"JEE Main\",\"examYear\":\"2025\",\"category\":\"Open\",\"closingRank\":\"9000\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.closingRank").value(9000));
        mvc.perform(delete("/api/admin/cutoffs/"+cutoffId).header("Authorization","Bearer "+adminToken)).andExpect(status().isNoContent());
        String placementJson=mvc.perform(post("/api/admin/placements").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"collegeId\":\""+createdId+"\",\"courseId\":\""+courseId+"\",\"year\":\"2025\",\"studentsPlaced\":\"80\",\"studentsEligible\":\"100\",\"averagePackage\":\"900000\",\"highestPackage\":\"1800000\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long placementId=com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(placementJson).get("id").asLong();
        mvc.perform(put("/api/admin/placements/"+placementId).header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"collegeId\":\""+createdId+"\",\"courseId\":\""+courseId+"\",\"year\":\"2025\",\"studentsPlaced\":\"90\",\"studentsEligible\":\"100\",\"averagePackage\":\"1000000\",\"highestPackage\":\"2000000\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.studentsPlaced").value(90));
        mvc.perform(delete("/api/admin/placements/"+placementId).header("Authorization","Bearer "+adminToken)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/admin/branches/"+branchId).header("Authorization","Bearer "+adminToken)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/admin/courses/"+courseId).header("Authorization","Bearer "+adminToken)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/admin/colleges/"+createdId).header("Authorization","Bearer "+adminToken)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/admin/colleges/"+comparisonCollege.getId()).header("Authorization","Bearer "+adminToken)).andExpect(status().isNoContent());
        mvc.perform(get("/api/students/profile").header("Authorization","Bearer "+adminToken)).andExpect(status().isForbidden());
        long studentId=users.findByEmailIgnoreCase("integration@example.com").orElseThrow().getId();
        mvc.perform(patch("/api/admin/users/"+studentId+"/active?active=false").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
        mvc.perform(get("/api/students/profile").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
    private static String randomSecret(){byte[] value=new byte[48];new SecureRandom().nextBytes(value);return Base64.getEncoder().encodeToString(value);}
}
