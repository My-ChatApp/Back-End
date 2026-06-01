# Shared helper: locate jmeter.bat on Windows / Linux / macOS.

function Resolve-JMeterBat {
    param(
        [string]$ExplicitHome = ""
    )

    $homes = @()
    if ($ExplicitHome) { $homes += $ExplicitHome.TrimEnd('\', '/') }
    if ($env:JMETER_HOME) { $homes += $env:JMETER_HOME.TrimEnd('\', '/') }

    foreach ($jmeterHomeDir in $homes) {
        $bat = Join-Path $jmeterHomeDir "bin\jmeter.bat"
        $sh = Join-Path $jmeterHomeDir "bin/jmeter"
        if (Test-Path -LiteralPath $bat) { return (Resolve-Path -LiteralPath $bat).Path }
        if (Test-Path -LiteralPath $sh) { return (Resolve-Path -LiteralPath $sh).Path }
    }

    foreach ($name in @("jmeter.bat", "jmeter")) {
        $cmd = Get-Command $name -ErrorAction SilentlyContinue
        if ($cmd -and $cmd.Source -notmatch "run-jmeter") {
            return $cmd.Source
        }
    }

    if ($IsWindows -or $env:OS -match "Windows") {
        $patterns = @(
            "C:\apache-jmeter*",
            "$env:ProgramFiles\apache-jmeter*",
            "$env:ProgramFiles\JMeter*",
            "${env:ProgramFiles(x86)}\apache-jmeter*",
            "$env:LOCALAPPDATA\Programs\JMeter",
            "$env:LOCALAPPDATA\Programs\apache-jmeter*",
            "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\DEVCOM.JMeter*",
            "$env:ProgramData\chocolatey\lib\jmeter\tools\*"
        )
        # winget DEVCOM.JMeter → %LOCALAPPDATA%\Programs\JMeter (exact path, no glob)
        $wingetJmeter = Join-Path $env:LOCALAPPDATA "Programs\JMeter\bin\jmeter.bat"
        if (Test-Path -LiteralPath $wingetJmeter) {
            return (Resolve-Path -LiteralPath $wingetJmeter).Path
        }
        foreach ($pattern in $patterns) {
            $dirs = Get-ChildItem -Path $pattern -Directory -ErrorAction SilentlyContinue
            foreach ($dir in $dirs) {
                $bat = Join-Path $dir.FullName "bin\jmeter.bat"
                if (Test-Path -LiteralPath $bat) {
                    return (Resolve-Path -LiteralPath $bat).Path
                }
                $found = Get-ChildItem -Path $dir.FullName -Filter "jmeter.bat" -Recurse -Depth 4 -ErrorAction SilentlyContinue |
                    Select-Object -First 1
                if ($found) { return $found.FullName }
            }
        }
    }

    return $null
}

function Get-JMeterInstallHint {
    @"
JMeter is not installed or not on PATH.

Windows (recommended):
  winget install --id DEVCOM.JMeter -e --accept-package-agreements --accept-source-agreements
  # New terminal, then:
  `$env:JMETER_HOME = "<path-to-apache-jmeter-5.6.x>"
  .\load-test\scripts\run-jmeter.ps1 -Threads 10 -RampUp 10 -Loops 2

Or run:
  .\load-test\scripts\install-jmeter.ps1

Manual: https://jmeter.apache.org/download_jmeter.cgi
  Extract zip, set JMETER_HOME to folder, add %JMETER_HOME%\bin to PATH.

Pass explicit home:
  .\load-test\scripts\run-jmeter.ps1 -JmeterHome "C:\apache-jmeter-5.6.3" ...
"@
}
