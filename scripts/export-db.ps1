# Export the local IntelliJ PostgreSQL database to a SQL dump file.
# Usage:  .\scripts\export-db.ps1
#         .\scripts\export-db.ps1 -Out dumps\my-snapshot.sql

param(
    [string]$Host     = "localhost",
    [string]$Port     = "5432",
    [string]$User     = "postgres",
    [string]$DbName   = "balgynbol-spring",
    [string]$Out      = "dumps\local-$(Get-Date -Format 'yyyyMMdd-HHmm').sql"
)

$dumpsDir = Split-Path $Out -Parent
if ($dumpsDir -and -not (Test-Path $dumpsDir)) {
    New-Item -ItemType Directory -Force $dumpsDir | Out-Null
}

Write-Host "Exporting $DbName@$Host`:$Port → $Out"

$env:PGPASSWORD = "1234"
& pg_dump `
    --host=$Host `
    --port=$Port `
    --username=$User `
    --no-password `
    --format=plain `
    --no-owner `
    --no-privileges `
    --data-only `
    --disable-triggers `
    $DbName | Out-File -Encoding utf8 $Out

if ($LASTEXITCODE -eq 0) {
    Write-Host "Done: $Out"
} else {
    Write-Error "pg_dump failed (exit $LASTEXITCODE). Make sure pg_dump is on your PATH."
}
