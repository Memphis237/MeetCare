$ErrorActionPreference = "Stop"

$workspace = Split-Path -Parent $PSScriptRoot
Set-Location $workspace

$javafxJars = @(
    "$env:USERPROFILE\.m2\repository\org\openjfx\javafx-base\21.0.6\javafx-base-21.0.6-win.jar",
    "$env:USERPROFILE\.m2\repository\org\openjfx\javafx-controls\21.0.6\javafx-controls-21.0.6-win.jar",
    "$env:USERPROFILE\.m2\repository\org\openjfx\javafx-fxml\21.0.6\javafx-fxml-21.0.6-win.jar",
    "$env:USERPROFILE\.m2\repository\org\openjfx\javafx-graphics\21.0.6\javafx-graphics-21.0.6-win.jar"
)

$dependencyJars = @(
    "$env:USERPROFILE\.m2\repository\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar",
    "$env:USERPROFILE\.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar"
)

$missing = @($javafxJars + $dependencyJars | Where-Object { -not (Test-Path $_) })
if ($missing.Count -gt 0) {
    $missingList = $missing -join "`n"
    throw "Dependances manquantes :`n$missingList"
}

if (Test-Path "target\classes\module-info.class") {
    Remove-Item -LiteralPath "target\classes\module-info.class" -Force
}

if (-not (Test-Path "target\classes")) {
    New-Item -ItemType Directory -Path "target\classes" | Out-Null
}

$classpath = ($javafxJars + $dependencyJars) -join ";"
$sources = Get-ChildItem -Recurse "src\main\java" -Filter *.java | ForEach-Object { $_.FullName }

& javac --release 20 -cp $classpath -d "target\classes" $sources

Copy-Item -Path "src\main\resources\*" -Destination "target\classes" -Recurse -Force

$modulePath = $javafxJars -join ";"
$runClassPath = @("target\classes") + $dependencyJars -join ";"

& java --module-path $modulePath --add-modules javafx.controls,javafx.fxml -cp $runClassPath org.example.meetcare.Main
