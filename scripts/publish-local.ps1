param(
  [Parameter(Mandatory = $true)][string]$Version,
  [ValidateSet('both', 'modrinth', 'curseforge')][string]$Platforms = 'both'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

function Import-DotEnv([string]$Path) {
  if (-not (Test-Path $Path)) { return }
  Get-Content $Path | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith('#')) { return }
    $eq = $line.IndexOf('=')
    if ($eq -le 0) { return }
    $key = $line.Substring(0, $eq).Trim()
    $val = $line.Substring($eq + 1).Trim().Trim('"').Trim("'")
    if (-not [Environment]::GetEnvironmentVariable($key)) {
      [Environment]::SetEnvironmentVariable($key, $val, 'Process')
    }
  }
}

@(
  (Join-Path $root '.env'),
  (Join-Path $env:USERPROFILE '.nightbeam\.env'),
  (Join-Path (Split-Path $root -Parent) '.env')
) | ForEach-Object { Import-DotEnv $_ }

node (Join-Path $PSScriptRoot 'publish-local.mjs') --version $Version --platforms $Platforms
