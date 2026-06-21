# Import a SQL dump into the running Docker PostgreSQL container.
# The Docker volume keeps existing schema; only data rows are replaced.
#
# Usage:  .\scripts\import-db.ps1 -Dump dumps\local-20260620-1030.sql
#         .\scripts\import-db.ps1          # picks the latest file in dumps\

param(
    [string]$Dump = "",
    [string]$Container = "balgyn801-main-db-1",
    [string]$DbName    = "balgynbol-spring",
    [string]$User      = "postgres"
)

# Auto-select latest dump when not specified
if (-not $Dump) {
    $latest = Get-ChildItem "dumps\*.sql" -ErrorAction SilentlyContinue |
              Sort-Object LastWriteTime -Descending |
              Select-Object -First 1
    if (-not $latest) {
        Write-Error "No dump files found in dumps\. Run export-db.ps1 first."
        exit 1
    }
    $Dump = $latest.FullName
}

if (-not (Test-Path $Dump)) {
    Write-Error "Dump file not found: $Dump"
    exit 1
}

Write-Host "Importing $Dump → $Container/$DbName"

# Copy dump into the container then execute it
$tmpPath = "/tmp/import-dump.sql"
docker cp $Dump "${Container}:${tmpPath}"
if ($LASTEXITCODE -ne 0) { Write-Error "docker cp failed"; exit 1 }

docker exec -i $Container psql -U $User -d $DbName -f $tmpPath
if ($LASTEXITCODE -ne 0) {
    Write-Error "psql import failed"
    exit 1
}

docker exec $Container rm $tmpPath
Write-Host "Done. Restart the app container to pick up new data:"
Write-Host "  docker compose restart app"
