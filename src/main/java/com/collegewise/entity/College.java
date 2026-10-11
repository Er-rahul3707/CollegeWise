package com.collegewise.entity;

import jakarta.persistence.*;

@Entity @Table(indexes={@Index(name="idx_college_name",columnList="name"),@Index(name="idx_college_location",columnList="state,city")})
public class College {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=160) private String name;
    @Column(length=2000) private String description;
    @Column(nullable=false,length=80) private String state;
    @Column(nullable=false,length=80) private String city;
    @Column(length=240) private String address;
    @Column(length=300) private String website;
    @Column(length=40) private String collegeType;
    private Integer establishedYear;
    private Double fees;
    private Integer ranking;
    private boolean hostelAvailable;
    @Column(length=80) private String accreditation;
    @Column(length=500) private String imageUrl;
    protected College(){}
    public College(String name,String description,String state,String city,String address,String website,String collegeType,Integer establishedYear,Double fees,Integer ranking,boolean hostelAvailable,String accreditation,String imageUrl){this.name=name;this.description=description;this.state=state;this.city=city;this.address=address;this.website=website;this.collegeType=collegeType;this.establishedYear=establishedYear;this.fees=fees;this.ranking=ranking;this.hostelAvailable=hostelAvailable;this.accreditation=accreditation;this.imageUrl=imageUrl;}
    public Long getId(){return id;} public String getName(){return name;} public String getDescription(){return description;} public String getState(){return state;} public String getCity(){return city;} public String getAddress(){return address;} public String getWebsite(){return website;} public String getCollegeType(){return collegeType;} public Integer getEstablishedYear(){return establishedYear;} public Double getFees(){return fees;} public Integer getRanking(){return ranking;} public boolean isHostelAvailable(){return hostelAvailable;} public String getAccreditation(){return accreditation;} public String getImageUrl(){return imageUrl;}
    public void updateFrom(College c){this.name=c.name;this.description=c.description;this.state=c.state;this.city=c.city;this.address=c.address;this.website=c.website;this.collegeType=c.collegeType;this.establishedYear=c.establishedYear;this.fees=c.fees;this.ranking=c.ranking;this.hostelAvailable=c.hostelAvailable;this.accreditation=c.accreditation;this.imageUrl=c.imageUrl;}
}
