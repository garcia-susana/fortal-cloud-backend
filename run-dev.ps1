param(
    [switch]$Help
)

if ($Help) {
    Write-Host @"
== Fortal-Cloud - Ambiente de Desenvolvimento ==

Antes de rodar, você precisa de:

1. PostgreSQL rodando local com um database "fortalcloud"
   Crie com:
     & "C:\Program Files\PostgreSQL\18\bin\createdb" -U postgres fortalcloud
   (ou use seu usuário/senha — configure no .env.dev)

2. Stripe (opcional para testar ativação manual)
   - Chaves de teste: https://dashboard.stripe.com/test/apikeys

3. Crie um arquivo .env.dev na raiz do projeto com:
@"
    Write-Host @"
   DB_USERNAME=postgres
   DB_PASSWORD=suasenha
   STRIPE_SECRET_KEY=sk_test_...
   STRIPE_PRICE_ID=price_1Test...
   STRIPE_WEBHOOK_SECRET=whsec_...
"@
    return
}

$envFile = Join-Path $PSScriptRoot ".env.dev"
if (Test-Path $envFile) {
    Write-Host "Carregando .env.dev..."
    Get-Content $envFile | ForEach-Object {
        if ($_ -match "^\s*([^#=]+)=(.*)\s*$") {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            Set-Item -Path "env:$name" -Value $value
            Write-Host "  $name=$value"
        }
    }
}

$javaHome = "C:\Program Files\Java\jdk-25.0.2"
if (-not (Test-Path $javaHome)) {
    Write-Host "Java 25 não encontrado em $javaHome. Procurando..."
    $javaHome = (Get-ChildItem "C:\Program Files\Java\jdk-*" -Directory | Select-Object -First 1).FullName
    if (-not $javaHome) {
        Write-Host "ERRO: JDK não encontrado. Instale o Java 25."
        exit 1
    }
}

$env:JAVA_HOME = $javaHome
$env:SPRING_PROFILES_ACTIVE = "dev"

Write-Host "Iniciando backend em http://localhost:8080 ..."
Write-Host "Perfil ativo: dev"
.\mvnw.cmd spring-boot:run
