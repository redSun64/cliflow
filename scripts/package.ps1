$ErrorActionPreference = "Stop"
if ([System.Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT) {
  throw "package.ps1 generates a Windows app-image and must run on Windows. jpackage does not cross-package native launchers."
}

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $Root

$jpackageCandidates = @()
if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
  $jpackageCandidates += Join-Path $env:JAVA_HOME "bin\jpackage.exe"
}

$javaCommand = Get-Command java -ErrorAction SilentlyContinue
if ($null -ne $javaCommand -and -not [string]::IsNullOrWhiteSpace($javaCommand.Source)) {
  $jpackageCandidates += Join-Path (Split-Path $javaCommand.Source -Parent) "jpackage.exe"
}

$jpackage = $jpackageCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if ($null -eq $jpackage) {
  $jpackageCommand = Get-Command jpackage -ErrorAction SilentlyContinue
  if ($null -ne $jpackageCommand) {
    $jpackage = $jpackageCommand.Source
  }
}
if ([string]::IsNullOrWhiteSpace($jpackage)) {
  throw "jpackage.exe was not found. Install a full JDK 21 and set JAVA_HOME to its directory (for example, C:\\Program Files\\Amazon Corretto\\jdk21...)."
}

mvn -DskipTests package
if (Test-Path dist) { Remove-Item -Recurse -Force dist }
New-Item -ItemType Directory dist | Out-Null

& $jpackage `
  --type app-image `
  --name cliflow `
  --win-console `
  --input acli-app/target `
  --main-jar cliflow-sdk-0.1.0-SNAPSHOT-all.jar `
  --main-class io.github.redsun64.acli.app.Main `
  --dest dist

Write-Host "Created self-contained application at: $Root/dist/cliflow"
