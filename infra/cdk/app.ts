#!/usr/bin/env node
import * as cdk from 'aws-cdk-lib';
import { AppConfig, MyChatAppStack } from './stack';

const PLACEHOLDER_DOMAIN = 'CHANGE_ME.example.com';

function loadConfig(app: cdk.App): AppConfig {
  const domainName = (app.node.tryGetContext('mychatapp:domainName') as string) ?? PLACEHOLDER_DOMAIN;
  if (domainName === PLACEHOLDER_DOMAIN) {
    cdk.Annotations.of(app).addWarning('Set mychatapp:domainName in cdk.json before deploy.');
  }
  return {
    environment: app.node.tryGetContext('mychatapp:environment') ?? 'dev',
    domainName,
    hostedZoneId: ((app.node.tryGetContext('mychatapp:hostedZoneId') as string) ?? '').trim(),
    mailFromSubdomain: app.node.tryGetContext('mychatapp:mailFromSubdomain') ?? 'mail',
    sesFromEmail:
      app.node.tryGetContext('mychatapp:sesFromEmail') ?? `noreply@${domainName}`,
    bucketNamePrefix: app.node.tryGetContext('mychatapp:bucketNamePrefix') ?? 'mychatapp-attachments',
    dynamodbTableName: app.node.tryGetContext('mychatapp:dynamodbTableName') ?? 'MyChatApp_Chat',
    sesImportExisting: app.node.tryGetContext('mychatapp:sesImportExisting') === true,
  };
}

const app = new cdk.App();
const config = loadConfig(app);

new MyChatAppStack(app, `MyChatApp-${config.environment}`, config, {
  env: {
    account: process.env.CDK_DEFAULT_ACCOUNT,
    region: process.env.CDK_DEFAULT_REGION ?? 'ap-southeast-1',
  },
  description: 'MyChatApp: S3 attachments + DynamoDB chat + SES domain mail',
});
