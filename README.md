# 🚀 ProjectBaseName - Base de Projeto Kotlin Backend

Uma base robusta e completa para desenvolvimento de aplicações backend em Kotlin com Spring Boot, incluindo autenticação, pagamentos, upload de arquivos, notificações e muito mais.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [Funcionalidades](#funcionalidades)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Configuração](#configuração)
- [Execução](#execução)
- [API Endpoints](#api-endpoints)
- [Autenticação e Segurança](#autenticação-e-segurança)
- [Sistema de Permissões](#sistema-de-permissões)
- [Integrações](#integrações)
- [Desenvolvimento](#desenvolvimento)
- [Deploy](#deploy)

## 🎯 Visão Geral

Este projeto serve como uma base sólida para novos projetos backend, oferecendo uma arquitetura bem estruturada com funcionalidades essenciais já implementadas:

- **Autenticação Multi-fator**: SMS, Email e Login Social (Google, Apple, Microsoft)
- **Sistema de Empresas**: Suporte a múltiplas empresas com hierarquia de usuários
- **Sistema de Pagamentos**: Integração com RevenueCat para assinaturas
- **Upload de Arquivos**: Integração com AWS S3
- **Notificações**: SMS via Twilio e Email via Microsoft 365
- **Sistema de Convites**: Para onboarding de usuários
- **Cache e Performance**: Redis para cache e otimizações
- **Monitoramento**: Actuator, Prometheus e New Relic

## 🏗️ Arquitetura

### Padrão Arquitetural
- **Clean Architecture** com separação clara de responsabilidades
- **Domain-Driven Design (DDD)** para modelagem de negócio
- **Hexagonal Architecture** para isolamento de dependências externas

### Camadas da Aplicação
```
┌─────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                     │
│  Controllers, Repositories, External Services, Configs      │
├─────────────────────────────────────────────────────────────┤
│                    Application Layer                        │
│  DTOs, Mappers, Security Services, Validation              │
├─────────────────────────────────────────────────────────────┤
│                      Domain Layer                           │
│  Entities, Models, Services, Exceptions, Enums             │
└─────────────────────────────────────────────────────────────┘
```

### Relacionamentos Principais
- ✅ **User 1:1 Company** (owner/ownedCompany)
- ✅ **User 1:N CompanyMember** (usuário pode ser membro de várias empresas)
- ✅ **Company 1:N CompanyMember** (empresa tem vários membros)
- ✅ **Unique constraint** em CompanyMember (company_id, user_id)
- ✅ **Invitation** referencia Company e User via FK

## 🛠️ Tecnologias

### Core
- **Kotlin 1.9.24** - Linguagem principal
- **Spring Boot 3.5.3** - Framework base
- **Java 21** - Runtime com Virtual Threads
- **Gradle** - Build tool com Kotlin DSL

### Banco de Dados
- **PostgreSQL** - Banco principal
- **Flyway** - Migração de banco
- **Hibernate/JPA** - ORM
- **Redis** - Cache e sessões

### Segurança
- **Spring Security** - Framework de segurança
- **JWT** - Autenticação stateless
- **OAuth2** - Login social
- **Password Encoder** - Criptografia de senhas

### Integrações Externas
- **AWS SDK v2** - S3, SNS, SQS
- **Twilio** - SMS e WhatsApp
- **Microsoft 365** - Email SMTP
- **RevenueCat** - Sistema de pagamentos
- **Google APIs** - OAuth2 e validação
- **Firebase** - Notificações push

### Monitoramento e Observabilidade
- **Spring Actuator** - Health checks e métricas
- **Prometheus** - Coleta de métricas
- **New Relic** - APM e monitoramento
- **Micrometer** - Métricas customizadas

### Documentação
- **OpenAPI 3** - Documentação da API
- **Swagger UI** - Interface de teste
- **SpringDoc** - Geração automática

### Testes
- **JUnit 5** - Framework de testes
- **TestContainers** - Testes de integração
- **MockK** - Mocking para Kotlin

## 🚀 Funcionalidades

### 🔐 Sistema de Autenticação

#### Login SMS (Principal)
1. `POST /v1/auth/login` → Envia SMS
2. `POST /v1/auth/verify-sms` → Verifica código → Token JWT

#### Login Email (Fallback)
1. `POST /v1/auth/login-email` → Envia email
2. `POST /v1/auth/verify-email` → Verifica código → Token JWT

#### Registro
1. `POST /v1/auth/register` → Cria usuário + SMS
2. `POST /v1/auth/activate` → Ativa conta → Token JWT

#### Social Login (OAuth2)
1. Redirect para provider (Google/Apple/Microsoft)
2. Callback para OAuth2SuccessHandler
3. Processar usuário OAuth2
4. Retornar JWT via deep link

### 🏢 Sistema de Empresas

#### Tipos de Empresa
- **PERSONAL**: Empresa pessoal (profissional individual)
- **BUSINESS**: Empresa de negócios (CNPJ obrigatório)

#### Hierarquia de Usuários
```
SUPER_USER > OWNER > ADMIN > MANAGER > EMPLOYEE > CLIENT/SUPPLIER > GUEST
```

#### Funcionalidades
- Criação de empresas pessoais e de negócios
- Gerenciamento de membros com diferentes roles
- Sistema de convites para onboarding
- Controle de acesso baseado em permissões

### 💳 Sistema de Pagamentos

#### Integração RevenueCat
- Webhooks para eventos de assinatura
- Processamento assíncrono via SQS
- Suporte a diferentes ambientes (sandbox/production)
- Criação automática de assinaturas gratuitas

#### Eventos Suportados
- **INITIAL_PURCHASE**: Primeira compra
- **RENEWAL**: Renovação de assinatura
- **CANCELLATION**: Cancelamento
- **EXPIRATION**: Expiração
- **UNCANCELLATION**: Reativação
- **PRODUCT_CHANGE**: Mudança de produto

### 📁 Upload de Arquivos

#### AWS S3 Integration
- Upload de múltiplos tipos de arquivo
- URLs pré-assinadas para acesso privado
- Organização por usuário e tipo
- Validação de extensões e tamanho

#### Tipos de Arquivo Suportados
- **PROFILE**: Fotos de perfil (jpg, jpeg, png, webp)
- **DOCUMENT**: Documentos (pdf, doc, docx, xls, xlsx, txt)
- **IMAGE**: Imagens gerais (jpg, jpeg, png, gif, webp, svg)
- **TASK**: Arquivos de tarefas
- **COMPANY**: Arquivos da empresa

### 📧 Sistema de Notificações

#### SMS via Twilio
- Envio de códigos de verificação
- Webhooks para status de entrega
- Suporte a WhatsApp Business
- Rate limiting e validações

#### Email via Microsoft 365
- Templates HTML com Thymeleaf
- Emails transacionais
- Suporte a cópia e cópia oculta
- Logs de entrega

### 🔗 Sistema de Convites

#### Funcionalidades
- Criação de convites por email
- Tokens únicos com expiração
- Aceitação/rejeição de convites
- Diferentes tipos de membro (EMPLOYEE, CLIENT, SUPPLIER)
- Cancelamento de convites pendentes

### 📍 Busca de Endereços

#### APIs Integradas
- **ViaCEP**: API principal
- **BrasilAPI**: Fallback
- Cache Redis para otimização
- Validação de CEP

### 🔄 Processamento Assíncrono

#### AWS SQS
- Processamento de webhooks de pagamento
- Retry automático em caso de falha
- Dead letter queue para mensagens problemáticas
- Concorrência configurável

## 📁 Estrutura do Projeto

```
src/main/kotlin/com/projectbasename/
├── application/                    # Camada de aplicação
│   ├── config/                    # Configurações
│   ├── dto/                       # Data Transfer Objects
│   ├── mapper/                    # Mappers entre camadas
│   ├── security/                  # Serviços de segurança
│   ├── util/                      # Utilitários
│   └── validation/                # Validações
├── domain/                        # Camada de domínio
│   ├── entity/                    # Entidades JPA
│   ├── enums/                     # Enums do domínio
│   ├── exception/                 # Exceções de negócio
│   ├── model/                     # Modelos de domínio
│   ├── repository/                # Interfaces de repositório
│   └── service/                   # Serviços de domínio
└── infrastructure/                # Camada de infraestrutura
    ├── cache/                     # Serviços de cache
    ├── controller/                # Controllers REST
    ├── integration/               # Integrações externas
    └── util/                      # Utilitários de infraestrutura
```

## ⚙️ Configuração

### Variáveis de Ambiente

```bash
# Database
DB_USERNAME=postgres
DB_PASSWORD=password
DB_URL=jdbc:postgresql://localhost:5435/projectbasename

# JWT
JWT_SECRET=your-super-secret-jwt-key-here

# AWS
AWS_ACCESS_KEY=your-aws-access-key
AWS_SECRET_KEY=your-aws-secret-key
AWS_REGION=us-east-1

# Twilio
TWILIO_ACCOUNT_SID=your-twilio-sid
TWILIO_AUTH_TOKEN=your-twilio-token
TWILIO_SMS_PHONE_NUMBER=+1234567890

# RevenueCat
REVENUECAT_SIGNATURE_KEY=your-revenuecat-signature-key
REVENUECAT_PUBLIC_KEY=your-revenuecat-public-key

# OAuth2
GOOGLE_OAUTH_CLIENT_ID=your-google-client-id
GOOGLE_OAUTH_CLIENT_SECRET=your-google-client-secret
MICROSOFT_OAUTH_CLIENT_ID=your-microsoft-client-id
MICROSOFT_OAUTH_CLIENT_SECRET=your-microsoft-client-secret
```

### Configuração do Banco

```sql
-- Criar banco PostgreSQL
CREATE DATABASE "projectbasename";

-- Executar migrações Flyway
./gradlew flywayMigrate
```

## 🚀 Execução

### Pré-requisitos
- Java 21
- PostgreSQL 14+
- Redis 6+
- Docker (opcional)

### Comandos Gradle

```bash
# Executar aplicação
./gradlew run

# Build do projeto
./gradlew build

# Executar testes
./gradlew test

# Executar checks (lint, tests)
./gradlew check

# Limpar build
./gradlew clean

# Executar migrações
./gradlew flywayMigrate

# Criar migração (usando script)
./create_migration.sh
```

### Docker Compose (Desenvolvimento)

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: projectbasename
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5435:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:6-alpine
    ports:
      - "6379:6379"

volumes:
  postgres_data:
```

## 📡 API Endpoints

### Autenticação
```
POST /v1/auth/register          # Registro de usuário
POST /v1/auth/activate          # Ativação de conta
POST /v1/auth/login             # Login com SMS
POST /v1/auth/verify-sms        # Verificação SMS
POST /v1/auth/social-login      # Login social
POST /v1/auth/refresh           # Renovar token
POST /v1/auth/logout            # Logout
```

### Usuários
```
GET    /v1/users/me             # Perfil do usuário logado
GET    /v1/users/{id}           # Buscar usuário por ID
PUT    /v1/users/{id}           # Atualizar usuário
POST   /v1/users/complete-profile # Completar perfil
GET    /v1/users/profile-status # Status do perfil
```

### Empresas
```
GET    /v1/companies/{id}       # Buscar empresa
PUT    /v1/companies/{id}       # Atualizar empresa
GET    /v1/companies/my-companies # Minhas empresas
GET    /v1/companies/personal   # Empresa pessoal
```

### Membros de Empresa
```
GET    /v1/companies/{id}/members     # Listar membros
POST   /v1/companies/{id}/members     # Adicionar membro
PUT    /v1/companies/{id}/members/{id} # Atualizar membro
DELETE /v1/companies/{id}/members/{id} # Remover membro
```

### Convites
```
POST   /v1/invitations              # Criar convite
POST   /v1/invitations/{token}/accept # Aceitar convite
POST   /v1/invitations/{token}/reject # Rejeitar convite
POST   /v1/invitations/{id}/cancel  # Cancelar convite
```

### Upload de Arquivos
```
POST   /v1/files/upload             # Upload de arquivo
GET    /v1/files/presigned-url      # URL pré-assinada
POST   /v1/files/profile-image      # Upload foto de perfil
```

### Assinaturas
```
GET    /v1/subscriptions            # Minhas assinaturas
GET    /v1/subscriptions/{id}       # Detalhes da assinatura
POST   /v1/admin/subscriptions/process-expired # Processar expiradas
```

### Endereços
```
GET    /v1/address/cep/{cep}        # Buscar por CEP
DELETE /v1/address/cep/{cep}/cache  # Invalidar cache
```

### Webhooks
```
POST   /v1/payments/webhook/revenuecat # Webhook RevenueCat
POST   /twilio-webhooks/sms-status     # Webhook Twilio SMS
```

## 🔐 Autenticação e Segurança

### JWT Tokens
- **Access Token**: 24h de validade
- **Refresh Token**: 30 dias de validade
- **Temp Token**: Para verificação SMS/Email

### OAuth2 Social Login
- **Google**: Validação via Google APIs
- **Apple**: Validação via JWT
- **Microsoft**: Validação via Microsoft Graph

### Rate Limiting
- 60 tokens por minuto por usuário
- Bloqueio temporário após 5 tentativas de login
- Cooldown de 60 segundos para reenvio de SMS

### Validação de Webhooks
- Assinatura HMAC-SHA256 para RevenueCat
- RequestValidator para Twilio
- Validação de origem e payload

## 🛡️ Sistema de Permissões

### Hierarquia de Roles
```
SUPER_USER > OWNER > ADMIN > MANAGER > EMPLOYEE > CLIENT/SUPPLIER > GUEST
```

### Tipos de Usuário
- **OWNER**: Pode criar empresa própria
- **INVITED**: Apenas membro de empresas de terceiros

### Validações por Funcionalidade

#### Gerenciamento de Usuários
- Criar usuário: Apenas SUPER_USER
- Listar usuários: Apenas SUPER_USER
- Atualizar usuário: Próprio usuário OU SUPER_USER
- Ver usuário: Próprio usuário OU SUPER_USER

#### Gerenciamento de Empresas
- Listar/ver empresa: Membro ativo
- Gerenciar empresa: OWNER ou ADMIN
- Criar empresa: Apenas OWNER sem empresa

#### Gerenciamento de Membros
- Listar membros: Membro ativo da empresa
- Adicionar/Remover membros: OWNER ou ADMIN
- Alterar roles: OWNER ou ADMIN

#### Gerenciamento de Convites
- Criar convite: OWNER ou ADMIN da empresa
- Listar convites: OWNER ou ADMIN
- Aceitar/Rejeitar convite: Destinatário do convite
- Cancelar convite: OWNER ou ADMIN que criou

#### Gerenciamento de Assinaturas
- Ver próprias assinaturas: Usuário autenticado
- Ver assinatura específica: Proprietário OU admin da empresa
- Cancelar/Reativar: Proprietário OU admin da empresa
- Endpoints administrativos: SUPER_USER ou OWNER

## 🔗 Integrações

### AWS Services
- **S3**: Upload e armazenamento de arquivos
- **SNS**: Notificações push
- **SQS**: Processamento assíncrono
- **CloudWatch**: Logs e métricas

### Twilio
- **SMS**: Códigos de verificação
- **WhatsApp**: Notificações
- **Webhooks**: Status de entrega

### Microsoft 365
- **SMTP**: Envio de emails
- **OAuth2**: Login social
- **Graph API**: Informações do usuário

### RevenueCat
- **Webhooks**: Eventos de assinatura
- **API**: Consulta de status
- **Validação**: Assinatura HMAC

### Google APIs
- **OAuth2**: Login social
- **Token Validation**: Validação de tokens
- **User Info**: Informações do usuário

## 🛠️ Desenvolvimento

### Criando Migrações
```bash
./create_migration.sh
# Seguir as instruções para criar nova migração
```

### Estrutura de Migração
```sql
-- V1__initial_migration.sql
-- V2__add_user_table.sql
-- V3__add_company_table.sql
-- etc...
```

### Padrões de Código
- **Kotlin**: Seguir convenções oficiais
- **KtLint**: Formatação automática
- **Naming**: snake_case para banco, camelCase para código
- **Documentação**: KDoc para funções públicas

### Testes
```bash
# Testes unitários
./gradlew test

# Testes de integração
./gradlew integrationTest

# Cobertura de código
./gradlew jacocoTestReport
```

### Debug e Logs
```yaml
logging:
  level:
    com.projectbasename: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
```

## 🚀 Deploy

### Docker
```dockerfile
FROM openjdk:21-jdk-slim
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Variáveis de Produção
```bash
# Configurações críticas
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=super-secret-production-key
DB_URL=jdbc:postgresql://prod-db:5432/projectbasename
REDIS_URL=redis://prod-redis:6379
```

### Health Checks
```
GET /actuator/health          # Status geral
GET /actuator/health/db       # Status do banco
GET /actuator/health/redis    # Status do Redis
GET /actuator/metrics         # Métricas da aplicação
```

### Monitoramento
- **New Relic**: APM e alertas
- **Prometheus**: Métricas customizadas
- **Grafana**: Dashboards
- **CloudWatch**: Logs e métricas AWS

## 📚 Documentação Adicional

### Swagger UI
- URL: `http://localhost:8080/swagger-ui.html`
- Documentação interativa da API
- Teste de endpoints

### Actuator
- URL: `http://localhost:8080/actuator`
- Health checks, métricas e informações do sistema

### Flyway
- Controle de versão do banco
- Migrações automáticas
- Rollback manual se necessário

## 🤝 Contribuição

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo `LICENSE` para mais detalhes.

## 🆘 Suporte

Para dúvidas ou problemas:
- Abra uma issue no GitHub
- Consulte a documentação do Swagger UI
- Verifique os logs da aplicação

---

**ProjectBaseName** - Base sólida para seus próximos projetos! 🚀