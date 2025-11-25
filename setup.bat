@echo off
echo ========================================
echo WhatsApp Clone - Setup Automatico
echo ========================================
echo.

REM Verificar Java
echo [1/6] Verificando Java...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java no esta instalado o no esta en PATH
    echo Por favor instala Java 17 o superior
    pause
    exit /b 1
)
java -version 2>&1 | findstr /i "version" | findstr /i "17\|18\|19\|20\|21" >nul
if %errorlevel% neq 0 (
    echo ADVERTENCIA: Se requiere Java 17 o superior
)
echo Java encontrado correctamente

REM Verificar Maven
echo.
echo [2/6] Verificando Maven...
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven no esta instalado o no esta en PATH
    echo Por favor instala Maven 3.6 o superior
    pause
    exit /b 1
)
echo Maven encontrado correctamente

REM Verificar MySQL
echo.
echo [3/6] Verificando MySQL...
mysql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ADVERTENCIA: MySQL no se encuentra en PATH
    echo Asegurate de que MySQL este instalado y ejecutandose
)

REM Configurar base de datos
echo.
echo [4/6] Configurando base de datos...
if not exist db.properties (
    if exist db.properties.example (
        echo Creando db.properties desde db.properties.example...
        copy db.properties.example db.properties
        echo.
        echo IMPORTANTE: Edita db.properties con tus credenciales de MySQL
        echo Presiona cualquier tecla para abrir db.properties...
        pause >nul
        notepad db.properties
    ) else (
        echo ERROR: No se encuentra db.properties.example
        pause
        exit /b 1
    )
) else (
    echo db.properties ya existe
)

REM Compilar proyecto
echo.
echo [5/6] Compilando proyecto...
echo Esto puede tardar unos minutos en la primera ejecucion...
call mvn clean install -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: La compilacion fallo
    echo Revisa los errores anteriores
    pause
    exit /b 1
)
echo Compilacion exitosa

REM Ejecutar aplicacion
echo.
echo [6/6] Iniciando aplicacion...
echo.
echo ========================================
echo Setup completado. Iniciando WhatsApp Clone...
echo ========================================
echo.
call mvn javafx:run

pause
