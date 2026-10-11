package com.collegewise.entity;

import jakarta.persistence.*;

@Entity
public class StudentProfile {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(optional=false) @JoinColumn(nullable=false, unique=true) private User user;
    @Column(length=20) private String phone;
    private Double tenthPercentage;
    private Double twelfthPercentage;
    @Column(length=50) private String entranceExam;
    private Integer examYear;
    @Column(name="entrance_rank") private Integer rank;
    private Double percentile;
    @Column(length=40) private String category;
    @Column(length=80) private String preferredCourse;
    @Column(length=80) private String preferredBranch;
    @Column(length=80) private String preferredState;
    @Column(length=80) private String preferredCity;
    private Double maximumBudget;
    private Boolean hostelRequired=false;
    @Column(length=120) private String careerInterest;
    protected StudentProfile() {}
    public StudentProfile(User user){this.user=user;}
    public Long getId(){return id;} public User getUser(){return user;} public String getPhone(){return phone;}
    public Double getTenthPercentage(){return tenthPercentage;} public Double getTwelfthPercentage(){return twelfthPercentage;}
    public String getEntranceExam(){return entranceExam;} public Integer getExamYear(){return examYear;} public Integer getRank(){return rank;}
    public Double getPercentile(){return percentile;} public String getCategory(){return category;} public String getPreferredCourse(){return preferredCourse;}
    public String getPreferredBranch(){return preferredBranch;} public String getPreferredState(){return preferredState;} public String getPreferredCity(){return preferredCity;}
    public Double getMaximumBudget(){return maximumBudget;} public Boolean getHostelRequired(){return hostelRequired;} public String getCareerInterest(){return careerInterest;}
    public void update(String phone,Double tenth,Double twelfth,String exam,Integer year,Integer rank,Double percentile,String category,String course,String branch,String state,String city,Double budget,Boolean hostel,String career){
        this.phone=phone;this.tenthPercentage=tenth;this.twelfthPercentage=twelfth;this.entranceExam=exam;this.examYear=year;this.rank=rank;this.percentile=percentile;this.category=category;this.preferredCourse=course;this.preferredBranch=branch;this.preferredState=state;this.preferredCity=city;this.maximumBudget=budget;this.hostelRequired=hostel;this.careerInterest=career;
    }
}
