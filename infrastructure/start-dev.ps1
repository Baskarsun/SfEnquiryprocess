# ============================================================
# Start local development services (no Docker needed)
# PostgreSQL 18 (Scoop) on port 5434  +  Redis 8 (Scoop)
# ============================================================

$PG_DATA = "C:\Users\SBaskar\scoop\apps\postgresql\current\data"
$PG_LOG  = "$PG_DATA\postgres-5434.log"
$PG_PORT = 5434

Write-Host "=== Starting PostgreSQL 18 on port $PG_PORT ===" -ForegroundColor Cyan

$pgStatus = & pg_ctl status -D $PG_DATA 2>&1
if ($pgStatus -match "server is running") {
    Write-Host "  Already running." -ForegroundColor Green
} else {
    & pg_ctl start -D $PG_DATA -o "-p $PG_PORT" -l $PG_LOG -w
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  Started." -ForegroundColor Green
    } else {
        Write-Host "  Failed to start PostgreSQL. Check: $PG_LOG" -ForegroundColor Red
        exit 1
    }
}

# Ensure DB and user exist
$check = & psql -U postgres -h 127.0.0.1 -p $PG_PORT -tA -c "SELECT 1 FROM pg_database WHERE datname='leasing_db'" 2>&1
if ($check -ne "1") {
    Write-Host "  Creating leasing_user and leasing_db..." -ForegroundColor Yellow
    & psql -U postgres -h 127.0.0.1 -p $PG_PORT -c "CREATE USER leasing_user WITH PASSWORD 'leasing_pass';" 2>&1 | Out-Null
    & psql -U postgres -h 127.0.0.1 -p $PG_PORT -c "CREATE DATABASE leasing_db OWNER leasing_user;" 2>&1 | Out-Null
    & psql -U postgres -h 127.0.0.1 -p $PG_PORT -c "GRANT ALL PRIVILEGES ON DATABASE leasing_db TO leasing_user;" 2>&1 | Out-Null
    Write-Host "  Done." -ForegroundColor Green
} else {
    Write-Host "  leasing_db already exists." -ForegroundColor Green
}

Write-Host ""
Write-Host "=== Starting Redis 8 on port 6379 ===" -ForegroundColor Cyan

$redisPing = & redis-cli -p 6379 ping 2>&1
if ($redisPing -eq "PONG") {
    Write-Host "  Already running." -ForegroundColor Green
} else {
    Start-Process -FilePath "redis-server" -ArgumentList "--port 6379" -WindowStyle Minimized
    Start-Sleep -Seconds 2
    $redisPing = & redis-cli -p 6379 ping 2>&1
    if ($redisPing -eq "PONG") {
        Write-Host "  Started." -ForegroundColor Green
    } else {
        Write-Host "  Failed to start Redis." -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== All services ready ===" -ForegroundColor Green
Write-Host "  PostgreSQL : 127.0.0.1:$PG_PORT  (leasing_user / leasing_pass / leasing_db)"
Write-Host "  Redis      : 127.0.0.1:6379"
Write-Host ""
Write-Host "  Run the service: cd ..\services\lead-service && mvn quarkus:dev"
Write-Host "  Swagger UI     : http://localhost:8080/swagger-ui"
