-- 企业安全风险评估（GB/T 20984 导向）业务表，位于 Supabase PostgreSQL schema `erm`
CREATE SCHEMA IF NOT EXISTS erm;

CREATE TABLE IF NOT EXISTS erm.tenant (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS erm.app_user (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL REFERENCES erm.tenant(id) ON DELETE CASCADE,
  username VARCHAR(128) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(255),
  role VARCHAR(32) NOT NULL DEFAULT 'USER',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (tenant_id, username)
);

CREATE TABLE IF NOT EXISTS erm.risk_asset (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL REFERENCES erm.tenant(id) ON DELETE CASCADE,
  name VARCHAR(512) NOT NULL,
  category VARCHAR(128),
  criticality INT NOT NULL DEFAULT 3,
  description TEXT,
  owner_label VARCHAR(255),
  location_label VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_risk_asset_tenant ON erm.risk_asset(tenant_id);

CREATE TABLE IF NOT EXISTS erm.risk_threat (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL REFERENCES erm.tenant(id) ON DELETE CASCADE,
  name VARCHAR(512) NOT NULL,
  category VARCHAR(128),
  description TEXT,
  source_label VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_risk_threat_tenant ON erm.risk_threat(tenant_id);

CREATE TABLE IF NOT EXISTS erm.risk_vulnerability (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL REFERENCES erm.tenant(id) ON DELETE CASCADE,
  name VARCHAR(512) NOT NULL,
  severity VARCHAR(32),
  description TEXT,
  related_asset_id BIGINT REFERENCES erm.risk_asset(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_risk_vuln_tenant ON erm.risk_vulnerability(tenant_id);

CREATE TABLE IF NOT EXISTS erm.security_measure (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL REFERENCES erm.tenant(id) ON DELETE CASCADE,
  name VARCHAR(512) NOT NULL,
  measure_type VARCHAR(64),
  description TEXT,
  effectiveness_note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_measure_tenant ON erm.security_measure(tenant_id);

CREATE TABLE IF NOT EXISTS erm.risk_assessment (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL REFERENCES erm.tenant(id) ON DELETE CASCADE,
  title VARCHAR(512) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  framework VARCHAR(64) NOT NULL DEFAULT 'GB/T 20984',
  chat_id VARCHAR(128),
  summary TEXT,
  high_risk_count INT NOT NULL DEFAULT 0,
  medium_risk_count INT NOT NULL DEFAULT 0,
  low_risk_count INT NOT NULL DEFAULT 0,
  created_by_user_id BIGINT REFERENCES erm.app_user(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_assessment_tenant ON erm.risk_assessment(tenant_id);

CREATE TABLE IF NOT EXISTS erm.assessment_asset (
  assessment_id BIGINT NOT NULL REFERENCES erm.risk_assessment(id) ON DELETE CASCADE,
  asset_id BIGINT NOT NULL REFERENCES erm.risk_asset(id) ON DELETE CASCADE,
  PRIMARY KEY (assessment_id, asset_id)
);

CREATE TABLE IF NOT EXISTS erm.assessment_threat (
  assessment_id BIGINT NOT NULL REFERENCES erm.risk_assessment(id) ON DELETE CASCADE,
  threat_id BIGINT NOT NULL REFERENCES erm.risk_threat(id) ON DELETE CASCADE,
  PRIMARY KEY (assessment_id, threat_id)
);

CREATE TABLE IF NOT EXISTS erm.assessment_vulnerability (
  assessment_id BIGINT NOT NULL REFERENCES erm.risk_assessment(id) ON DELETE CASCADE,
  vulnerability_id BIGINT NOT NULL REFERENCES erm.risk_vulnerability(id) ON DELETE CASCADE,
  PRIMARY KEY (assessment_id, vulnerability_id)
);

CREATE TABLE IF NOT EXISTS erm.assessment_measure (
  assessment_id BIGINT NOT NULL REFERENCES erm.risk_assessment(id) ON DELETE CASCADE,
  measure_id BIGINT NOT NULL REFERENCES erm.security_measure(id) ON DELETE CASCADE,
  PRIMARY KEY (assessment_id, measure_id)
);

CREATE TABLE IF NOT EXISTS erm.assessed_risk (
  id BIGSERIAL PRIMARY KEY,
  assessment_id BIGINT NOT NULL REFERENCES erm.risk_assessment(id) ON DELETE CASCADE,
  title VARCHAR(512),
  likelihood INT NOT NULL,
  impact INT NOT NULL,
  risk_level VARCHAR(16) NOT NULL,
  notes TEXT,
  treatment VARCHAR(32),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_assessed_risk_a ON erm.assessed_risk(assessment_id);
