<#
Builds the backend and web images and pushes them to ECR, tagged with the current Git commit.
Run from the repository root:   .\scripts\push-images.ps1
Needs: Docker Desktop running, the AWS profile selected (AWS_PROFILE), and a clean Git working tree.
ECR tags are immutable, so pushing the same tag twice fails. Use -Tag to choose another one.
#>
param(
    [string]$Region = "ap-south-1",
    [string]$Project = "url-shortener",
    [string]$Tag = ""
)

$ErrorActionPreference = "Stop"

function Invoke-Checked {
    param([scriptblock]$Command)
    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code $LASTEXITCODE"
    }
}

if (git status --porcelain) {
    throw "The Git working tree has uncommitted changes. Commit or stash them first, so the tag matches the code."
}

if (-not $Tag) {
    $Tag = (git rev-parse --short HEAD).Trim()
}

$account = aws sts get-caller-identity --query Account --output text
if ($LASTEXITCODE -ne 0) {
    throw "Could not read the AWS identity. Is AWS_PROFILE set?"
}
$account = $account.Trim()
$registry = "${account}.dkr.ecr.${Region}.amazonaws.com"

# Stop early, before the slow build, if the registries do not exist yet (terraform apply creates them).
foreach ($name in @("backend", "web")) {
    aws ecr describe-repositories --repository-names "${Project}-${name}" --region $Region | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "The ECR repository ${Project}-${name} does not exist. Run terraform apply first."
    }
}

Write-Host "Pushing images tagged $Tag"

# The login goes through cmd.exe on purpose. In Windows PowerShell 5.1, piping the password
# straight into docker can corrupt it, and ECR then answers 400 Bad Request.
cmd /c "aws ecr get-login-password --region $Region | docker login --username AWS --password-stdin $registry"
if ($LASTEXITCODE -ne 0) {
    throw "Docker login to ECR failed."
}

foreach ($name in @("backend", "web")) {
    $image = "${registry}/${Project}-${name}:${Tag}"
    Invoke-Checked { docker build -t $image ".\$name" }
    Invoke-Checked { docker push $image }
}

Write-Host "Done. Deploy on the server with: IMAGE_TAG=$Tag"