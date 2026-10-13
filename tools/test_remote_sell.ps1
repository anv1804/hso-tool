$ErrorActionPreference = 'Stop'
$RemoteSellRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $RemoteSellRoot
$RemoteSellClasses = Join-Path $RemoteSellRoot 'patch_bin\remote_sell_game'
$RemoteSellTests = Join-Path $RemoteSellRoot 'patch_bin\remote_sell_tests'
$RemoteSellBuildRoot = [System.IO.Path]::GetFullPath((Join-Path $RemoteSellRoot 'patch_bin')) + [System.IO.Path]::DirectorySeparatorChar
foreach ($RemoteSellOutput in @($RemoteSellClasses, $RemoteSellTests)) {
    $RemoteSellResolvedOutput = [System.IO.Path]::GetFullPath($RemoteSellOutput)
    if (-not $RemoteSellResolvedOutput.StartsWith($RemoteSellBuildRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Refusing to clean compiled outputs outside patch_bin'
    }
    if (Test-Path -LiteralPath $RemoteSellResolvedOutput) {
        Remove-Item -LiteralPath $RemoteSellResolvedOutput -Recurse -Force
    }
}
New-Item -ItemType Directory -Path $RemoteSellClasses, $RemoteSellTests -Force | Out-Null
$RemoteSellSourcesFile = Join-Path $RemoteSellTests 'sources.txt'
$RemoteSellSources = Get-ChildItem -LiteralPath (Join-Path $RemoteSellRoot 'src_java') -Recurse -Filter '*.java' |
    ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
[System.IO.File]::WriteAllLines($RemoteSellSourcesFile, $RemoteSellSources,
    (New-Object System.Text.UTF8Encoding($false)))

& javac -encoding UTF-8 -source 8 -target 8 -cp microemulator-3.0.0.jar -d $RemoteSellClasses "@$RemoteSellSourcesFile"
if ($LASTEXITCODE -ne 0) { throw 'Game compilation failed' }
$RemoteSellClasspath = "$RemoteSellClasses;microemulator-3.0.0.jar"
& javac -encoding UTF-8 -source 8 -target 8 -cp $RemoteSellClasspath -d $RemoteSellTests tools/tests/RemoteSellControllerTest.java tools/tests/RemoteSellPacketTest.java tools/tests/RemoteSellJsonTest.java tools/tests/GrahamNpcMenuTest.java
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& java -cp "$RemoteSellClasspath;$RemoteSellTests" RemoteSellControllerTest
if ($LASTEXITCODE -ne 0) { throw 'Controller tests failed' }
& java -cp "$RemoteSellClasspath;$RemoteSellTests" RemoteSellPacketTest
if ($LASTEXITCODE -ne 0) { throw 'Packet tests failed' }
& java '-Djava.awt.headless=true' -cp "$RemoteSellClasspath;$RemoteSellTests" RemoteSellJsonTest
if ($LASTEXITCODE -ne 0) { throw 'JSON tests failed' }
& java '-Djava.awt.headless=true' -cp "$RemoteSellClasspath;$RemoteSellTests" GrahamNpcMenuTest
if ($LASTEXITCODE -ne 0) { throw 'Graham menu tests failed' }
& python -m unittest discover -s tools/tests -p 'test_*.py' -v
if ($LASTEXITCODE -ne 0) { throw 'Tool tests failed' }
Write-Host 'Remote sell validation passed. No game account or live server was contacted.'
