-- =============================================================================
-- V1__seed_demo_data.sql
-- =============================================================================
-- Idempotent demo data seed. Replaces the old DataSeeder.java.
--
-- Demo credentials (all use the same password: Password123!):
--   admin@camjobs.cm                — SYSTEM_ADMIN
--   recruiter.douala@camjobs.cm     — RECRUITER (owns "MTN Cameroon", APPROVED)
--   recruiter.yaounde@camjobs.cm    — RECRUITER (owns "Green Sahel NGO", PENDING)
--   recruiter.bamenda@camjobs.cm    — RECRUITER (owns "QuickRich Holdings", REJECTED)
--   seeker.alice@camjobs.cm         — JOB_SEEKER
--   seeker.bob@camjobs.cm           — JOB_SEEKER
--   seeker.celine@camjobs.cm        — JOB_SEEKER
--   seeker.daniel@camjobs.cm        — JOB_SEEKER
--   seeker.estelle@camjobs.cm       — JOB_SEEKER
--
-- Idempotency:
--   Every INSERT uses ON CONFLICT (id) DO NOTHING (or (email) for users).
--   Re-running this migration is a no-op once the data is present.
--
-- Schema-not-ready guard:
--   The outer DO block checks for the existence of the `users` table. On a
--   brand-new database where Flyway runs before Hibernate has had a chance
--   to create the schema (`spring.jpa.hibernate.ddl-auto=update`), the block
--   raises a NOTICE and exits. After Hibernate creates the tables you can
--   re-trigger this migration by removing its row from `flyway_schema_history`
--   and restarting, or by simply running it manually.
-- =============================================================================

DO $$
DECLARE
    -- Spring Security DelegatingPasswordEncoder bcrypt hash for "Password123!"
    bcrypt_password TEXT := '{bcrypt}$2b$10$1GNvREL5n7Z/07sHxbL4YOABOh0.f/uAhqDRZ3pRvemXnZpcgs2CO';

    -- Fixed UUIDs so re-runs and inter-table foreign keys stay stable.
    admin_id        UUID := '10000000-0000-0000-0000-000000000001';
    rec_douala_id   UUID := '10000000-0000-0000-0000-000000000002';
    rec_yaounde_id  UUID := '10000000-0000-0000-0000-000000000003';
    rec_bamenda_id  UUID := '10000000-0000-0000-0000-000000000004';
    alice_id        UUID := '10000000-0000-0000-0000-000000000005';
    bob_id          UUID := '10000000-0000-0000-0000-000000000006';
    celine_id       UUID := '10000000-0000-0000-0000-000000000007';
    daniel_id       UUID := '10000000-0000-0000-0000-000000000008';
    estelle_id      UUID := '10000000-0000-0000-0000-000000000009';

    mtn_id          UUID := '20000000-0000-0000-0000-000000000001';
    afriland_id     UUID := '20000000-0000-0000-0000-000000000002';
    chococam_id     UUID := '20000000-0000-0000-0000-000000000003';
    orange_id       UUID := '20000000-0000-0000-0000-000000000004';
    camrail_id      UUID := '20000000-0000-0000-0000-000000000005';
    sofavinc_id     UUID := '20000000-0000-0000-0000-000000000006';
    cam_health_id   UUID := '20000000-0000-0000-0000-000000000007';
    edu_first_id    UUID := '20000000-0000-0000-0000-000000000008';
    green_sahel_id  UUID := '20000000-0000-0000-0000-000000000009';
    quickrich_id    UUID := '20000000-0000-0000-0000-00000000000a';
