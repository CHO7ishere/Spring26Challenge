# Simple test runner for Player.java
# Runs the player against a scenario file and shows output

param(
    [string]$Scenario = "scenarii/default.txt",
    [switch]$Verbose = $false
)

$ErrorActionPreference = "Stop"

# Check if Java is available
$javaCheck = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCheck) {
    Write-Host "Java not found. Please install Java."
    exit 1
}

# Compile the player
Write-Host "Compiling Player.java..."
javac Player.java 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!"
    exit 1
}

Write-Host "Compilation successful"

# Check if scenario exists
if (-not (Test-Path $Scenario)) {
    Write-Host "Scenario not found: $Scenario"
    Write-Host "Creating default scenario..."
    New-Item -ItemType Directory -Path "scenarii" -Force | Out-Null

    @"
16 8
................
................
.....+++........
...~~~~~~~.......
................
................
................
0...............1
5 5 5 5 5 0
5 5 5 5 5 0
3
PLUM 3 2 1 6 0 8
LEMON 5 2 1 6 0 8
APPLE 7 2 1 11 0 9
2
0 0 3 2 1 1 1 0 0 0 0 0 0
1 12 5 1 1 1 1 0 0 0 0 0 0
"@ | Out-File -FilePath $Scenario -Encoding utf8
}

# Run the player with the scenario
Write-Host "Running player against scenario: $Scenario"
Write-Host ""

# Read scenario and feed to player
$content = Get-Content $Scenario -Raw

# Create a temp script to run the player
$tempScript = [System.IO.Path]::GetTempFileName() + ".ps1"

# Run player and capture output
$process = Start-Process -FilePath "java" -ArgumentList "Player" -NoNewWindow -Wait -PassThru -RedirectStandardInput "$Scenario" -RedirectStandardOutput "$env:TEMP\player_out.txt" -RedirectStandardError "$env:TEMP\player_err.txt"

if (Test-Path "$env:TEMP\player_out.txt") {
    Write-Host "Player output:"
    Get-Content "$env:TEMP\player_out.txt"
}

if ($Verbose -and (Test-Path "$env:TEMP\player_err.txt")) {
    Write-Host "Errors:"
    Get-Content "$env:TEMP\player_err.txt"
}

# Cleanup
Remove-Item "$env:TEMP\player_out.txt" -ErrorAction SilentlyContinue
Remove-Item "$env:TEMP\player_err.txt" -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "Done!"
