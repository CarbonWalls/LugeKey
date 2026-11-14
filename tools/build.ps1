# Builds LugeKey APK without Gradle, using the installed Android SDK tools.
# Prereq: Android Studio command-line tools + SDK in %LOCALAPPDATA%\Android\Sdk.
$ErrorActionPreference = "Continue"

$Root   = Split-Path -Parent $PSScriptRoot
$SDK    = "$env:LOCALAPPDATA\Android\Sdk"
$BT     = "$SDK\build-tools\35.0.0"
$JBR    = "C:\Program Files\Android\Android Studio\jbr"
$Java   = "$JBR\bin\java.exe"
$Jar    = "$JBR\bin\jar.exe"

$aapt2      = "$BT\aapt2.exe"
$androidJar = "$SDK\platforms\android-35\android.jar"
$d8jar      = "$BT\lib\d8.jar"
$apksignerJar = "$BT\lib\apksigner.jar"
$zipalign   = "$BT\zipalign.exe"

$Name = "LugeKey"
$Version = "0.2.2"

Push-Location $Root
try {
    Remove-Item -Recurse -Force gen, classes, dexout, build -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path gen, classes, dexout, build | Out-Null

    Write-Host "== 1/6 aapt2 compile resources"
    & $aapt2 compile --dir res -o build\compiled.flata
    if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

    Write-Host "== 2/6 aapt2 link + generate R.java"
    & $aapt2 link -o build\base.apk -I $androidJar --manifest AndroidManifest.xml `
        -R build\compiled.flata --java gen --min-sdk-version 26 --target-sdk-version 35 `
        --version-code 1 --version-name $Version --auto-add-overlay
    if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

    Write-Host "== 3/6 javac"
    $sources = @(Get-ChildItem -Recurse -Filter *.java src, gen | ForEach-Object { $_.FullName })
    & $JBR\bin\javac.exe -encoding UTF-8 --release 11 -classpath $androidJar `
        -Xlint:-options -d classes $sources 2>&1 | ForEach-Object { Write-Host "javac: $_" }
    if ($LASTEXITCODE -ne 0) { throw "javac failed" }

    Write-Host "== 4/6 d8 (dex)"
    & $Java -cp $d8jar com.android.tools.r8.D8 --release --min-api 26 --lib $androidJar `
        --output dexout @(Get-ChildItem -Recurse -Filter *.class classes | ForEach-Object { $_.FullName })
    if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

    Write-Host "== 5/6 add dex + zipalign"
    & $Jar uf build\base.apk -C dexout classes.dex
    & $zipalign -f 4 build\base.apk build\aligned.apk
    if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

    Write-Host "== 6/6 sign"
    $ks = "tools\debug.keystore"
    if (-not (Test-Path $ks)) {
        & $JBR\bin\keytool.exe -genkeypair -keystore $ks -alias lugekey `
            -keyalg RSA -keysize 2048 -validity 10000 -storepass lugekey -keypass lugekey `
            -dname "CN=LugeKey, OU=Dev, O=CarbonWalls, C=US" 2>&1 | Out-Null
    }
    & $Java -jar $apksignerJar sign --ks $ks --ks-pass pass:lugekey --key-pass pass:lugekey `
        --out "$Name-$Version.apk" build\aligned.apk
    if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

    & $Java -jar $apksignerJar verify --print-certs "$Name-$Version.apk"
    Write-Host "== DONE: $Root\$Name-$Version.apk"
}
finally {
    Pop-Location
}
