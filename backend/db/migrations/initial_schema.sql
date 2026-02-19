-- ============================================================================
-- MATRIMONY APP - DATABASE MIGRATIONS
-- PostgreSQL 14+ Schema
-- Created: February 2025
-- ============================================================================

-- ============================================================================
-- 1. CORE TABLES
-- ============================================================================

-- Users Table
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  phone VARCHAR(15) UNIQUE,
  email VARCHAR(255) UNIQUE,
  password_hash VARCHAR(255),
  is_verified BOOLEAN DEFAULT false,
  verification_code VARCHAR(10),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_login TIMESTAMP,
  is_active BOOLEAN DEFAULT true,
  is_banned BOOLEAN DEFAULT false,
  ban_reason TEXT,
  ban_expires_at TIMESTAMP,
  role VARCHAR(50) DEFAULT 'user'
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_created_at ON users(created_at DESC);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_is_banned ON users(is_banned);

-- Profiles Table
CREATE TABLE profiles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100),
  date_of_birth DATE NOT NULL,
  gender VARCHAR(20) NOT NULL,  -- 'male', 'female', 'other'
  height_cm INT,
  marital_status VARCHAR(50) NOT NULL,  -- 'never_married', 'divorced', 'widowed', 'annulled'
  bio TEXT,
  religion VARCHAR(50),
  caste VARCHAR(100),
  sub_caste VARCHAR(100),
  mother_tongue VARCHAR(50),
  country VARCHAR(100) DEFAULT 'India',
  state VARCHAR(100),
  city VARCHAR(100),
  education_level VARCHAR(50),  -- 'high_school', 'bachelors', 'masters', 'phd'
  education_field VARCHAR(100),
  profession VARCHAR(100),
  company VARCHAR(100),
  income_band VARCHAR(50),  -- 'below_5L', '5-10L', '10-25L', '25-50L', '50L+'
  food_habit VARCHAR(50),  -- 'vegetarian', 'non_vegetarian', 'vegan', 'eggetarian'
  drinks VARCHAR(50),  -- 'no', 'occasionally', 'regularly'
  smokes VARCHAR(50),  -- 'no', 'occasionally', 'regularly'
  hobbies TEXT[],
  religiousness VARCHAR(50),  -- 'orthodox', 'moderate', 'liberal'
  completion_percentage INT DEFAULT 0,
  role VARCHAR(50) DEFAULT 'self',  -- 'self', 'parent', 'relative'
  intent VARCHAR(50) DEFAULT 'flexible',  -- 'within_1yr', 'flexible', 'exploring'
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_profiles_user_id ON profiles(user_id);
CREATE INDEX idx_profiles_religion_caste ON profiles(religion, caste);
CREATE INDEX idx_profiles_date_of_birth ON profiles(date_of_birth);
CREATE INDEX idx_profiles_city ON profiles(city);
CREATE INDEX idx_profiles_state ON profiles(state);
CREATE INDEX idx_profiles_created_at ON profiles(created_at DESC);

-- Partner Preferences Table
CREATE TABLE partner_preferences (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  min_age INT DEFAULT 18,
  max_age INT DEFAULT 60,
  preferred_locations TEXT[],
  religion_open BOOLEAN DEFAULT true,
  preferred_religions VARCHAR(100)[],
  caste_open BOOLEAN DEFAULT true,
  preferred_castes VARCHAR(100)[],
  min_education VARCHAR(50),
  min_income_band VARCHAR(50),
  food_habit_preferences VARCHAR(50)[],
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_partner_pref_user_id ON partner_preferences(user_id);

-- ============================================================================
-- 2. MEDIA & VERIFICATION TABLES
-- ============================================================================

-- Photos Table
CREATE TABLE photos (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  photo_url VARCHAR(500) NOT NULL,
  thumbnail_url VARCHAR(500),
  is_primary BOOLEAN DEFAULT false,
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  width INT,
  height INT,
  file_size_kb INT
);

CREATE INDEX idx_photos_user_id ON photos(user_id);
CREATE INDEX idx_photos_primary ON photos(user_id, is_primary);

-- Verifications Table
CREATE TABLE verifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type VARCHAR(50) NOT NULL,  -- 'phone', 'email', 'id', 'linkedin'
  status VARCHAR(50) DEFAULT 'not_started',  -- 'not_started', 'pending', 'verified', 'rejected'
  document_url VARCHAR(500),
  rejection_reason TEXT,
  verified_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, type)
);

CREATE INDEX idx_verifications_user_id ON verifications(user_id);
CREATE INDEX idx_verifications_status ON verifications(status);
CREATE INDEX idx_verifications_type ON verifications(type);

-- ============================================================================
-- 3. INTEREST & MATCHING TABLES
-- ============================================================================

-- Likes (Interest) Table
CREATE TABLE likes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  receiver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status VARCHAR(50) DEFAULT 'sent',  -- 'sent', 'accepted', 'rejected', 'blocked'
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CHECK (sender_id != receiver_id),
  UNIQUE(sender_id, receiver_id)
);

CREATE INDEX idx_likes_sender_id ON likes(sender_id);
CREATE INDEX idx_likes_receiver_id ON likes(receiver_id);
CREATE INDEX idx_likes_status ON likes(status);
CREATE INDEX idx_likes_created_at ON likes(created_at DESC);

