# Genera los secretos de desarrollo local en ./secrets/ (ignorados por git).
# No sobrescribe secretos existentes y nunca los imprime en pantalla.
$ErrorActionPreference = "Stop"

$dir = Join-Path $PSScriptRoot "..\secrets"
New-Item -ItemType Directory -Force $dir | Out-Null

$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
foreach ($name in "db_superuser_password", "db_owner_password", "db_app_password") {
    $path = Join-Path $dir $name
    if (Test-Path $path) {
        Write-Host "Ya existe, no se toca: $name"
        continue
    }
    # 32 bytes aleatorios criptográficamente seguros -> 64 caracteres hex (sin comillas raras).
    $bytes = New-Object byte[] 32
    $rng.GetBytes($bytes)
    $secret = -join ($bytes | ForEach-Object { $_.ToString("x2") })
    # Sin BOM y sin salto de línea al final.
    [System.IO.File]::WriteAllText($path, $secret)
    Write-Host "Creado: $name"
}
$rng.Dispose()
