# Troll Farm Launcher
# Downloads the referee and runs local games

param(
    [string]$Player1 = "java Player",
    [string]$Player2 = "java Player",
    [int]$Seed = 1,
    [switch]$Watch = $false
)

$ErrorActionPreference = "Stop"

$REFEREVE_JAR = "troll-farm-1.0-SNAPSHOT.jar"
$REPO_URL = "https://github.com/eulerscheZahl/Troll-Farm"
$RELEASE_URL = "https://github.com/eulerscheZahl/Troll-Farm/releases/download"

function Ensure-Referee {
    $version = "v1.0.3"
    $jarUrl = "$RELEASE_URL/$version/troll-farm-1.0-SNAPSHOT.jar"

    Write-Host "Downloading referee from $jarUrl..." -ForegroundColor Yellow

    try {
        Invoke-WebRequest -Uri $jarUrl -OutFile $REFEREVE_JAR -UseBasicParsing -TimeoutSec 60
        Write-Host "Downloaded referee successfully" -ForegroundColor Green
    } catch {
        Write-Host "Failed to download referee: $_" -ForegroundColor Red
        Write-Host "Please download manually from $REPO_URL/releases" -ForegroundColor Yellow
        exit 1
    }
}

# Ensure referee is available
if (-not (Test-Path $REFEREVE_JAR)) {
    Write-Host "Referee jar not found. Downloading..." -ForegroundColor Yellow
    Ensure-Referee
}

# Build the command
$cmd = "java -jar $REFEREVE_JAR -p1 `"$Player1`" -p2 `"$Player2`" -seed $Seed"

if ($Watch) {
    $cmd += " -s"
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Troll Farm Local Launcher" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Player 1: $Player1" -ForegroundColor White
Write-Host "Player 2: $Player2" -ForegroundColor White
Write-Host "Seed: $Seed" -ForegroundColor White
Write-Host "Watch: $Watch" -ForegroundColor White
Write-Host ""
Write-Host "Running: $cmd" -ForegroundColor Gray
Write-Host ""

# Run the game
Invoke-Expression $cmd
