#!/usr/bin/env bash
# Tạo bảng MyChatApp_Chat trên DynamoDB Local (designDB.md §3)
set -euo pipefail

ENDPOINT="${DYNAMODB_ENDPOINT:-http://localhost:8000}"
REGION="${AWS_REGION:-us-east-1}"
TABLE="${DYNAMODB_TABLE:-MyChatApp_Chat}"

export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-local}"
export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-local}"

echo "Endpoint: $ENDPOINT | Table: $TABLE | Region: $REGION"

if aws dynamodb describe-table --table-name "$TABLE" --endpoint-url "$ENDPOINT" --region "$REGION" &>/dev/null; then
  echo "Table '$TABLE' already exists."
  exit 0
fi

aws dynamodb create-table \
  --table-name "$TABLE" \
  --attribute-definitions \
    AttributeName=PK,AttributeType=S \
    AttributeName=SK,AttributeType=S \
  --key-schema \
    AttributeName=PK,KeyType=HASH \
    AttributeName=SK,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url "$ENDPOINT" \
  --region "$REGION"

echo "Waiting for table to become ACTIVE..."
aws dynamodb wait table-exists \
  --table-name "$TABLE" \
  --endpoint-url "$ENDPOINT" \
  --region "$REGION"

echo "Done: $TABLE"
