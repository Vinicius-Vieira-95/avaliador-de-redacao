# Sobe o Keycloak de desenvolvimento sem Docker (precisa de Java 21+).
# Na primeira vez baixa o Keycloak (~200 MB) para keycloak/servidor/, que fica fora do Git.
# Uso, na raiz do projeto:  .\keycloak\iniciar-keycloak.ps1
# Console de administração: http://localhost:8081 (admin / admin)

$ErrorActionPreference = 'Stop'
$versao = '26.8.1'
$pasta = Join-Path $PSScriptRoot 'servidor'
$servidor = Join-Path $pasta "keycloak-$versao"

if (-not (Test-Path $servidor)) {
    New-Item -ItemType Directory -Force $pasta | Out-Null
    $zip = Join-Path $pasta "keycloak-$versao.zip"
    Write-Host "Baixando Keycloak $versao..."
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest "https://github.com/keycloak/keycloak/releases/download/$versao/keycloak-$versao.zip" -OutFile $zip
    Expand-Archive $zip -DestinationPath $pasta
    Remove-Item $zip
}

# O realm é importado só se ainda não existir; para recriá-lo do zero, apague keycloak/servidor/.
$importacao = Join-Path $servidor 'data\import'
New-Item -ItemType Directory -Force $importacao | Out-Null
Copy-Item (Join-Path $PSScriptRoot 'realm-redacao-enem.json') $importacao -Force

$env:KC_BOOTSTRAP_ADMIN_USERNAME = 'admin'
$env:KC_BOOTSTRAP_ADMIN_PASSWORD = 'admin'
& (Join-Path $servidor 'bin\kc.bat') start-dev --http-port=8081 --import-realm
