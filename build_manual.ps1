$ErrorActionPreference = 'Stop'
$base = 'C:\Users\User\Documents\ThreeKingdomsRebuild'
$sdk = 'C:\Users\User\AppData\Local\Android\Sdk'
$jbr = 'C:\Program Files\Android\Android Studio\jbr'
$bt = Join-Path $sdk 'build-tools\36.0.0'
$androidJar = Join-Path $sdk 'platforms\android-37.0\android.jar'
$boot = Join-Path $base '.bootstrap'
$out = Join-Path $base 'app\build\outputs\apk\debug\app-debug.apk'
$env:JAVA_HOME = $jbr
$env:Path = "$jbr\bin;$env:Path"
& "$jbr\bin\javac.exe" -source 17 -target 17 -encoding UTF-8 -classpath $androidJar -d "$boot\classes" "$base\app\src\main\java\com\openai\threekingdoms\MainActivity.java"
& "$jbr\bin\jar.exe" --create --file "$boot\classes.jar" -C "$boot\classes" .
& "$bt\d8.bat" --lib $androidJar --output "$boot\dex" "$boot\classes.jar"
& "$bt\aapt2.exe" link --manifest "$boot\AndroidManifest.xml" -I $androidJar -o "$boot\base-unsigned.apk"
Copy-Item "$boot\dex\classes.dex" "$boot\classes.dex" -Force
Push-Location $boot
& "$bt\aapt.exe" add "base-unsigned.apk" "classes.dex"
Pop-Location
& "$bt\zipalign.exe" -f -p 4 "$boot\base-unsigned.apk" "$boot\app-aligned.apk"
$ks = 'C:\Users\User\.android\debug.keystore'
New-Item -ItemType Directory -Path (Split-Path $ks) -Force | Out-Null
if (!(Test-Path $ks)) { & "$jbr\bin\keytool.exe" -genkeypair -keystore $ks -storepass android -alias androiddebugkey -keypass android -dname 'CN=Android Debug,O=Android,C=US' -keyalg RSA -keysize 2048 -validity 10000 }
& "$bt\apksigner.bat" sign --ks $ks --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out $out "$boot\app-aligned.apk"
& "$bt\apksigner.bat" verify --verbose --print-certs $out
Get-Item $out | Select-Object FullName,Length,LastWriteTime | Format-List
