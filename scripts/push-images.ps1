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

$account = (aws sts get-caller-identity --query Account --output text).Trim()
if ($LASTEXITCODE -ne 0) {
    throw "Could not read the AWS identity. Is AWS_PROFILE set?"
}
$registry = "${account}.dkr.ecr.${Region}.amazonaws.com"

Write-Host "Pushing images tagged $Tag"
aws ecr get-login-password --region $Region | docker login --username AWS --password-stdin $registry
if ($LASTEXITCODE -ne 0) {
    throw "Docker login to ECR failed."
}

foreach ($name in @("backend", "web")) {
    $image = "${registry}/${Project}-${name}:${Tag}"
    Invoke-Checked { docker build -t $image ".\$name" }
    Invoke-Checked { docker push $image }
}

Write-Host "Done. Deploy on the server with: IMAGE_TAG=$Tag"
