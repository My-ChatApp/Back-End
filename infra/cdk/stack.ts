import * as cdk from 'aws-cdk-lib';
import * as cloudfront from 'aws-cdk-lib/aws-cloudfront';
import * as origins from 'aws-cdk-lib/aws-cloudfront-origins';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as ses from 'aws-cdk-lib/aws-ses';
import { Construct } from 'constructs';

export interface AppConfig {
  environment: string;
  domainName: string;
  hostedZoneId: string;
  mailFromSubdomain: string;
  sesFromEmail: string;
  bucketNamePrefix: string;
  /** Single-table chat messages — trùng tên với DynamoDB Local (designDB.md) */
  dynamodbTableName: string;
  /** true nếu domain đã verify trên SES — tránh lỗi "already exists" */
  sesImportExisting: boolean;
}

export class MyChatAppStack extends cdk.Stack {
  constructor(scope: Construct, id: string, config: AppConfig, props?: cdk.StackProps) {
    super(scope, id, props);

    const {
      environment,
      domainName,
      hostedZoneId,
      mailFromSubdomain,
      sesFromEmail,
      bucketNamePrefix,
      dynamodbTableName,
      sesImportExisting,
    } = config;
    const mailFromDomain = `${mailFromSubdomain}.${domainName}`;
    const isProd = environment === 'prod';

    const chatTable = new dynamodb.Table(this, 'ChatTable', {
      tableName: dynamodbTableName,
      partitionKey: { name: 'PK', type: dynamodb.AttributeType.STRING },
      sortKey: { name: 'SK', type: dynamodb.AttributeType.STRING },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      pointInTimeRecoverySpecification: { pointInTimeRecoveryEnabled: isProd },
      removalPolicy: isProd ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY,
    });

    const bucket = new s3.Bucket(this, 'AttachmentsBucket', {
      bucketName: `${bucketNamePrefix}-${environment}-${this.account}-${this.region}`.toLowerCase(),
      encryption: s3.BucketEncryption.S3_MANAGED,
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      enforceSSL: true,
      removalPolicy: isProd ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY,
      autoDeleteObjects: !isProd,
      cors: [
        {
          allowedMethods: [s3.HttpMethods.GET, s3.HttpMethods.PUT, s3.HttpMethods.POST, s3.HttpMethods.HEAD],
          allowedOrigins: ['*'],
          allowedHeaders: ['*'],
          exposedHeaders: ['ETag'],
          maxAge: 3000,
        },
      ],
      lifecycleRules: [
        { id: 'abort-incomplete-multipart', abortIncompleteMultipartUploadAfter: cdk.Duration.days(7) },
      ],
    });

    const distribution = new cloudfront.Distribution(this, 'AttachmentsCdn', {
      comment: `MyChatApp ${environment} attachments CDN`,
      defaultBehavior: {
        origin: origins.S3BucketOrigin.withOriginAccessControl(bucket),
        viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
        allowedMethods: cloudfront.AllowedMethods.ALLOW_GET_HEAD,
        cachedMethods: cloudfront.CachedMethods.CACHE_GET_HEAD,
      },
    });

    const mediaPublicBaseUrl = `https://${distribution.distributionDomainName}`;

    if (sesImportExisting) {
      ses.EmailIdentity.fromEmailIdentityName(this, 'DomainIdentity', domainName);
      cdk.Annotations.of(this).addInfo('SES domain already exists — using import reference.');
    } else {
      let identity: ses.Identity;
      if (hostedZoneId) {
        const zone = route53.HostedZone.fromHostedZoneAttributes(this, 'HostedZone', {
          hostedZoneId,
          zoneName: domainName,
        });
        identity = ses.Identity.publicHostedZone(zone);
      } else {
        cdk.Annotations.of(this).addWarning('Set mychatapp:hostedZoneId or add SES DNS records manually.');
        identity = ses.Identity.domain(domainName);
      }

      new ses.EmailIdentity(this, 'DomainIdentity', {
        identity,
        mailFromDomain,
        feedbackForwarding: true,
      });
    }

    const s3Policy = new iam.ManagedPolicy(this, 'S3AttachmentsPolicy', {
      managedPolicyName: `mychatapp-${environment}-s3-attachments`,
      statements: [
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          actions: ['s3:GetObject', 's3:PutObject', 's3:DeleteObject', 's3:AbortMultipartUpload'],
          resources: [bucket.arnForObjects('*')],
        }),
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          actions: ['s3:ListBucket', 's3:GetBucketLocation'],
          resources: [bucket.bucketArn],
        }),
      ],
    });

    const sesSendPolicy = new iam.ManagedPolicy(this, 'SesSendPolicy', {
      managedPolicyName: `mychatapp-${environment}-ses-send`,
      statements: [
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          actions: ['ses:SendEmail', 'ses:SendRawEmail'],
          resources: ['*'],
          conditions: { StringLike: { 'ses:FromAddress': [`*@${domainName}`] } },
        }),
      ],
    });

    const dynamoDbPolicy = new iam.ManagedPolicy(this, 'DynamoDbChatPolicy', {
      managedPolicyName: `mychatapp-${environment}-dynamodb-chat`,
      statements: [
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          actions: [
            'dynamodb:GetItem',
            'dynamodb:PutItem',
            'dynamodb:UpdateItem',
            'dynamodb:DeleteItem',
            'dynamodb:Query',
            'dynamodb:BatchGetItem',
            'dynamodb:TransactWriteItems',
            'dynamodb:TransactGetItems',
          ],
          resources: [chatTable.tableArn, `${chatTable.tableArn}/index/*`],
        }),
      ],
    });

    cdk.Tags.of(this).add('app', 'mychatapp');
    cdk.Tags.of(this).add('environment', environment);

    const exportPrefix = `MyChatApp-${environment}`;
    new cdk.CfnOutput(this, 'AttachmentsBucketName', {
      value: bucket.bucketName,
      exportName: `${exportPrefix}-AttachmentsBucketName`,
    });
    new cdk.CfnOutput(this, 'AttachmentsBucketArn', {
      value: bucket.bucketArn,
      exportName: `${exportPrefix}-AttachmentsBucketArn`,
    });
    new cdk.CfnOutput(this, 'S3AttachmentsPolicyArn', {
      value: s3Policy.managedPolicyArn,
      exportName: `${exportPrefix}-S3AttachmentsPolicyArn`,
    });
    new cdk.CfnOutput(this, 'MediaPublicBaseUrl', {
      value: mediaPublicBaseUrl,
      exportName: `${exportPrefix}-MediaPublicBaseUrl`,
    });
    new cdk.CfnOutput(this, 'AttachmentsCdnDistributionId', {
      value: distribution.distributionId,
      exportName: `${exportPrefix}-AttachmentsCdnDistributionId`,
    });
    new cdk.CfnOutput(this, 'SesDomainName', { value: domainName, exportName: `${exportPrefix}-SesDomain` });
    new cdk.CfnOutput(this, 'SesMailFromDomain', {
      value: mailFromDomain,
      exportName: `${exportPrefix}-SesMailFromDomain`,
    });
    new cdk.CfnOutput(this, 'SesFromEmail', { value: sesFromEmail, exportName: `${exportPrefix}-SesFromEmail` });
    new cdk.CfnOutput(this, 'SesSmtpEndpoint', {
      value: `email-smtp.${this.region}.amazonaws.com`,
    });
    new cdk.CfnOutput(this, 'SesSendPolicyArn', {
      value: sesSendPolicy.managedPolicyArn,
      exportName: `${exportPrefix}-SesSendPolicyArn`,
    });
    new cdk.CfnOutput(this, 'DynamoDbTableName', {
      value: chatTable.tableName,
      exportName: `${exportPrefix}-DynamoDbTableName`,
    });
    new cdk.CfnOutput(this, 'DynamoDbTableArn', {
      value: chatTable.tableArn,
      exportName: `${exportPrefix}-DynamoDbTableArn`,
    });
    new cdk.CfnOutput(this, 'DynamoDbChatPolicyArn', {
      value: dynamoDbPolicy.managedPolicyArn,
      exportName: `${exportPrefix}-DynamoDbChatPolicyArn`,
    });
  }
}