-- Matches Table
CREATE TABLE matches (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_a_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  user_b_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_message_at TIMESTAMP,
  is_active BOOLEAN DEFAULT true,
  UNIQUE(user_a_id, user_b_id),
  CHECK (user_a_id < user_b_id)
);

CREATE INDEX idx_matches_user_a ON matches(user_a_id);
CREATE INDEX idx_matches_user_b ON matches(user_b_id);
CREATE INDEX idx_matches_active ON matches(is_active);
CREATE INDEX idx_matches_created_at ON matches(created_at DESC);

-- ============================================================================
-- 4. CHAT & MESSAGING TABLES
-- ============================================================================

-- Messages Table
CREATE TABLE messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
  sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  content TEXT NOT NULL,
  is_read BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_match_id ON messages(match_id, created_at DESC);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_is_read ON messages(is_read);

-- ============================================================================
-- 5. SUBSCRIPTION & PAYMENT TABLES
-- ============================================================================

-- Subscriptions Table
CREATE TABLE subscriptions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  plan VARCHAR(50) NOT NULL DEFAULT 'free',  -- 'free', 'standard', 'till_marriage'
  status VARCHAR(50) DEFAULT 'active',  -- 'active', 'inactive', 'cancelled'
  started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP,
  auto_renew BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_expires_at ON subscriptions(expires_at);
CREATE INDEX idx_subscriptions_plan ON subscriptions(plan);

-- Payments Table
CREATE TABLE payments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  subscription_id UUID REFERENCES subscriptions(id),
  amount_inr DECIMAL(10, 2) NOT NULL,
  plan VARCHAR(50) NOT NULL,
  status VARCHAR(50) DEFAULT 'pending',  -- 'pending', 'completed', 'failed', 'refunded'
  razorpay_order_id VARCHAR(100) UNIQUE,
  razorpay_payment_id VARCHAR(100) UNIQUE,
  razorpay_signature VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_razorpay_payment_id ON payments(razorpay_payment_id);
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);

-- ============================================================================
-- 6. SEARCH & PREFERENCE TABLES
-- ============================================================================

-- Saved Searches Table
CREATE TABLE saved_searches (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name VARCHAR(100) NOT NULL,
  filters JSONB NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_saved_searches_user_id ON saved_searches(user_id);

-- ============================================================================
-- 7. MODERATION & REPORTING TABLES
-- ============================================================================

-- Reports Table
CREATE TABLE reports (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  reported_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  report_type VARCHAR(50) NOT NULL,  -- 'inappropriate', 'fake_profile', 'harassment', 'scam', 'other'
  description TEXT,
  status VARCHAR(50) DEFAULT 'open',  -- 'open', 'reviewing', 'resolved', 'dismissed'
  admin_notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_reported_user_id ON reports(reported_user_id);
CREATE INDEX idx_reports_created_at ON reports(created_at DESC);

-- ============================================================================
-- 8. ADMIN TABLES
-- ============================================================================

-- Admin Users Table
CREATE TABLE admin_users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(50) DEFAULT 'admin',  -- 'admin', 'moderator', 'support'
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_users_email ON admin_users(email);
CREATE INDEX idx_admin_users_role ON admin_users(role);

-- ============================================================================
-- 9. AUDIT LOG TABLE (Optional but Recommended)
-- ============================================================================

CREATE TABLE audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  action VARCHAR(100) NOT NULL,
  resource_type VARCHAR(100),
  resource_id VARCHAR(255),
  changes JSONB,
  ip_address VARCHAR(45),
  user_agent VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- ============================================================================
-- 10. MATERIALIZED VIEW FOR FEED RANKING (Optional)
-- ============================================================================

-- This view is used to rank profiles for efficient feed generation
-- Refresh every hour via scheduled job
CREATE MATERIALIZED VIEW profile_rankings AS
SELECT
  p.id,
  p.user_id,
  p.first_name,
  p.last_name,
  p.city,
  p.religion,
  p.caste,
  p.education_level,
  p.profession,
  p.date_of_birth,
  EXTRACT(YEAR FROM AGE(p.date_of_birth))::INT AS age,
  (SELECT COUNT(*) FROM verifications v WHERE v.user_id = p.user_id AND v.status = 'verified') AS verification_count,
  COALESCE(u.last_login, u.created_at) AS last_active,
  EXTRACT(EPOCH FROM (NOW() - COALESCE(u.last_login, u.created_at)))::INT AS seconds_since_active
FROM profiles p
INNER JOIN users u ON p.user_id = u.id
WHERE u.is_active = true
  AND u.is_banned = false
  AND p.completion_percentage >= 50;

CREATE INDEX idx_profile_rankings_city ON profile_rankings(city);
CREATE INDEX idx_profile_rankings_age ON profile_rankings(age);
CREATE INDEX idx_profile_rankings_religion ON profile_rankings(religion);

-- ============================================================================
-- 11. SEED DATA (OPTIONAL - for development/testing)
-- ============================================================================

-- Uncomment to add sample data for testing

-- INSERT INTO users (phone, email, password_hash, is_verified, created_at)
-- VALUES (
--   '+919876543210',
--   'rahul.kumar@example.com',
--   '$2b$12$...hashed_password...',
--   true,
--   CURRENT_TIMESTAMP
-- );

-- ============================================================================
-- END OF MIGRATIONS
-- ============================================================================
