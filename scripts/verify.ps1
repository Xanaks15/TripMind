param([switch]$Release)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    # Prefer the verified, project-local JDK when available. Environment changes
    # apply only to this PowerShell process, never to the machine configuration.
    $localJdk = Get-ChildItem -Directory '.tools/jdk' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
    if (Test-Path '.tools/android-sdk') {
        $env:ANDROID_HOME = Join-Path $projectRoot '.tools/android-sdk'
    }
    $env:GRADLE_USER_HOME = Join-Path $projectRoot '.tools/gradle-home'
    # Android tooling otherwise falls back to C:\.android in some sandboxed shells.
    $env:ANDROID_USER_HOME = Join-Path $projectRoot '.tools/android-home'
    New-Item -ItemType Directory -Path $env:ANDROID_USER_HOME -Force | Out-Null
    # Windows packaged terminals may remap TEMP, breaking Java's local sockets.
    $socketDirectory = Join-Path $projectRoot '.tools/s'
    New-Item -ItemType Directory -Path $socketDirectory -Force | Out-Null
    $javaUserHome = Join-Path $projectRoot '.tools/user-home'
    New-Item -ItemType Directory -Path $javaUserHome -Force | Out-Null
    $env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS " +
        '"-Djdk.net.unixdomain.tmpdir=' + $socketDirectory + '" ' +
        '"-Duser.home=' + $javaUserHome + '"'

    $tasks = @(':app:assembleDebug', ':app:testDebugUnitTest', ':app:lintDebug')
    if ($Release) { $tasks += ':app:assembleRelease' }
    & .\gradlew.bat @tasks --console=plain --no-daemon --no-watch-fs
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}
