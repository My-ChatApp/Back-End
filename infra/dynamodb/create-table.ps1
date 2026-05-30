# Tạo bảng MyChatApp_Chat trên DynamoDB Local (Windows / PowerShell)
param(
    [string]$Endpoint = "http://localhost:8000",
    [string]$Region = "us-east-1",
    [string]$Table = "MyChatApp_Chat"
)

$env:AWS_ACCESS_KEY_ID = if ($env:AWS_ACCESS_KEY_ID) { $env:AWS_ACCESS_KEY_ID } else { "local" }
$env:AWS_SECRET_ACCESS_KEY = if ($env:AWS_SECRET_ACCESS_KEY) { $env:AWS_SECRET_ACCESS_KEY } else { "local" }

Write-Host "Endpoint: $Endpoint | Table: $Table | Region: $Region"

$describe = aws dynamodb describe-table --table-name $Table --endpoint-url $Endpoint --region $Region 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "Table '$Table' already exists."
    exit 0
}

aws dynamodb create-table `
  --table-name $Table `
  --attribute-definitions AttributeName=PK,AttributeType=S AttributeName=SK,AttributeType=S `
  --key-schema AttributeName=PK,KeyType=HASH AttributeName=SK,KeyType=RANGE `
  --billing-mode PAY_PER_REQUEST `
  --endpoint-url $Endpoint `
  --region $Region

if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

aws dynamodb wait table-exists --table-name $Table --endpoint-url $Endpoint --region $Region
Write-Host "Done: $Table"
