# ElastiCache for Valkey (production)

Chat-service dùng `spring.data.redis` — local: Docker Valkey (`localhost:6379`).

## Triển khai AWS (khi đã có VPC)

1. Tạo ElastiCache **replication group** engine `valkey` trong private subnet (cùng VPC với ECS/EKS chạy chat-service).
2. Bật **AUTH token** và in-transit encryption (TLS).
3. Security group: chỉ cho phép inbound `6379` từ SG của chat-service.
4. Thêm CloudFormation output `ValkeyPrimaryEndpoint` (primary node address).
5. Cập nhật `.env` production:

```properties
SPRING_DATA_REDIS_HOST=<primary-endpoint>
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=<auth-token>
SPRING_DATA_REDIS_SSL_ENABLED=true
```

6. Chạy `.\scripts\sync-cdk-env.ps1` sau khi stack có output `ValkeyPrimaryEndpoint`.

CDK construct có thể bổ sung vào `stack.ts` khi VPC/subnet đã có trong stack (hiện stack chưa tạo VPC — thêm Valkey cùng lúc với ECS/RDS).