BEGIN
    -- Bail out cleanly if Hibernate hasn't created the schema yet.
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
         WHERE table_schema = current_schema()
           AND table_name   = 'users'
    ) THEN
        RAISE NOTICE 'V2__seed_demo_data: `users` table not found — Hibernate has not initialized the schema yet. Skipping seed.';
        RETURN;
    END IF;

    -- =========================================================================
    -- USERS
    -- =========================================================================
    INSERT INTO users (id, email, password, is_active, role, registration_date, deleted, created_at, updated_at)
    VALUES
        (admin_id,        'admin@camjobs.cm',              bcrypt_password, true, 'SYSTEM_ADMIN', CURRENT_DATE - 30, false, NOW(), NOW()),
        (rec_douala_id,   'recruiter.douala@camjobs.cm',   bcrypt_password, true, 'RECRUITER',    CURRENT_DATE - 30, false, NOW(), NOW()),
        (rec_yaounde_id,  'recruiter.yaounde@camjobs.cm',  bcrypt_password, true, 'RECRUITER',    CURRENT_DATE - 30, false, NOW(), NOW()),
        (rec_bamenda_id,  'recruiter.bamenda@camjobs.cm',  bcrypt_password, true, 'RECRUITER',    CURRENT_DATE - 30, false, NOW(), NOW()),
        (alice_id,        'seeker.alice@camjobs.cm',       bcrypt_password, true, 'JOB_SEEKER',   CURRENT_DATE - 30, false, NOW(), NOW()),
        (bob_id,          'seeker.bob@camjobs.cm',         bcrypt_password, true, 'JOB_SEEKER',   CURRENT_DATE - 30, false, NOW(), NOW()),
        (celine_id,       'seeker.celine@camjobs.cm',      bcrypt_password, true, 'JOB_SEEKER',   CURRENT_DATE - 30, false, NOW(), NOW()),
        (daniel_id,       'seeker.daniel@camjobs.cm',      bcrypt_password, true, 'JOB_SEEKER',   CURRENT_DATE - 30, false, NOW(), NOW()),
        (estelle_id,      'seeker.estelle@camjobs.cm',     bcrypt_password, true, 'JOB_SEEKER',   CURRENT_DATE - 30, false, NOW(), NOW())
    ON CONFLICT (email) DO NOTHING;

    -- =========================================================================
    -- RECRUITER PROFILES  (id == user_id via @MapsId)
    -- =========================================================================
    INSERT INTO recruiter_profiles (id, user_id, first_name, last_name, city, state, country, company, deleted, created_at, updated_at)
    VALUES
        (rec_douala_id,  rec_douala_id,  'Marie',       'Kouam',  'Bonanjo',           'Littoral',     'Cameroon', 'MTN Cameroon',       false, NOW(), NOW()),
        (rec_yaounde_id, rec_yaounde_id, 'Jean-Paul',   'Mballa', 'Domayo',            'Extrême-Nord', 'Cameroon', 'Green Sahel NGO',    false, NOW(), NOW()),
        (rec_bamenda_id, rec_bamenda_id, 'Patience',    'Ngwa',   'Commercial Avenue', 'Nord-Ouest',   'Cameroon', 'QuickRich Holdings', false, NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

    -- =========================================================================
    -- JOB SEEKER PROFILES  (id == user_id via @MapsId)
    -- Embedded Address fields are stored on this table.
    -- =========================================================================
    INSERT INTO job_seeker_profiles (id, user_id, first_name, last_name, phone_number,
                                     street, city, state_region, region, country,
                                     deleted, created_at, updated_at)
    VALUES
        (alice_id,   alice_id,   'Alice',   'Tchatchoua', '+237 6 90 11 22 33', 'Bali',     'Douala',    'Littoral',   'LITTORAL',   'Cameroon', false, NOW(), NOW()),
        (bob_id,     bob_id,     'Bob',     'Mbida',      '+237 6 77 22 33 44', 'Bastos',   'Yaoundé',   'Centre',     'CENTRE',     'Cameroon', false, NOW(), NOW()),
        (celine_id,  celine_id,  'Céline',  'Ngono',      '+237 6 55 33 44 55', 'Marché B', 'Bafoussam', 'Ouest',      'OUEST',      'Cameroon', false, NOW(), NOW()),
        (daniel_id,  daniel_id,  'Daniel',  'Fonkou',     '+237 6 91 22 33 44', 'Up Station', 'Buea',    'Sud-Ouest',  'SUD_OUEST',  'Cameroon', false, NOW(), NOW()),
        (estelle_id, estelle_id, 'Estelle', 'Ayuk',       '+237 6 78 99 88 77', 'Old Town', 'Bamenda',   'Nord-Ouest', 'NORD_OUEST', 'Cameroon', false, NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

    -- =========================================================================
    -- COMPANIES
    -- Embedded Address fields live on the same row.
    -- =========================================================================
    INSERT INTO companies (id, name, description, website, industry, size, status,
                           rejection_reason, verified_at,
                           street, city, state_region, region, country,
                           deleted, created_at, updated_at, created_by)
    VALUES
        (mtn_id,         'MTN Cameroon',
            'MTN Cameroon is a leading telecommunications provider serving over 10 million subscribers across the country.',
            'https://www.mtn.cm', 'TELECOMMUNICATIONS', 'ENTERPRISE', 'APPROVED', NULL, NOW() - INTERVAL '20 days',
            'Bonanjo', 'Douala', 'Littoral', 'LITTORAL', 'Cameroon',
            false, NOW(), NOW(), rec_douala_id),

        (afriland_id,    'Afriland First Bank',
            'Pan-African bank headquartered in Yaoundé, providing retail, corporate and digital banking.',
            'https://www.afrilandfirstbank.com', 'BANKING_FINANCE', 'LARGE', 'APPROVED', NULL, NOW() - INTERVAL '20 days',
            'Centre Ville', 'Yaoundé', 'Centre', 'CENTRE', 'Cameroon',
            false, NOW(), NOW(), NULL),

        (chococam_id,    'Chococam SA',
            'Largest confectionery and food manufacturer in Central Africa, makers of Tartina and Mambo.',
            'https://www.chococam.com', 'MANUFACTURING', 'LARGE', 'APPROVED', NULL, NOW() - INTERVAL '20 days',
            'Bassa', 'Douala', 'Littoral', 'LITTORAL', 'Cameroon',
            false, NOW(), NOW(), NULL),

        (orange_id,      'Orange Cameroun',
            'Telecom operator part of the Orange Group, offering mobile, fixed, broadband and Orange Money services.',
            'https://www.orange.cm', 'TELECOMMUNICATIONS', 'ENTERPRISE', 'APPROVED', NULL, NOW() - INTERVAL '20 days',
            'Akwa', 'Douala', 'Littoral', 'LITTORAL', 'Cameroon',
            false, NOW(), NOW(), NULL),

        (camrail_id,     'Camrail',
            'National railway concessionaire — freight, passenger services and logistics across Cameroon.',
            'https://www.camrail.net', 'LOGISTICS_TRANSPORT', 'LARGE', 'APPROVED', NULL, NOW() - INTERVAL '20 days',
            'Bessengue', 'Douala', 'Littoral', 'LITTORAL', 'Cameroon',
            false, NOW(), NOW(), NULL),

        (sofavinc_id,    'SoFaVinc Agribusiness',
            'Cooperative supporting cocoa, coffee and palm-oil farmers across the West and South-West regions.',
            'https://www.sofavinc.cm', 'AGRICULTURE', 'MEDIUM', 'APPROVED', NULL, NOW() - INTERVAL '20 days',
            'Marché A', 'Bafoussam', 'Ouest', 'OUEST', 'Cameroon',
            false, NOW(), NOW(), NULL),

        (cam_health_id,  'Cameroon Baptist Convention Health Services',
            'Faith-based healthcare network operating hospitals and clinics throughout Cameroon.',
            'https://www.cbchealthservices.org', 'HEALTHCARE', 'LARGE', 'APPROVED', NULL, NOW() - INTERVAL '20 days',
            'Hospital Hill', 'Bamenda', 'Nord-Ouest', 'NORD_OUEST', 'Cameroon',
            false, NOW(), NOW(), NULL),

        (edu_first_id,   'EduFirst Cameroon',
            'EdTech startup building bilingual learning platforms for primary and secondary schools.',
            'https://www.edufirst.cm', 'EDUCATION', 'SMALL', 'APPROVED', NULL, NOW() - INTERVAL '20 days',
            'Bastos', 'Yaoundé', 'Centre', 'CENTRE', 'Cameroon',
            false, NOW(), NOW(), NULL),

        (green_sahel_id, 'Green Sahel NGO',
            'NGO promoting climate-resilient agriculture in the Far North region.',
            'https://www.greensahel.org', 'NGO_NONPROFIT', 'SMALL', 'PENDING', NULL, NULL,
            'Domayo', 'Maroua', 'Extrême-Nord', 'EXTREME_NORD', 'Cameroon',
            false, NOW(), NOW(), rec_yaounde_id),

        (quickrich_id,   'QuickRich Holdings',
            'Investment firm with insufficient documentation provided.',
            'https://www.quickrich.cm', 'OTHER', 'MICRO', 'REJECTED',
            'Could not verify business registration documents.', NULL,
            'Mfoundi', 'Yaoundé', 'Centre', 'CENTRE', 'Cameroon',
            false, NOW(), NOW(), rec_bamenda_id)
    ON CONFLICT (id) DO NOTHING;

    -- =========================================================================
    -- JOBS  (createdBy = the recruiter who owns the company)
    -- =========================================================================
    INSERT INTO jobs (id, company_id, is_active, is_saved, views, title, description, benefits,
                      type, salary, salary_currency, site, posted_date,
                      street, city, state_region, region, country,
                      deleted, created_at, updated_at, created_by)
    VALUES
        -- MTN Cameroon ------------------------------------------------------
        ('30000000-0000-0000-0000-000000000001', mtn_id,        true, false, 142,
         'Senior Network Engineer',
         'Lead engineer for our LTE/5G core network. Plan capacity, troubleshoot incidents, mentor junior engineers.',
         'Health insurance · Annual bonus · Mobile data allowance · Training budget',
         'FULL_TIME', 1500000, 'XAF', 'HYBRID', CURRENT_DATE - 5,
         'Bonanjo', 'Douala', 'Littoral', 'LITTORAL', 'Cameroon',
         false, NOW(), NOW(), rec_douala_id),

        ('30000000-0000-0000-0000-000000000002', mtn_id,        true, false, 87,
         'Customer Experience Associate',
         'Front-line support for MTN Mobile Money customers. Bilingual English/French required.',
         'Performance bonus · Health insurance · Training',
         'FULL_TIME', 450000, 'XAF', 'ONSITE', CURRENT_DATE - 8,
         'Akwa', 'Douala', 'Littoral', 'LITTORAL', 'Cameroon',
         false, NOW(), NOW(), rec_douala_id),

        ('30000000-0000-0000-0000-000000000003', mtn_id,        true, false, 211,
         'Data Engineer (Python/Spark)',
         'Build and maintain our customer-360 data lake. Strong SQL + PySpark experience required.',
         'Stock options · Remote-friendly · Health insurance',
         'FULL_TIME', 1200000, 'XAF', 'REMOTE', CURRENT_DATE - 3,
         'Bonanjo', 'Douala', 'Littoral', 'LITTORAL', 'Cameroon',
         false, NOW(), NOW(), rec_douala_id),

        -- Afriland First Bank ----------------------------------------------
        ('30000000-0000-0000-0000-000000000004', afriland_id,   true, false, 95,
         'Credit Risk Analyst',
         'Assess loan applications using our internal scoring model and recommend approval levels.',
         'Annual bonus · Pension scheme · Health insurance',
         'FULL_TIME', 950000, 'XAF', 'ONSITE', CURRENT_DATE - 12,
         'Centre Ville', 'Yaoundé', 'Centre', 'CENTRE', 'Cameroon',
         false, NOW(), NOW(), NULL),

        ('30000000-0000-0000-0000-000000000005', afriland_id,   true, false, 168,
         'Mobile Banking Product Manager',
         'Own the roadmap for our mobile banking app. Coordinate with engineering, marketing and risk.',
         'Performance bonus · Stock options · Health insurance',
         'FULL_TIME', 1400000, 'XAF', 'HYBRID', CURRENT_DATE - 6,
         'Centre Ville', 'Yaoundé', 'Centre', 'CENTRE', 'Cameroon',
         false, NOW(), NOW(), NULL),

        -- Chococam ---------------------------------------------------------
        ('30000000-0000-0000-0000-000000000006', chococam_id,   true, false, 64,
         'Production Supervisor',
         'Supervise the daytime shift at our Bassa factory. Enforce quality and safety procedures.',
         'Performance bonus · Health insurance · Subsidized lunch',
         'FULL_TIME', 750000, 'XAF', 'ONSITE', CURRENT_DATE - 14,
         'Bassa', 'Douala', 'Littoral', 'LITTORAL', 'Cameroon',
         false, NOW(), NOW(), NULL),

        ('30000000-0000-0000-0000-000000000007', chococam_id,   true, false, 39,
         'Quality Assurance Technician',
         'Test incoming raw cocoa and outgoing batches against our quality standards.',
         'Health insurance · Subsidized transport',
         'FULL_TIME', 520000, 'XAF', 'ONSITE', CURRENT_DATE - 18,
         'Bassa', 'Douala', 'Littoral', 'LITTORAL', 'Cameroon',
         false, NOW(), NOW(), NULL),

        -- Orange Cameroun --------------------------------------------------
        ('30000000-0000-0000-0000-000000000008', orange_id,     true, false, 187,
         'iOS Engineer (Orange Money)',
         'Ship features in the Orange Money app — Swift, modern architecture, secure-by-default.',
         'Stock options · Remote-friendly · Health insurance · Equipment budget',
         'FULL_TIME', 1300000, 'XAF', 'REMOTE', CURRENT_DATE - 4,
         'Akwa', 'Douala', 'Littoral', 'LITTORAL', 'Cameroon',
         false, NOW(), NOW(), NULL),

        ('30000000-0000-0000-0000-000000000009', orange_id,     true, false, 122,
         'Marketing Campaign Manager',
         'Plan, run and measure cross-channel campaigns for our consumer mobile offers.',
         'Performance bonus · Mobile allowance · Health insurance',
         'FULL_TIME', 1100000, 'XAF', 'HYBRID', CURRENT_DATE - 9,
         'Akwa', 'Douala', 'Littoral', 'LITTORAL', 'Cameroon',
         false, NOW(), NOW(), NULL),

        ('30000000-0000-0000-0000-00000000000a', orange_id,     true, false, 58,
         'Retail Sales Advisor (Bamenda)',
         'Welcome customers in our Bamenda store and advise them on prepaid and postpaid plans.',
         'Sales commission · Health insurance',
         'FULL_TIME', 280000, 'XAF', 'ONSITE', CURRENT_DATE - 11,
         'Commercial Avenue', 'Bamenda', 'Nord-Ouest', 'NORD_OUEST', 'Cameroon',
         false, NOW(), NOW(), NULL),

        -- Camrail ----------------------------------------------------------
        ('30000000-0000-0000-0000-00000000000b', camrail_id,    true, false, 71,
         'Locomotive Mechanical Engineer',
         'Maintenance lead for our diesel-electric locomotive fleet at the Bessengue depot.',
         'Health insurance · Pension · Subsidized housing',
         'FULL_TIME', 1050000, 'XAF', 'ONSITE', CURRENT_DATE - 7,
         'Bessengue', 'Douala', 'Littoral', 'LITTORAL', 'Cameroon',
         false, NOW(), NOW(), NULL),

        ('30000000-0000-0000-0000-00000000000c', camrail_id,    true, false, 44,
         'Operations Coordinator (Ngaoundéré)',
         'Coordinate freight loading, train dispatch and customer communication at our Ngaoundéré yard.',
         'Health insurance · Travel allowance',
         'FULL_TIME', 680000, 'XAF', 'ONSITE', CURRENT_DATE - 15,
         'Gare', 'Ngaoundéré', 'Adamaoua', 'ADAMAOUA', 'Cameroon',
         false, NOW(), NOW(), NULL),

        -- SoFaVinc Agribusiness --------------------------------------------
        ('30000000-0000-0000-0000-00000000000d', sofavinc_id,   true, false, 53,
         'Field Agronomist',
         'Train and support cocoa cooperative members in the Ouest region. Frequent field visits.',
         'Vehicle allowance · Health insurance · Per diem',
         'FULL_TIME', 550000, 'XAF', 'ONSITE', CURRENT_DATE - 13,
         'Marché A', 'Bafoussam', 'Ouest', 'OUEST', 'Cameroon',
         false, NOW(), NOW(), NULL),

        ('30000000-0000-0000-0000-00000000000e', sofavinc_id,   true, false, 49,
         'Cooperative Finance Officer',
         'Track member payouts, reconcile bank transactions, prepare monthly cooperative statements.',
         'Health insurance · Annual bonus',
         'FULL_TIME', 620000, 'XAF', 'HYBRID', CURRENT_DATE - 16,
         'Marché A', 'Bafoussam', 'Ouest', 'OUEST', 'Cameroon',
         false, NOW(), NOW(), NULL),

        -- Cameroon Baptist Health Services ---------------------------------
        ('30000000-0000-0000-0000-00000000000f', cam_health_id, true, false, 82,
         'Registered Nurse — Mbingo Baptist Hospital',
         'Ward nursing care, patient education, supervision of nursing assistants. State Registered Nurse required.',
         'Subsidized housing · Health insurance · Continuing education',
         'FULL_TIME', 450000, 'XAF', 'ONSITE', CURRENT_DATE - 10,
         'Mbingo', 'Bamenda', 'Nord-Ouest', 'NORD_OUEST', 'Cameroon',
         false, NOW(), NOW(), NULL),

        ('30000000-0000-0000-0000-000000000010', cam_health_id, true, false, 67,
         'Public-Health Programme Coordinator',
         'Coordinate our maternal-health outreach across the Northwest. Driving licence essential.',
         'Vehicle allowance · Health insurance',
         'FULL_TIME', 780000, 'XAF', 'HYBRID', CURRENT_DATE - 17,
         'Hospital Hill', 'Bamenda', 'Nord-Ouest', 'NORD_OUEST', 'Cameroon',
         false, NOW(), NOW(), NULL),

        -- EduFirst Cameroon ------------------------------------------------
        ('30000000-0000-0000-0000-000000000011', edu_first_id,  true, false, 204,
         'Full-Stack Developer (React + Node)',
         'Build features in our K-12 learning platform. Bilingual French/English content rendering.',
         'Stock options · Equipment budget · Health insurance · Remote-first',
         'FULL_TIME', 950000, 'XAF', 'REMOTE', CURRENT_DATE - 2,
         'Bastos', 'Yaoundé', 'Centre', 'CENTRE', 'Cameroon',
         false, NOW(), NOW(), NULL),

        ('30000000-0000-0000-0000-000000000012', edu_first_id,  true, false, 33,
         'Pedagogical Content Designer (FR)',
         'Design lesson scripts and learning sequences for our French primary curriculum.',
         'Health insurance · Annual bonus',
         'PART_TIME', 400000, 'XAF', 'REMOTE', CURRENT_DATE - 19,
         'Bastos', 'Yaoundé', 'Centre', 'CENTRE', 'Cameroon',
         false, NOW(), NOW(), NULL),

        ('30000000-0000-0000-0000-000000000013', edu_first_id,  true, false, 76,
         'Customer Success Lead (Schools)',
         'Onboard partner schools, train teachers, monitor adoption KPIs. Frequent travel.',
         'Travel allowance · Health insurance',
         'FULL_TIME', 820000, 'XAF', 'HYBRID', CURRENT_DATE - 8,
         'Bastos', 'Yaoundé', 'Centre', 'CENTRE', 'Cameroon',
         false, NOW(), NOW(), NULL),

        -- Sud region opportunity -------------------------------------------
        ('30000000-0000-0000-0000-000000000014', cam_health_id, true, false, 41,
         'Community Health Worker (Sud)',
         'Support outreach clinics in rural Sud. Vaccination drives, mother-and-child care.',
         'Per diem · Health insurance',
         'CONTRACT', 320000, 'XAF', 'ONSITE', CURRENT_DATE - 6,
         'Centre Ville', 'Ebolowa', 'Sud', 'SUD', 'Cameroon',
         false, NOW(), NOW(), NULL),

        -- Est region -------------------------------------------------------
        ('30000000-0000-0000-0000-000000000015', sofavinc_id,   true, false, 58,
         'Forest-Coffee Project Lead (Est)',
         'Lead our agroforestry coffee initiative in the Est region. Strong project-management background required.',
         'Field allowance · Health insurance',
         'FULL_TIME', 900000, 'XAF', 'ONSITE', CURRENT_DATE - 21,
         'Centre Ville', 'Bertoua', 'Est', 'EST', 'Cameroon',
         false, NOW(), NOW(), NULL),

        -- Internship -------------------------------------------------------
        ('30000000-0000-0000-0000-000000000016', edu_first_id,  true, false, 156,
         'Software Engineering Intern (6 months)',
         'Pair with our senior engineers on real product features. Stipend + mentorship.',
         'Stipend · Mentorship · Possibility of full-time offer',
         'INTERN', 150000, 'XAF', 'HYBRID', CURRENT_DATE - 1,
         'Bastos', 'Yaoundé', 'Centre', 'CENTRE', 'Cameroon',
         false, NOW(), NOW(), NULL)
    ON CONFLICT (id) DO NOTHING;

    RAISE NOTICE 'V2__seed_demo_data: seed complete. Demo password is "Password123!".';
END $$;
