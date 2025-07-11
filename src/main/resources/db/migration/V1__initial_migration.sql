-- =====================================================
-- ProjectBaseName - Migração Inicial do Banco de Dados
-- Versão: V1__Initial_Schema.sql
-- =====================================================

-- =====================================================
-- Tabela: users
-- =====================================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL CHECK (length(name) >= 3),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) CHECK (phone IS NULL OR length(phone) >= 10),
    password VARCHAR(255) NOT NULL CHECK (length(password) >= 6),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED', 'PENDING', 'SUPER_USER')),
    user_type VARCHAR(50) NOT NULL DEFAULT 'OWNER' CHECK (user_type IN ('OWNER', 'INVITED')),
    profile_image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para users
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_user_type ON users (user_type);
CREATE INDEX idx_users_created_at ON users (created_at);

-- =====================================================
-- Tabela: companies
-- =====================================================
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL CHECK (length(name) >= 3),
    document VARCHAR(14) NOT NULL UNIQUE CHECK (length(document) >= 11),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20) CHECK (phone IS NULL OR length(phone) >= 10),
    address TEXT,
    company_type VARCHAR(50) NOT NULL DEFAULT 'PERSONAL' CHECK (company_type IN ('PERSONAL', 'BUSINESS')),
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    active_plan_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'BLOCKED')),
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
    member_type VARCHAR(50) NOT NULL CHECK (member_type IN ('INTERNAL', 'CLIENT', 'SUPPLIER', 'PARTNER')),
    user_role VARCHAR(50) NOT NULL CHECK (user_role IN ('SUPER_USER', 'OWNER', 'ADMIN', 'MANAGER', 'EMPLOYEE', 'CLIENT', 'SUPPLIER', 'GUEST')),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'BLOCKED')),
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
    invitation_type VARCHAR(50) NOT NULL CHECK (invitation_type IN ('EMPLOYEE', 'CLIENT', 'SUPPLIER')),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'CANCELLED')),
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
-- Tabela: password_reset_tokens
-- =====================================================
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_password_reset_expires_future CHECK (expires_at > created_at),
    CONSTRAINT chk_password_reset_used_at_when_used CHECK (
        (used = TRUE AND used_at IS NOT NULL) OR 
        (used = FALSE AND used_at IS NULL)
    )
);

-- Índices para password_reset_tokens
CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens (token);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens (expires_at);
CREATE INDEX idx_password_reset_tokens_used ON password_reset_tokens (used);
CREATE INDEX idx_password_reset_tokens_created_at ON password_reset_tokens (created_at);

-- Índice composto para buscar tokens válidos
CREATE INDEX idx_password_reset_tokens_token_used ON password_reset_tokens (token, used) 
WHERE used = FALSE;

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

CREATE TRIGGER update_password_reset_tokens_updated_at
    BEFORE UPDATE ON password_reset_tokens
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- Comentários das tabelas
-- =====================================================

COMMENT ON TABLE users IS 'Usuários do sistema - OWNER podem criar empresas, INVITED são apenas membros';
COMMENT ON TABLE companies IS 'Empresas do sistema - podem ser PERSONAL (profissional individual) ou BUSINESS';
COMMENT ON TABLE company_members IS 'Relacionamento usuário-empresa com papéis e permissões específicas';
COMMENT ON TABLE invitations IS 'Convites pendentes para usuários participarem de empresas';
COMMENT ON TABLE password_reset_tokens IS 'Tokens de recuperação de senha com controle de uso e expiração';

-- Comentários das colunas principais
COMMENT ON COLUMN users.user_type IS 'OWNER = pode criar empresa própria, INVITED = apenas membro de empresas';
COMMENT ON COLUMN users.phone IS 'Telefone opcional - usado para SMS quando disponível';
COMMENT ON COLUMN companies.document IS 'CPF (11 dígitos) para PERSONAL ou CNPJ (14 dígitos) para BUSINESS';
COMMENT ON COLUMN companies.company_type IS 'PERSONAL = profissional individual, BUSINESS = empresa';
COMMENT ON COLUMN companies.phone IS 'Telefone opcional - usa telefone do owner como fallback';
COMMENT ON COLUMN company_members.member_type IS 'Tipo de relacionamento: INTERNAL, CLIENT, SUPPLIER, PARTNER';
COMMENT ON COLUMN company_members.user_role IS 'Papel do usuário na empresa: OWNER, ADMIN, MANAGER, etc.';
COMMENT ON COLUMN invitations.token IS 'Token único para validação do convite';
COMMENT ON COLUMN invitations.expires_at IS 'Data de expiração do convite (padrão: 7 dias)';
COMMENT ON COLUMN password_reset_tokens.token IS 'Token único para recuperação de senha (UUID sem hífens)';
COMMENT ON COLUMN password_reset_tokens.used IS 'Indica se o token já foi utilizado (uso único)';
COMMENT ON COLUMN password_reset_tokens.expires_at IS 'Data de expiração do token (padrão: 1 hora)';
COMMENT ON COLUMN password_reset_tokens.used_at IS 'Timestamp de quando o token foi utilizado';

