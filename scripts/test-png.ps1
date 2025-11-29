param(
  [string]$HostUrl = "http://localhost:8080",
  [string]$Payload = "$PSScriptRoot/../sample/test-payload.json",
  [string]$OutDir = "$PSScriptRoot/../out"
)

$ErrorActionPreference = 'Stop'

if (!(Test-Path -Path $OutDir)) { New-Item -ItemType Directory -Force -Path $OutDir | Out-Null }

$uri = "$HostUrl/api/v1/dashboard/png"
Write-Host "POST $uri"

curl.exe -s -f -X POST `
  "$uri" `
  -H "Content-Type: application/json" `
  -H "Accept: image/png" `
  --data-binary "@$Payload" `
  -o "$OutDir/dashboard.png"

Write-Host "Saved -> $OutDir/dashboard.png"