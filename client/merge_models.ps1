param(
    [Parameter(Mandatory = $true)] [string] $DonorCacheDir,
    [Parameter(Mandatory = $false)] [string] $TargetCacheDir = "./Cache",
    [Parameter(Mandatory = $false)] [string] $Ids = "39050,39070"
)

$ErrorActionPreference = "Stop"

Write-Host "Compiling client..."
./gradlew compileJava | Out-Host

Write-Host "Merging model ids: $Ids"
$cp = "build/classes/java/main"
java -cp $cp com.runescape.tools.ModelMergeTool $DonorCacheDir $TargetCacheDir $Ids

Write-Host "Complete."

