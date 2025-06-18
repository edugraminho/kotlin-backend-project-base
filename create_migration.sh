#!/bin/bash

# Este script cria arquivos de migração Flyway seguindo as convenções:
# - Localização: src/main/resources/db/migration/
# - Nomenclatura: V{numero}__{descricao}.sql
# - Versionamento sequencial automático


# Configurações
MIGRATION_OUTPUT_FOLDER="src/main/resources/db/migration"
OUTPUT_FILE_PREFIX="V"
OUTPUT_FILE_SUFFIX=".sql"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo "=================================================="
echo -e "${BLUE}CRIADOR DE MIGRAÇÕES FLYWAY - Project${NC}"
echo "=================================================="
echo ""

mkdir -p "$MIGRATION_OUTPUT_FOLDER"

# Detectar próximo número de versão automaticamente
NEXT_VERSION=1
if [ -d "$MIGRATION_OUTPUT_FOLDER" ]; then
    # Buscar o maior número de versão existente
    LATEST_VERSION=$(find "$MIGRATION_OUTPUT_FOLDER" -name "V*.sql" -type f | \
                    sed 's/.*\/V\([0-9]*\)__.*/\1/' | \
                    sort -n | tail -1)

    if [ ! -z "$LATEST_VERSION" ]; then
        NEXT_VERSION=$((LATEST_VERSION + 1))
    fi
fi

echo -e "${GREEN}Próxima versão detectada: V${NEXT_VERSION}${NC}"
echo ""

# Solicitar nome da migração
echo -e "${BLUE} Digite o nome da migração:${NC}"
echo -e "${YELLOW}   • Use snake_case_para_separar_palavras${NC}"
echo -e "${YELLOW}   • Exemplo: create_task_table, add_user_permissions${NC}"
echo -e "${YELLOW}   • CTRL+C para cancelar${NC}"
echo ""
echo -n "Nome da migração: "
read MIGRATION_NAME

# Validar entrada
if [ -z "$MIGRATION_NAME" ]; then
    echo -e "${RED} Nome da migração não pode estar vazio${NC}"
    exit 1
fi

# Remover espaços e caracteres especiais
MIGRATION_NAME=$(echo "$MIGRATION_NAME" | tr ' ' '_' | tr -cd '[:alnum:]_')

# Criar nome do arquivo
MIGRATION_FILE_NAME="${OUTPUT_FILE_PREFIX}${NEXT_VERSION}__${MIGRATION_NAME}${OUTPUT_FILE_SUFFIX}"
FULL_PATH="$MIGRATION_OUTPUT_FOLDER/$MIGRATION_FILE_NAME"

echo ""
echo -e "${BLUE} Criando migração...${NC}"

# Criar arquivo com template básico
cat > "$FULL_PATH" << EOF
-- $MIGRATION_FILE_NAME
-- Data: $(date '+%d/%m/%Y %H:%M:%S')

EOF

echo -e "${GREEN} Migração criada com sucesso!${NC}"