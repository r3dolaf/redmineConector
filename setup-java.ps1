# Script Helper - Configurar Java y construir instalador
# Detecta automaticamente Java y configura el PATH

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Java Detection & Setup Helper       " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Buscar Java en ubicaciones comunes
$javaPaths = @(
    "C:\Program Files\Java\*\bin",
    "C:\Program Files (x86)\Java\*\bin",
    "C:\Program Files\Eclipse Adoptium\*\bin",
    "C:\Program Files\AdoptOpenJDK\*\bin",
    "C:\Program Files\Zulu\*\bin",
    "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\*\bin",
    "C:\Users\$env:USERNAME\.jdks\*\bin"
)

Write-Host "[1/3] Buscando instalaciones de Java..." -ForegroundColor Yellow

$foundJavas = @()
foreach ($path in $javaPaths) {
    $dirs = Get-ChildItem -Path $path -ErrorAction SilentlyContinue
    foreach ($dir in $dirs) {
        $javacPath = Join-Path $dir.FullName "javac.exe"
        if (Test-Path $javacPath) {
            # Obtener version
            $versionOutput = & $javacPath -version 2>&1 | Out-String
            $foundJavas += @{
                Path      = $dir.FullName
                JavacPath = $javacPath
                Version   = $versionOutput.Trim()
            }
        }
    }
}

if ($foundJavas.Count -eq 0) {
    Write-Host ""
    Write-Host "[ERROR] No se encontro ninguna instalacion de Java con javac" -ForegroundColor Red
    Write-Host ""
    Write-Host "Opciones:" -ForegroundColor Yellow
    Write-Host "  1. Descargar JDK desde: https://adoptium.net/" -ForegroundColor White
    Write-Host "  2. Instalar con Chocolatey: choco install temurin17" -ForegroundColor White
    Write-Host "  3. Si usas Eclipse, busca en: C:\Users\$env:USERNAME\.p2\pool\plugins\org.eclipse.justj.*" -ForegroundColor White
    Write-Host ""
    exit 1
}

Write-Host "      [OK] Encontradas $($foundJavas.Count) instalacion(es) de Java" -ForegroundColor Green
Write-Host ""

# Mostrar opciones
for ($i = 0; $i -lt $foundJavas.Count; $i++) {
    $java = $foundJavas[$i]
    Write-Host "  [$($i+1)] $($java.Version)" -ForegroundColor Cyan
    Write-Host "      Path: $($java.Path)" -ForegroundColor DarkGray
    Write-Host ""
}

# Seleccionar la mas reciente (por defecto)
$selectedJava = $foundJavas[0]
Write-Host "[2/3] Seleccionada: $($selectedJava.Version)" -ForegroundColor Yellow
Write-Host "      Path: $($selectedJava.Path)" -ForegroundColor DarkGray
Write-Host ""

# Configurar PATH temporalmente para esta sesion
Write-Host "[3/3] Configurando PATH temporal..." -ForegroundColor Yellow
$env:Path = "$($selectedJava.Path);$env:Path"
$env:JAVA_HOME = Split-Path -Parent $selectedJava.Path

Write-Host "      [OK] JAVA_HOME = $env:JAVA_HOME" -ForegroundColor DarkGray
Write-Host "      [OK] PATH actualizado" -ForegroundColor DarkGray
Write-Host ""

# Verificar
$javacTest = & javac -version 2>&1
Write-Host "      Verificacion: $javacTest" -ForegroundColor Green
Write-Host ""

# Preguntar si quiere ejecutar build
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
$response = Read-Host "Ejecutar build-complete.ps1 ahora? (S/N)"

if ($response -eq "S" -or $response -eq "s") {
    Write-Host ""
    # Ejecutar con politica Bypass explicita para evitar errores de permisos
    powershell -NoProfile -ExecutionPolicy Bypass -File ".\build-complete.ps1"
}
else {
    Write-Host ""
    Write-Host "Para ejecutar el build manualmente:" -ForegroundColor Yellow
    Write-Host "  1. Ejecuta este script primero: .\setup-java.ps1" -ForegroundColor White
    Write-Host "  2. Luego ejecuta: .\build-complete.ps1" -ForegroundColor White
    Write-Host ""
    Write-Host "O ejecuta todo en una linea:" -ForegroundColor Yellow
    Write-Host '  .\setup-java.ps1; .\build-complete.ps1' -ForegroundColor White
    Write-Host ""
}
