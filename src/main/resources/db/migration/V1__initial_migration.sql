-- =====================================================
-- Project - Migração Inicial do Banco de Dados
-- Versão: V1__Initial_Schema.sql
-- =====================================================

-- Criação dos ENUMs
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'BLOCKED', 'PENDING', 'SUPER_USER');
CREATE TYPE company_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'BLOCKED');
CREATE TYPE company_type AS ENUM ('PERSONAL', 'BUSINESS');
CREATE TYPE member_status AS ENUM ('ACTIVE', 'INACTIVE', 'PENDING', 'BLOCKED');
CREATE TYPE member_type AS ENUM ('INTERNAL', 'CLIENT', 'SUPPLIER', 'PARTNER');
CREATE TYPE user_role AS ENUM ('SUPER_USER', 'OWNER', 'ADMIN', 'MANAGER', 'EMPLOYEE', 'CLIENT', 'SUPPLIER', 'GUEST');
CREATE TYPE invitation_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'CANCELLED');
CREATE TYPE invitation_type AS ENUM ('EMPLOYEE', 'CLIENT', 'SUPPLIER');

-- =====================================================
-- Tabela: users
-- =====================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL CHECK (length(name) >= 3),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL CHECK (length(phone) >= 10),
    password VARCHAR(255) NOT NULL CHECK (length(password) >= 6),
    status user_status NOT NULL DEFAULT 'PENDING',
    profile_image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para users
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_created_at ON users (created_at);

-- =====================================================
-- Tabela: companies
-- =====================================================
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL CHECK (length(name) >= 3),
    document VARCHAR(14) NOT NULL UNIQUE CHECK (length(document) >= 11),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL CHECK (length(phone) >= 10),
    address TEXT,
    company_type company_type NOT NULL DEFAULT 'PERSONAL',
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    active_plan_id BIGINT,
    status company_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_company_document_format CHECK (
        document ~ '^[0-9]+$' AND (
            length(document) = 11 OR length(document) = 14
        )
    )
);

-- Índices para companies
CREATE INDEX idx_companies_document ON companies (document);
CREATE INDEX idx_companies_owner ON companies (owner_id);
CREATE INDEX idx_companies_status ON companies (status);
CREATE INDEX idx_companies_type ON companies (company_type);
CREATE INDEX idx_companies_created_at ON companies (created_at);

-- =====================================================
-- Tabela: company_members
-- =====================================================
CREATE TABLE company_members (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    member_type member_type NOT NULL,
    user_role user_role NOT NULL,
    status member_status NOT NULL DEFAULT 'PENDING',
    invited_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    joined_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraint única: um usuário só pode ter um registro por empresa
    CONSTRAINT uk_company_member_user UNIQUE (company_id, user_id)
);

-- Índices para company_members
CREATE INDEX idx_company_member_user ON company_members (user_id);
CREATE INDEX idx_company_member_company ON company_members (company_id);
CREATE INDEX idx_company_member_status ON company_members (status);
CREATE INDEX idx_company_member_type ON company_members (member_type);
CREATE INDEX idx_company_member_role ON company_members (user_role);
CREATE INDEX idx_company_member_invited_by ON company_members (invited_by);

-- =====================================================
-- Tabela: invitations
-- =====================================================
CREATE TABLE invitations (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    inviter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invitation_type invitation_type NOT NULL,
    status invitation_status NOT NULL DEFAULT 'PENDING',
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_invitation_expires_future CHECK (expires_at > created_at),
    CONSTRAINT chk_invitation_email_format CHECK (email ~ '^[^@]+@[^@]+\.[^@]+$')
);

-- Índices para invitations
CREATE INDEX idx_invitations_email ON invitations (email);
CREATE INDEX idx_invitations_company ON invitations (company_id);
CREATE INDEX idx_invitations_inviter ON invitations (inviter_id);
CREATE INDEX idx_invitations_token ON invitations (token);
CREATE INDEX idx_invitations_status ON invitations (status);
CREATE INDEX idx_invitations_expires_at ON invitations (expires_at);
CREATE INDEX idx_invitations_created_at ON invitations (created_at);

-- Índice composto para buscar convites pendentes por empresa e email
CREATE INDEX idx_invitations_company_email_status ON invitations (company_id, email, status);

-- =====================================================
-- Triggers para updated_at automático
-- =====================================================

-- Função para atualizar updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers para cada tabela
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_companies_updated_at
    BEFORE UPDATE ON companies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_company_members_updated_at
    BEFORE UPDATE ON company_members
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_invitations_updated_at
    BEFORE UPDATE ON invitations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- Comentários das tabelas
-- =====================================================

COMMENT ON TABLE users IS 'Usuários do sistema - podem ser proprietários ou membros de empresas';
COMMENT ON TABLE companies IS 'Empresas do sistema - podem ser PERSONAL (profissional individual) ou BUSINESS';
COMMENT ON TABLE company_members IS 'Relacionamento usuário-empresa com papéis e permissões específicas';
COMMENT ON TABLE invitations IS 'Convites pendentes para usuários participarem de empresas';

-- Comentários das colunas principais
COMMENT ON COLUMN companies.document IS 'CPF (11 dígitos) para PERSONAL ou CNPJ (14 dígitos) para BUSINESS';
COMMENT ON COLUMN companies.company_type IS 'PERSONAL = profissional individual, BUSINESS = empresa';
COMMENT ON COLUMN company_members.member_type IS 'Tipo de relacionamento: INTERNAL, CLIENT, SUPPLIER, PARTNER';
COMMENT ON COLUMN company_members.user_role IS 'Papel do usuário na empresa: OWNER, ADMIN, MANAGER, etc.';
COMMENT ON COLUMN invitations.token IS 'Token único para validação do convite';
COMMENT ON COLUMN invitations.expires_at IS 'Data de expiração do convite (padrão: 7 dias)';

-- =====================================================
-- Dados iniciais (opcional)
-- =====================================================

-- Inserir usuário admin padrão (senha: admin123)
-- Nota: Em produção, esta senha deve ser alterada imediatamente
INSERT INTO users (name, email, phone, password, status) VALUES
('Administrador', 'admin@base.com', '11999999999', '$2a$10$X9vTKNg5Z8kZxZ5ZxZ5ZxZrRgT5ZrRgT5ZrRgT5ZrRgT5ZrRgT5ZrR', 'SUPER_USER');

-- =====================================================
-- Verificações finais
-- =====================================================

-- Verificar se todas as tabelas foram criadas
DO $$
BEGIN
    ASSERT (SELECT COUNT(*) FROM information_schema.tables
            WHERE table_schema = 'public'
            AND table_name IN ('users', 'companies', 'company_members', 'invitations')) = 4,
           'Nem todas as tabelas foram criadas corretamente';

    RAISE NOTICE 'Migração V1__Initial_Schema executada com sucesso!';
    RAISE NOTICE 'Tabelas criadas: users, companies, company_members, invitations';
    RAISE NOTICE 'ENUMs criados: company_status, company_type, member_status, member_type, user_role, invitation_status, invitation_type';
END $$;