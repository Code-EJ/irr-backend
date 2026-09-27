-- Explicit organization membership; existing creator IDs are never inferred as ownership.
-- Author: Enzo Ribas (https://github.com/oEnzoRibas)
CREATE TABLE organization (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL CHECK (length(btrim(name)) > 0),
    organization_type VARCHAR(50) NOT NULL CHECK (length(btrim(organization_type)) > 0),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE organization_membership (
    organization_id UUID NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role VARCHAR(20) NOT NULL CHECK (role IN ('MEMBER','MANAGER')),
    is_active BOOLEAN NOT NULL DEFAULT true,
    granted_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (organization_id, user_id)
);
CREATE INDEX ix_organization_membership_user ON organization_membership(user_id, organization_id) WHERE is_active;
CREATE TABLE organization_access_audit (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization(id) ON DELETE RESTRICT,
    actor_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    subject_user_id UUID REFERENCES users(id) ON DELETE RESTRICT,
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATE','GRANT','REVOKE')),
    previous_role VARCHAR(20),
    assigned_role VARCHAR(20),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_organization_access_audit_org ON organization_access_audit(organization_id, recorded_at);
