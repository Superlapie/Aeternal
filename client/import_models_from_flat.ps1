param(
    [Parameter(Mandatory = $true)] [string] $FlatCacheRoot,
    [Parameter(Mandatory = $false)] [string] $TargetCacheDir = "./Cache",
    [Parameter(Mandatory = $false)] [string] $Ids = "ALL"
)

$ErrorActionPreference = "Stop"

Write-Host "Compiling client..."
./gradlew compileJava | Out-Host

Write-Host "Importing models ($Ids) from $FlatCacheRoot into $TargetCacheDir"
$cp = "build/classes/java/main"
java -cp $cp com.runescape.tools.FlatModelImportTool $FlatCacheRoot $TargetCacheDir $Ids

Write-Host "Complete."

