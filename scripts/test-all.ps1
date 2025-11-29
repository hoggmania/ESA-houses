param(
  [string]$HostUrl = "http://localhost:8080",
  [string]$Payload = "$PSScriptRoot/../sample/test-payload.json",
  [string]$OutDir = "$PSScriptRoot/../out"
)

$ErrorActionPreference = 'Stop'

& "$PSScriptRoot/test-svg.ps1" -HostUrl $HostUrl -Payload $Payload -OutDir $OutDir
& "$PSScriptRoot/test-png.ps1" -HostUrl $HostUrl -Payload $Payload -OutDir $OutDir

Write-Host "Done. Files saved to $OutDir"