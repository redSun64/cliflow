param(
    [string]$OutputPath = "acli-app/target/command-preview.md"
)

$ErrorActionPreference = "Stop"
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $projectRoot

$javaCandidates = @()
if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $javaCandidates += Join-Path $env:JAVA_HOME "bin\java.exe"
}

$jpackageCommand = Get-Command jpackage -ErrorAction SilentlyContinue
if ($null -ne $jpackageCommand -and -not [string]::IsNullOrWhiteSpace($jpackageCommand.Source)) {
    $javaCandidates += Join-Path (Split-Path $jpackageCommand.Source -Parent) "java.exe"
}

$javaCommand = Get-Command java -ErrorAction SilentlyContinue
if ($null -ne $javaCommand -and -not [string]::IsNullOrWhiteSpace($javaCommand.Source)) {
    $javaCandidates += $javaCommand.Source
}

$java = $javaCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($java)) {
    throw "java.exe was not found. Install a full JDK 21 and set JAVA_HOME to its directory (for example, C:\\Program Files\\Amazon Corretto\\jdk21...)."
}

mvn -pl acli-app -am package -DskipTests
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

& $java -cp "acli-app/target/cliflow-sdk-0.1.0-SNAPSHOT-all.jar" io.github.redsun64.acli.app.CommandPreviewMain --output $OutputPath
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "Preview written to: $OutputPath"
