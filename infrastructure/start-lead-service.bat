@echo off
echo ============================================================
echo  Starting Lead Management Service (Quarkus dev mode)
echo ============================================================
echo.
echo  PostgreSQL : 127.0.0.1:5434
echo  Redis      : 127.0.0.1:6379
echo  Swagger UI : http://localhost:8080/swagger-ui
echo.
cd /d "%~dp0..\services\lead-service"
mvn quarkus:dev
