package com.collegewise.config;

import com.collegewise.entity.*;
import com.collegewise.repository.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Loads a compact, restart-safe catalogue for presentations and local demos. */
@Component
public class DemoDataSeeder implements CommandLineRunner {
    private final CollegeRepository colleges;
    private final CourseRepository courses;
    private final BranchRepository branches;
    private final CutoffRepository cutoffs;
    private final PlacementRepository placements;
    @Value("${app.seed.demo-data:true}") private boolean enabled;

    public DemoDataSeeder(CollegeRepository colleges, CourseRepository courses, BranchRepository branches,
                          CutoffRepository cutoffs, PlacementRepository placements) {
        this.colleges=colleges; this.courses=courses; this.branches=branches;
        this.cutoffs=cutoffs; this.placements=placements;
    }

    @Override @Transactional
    public void run(String... args) {
        if (!enabled) return;
        Course btech=course("B.Tech", "Bachelor of Technology undergraduate engineering programmes");
        Course be=course("B.E.", "Bachelor of Engineering undergraduate engineering programmes");
        List<String> branchNames=List.of("Computer Science and Engineering","Electronics and Communication Engineering","Electrical Engineering","Mechanical Engineering");
        for (int i=0;i<DATA.size();i++) {
            D d=DATA.get(i);
            College c=colleges.findByNameIgnoreCase(d.name()).orElseGet(()->colleges.save(new College(
                    d.name(), d.description()+" Demo note: fees, placement figures and cutoffs are illustrative estimates; confirm current details with the institution.",
                    d.state(),d.city(),d.city()+", "+d.state(),d.website(),d.type(),d.established(),d.fees(),d.rank(),true,
                    "NIRF Engineering 2025",null)));
            Course programme=d.programme().equals("B.E.")?be:btech;
            Branch cse=null;
            for(String name:branchNames){
                Branch branch=branches.findByCollegeIdAndNameIgnoreCase(c.getId(),name).orElseGet(()->branches.save(new Branch(c,name,"Illustrative programme entry for the CollegeWise demo catalogue.")));
                if(name.equals("Computer Science and Engineering")) cse=branch;
            }
            if (d.exam()!=null && !hasCutoff(c,programme,cse,d.exam()))
                cutoffs.save(new Cutoff(c,programme,cse,d.exam(),2025,"Open",d.cutoff(),null));
            if (!hasPlacement(c,programme))
                placements.save(new Placement(c,programme,2025,d.placed(),d.eligible(),d.averagePackage(),d.highestPackage()));
        }
    }

    private Course course(String name,String description){return courses.findByNameIgnoreCase(name).orElseGet(()->courses.save(new Course(name,description)));}
    private boolean hasCutoff(College college,Course course,Branch branch,String exam){
        return cutoffs.findByCollege(college).stream().anyMatch(x->x.getCourse()!=null&&x.getCourse().getId().equals(course.getId())
                &&x.getBranch()!=null&&x.getBranch().getId().equals(branch.getId())&&x.getEntranceExam().equalsIgnoreCase(exam)
                &&x.getExamYear()==2025&&"Open".equalsIgnoreCase(x.getCategory()));
    }
    private boolean hasPlacement(College college,Course course){
        return placements.findByCollegeId(college.getId()).stream().anyMatch(x->x.getCourse()!=null&&x.getCourse().getId().equals(course.getId())&&x.getYear()==2025);
    }