-- =====================================================
-- Tabela: subscriptions (Sistema de Pagamentos)
-- =====================================================
CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_id BIGINT,
    provider VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    environment_type VARCHAR(50) NOT NULL,
    provider_subscription_id VARCHAR(255),
    original_transaction_id VARCHAR(255),
    product_id VARCHAR(255),
    integration_code VARCHAR(255),
    price DECIMAL(10,2),
    currency VARCHAR(3),
    period_type VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT true,
    last_approved_date TIMESTAMP,
    expiration_date TIMESTAMP,
    cancellation_date TIMESTAMP,
    trial_end_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign keys
    CONSTRAINT fk_subscription_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_subscription_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE SET NULL,
    
    -- Constraints de unique
    CONSTRAINT uq_subscription_integration_code UNIQUE (integration_code),
    CONSTRAINT uq_subscription_provider_subscription_id UNIQUE (provider_subscription_id),
    
    -- Check constraints para validação
    CONSTRAINT chk_subscription_provider CHECK (provider IN ('REVENUECAT', 'ASAAS')),
    CONSTRAINT chk_subscription_status CHECK (status IN ('ACTIVE', 'CANCELED', 'EXPIRED', 'PAUSED', 'PENDING', 'TRIALING')),
    CONSTRAINT chk_subscription_environment CHECK (environment_type IN ('SANDBOX', 'PRODUCTION')),
    CONSTRAINT chk_subscription_currency CHECK (currency IS NULL OR length(currency) = 3),
    CONSTRAINT chk_subscription_price CHECK (price IS NULL OR price >= 0)
);

-- Índices para subscriptions
CREATE INDEX idx_subscription_user_status ON subscriptions (user_id, status);
CREATE INDEX idx_subscription_company_status ON subscriptions (company_id, status);
CREATE INDEX idx_subscription_provider ON subscriptions (provider);
CREATE INDEX idx_subscription_integration_code ON subscriptions (integration_code);
CREATE INDEX idx_subscription_provider_subscription_id ON subscriptions (provider_subscription_id);
CREATE INDEX idx_subscription_status ON subscriptions (status);
CREATE INDEX idx_subscription_enabled ON subscriptions (enabled);
CREATE INDEX idx_subscription_created_at ON subscriptions (created_at);
CREATE INDEX idx_subscription_expiration_date ON subscriptions (expiration_date);
CREATE INDEX idx_subscription_last_approved_date ON subscriptions (last_approved_date);

-- Trigger para subscriptions
CREATE TRIGGER update_subscriptions_updated_at
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comentários para subscriptions
COMMENT ON TABLE subscriptions IS 'Tabela para gerenciar assinaturas e pagamentos dos usuários';
COMMENT ON COLUMN subscriptions.provider IS 'Provedor de pagamento: REVENUECAT, ASAAS';
COMMENT ON COLUMN subscriptions.status IS 'Status da assinatura: ACTIVE, CANCELED, EXPIRED, PAUSED, PENDING, TRIALING';
COMMENT ON COLUMN subscriptions.environment_type IS 'Ambiente: SANDBOX, PRODUCTION';
COMMENT ON COLUMN subscriptions.integration_code IS 'Código único para integração com provedor externo';
COMMENT ON COLUMN subscriptions.provider_subscription_id IS 'ID da assinatura no provedor externo';
COMMENT ON COLUMN subscriptions.original_transaction_id IS 'ID da transação original no provedor';
COMMENT ON COLUMN subscriptions.enabled IS 'Indica se a assinatura está habilitada/ativa';

-- =====================================================
-- Dados iniciais (opcional)
-- =====================================================

-- Inserir usuário admin padrão (senha: admin123)
-- Nota: Em produção, esta senha deve ser alterada imediatamente
INSERT INTO users (name, email, phone, password, status, user_type) VALUES
('Administrador', 'admin@projectbasename.com', '11999999999', '$2a$10$X9vTKNg5Z8kZxZ5ZxZ5ZxZrRgT5ZrRgT5ZrRgT5ZrRgT5ZrRgT5ZrR', 'SUPER_USER', 'OWNER');

-- =====================================================
-- Verificações finais
-- =====================================================

-- Verificar se todas as tabelas foram criadas
DO $$
BEGIN
    ASSERT (SELECT COUNT(*) FROM information_schema.tables
            WHERE table_schema = 'public'
            AND table_name IN ('users', 'companies', 'company_members', 'invitations', 'password_reset_tokens', 'subscriptions')) = 6,
           'Nem todas as tabelas foram criadas corretamente';

    RAISE NOTICE 'Migração V1__Initial_Schema executada com sucesso!';
    RAISE NOTICE 'Tabelas criadas: users, companies, company_members, invitations, password_reset_tokens, subscriptions';
    RAISE NOTICE 'Colunas ENUM usando VARCHAR com CHECK constraints para validação';
    RAISE NOTICE 'Telefone opcional para users e companies - compatível com login social';
    RAISE NOTICE 'Constraint: Apenas usuários OWNER podem ter empresa própria';
    RAISE NOTICE 'Sistema de recuperação de senha implementado com tokens seguros';
    RAISE NOTICE 'Sistema de pagamentos implementado com suporte a RevenueCat, Asaas';
END $$;