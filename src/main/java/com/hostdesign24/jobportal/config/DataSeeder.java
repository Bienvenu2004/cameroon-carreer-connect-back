package com.hostdesign24.jobportal.config;

import com.hostdesign24.jobportal.model.Address;
import com.hostdesign24.jobportal.model.Company;
import com.hostdesign24.jobportal.model.Job;
import com.hostdesign24.jobportal.model.JobSeekerProfile;
import com.hostdesign24.jobportal.model.RecruiterProfile;
import com.hostdesign24.jobportal.model.User;
import com.hostdesign24.jobportal.model.enums.CompanySize;
import com.hostdesign24.jobportal.model.enums.CompanyStatus;
import com.hostdesign24.jobportal.model.enums.Industry;
import com.hostdesign24.jobportal.model.enums.JobSite;
import com.hostdesign24.jobportal.model.enums.JobType;
import com.hostdesign24.jobportal.model.enums.Region;
import com.hostdesign24.jobportal.model.enums.SalaryCurrency;
import com.hostdesign24.jobportal.model.enums.UserRole;
import com.hostdesign24.jobportal.repository.CompanyRepository;
import com.hostdesign24.jobportal.repository.JobRepository;
import com.hostdesign24.jobportal.repository.JobSeekerProfileRepository;
import com.hostdesign24.jobportal.repository.RecruiterProfileRepository;
import com.hostdesign24.jobportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Idempotent demo seeder. Runs on every startup, but only inserts data when
 * the users table is empty — safe for dev, staging, and demo environments.
 *
 * Disable in production by activating the "prod" profile.
 *
 * Demo credentials (all passwords: Password123!):
 *   admin@camjobs.cm                — SYSTEM_ADMIN
 *   recruiter.douala@camjobs.cm     — RECRUITER (approved company)
 *   recruiter.yaounde@camjobs.cm    — RECRUITER (pending company)
 *   recruiter.bamenda@camjobs.cm    — RECRUITER (rejected company)
 *   seeker.alice@camjobs.cm         — JOB_SEEKER
 *   seeker.bob@camjobs.cm           — JOB_SEEKER
 *   seeker.celine@camjobs.cm        — JOB_SEEKER
 *   seeker.daniel@camjobs.cm        — JOB_SEEKER
 *   seeker.estelle@camjobs.cm       — JOB_SEEKER
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "Password123!";

    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("[seed] Users already present, skipping seed.");
            return;
        }
        log.info("[seed] Empty database detected — inserting demo data.");

        seedAll();

        log.info("[seed] Done. Demo accounts (password: {}):", DEMO_PASSWORD);
        log.info("[seed]   admin@camjobs.cm                (SYSTEM_ADMIN)");
        log.info("[seed]   recruiter.douala@camjobs.cm     (RECRUITER, approved company)");
        log.info("[seed]   recruiter.yaounde@camjobs.cm    (RECRUITER, pending company)");
        log.info("[seed]   recruiter.bamenda@camjobs.cm    (RECRUITER, rejected company)");
        log.info("[seed]   seeker.alice@camjobs.cm         (JOB_SEEKER)");
        log.info("[seed]   seeker.bob@camjobs.cm           (JOB_SEEKER)");
        log.info("[seed]   seeker.celine@camjobs.cm        (JOB_SEEKER)");
        log.info("[seed]   seeker.daniel@camjobs.cm        (JOB_SEEKER)");
        log.info("[seed]   seeker.estelle@camjobs.cm       (JOB_SEEKER)");
    }

    private void seedAll() {
        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        /* ---------------- ADMIN ---------------- */
        createUser("admin@camjobs.cm", hash, UserRole.SYSTEM_ADMIN, true);

        /* ---------------- COMPANIES ---------------- */
        Company mtnCm = saveCompany("MTN Cameroon",
                "MTN Cameroon is a leading telecommunications provider serving over 10 million subscribers across the country.",
                "https://www.mtn.cm", Industry.TELECOMMUNICATIONS, CompanySize.ENTERPRISE,
                CompanyStatus.APPROVED, addr("Bonanjo", "Douala", Region.LITTORAL));

        Company afriland = saveCompany("Afriland First Bank",
                "Pan-African bank headquartered in Yaoundé, providing retail, corporate and digital banking.",
                "https://www.afrilandfirstbank.com", Industry.BANKING_FINANCE, CompanySize.LARGE,
                CompanyStatus.APPROVED, addr("Centre Ville", "Yaoundé", Region.CENTRE));

        Company chococam = saveCompany("Chococam SA",
                "Largest confectionery and food manufacturer in Central Africa, makers of Tartina and Mambo.",
                "https://www.chococam.com", Industry.MANUFACTURING, CompanySize.LARGE,
                CompanyStatus.APPROVED, addr("Bassa", "Douala", Region.LITTORAL));

        Company orange = saveCompany("Orange Cameroun",
                "Telecom operator part of the Orange Group, offering mobile, fixed, broadband and Orange Money services.",
                "https://www.orange.cm", Industry.TELECOMMUNICATIONS, CompanySize.ENTERPRISE,
                CompanyStatus.APPROVED, addr("Akwa", "Douala", Region.LITTORAL));

        Company camrail = saveCompany("Camrail",
                "National railway concessionaire — freight, passenger services and logistics across Cameroon.",
                "https://www.camrail.net", Industry.LOGISTICS_TRANSPORT, CompanySize.LARGE,
                CompanyStatus.APPROVED, addr("Bessengue", "Douala", Region.LITTORAL));

        Company sofavinc = saveCompany("SoFaVinc Agribusiness",
                "Cooperative supporting cocoa, coffee and palm-oil farmers across the West and South-West regions.",
                "https://www.sofavinc.cm", Industry.AGRICULTURE, CompanySize.MEDIUM,
                CompanyStatus.APPROVED, addr("Marché A", "Bafoussam", Region.OUEST));

        Company camHealth = saveCompany("Cameroon Baptist Convention Health Services",
                "Faith-based healthcare network operating hospitals and clinics throughout Cameroon.",
                "https://www.cbchealthservices.org", Industry.HEALTHCARE, CompanySize.LARGE,
                CompanyStatus.APPROVED, addr("Hospital Hill", "Bamenda", Region.NORD_OUEST));

        Company eduFirst = saveCompany("EduFirst Cameroon",
                "EdTech startup building bilingual learning platforms for primary and secondary schools.",
                "https://www.edufirst.cm", Industry.EDUCATION, CompanySize.SMALL,
                CompanyStatus.APPROVED, addr("Bastos", "Yaoundé", Region.CENTRE));

        // Pending verification
        Company greenSahel = saveCompany("Green Sahel NGO",
                "NGO promoting climate-resilient agriculture in the Far North region.",
                "https://www.greensahel.org", Industry.NGO_NONPROFIT, CompanySize.SMALL,
                CompanyStatus.PENDING, addr("Domayo", "Maroua", Region.EXTREME_NORD));

        // Rejected
        Company suspicious = saveCompany("QuickRich Holdings",
                "Investment firm with insufficient documentation provided.",
                "https://www.quickrich.cm", Industry.OTHER, CompanySize.MICRO,
                CompanyStatus.REJECTED, addr("Mfoundi", "Yaoundé", Region.CENTRE));
        suspicious.setRejectionReason("Could not verify business registration documents.");
        companyRepository.save(suspicious);

        /* ---------------- RECRUITERS ---------------- */
        User recDouala = createUser("recruiter.douala@camjobs.cm", hash, UserRole.RECRUITER, true);
        recruiterProfile(recDouala, "Marie", "Kouam", "MTN Cameroon",
                "Bonanjo", "Littoral", "Cameroon");

        User recYaounde = createUser("recruiter.yaounde@camjobs.cm", hash, UserRole.RECRUITER, true);
        recruiterProfile(recYaounde, "Jean-Paul", "Mballa", "Green Sahel NGO",
                "Domayo", "Extrême-Nord", "Cameroon");

        User recBamenda = createUser("recruiter.bamenda@camjobs.cm", hash, UserRole.RECRUITER, true);
        recruiterProfile(recBamenda, "Patience", "Ngwa", "QuickRich Holdings",
                "Commercial Avenue", "Nord-Ouest", "Cameroon");

        /* ---------------- JOB SEEKERS ---------------- */
        User alice = createUser("seeker.alice@camjobs.cm", hash, UserRole.JOB_SEEKER, true);
        jobSeekerProfile(alice, "Alice", "Tchatchoua", "+237 6 90 11 22 33",
                "Bali", "Douala", "Cameroon", Region.LITTORAL);

        User bob = createUser("seeker.bob@camjobs.cm", hash, UserRole.JOB_SEEKER, true);
        jobSeekerProfile(bob, "Bob", "Mbida", "+237 6 77 22 33 44",
                "Bastos", "Yaoundé", "Cameroon", Region.CENTRE);

        User celine = createUser("seeker.celine@camjobs.cm", hash, UserRole.JOB_SEEKER, true);
        jobSeekerProfile(celine, "Céline", "Ngono", "+237 6 55 33 44 55",
                "Marché B", "Bafoussam", "Cameroon", Region.OUEST);

        User daniel = createUser("seeker.daniel@camjobs.cm", hash, UserRole.JOB_SEEKER, true);
        jobSeekerProfile(daniel, "Daniel", "Fonkou", "+237 6 91 22 33 44",
                "Up Station", "Buea", "Cameroon", Region.SUD_OUEST);

        User estelle = createUser("seeker.estelle@camjobs.cm", hash, UserRole.JOB_SEEKER, true);
        jobSeekerProfile(estelle, "Estelle", "Ayuk", "+237 6 78 99 88 77",
                "Old Town", "Bamenda", "Cameroon", Region.NORD_OUEST);

        /* ---------------- JOBS ---------------- */
        // MTN Cameroon
        seedJob(mtnCm, recDouala, "Senior Network Engineer",
                "Lead engineer for our LTE/5G core network. Plan capacity, troubleshoot incidents, mentor junior engineers.",
                "Health insurance · Annual bonus · Mobile data allowance · Training budget",
                JobType.FULL_TIME, JobSite.HYBRID,
                new BigDecimal("1500000"), Region.LITTORAL, "Bonanjo", "Douala");

        seedJob(mtnCm, recDouala, "Customer Experience Associate",
                "Front-line support for MTN Mobile Money customers. Bilingual English/French required.",
                "Performance bonus · Health insurance · Training",
                JobType.FULL_TIME, JobSite.ONSITE,
                new BigDecimal("450000"), Region.LITTORAL, "Akwa", "Douala");

        seedJob(mtnCm, recDouala, "Data Engineer (Python/Spark)",
                "Build and maintain our customer-360 data lake. Strong SQL + PySpark experience required.",
                "Stock options · Remote-friendly · Health insurance",
                JobType.FULL_TIME, JobSite.REMOTE,
                new BigDecimal("1200000"), Region.LITTORAL, "Bonanjo", "Douala");

        // Afriland
        seedJob(afriland, recDouala, "Credit Risk Analyst",
                "Assess loan applications using our internal scoring model and recommend approval levels.",
                "Annual bonus · Pension scheme · Health insurance",
                JobType.FULL_TIME, JobSite.ONSITE,
                new BigDecimal("950000"), Region.CENTRE, "Centre Ville", "Yaoundé");

        seedJob(afriland, recDouala, "Mobile Banking Product Manager",
                "Own the roadmap for our mobile banking app. Coordinate with engineering, marketing and risk.",
                "Performance bonus · Stock options · Health insurance",
                JobType.FULL_TIME, JobSite.HYBRID,
                new BigDecimal("1400000"), Region.CENTRE, "Centre Ville", "Yaoundé");

        // Chococam
        seedJob(chococam, recDouala, "Production Supervisor",
                "Supervise the daytime shift at our Bassa factory. Enforce quality and safety procedures.",
                "Performance bonus · Health insurance · Subsidized lunch",
                JobType.FULL_TIME, JobSite.ONSITE,
                new BigDecimal("750000"), Region.LITTORAL, "Bassa", "Douala");

        seedJob(chococam, recDouala, "Quality Assurance Technician",
                "Test incoming raw cocoa and outgoing batches against our quality standards.",
                "Health insurance · Subsidized transport",
                JobType.FULL_TIME, JobSite.ONSITE,
                new BigDecimal("520000"), Region.LITTORAL, "Bassa", "Douala");

        // Orange
        seedJob(orange, recDouala, "iOS Engineer (Orange Money)",
                "Ship features in the Orange Money app — Swift, modern architecture, secure-by-default.",
                "Stock options · Remote-friendly · Health insurance · Equipment budget",
                JobType.FULL_TIME, JobSite.REMOTE,
                new BigDecimal("1300000"), Region.LITTORAL, "Akwa", "Douala");

        seedJob(orange, recDouala, "Marketing Campaign Manager",
                "Plan, run and measure cross-channel campaigns for our consumer mobile offers.",
                "Performance bonus · Mobile allowance · Health insurance",
                JobType.FULL_TIME, JobSite.HYBRID,
                new BigDecimal("1100000"), Region.LITTORAL, "Akwa", "Douala");

        seedJob(orange, recDouala, "Retail Sales Advisor (Bamenda)",
                "Welcome customers in our Bamenda store and advise them on prepaid and postpaid plans.",
                "Sales commission · Health insurance",
                JobType.FULL_TIME, JobSite.ONSITE,
                new BigDecimal("280000"), Region.NORD_OUEST, "Commercial Avenue", "Bamenda");

        // Camrail
        seedJob(camrail, recDouala, "Locomotive Mechanical Engineer",
                "Maintenance lead for our diesel-electric locomotive fleet at the Bessengue depot.",
                "Health insurance · Pension · Subsidized housing",
                JobType.FULL_TIME, JobSite.ONSITE,
                new BigDecimal("1050000"), Region.LITTORAL, "Bessengue", "Douala");

        seedJob(camrail, recDouala, "Operations Coordinator (Ngaoundéré)",
                "Coordinate freight loading, train dispatch and customer communication at our Ngaoundéré yard.",
                "Health insurance · Travel allowance",
                JobType.FULL_TIME, JobSite.ONSITE,
                new BigDecimal("680000"), Region.ADAMAOUA, "Gare", "Ngaoundéré");

        // SoFaVinc Agribusiness
        seedJob(sofavinc, recDouala, "Field Agronomist",
                "Train and support cocoa cooperative members in the Ouest region. Frequent field visits.",
                "Vehicle allowance · Health insurance · Per diem",
                JobType.FULL_TIME, JobSite.ONSITE,
                new BigDecimal("550000"), Region.OUEST, "Marché A", "Bafoussam");

        seedJob(sofavinc, recDouala, "Cooperative Finance Officer",
                "Track member payouts, reconcile bank transactions, prepare monthly cooperative statements.",
                "Health insurance · Annual bonus",
                JobType.FULL_TIME, JobSite.HYBRID,
                new BigDecimal("620000"), Region.OUEST, "Marché A", "Bafoussam");

        // Cameroon Baptist Health
        seedJob(camHealth, recDouala, "Registered Nurse — Mbingo Baptist Hospital",
                "Ward nursing care, patient education, supervision of nursing assistants. State Registered Nurse required.",
                "Subsidized housing · Health insurance · Continuing education",
                JobType.FULL_TIME, JobSite.ONSITE,
                new BigDecimal("450000"), Region.NORD_OUEST, "Mbingo", "Bamenda");

        seedJob(camHealth, recDouala, "Public-Health Programme Coordinator",
                "Coordinate our maternal-health outreach across the Northwest. Driving licence essential.",
                "Vehicle allowance · Health insurance",
                JobType.FULL_TIME, JobSite.HYBRID,
                new BigDecimal("780000"), Region.NORD_OUEST, "Hospital Hill", "Bamenda");

        // EduFirst
        seedJob(eduFirst, recDouala, "Full-Stack Developer (React + Node)",
                "Build features in our K-12 learning platform. Bilingual French/English content rendering.",
                "Stock options · Equipment budget · Health insurance · Remote-first",
                JobType.FULL_TIME, JobSite.REMOTE,
                new BigDecimal("950000"), Region.CENTRE, "Bastos", "Yaoundé");

        seedJob(eduFirst, recDouala, "Pedagogical Content Designer (FR)",
                "Design lesson scripts and learning sequences for our French primary curriculum.",
                "Health insurance · Annual bonus",
                JobType.PART_TIME, JobSite.REMOTE,
                new BigDecimal("400000"), Region.CENTRE, "Bastos", "Yaoundé");

        seedJob(eduFirst, recDouala, "Customer Success Lead (Schools)",
                "Onboard partner schools, train teachers, monitor adoption KPIs. Frequent travel.",
                "Travel allowance · Health insurance",
                JobType.FULL_TIME, JobSite.HYBRID,
                new BigDecimal("820000"), Region.CENTRE, "Bastos", "Yaoundé");

        // South region opportunity
        seedJob(camHealth, recDouala, "Community Health Worker (Sud)",
                "Support outreach clinics in rural Sud. Vaccination drives, mother-and-child care.",
                "Per diem · Health insurance",
                JobType.CONTRACT, JobSite.ONSITE,
                new BigDecimal("320000"), Region.SUD, "Centre Ville", "Ebolowa");

        // Est region
        seedJob(sofavinc, recDouala, "Forest-Coffee Project Lead (Est)",
                "Lead our agroforestry coffee initiative in the Est region. Strong project-management background required.",
                "Field allowance · Health insurance",
                JobType.FULL_TIME, JobSite.ONSITE,
                new BigDecimal("900000"), Region.EST, "Centre Ville", "Bertoua");

        // Internship
        seedJob(eduFirst, recDouala, "Software Engineering Intern (6 months)",
                "Pair with our senior engineers on real product features. Stipend + mentorship.",
                "Stipend · Mentorship · Possibility of full-time offer",
                JobType.INTERN, JobSite.HYBRID,
                new BigDecimal("150000"), Region.CENTRE, "Bastos", "Yaoundé");

        log.info("[seed] Inserted {} users, {} companies, {} jobs.",
                userRepository.count(), companyRepository.count(), jobRepository.count());
    }

    /* ---------------- helpers ---------------- */

    private User createUser(String email, String passwordHash, UserRole role, boolean active) {
        User u = new User();
        u.setEmail(email);
        u.setPassword(passwordHash);
        u.setRole(role);
        u.setActive(active);
        u.setRegistrationDate(LocalDate.now().minusDays(30));
        return userRepository.save(u);
    }

    private Address addr(String street, String city, Region region) {
        Address a = new Address();
        a.setStreet(street);
        a.setCity(city);
        a.setRegion(region);
        a.setCountry("Cameroon");
        a.setStateRegion(region.getDisplayName());
        return a;
    }

    private Company saveCompany(String name, String description, String website,
                                Industry industry, CompanySize size,
                                CompanyStatus status, Address address) {
        Company c = new Company();
        c.setName(name);
        c.setDescription(description);
        c.setWebsite(website);
        c.setIndustry(industry);
        c.setSize(size);
        c.setStatus(status);
        if (status == CompanyStatus.APPROVED) {
            c.setVerifiedAt(LocalDateTime.now().minusDays(20));
        }
        c.setAddress(address);
        return companyRepository.save(c);
    }

    private void recruiterProfile(User user, String first, String last, String company,
                                  String city, String state, String country) {
        RecruiterProfile p = new RecruiterProfile(user);
        p.setFirstName(first);
        p.setLastName(last);
        p.setCompany(company);
        p.setCity(city);
        p.setState(state);
        p.setCountry(country);
        recruiterProfileRepository.save(p);
    }

    private void jobSeekerProfile(User user, String first, String last, String phone,
                                  String city, String stateOrCity, String country, Region region) {
        JobSeekerProfile p = new JobSeekerProfile(user);
        p.setFirstName(first);
        p.setLastName(last);
        p.setPhoneNumber(phone);
        Address a = new Address();
        a.setCity(city);
        a.setCountry(country);
        a.setRegion(region);
        a.setStateRegion(region.getDisplayName());
        p.setAddress(a);
        jobSeekerProfileRepository.save(p);
    }

    private Job seedJob(Company company, User postedBy, String title, String description,
                        String benefits, JobType type, JobSite site, BigDecimal salary,
                        Region region, String street, String city) {
        Job j = new Job();
        j.setCompany(company);
        j.setTitle(title);
        j.setDescription(description);
        j.setBenefits(benefits);
        j.setType(type);
        j.setSite(site);
        j.setSalary(salary);
        j.setSalaryCurrency(SalaryCurrency.XAF);
        j.setActive(true);
        j.setPostedDate(LocalDate.now().minusDays((long) (Math.random() * 21)));
        Address a = new Address();
        a.setStreet(street);
        a.setCity(city);
        a.setRegion(region);
        a.setStateRegion(region.getDisplayName());
        a.setCountry("Cameroon");
        j.setLocation(a);
        // Fake some views to make the analytics look alive.
        j.setViews((int) (Math.random() * 250) + 30);
        return jobRepository.save(j);
    }
}