    private record D(String name,String city,String state,int rank,String website,int established,double fees,
                     String type,String programme,String exam,int cutoff,int eligible,int placed,double averagePackage,double highestPackage,String description){}
    // NIRF 2025 Engineering order anchors the ranking field; the other numeric outcomes are demo estimates.
    private static final List<D> DATA=List.of(
        d("Indian Institute of Technology Madras","Chennai","Tamil Nadu",1,"https://www.iitm.ac.in",1959,1100000,"Public","B.Tech","JEE Advanced",500,900,810,2200000,6800000,"Research-led engineering institute with programmes across computing, core engineering and emerging technologies."),
        d("Indian Institute of Technology Delhi","New Delhi","Delhi",2,"https://home.iitd.ac.in",1961,1050000,"Public","B.Tech","JEE Advanced",600,850,765,2500000,7000000,"Engineering and technology institute in the national capital with a broad research and entrepreneurship community."),
        d("Indian Institute of Technology Bombay","Mumbai","Maharashtra",3,"https://www.iitb.ac.in",1958,1100000,"Public","B.Tech","JEE Advanced",700,850,765,2300000,7000000,"Technology-focused institute in Mumbai offering undergraduate engineering and interdisciplinary study."),
        d("Indian Institute of Technology Kanpur","Kanpur","Uttar Pradesh",4,"https://www.iitk.ac.in",1959,1000000,"Public","B.Tech","JEE Advanced",1200,800,720,2100000,6500000,"Residential research institute with engineering, science and humanities programmes."),
        d("Indian Institute of Technology Kharagpur","Kharagpur","West Bengal",5,"https://www.iitkgp.ac.in",1951,1050000,"Public","B.Tech","JEE Advanced",1800,1000,890,1900000,6000000,"A large multidisciplinary technical university with a wide range of engineering departments."),
        d("Indian Institute of Technology Roorkee","Roorkee","Uttarakhand",6,"https://www.iitr.ac.in",1847,1000000,"Public","B.Tech","JEE Advanced",2200,900,810,1900000,6000000,"Historic technical institute known for engineering, architecture and applied sciences."),
        d("Indian Institute of Technology Hyderabad","Hyderabad","Telangana",7,"https://www.iith.ac.in",2008,1000000,"Public","B.Tech","JEE Advanced",2600,650,585,2000000,6500000,"Young research-focused institute with engineering programmes and an interdisciplinary campus culture."),
        d("Indian Institute of Technology Guwahati","Guwahati","Assam",8,"https://www.iitg.ac.in",1994,1000000,"Public","B.Tech","JEE Advanced",3000,650,580,1900000,6000000,"Residential technical institute serving the Northeast with engineering and research programmes."),
        d("National Institute of Technology Tiruchirappalli","Tiruchirappalli","Tamil Nadu",9,"https://www.nitt.edu",1964,700000,"Public","B.Tech","JEE Main",12000,850,765,1400000,4500000,"National institute offering engineering education and research in the Tiruchirappalli region."),
        d("Indian Institute of Technology (BHU) Varanasi","Varanasi","Uttar Pradesh",10,"https://www.iitbhu.ac.in",1919,1000000,"Public","B.Tech","JEE Advanced",3500,700,630,1800000,5500000,"Engineering institute within the Banaras Hindu University academic community."),
        d("Birla Institute of Technology and Science, Pilani","Pilani","Rajasthan",11,"https://www.bits-pilani.ac.in",1964,2600000,"Deemed","B.E.",null,0,700,630,1900000,6000000,"Private university with engineering programmes and practice-oriented learning; entrance selection uses BITSAT scores."),
        d("Indian Institute of Technology Indore","Indore","Madhya Pradesh",12,"https://www.iiti.ac.in",2009,1000000,"Public","B.Tech","JEE Advanced",4500,550,490,1800000,5500000,"Research-led institute in central India with undergraduate engineering and science programmes."),
        d("National Institute of Technology Rourkela","Rourkela","Odisha",13,"https://www.nitrkl.ac.in",1961,700000,"Public","B.Tech","JEE Main",18000,750,660,1200000,4000000,"Public technical institute with engineering programmes and a residential campus in Odisha."),
        d("SRM Institute of Science and Technology","Chennai","Tamil Nadu",14,"https://www.srmist.edu.in",1985,1800000,"Deemed","B.Tech","SRMJEEE",25000,1200,1020,800000,4000000,"Multidisciplinary university with a broad engineering portfolio and campuses in several regions."),
        d("Indian Institute of Technology (ISM) Dhanbad","Dhanbad","Jharkhand",15,"https://www.iitism.ac.in",1926,1000000,"Public","B.Tech","JEE Advanced",6000,700,630,1600000,5000000,"Technical institute with strengths spanning engineering, earth sciences and energy-related fields."),
        d("Vellore Institute of Technology","Vellore","Tamil Nadu",16,"https://vit.ac.in",1984,1700000,"Private","B.Tech","VITEEE",30000,1500,1275,900000,4500000,"Private university offering a wide selection of engineering programmes at its Vellore campus."),
        d("National Institute of Technology Karnataka, Surathkal","Surathkal","Karnataka",17,"https://www.nitk.ac.in",1960,700000,"Public","B.Tech","JEE Main",15000,750,675,1400000,4500000,"Public technical institute on the Karnataka coast with engineering and applied science programmes."),
        d("Jadavpur University","Kolkata","West Bengal",18,"https://jadavpuruniversity.in",1906,150000,"State University","B.E.","WBJEE",1200,500,450,1200000,4500000,"Public university in Kolkata with engineering, science, arts and interdisciplinary faculties."),
        d("Indian Institute of Technology Patna","Patna","Bihar",19,"https://www.iitp.ac.in",2008,1000000,"Public","B.Tech","JEE Advanced",8000,450,405,1500000,4500000,"Research-oriented technical institute with engineering programmes in Bihar's capital region."),
        d("Anna University","Chennai","Tamil Nadu",20,"https://www.annauniv.edu",1978,250000,"State University","B.E.","TNEA",5000,600,510,900000,3500000,"State technical university in Chennai with engineering education and affiliated institutions.")
    );
    private static D d(String n,String c,String s,int r,String w,int y,double f,String t,String p,String e,int k,int eligible,int placed,double avg,double high,String desc){return new D(n,c,s,r,w,y,f,t,p,e,k,eligible,placed,avg,high,desc);}
}
