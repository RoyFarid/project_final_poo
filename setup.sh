#!/bin/bash

echo "========================================"
echo "WhatsApp Clone - Setup Automático"
echo "========================================"
echo ""

# Colores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Función para verificar comandos
check_command() {
    if command -v $1 &> /dev/null; then
        echo -e "${GREEN}✓${NC} $2 encontrado"
        return 0
    else
        echo -e "${RED}✗${NC} $2 no encontrado"
        return 1
    fi
}

# Verificar Java
echo "[1/6] Verificando Java..."
if check_command java "Java"; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        echo -e "${YELLOW}ADVERTENCIA:${NC} Se requiere Java 17 o superior"
    fi
else
    echo -e "${RED}ERROR:${NC} Por favor instala Java 17 o superior"
    exit 1
fi

# Verificar Maven
echo ""
echo "[2/6] Verificando Maven..."
if ! check_command mvn "Maven"; then
    echo -e "${RED}ERROR:${NC} Por favor instala Maven 3.6 o superior"
    exit 1
fi

# Verificar MySQL
echo ""
echo "[3/6] Verificando MySQL..."
if ! check_command mysql "MySQL"; then
    echo -e "${YELLOW}ADVERTENCIA:${NC} MySQL no se encuentra en PATH"
    echo "Asegúrate de que MySQL esté instalado y ejecutándose"
fi

# Configurar base de datos
echo ""
echo "[4/6] Configurando base de datos..."
if [ ! -f "db.properties" ]; then
    if [ -f "db.properties.example" ]; then
        echo "Creando db.properties desde db.properties.example..."
        cp db.properties.example db.properties
        echo ""
        echo -e "${YELLOW}IMPORTANTE:${NC} Edita db.properties con tus credenciales de MySQL"
        read -p "Presiona Enter para continuar después de editar db.properties..."
        ${EDITOR:-nano} db.properties
    else
        echo -e "${RED}ERROR:${NC} No se encuentra db.properties.example"
        exit 1
    fi
else
    echo "db.properties ya existe"
fi

# Compilar proyecto
echo ""
echo "[5/6] Compilando proyecto..."
echo "Esto puede tardar unos minutos en la primera ejecución..."
mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR:${NC} La compilación falló"
    echo "Revisa los errores anteriores"
    exit 1
fi
echo -e "${GREEN}Compilación exitosa${NC}"

# Ejecutar aplicación
echo ""
echo "[6/6] Iniciando aplicación..."
echo ""
echo "========================================"
echo "Setup completado. Iniciando WhatsApp Clone..."
echo "========================================"
echo ""
mvn javafx:run
