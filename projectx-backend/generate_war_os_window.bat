@echo off

set pwd=D:\projects\projectx
set module_name=projectx-backend

echo ========================================
echo -- Clean project modules (ONCE)
echo ========================================

cd /d "%pwd%"
call mvn clean

echo ========================================
echo -- Build compressor & minify assets
echo ========================================

cd /d "%pwd%\projectx-compressor"
call mvn clean package

cd /d "%pwd%\target\compressor"
call java -jar projectx-compressor-jar-with-dependencies.jar %pwd% %module_name%

cd /d "%pwd%"

echo ========================================
echo -- Install parent POM
echo ========================================

call mvn install -N

echo ========================================
echo -- Build persistence module
echo ========================================

cd /d "%pwd%\projectx-persistence"
call mvn package -P_production -Dmaven.test.skip=true

echo ========================================
echo -- Build backend WAR (NO CLEAN)
echo ========================================

cd /d "%pwd%\%module_name%"
call mvn package -P_production ^
    -Dmaven.test.skip=true ^
    -Dmaven.site.skip=true ^
    -Dmaven.javadoc.skip=true

echo ========================================
echo -- Production build completed
echo ========================================

pause
