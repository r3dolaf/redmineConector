# ============================================================
# Limpieza Profunda del Proyecto
# ============================================================
# Elimina archivos innecesarios manteniendo solo lo esencial
# ============================================================

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Limpieza Profunda de Proyecto" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$removed = @()
$kept = @()

# ============================================================
# 1. ARCHIVOS DE BUILD TEMPORALES (root)
# ============================================================
Write-Host "[1/6] Eliminando archivos de build..." -ForegroundColor Yellow

$buildFiles = @(
    "bin",
    "build", 
    "installer",
    "portable",
    "*.jar",
    "*.zip",
    "manifest.txt",
    "*-Portable"
)

foreach ($pattern in $buildFiles) {
    Get-ChildItem -Path . -Filter $pattern -ErrorAction SilentlyContinue | ForEach-Object {
        try {
            Remove-Item -Recurse -Force $_.FullName -ErrorAction Stop
            $removed += $_.Name
            Write-Host "  Eliminado: $($_.Name)" -ForegroundColor DarkGray
        }
        catch {
            Write-Host "  Bloqueado: $($_.Name) (en uso)" -ForegroundColor Yellow
        }
    }
}

# ============================================================
# 2. DOCUMENTACION REDUNDANTE/OBSOLETA
# ============================================================
Write-Host ""
Write-Host "[2/6] Eliminando documentacion obsoleta..." -ForegroundColor Yellow

$obsoleteDocs = @(
    "INSTALLER_QUICKSTART.md",  # Ya innecesario con build-complete.ps1
    "RedmineConnector.bat",     # Se genera automaticamente
    "RedmineConnector.ps1",     # Se genera automaticamente  
    "README.txt",               # Se genera automaticamente
    "docs-md\DISTRIBUTION_GUIDE.md",  # Info duplicada en README
    "docs-md\build\BUILD_ANT_GUIDE.md" # Ya no usamos Ant
)

foreach ($file in $obsoleteDocs) {
    if (Test-Path $file) {
        Remove-Item -Force $file
        $removed += Split-Path -Leaf $file
        Write-Host "  Eliminado: $file" -ForegroundColor DarkGray
    }
}

# ============================================================
# 3. ARCHIVOS DE CONFIGURACION TEMPORALES Y CACHE
# ============================================================
Write-Host ""
Write-Host "[3/6] Eliminando caches y configs temporales..." -ForegroundColor Yellow

$tempConfigs = @(
    ".redmine_notifications.dat",
    ".redmine_notified_tasks.dat",
    ".redmine_heuristic.json",
    "redmine_config.properties.backup",
    "redmine_config.properties.template",
    "error_dump.txt"
)

foreach ($file in $tempConfigs) {
    if (Test-Path $file) {
        Remove-Item -Force $file
        $removed += $file
        Write-Host "  Eliminado: $file" -ForegroundColor DarkGray
    }
}

if (Test-Path "cache") {
    Remove-Item -Recurse -Force "cache"
    $removed += "cache"
    Write-Host "  Eliminado: cache/" -ForegroundColor DarkGray
}

# ============================================================
# 4. BUILD.XML (Obsoleto - usamos PowerShell)
# ============================================================
Write-Host ""
Write-Host "[4/6] Eliminando build.xml obsoleto..." -ForegroundColor Yellow

if (Test-Path "build.xml") {
    Remove-Item -Force "build.xml"
    $removed += "build.xml"
    Write-Host "  Eliminado: build.xml (ya no se usa)" -ForegroundColor DarkGray
}

# ============================================================
# 5. ARCHIVOS TEMPORALES DEL SISTEMA
# ============================================================
Write-Host ""
Write-Host "[5/6] Eliminando temporales del sistema..." -ForegroundColor Yellow

$systemTemp = @("*.log", "*.tmp", "*~", "Thumbs.db", ".DS_Store", "*.swp")
foreach ($pattern in $systemTemp) {
    Get-ChildItem -Path . -Recurse -Filter $pattern -ErrorAction SilentlyContinue | ForEach-Object {
        Remove-Item -Force $_.FullName
        Write-Host "  Eliminado: $($_.FullName)" -ForegroundColor DarkGray
        $removed += $_.Name
    }
}

# ============================================================
# 6. VERIFICAR ARCHIVOS ESENCIALES
# ============================================================
Write-Host ""
Write-Host "[6/6] Verificando archivos esenciales..." -ForegroundColor Yellow

$essential = @(
    "README.md",
    "build-complete.ps1",
    "cleanup.ps1", 
    "setup-java.ps1",
    "src",
    "docs",
    "config",
    "docs-md",
    ".gitignore",
    ".project",
    ".classpath"
)

foreach ($item in $essential) {
    if (Test-Path $item) {
        $kept += $item
        Write-Host "  Conservado: $item" -ForegroundColor Green
    }
    else {
        Write-Host "  FALTA: $item" -ForegroundColor Red
    }
}

# ============================================================
# RESUMEN
# ============================================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Limpieza Completada" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Archivos eliminados: $($removed.Count)" -ForegroundColor Cyan
Write-Host "Archivos conservados: $($kept.Count)" -ForegroundColor Cyan
Write-Host ""
Write-Host "Estructura del proyecto:" -ForegroundColor Yellow
Write-Host "  src/                  Codigo fuente Java" -ForegroundColor White
Write-Host "  docs/                 Documentacion proyecto" -ForegroundColor White
Write-Host "  config/               Configuraciones (redmine_config.properties)" -ForegroundColor White
Write-Host "  docs-md/              Documentacion Markdown" -ForegroundColor White
Write-Host "  build-complete.ps1    Script de build principal" -ForegroundColor White
Write-Host "  setup-java.ps1        Configurar Java" -ForegroundColor White
Write-Host "  cleanup.ps1           Este script" -ForegroundColor White
Write-Host "  README.md             Documentacion principal" -ForegroundColor White
Write-Host ""
Write-Host "Para construir el proyecto:" -ForegroundColor Cyan
Write-Host "  .\build-complete.ps1" -ForegroundColor White
Write-Host ""
