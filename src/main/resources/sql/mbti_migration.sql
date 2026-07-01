-- caregiver_profiles 테이블
ALTER TABLE caregiver_profiles
  ADD COLUMN mbti_type VARCHAR(8) NULL;

-- user_profiles 테이블
ALTER TABLE user_profiles
  ADD COLUMN mbti_type VARCHAR(8) NULL;

-- 선호 MBTI 조인 테이블
CREATE TABLE IF NOT EXISTS user_preferred_mbti (
  user_id   BIGINT      NOT NULL,
  mbti_type VARCHAR(8)  NOT NULL,
  PRIMARY KEY (user_id, mbti_type),
  CONSTRAINT fk_upm_user FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE
);
