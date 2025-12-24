# ⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️
#
# Terraform Configuration with INTENTIONAL vulnerabilities:
# - Public S3 buckets
# - Open security groups
# - Hardcoded credentials
# - Unencrypted resources
# - Overly permissive IAM

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

# ============================================================
# VULNERABILITY: Hardcoded AWS Credentials
# ============================================================
provider "aws" {
  region     = "us-east-1"
  # VULNERABILITY: Hardcoded credentials in code!
  access_key = "AKIAIOSFODNN7EXAMPLE"
  secret_key = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
}

# ============================================================
# VULNERABILITY: Public S3 Bucket
# ============================================================
resource "aws_s3_bucket" "data_bucket" {
  bucket = "death-cocktail-data-bucket"

  tags = {
    Name        = "Death Cocktail Data"
    Environment = "production"
    # VULNERABILITY: Sensitive tag exposed
    Contains    = "customer-pii-data"
  }
}

# VULNERABILITY: Public access block disabled
resource "aws_s3_bucket_public_access_block" "data_bucket_public" {
  bucket = aws_s3_bucket.data_bucket.id

  block_public_acls       = false  # VULNERABILITY: Public ACLs allowed
  block_public_policy     = false  # VULNERABILITY: Public policies allowed
  ignore_public_acls      = false
  restrict_public_buckets = false  # VULNERABILITY: Public access allowed
}

# VULNERABILITY: Bucket policy allows public read
resource "aws_s3_bucket_policy" "data_bucket_policy" {
  bucket = aws_s3_bucket.data_bucket.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "PublicReadGetObject"
        Effect    = "Allow"
        Principal = "*"  # VULNERABILITY: Anyone can access!
        Action    = [
          "s3:GetObject",
          "s3:PutObject",      # VULNERABILITY: Anyone can write!
          "s3:DeleteObject",   # VULNERABILITY: Anyone can delete!
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.data_bucket.arn,
          "${aws_s3_bucket.data_bucket.arn}/*"
        ]
      }
    ]
  })
}

# VULNERABILITY: Server-side encryption disabled
resource "aws_s3_bucket_server_side_encryption_configuration" "data_bucket_encryption" {
  bucket = aws_s3_bucket.data_bucket.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"  # Actually enabled but...
    }
    bucket_key_enabled = false   # VULNERABILITY: Key not enabled
  }
}

# VULNERABILITY: Versioning disabled - no recovery
resource "aws_s3_bucket_versioning" "data_bucket_versioning" {
  bucket = aws_s3_bucket.data_bucket.id
  versioning_configuration {
    status = "Disabled"  # VULNERABILITY: No versioning
  }
}

# VULNERABILITY: Logging disabled
# Missing: aws_s3_bucket_logging resource

# ============================================================
# VULNERABILITY: Wide Open Security Group
# ============================================================
resource "aws_security_group" "allow_all" {
  name        = "allow_all_traffic"
  description = "Security group that allows all traffic"
  vpc_id      = aws_vpc.main.id

  # VULNERABILITY: All inbound traffic allowed from anywhere
  ingress {
    description = "All traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"  # All protocols
    cidr_blocks = ["0.0.0.0/0"]  # VULNERABILITY: From anywhere!
  }

  # VULNERABILITY: All outbound traffic allowed
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "allow-all-sg"
  }
}

# VULNERABILITY: Specific dangerous ports exposed
resource "aws_security_group" "exposed_services" {
  name        = "exposed_services"
  description = "Exposes dangerous services"
  vpc_id      = aws_vpc.main.id

  # VULNERABILITY: SSH from anywhere
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # VULNERABILITY: RDP from anywhere
  ingress {
    from_port   = 3389
    to_port     = 3389
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # VULNERABILITY: MySQL from anywhere
  ingress {
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # VULNERABILITY: PostgreSQL from anywhere
  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # VULNERABILITY: MongoDB from anywhere
  ingress {
    from_port   = 27017
    to_port     = 27017
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # VULNERABILITY: Redis from anywhere
  ingress {
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # VULNERABILITY: Elasticsearch from anywhere
  ingress {
    from_port   = 9200
    to_port     = 9300
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# ============================================================
# VULNERABILITY: Overly Permissive IAM Role
# ============================================================
resource "aws_iam_role" "admin_role" {
  name = "death-cocktail-admin-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
          # VULNERABILITY: Any AWS account can assume!
          AWS = "*"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

# VULNERABILITY: Full admin access
resource "aws_iam_role_policy" "admin_policy" {
  name = "admin-policy"
  role = aws_iam_role.admin_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "*"          # VULNERABILITY: Full access to everything!
        Resource = "*"
      }
    ]
  })
}

# VULNERABILITY: IAM user with admin access and access keys
resource "aws_iam_user" "admin_user" {
  name = "death-cocktail-admin"
}

resource "aws_iam_user_policy" "admin_user_policy" {
  name = "admin-access"
  user = aws_iam_user.admin_user.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = "*"
        Resource = "*"
      }
    ]
  })
}

# VULNERABILITY: Access keys created and exposed
resource "aws_iam_access_key" "admin_key" {
  user = aws_iam_user.admin_user.name
}

# VULNERABILITY: Outputting secret access key!
output "admin_access_key_id" {
  value = aws_iam_access_key.admin_key.id
}

output "admin_secret_access_key" {
  value     = aws_iam_access_key.admin_key.secret
  sensitive = false  # VULNERABILITY: Not marked sensitive!
}

# ============================================================
# VULNERABILITY: Unencrypted RDS Instance
# ============================================================
resource "aws_db_instance" "database" {
  identifier           = "death-cocktail-db"
  allocated_storage    = 20
  engine              = "mysql"
  engine_version      = "5.7"
  instance_class      = "db.t2.micro"
  db_name             = "production"
  username            = "admin"
  password            = "password123"  # VULNERABILITY: Hardcoded weak password!
  skip_final_snapshot = true

  # VULNERABILITY: Publicly accessible
  publicly_accessible = true

  # VULNERABILITY: Not encrypted
  storage_encrypted = false

  # VULNERABILITY: No IAM authentication
  iam_database_authentication_enabled = false

  # VULNERABILITY: Weak password policy
  # Missing: parameter_group with password policies

  # VULNERABILITY: No backup retention
  backup_retention_period = 0

  # VULNERABILITY: Auto minor upgrade disabled
  auto_minor_version_upgrade = false

  vpc_security_group_ids = [aws_security_group.allow_all.id]
}

# ============================================================
# VULNERABILITY: EC2 Instance with IMDSv1
# ============================================================
resource "aws_instance" "backend" {
  ami           = "ami-12345678"
  instance_type = "t2.micro"

  # VULNERABILITY: IMDSv1 enabled (SSRF risk)
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "optional"  # VULNERABILITY: IMDSv1 allowed
    http_put_response_hop_limit = 2
  }

  # VULNERABILITY: User data with secrets
  user_data = <<-EOF
              #!/bin/bash
              export DB_PASSWORD="password123"
              export API_KEY="sk_live_xxxxxxxxxxxxx"
              export AWS_SECRET_KEY="wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
              EOF

  # VULNERABILITY: Root volume not encrypted
  root_block_device {
    encrypted = false
  }

  vpc_security_group_ids = [aws_security_group.allow_all.id]
  iam_instance_profile   = aws_iam_instance_profile.admin_profile.name
}

resource "aws_iam_instance_profile" "admin_profile" {
  name = "admin-profile"
  role = aws_iam_role.admin_role.name
}

# ============================================================
# VULNERABILITY: VPC with default settings
# ============================================================
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  # VULNERABILITY: No flow logs
  # Missing: aws_flow_log resource
}

# VULNERABILITY: Public subnet
resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  map_public_ip_on_launch = true  # VULNERABILITY: Auto-assigns public IPs
}

# ============================================================
# VULNERABILITY: Lambda with overpermissive role
# ============================================================
resource "aws_lambda_function" "vulnerable_lambda" {
  filename         = "lambda.zip"
  function_name    = "death-cocktail-lambda"
  role            = aws_iam_role.admin_role.arn
  handler         = "index.handler"
  runtime         = "nodejs14.x"

  environment {
    variables = {
      # VULNERABILITY: Secrets in environment variables
      DB_PASSWORD    = "password123"
      API_SECRET     = "super_secret_key"
      ENCRYPTION_KEY = "0123456789abcdef"
    }
  }
}

# ============================================================
# VULNERABILITY: CloudTrail disabled
# ============================================================
# Missing: aws_cloudtrail resource - no audit logging!

# ============================================================
# VULNERABILITY: KMS key with permissive policy
# ============================================================
resource "aws_kms_key" "main" {
  description = "Main encryption key"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          AWS = "*"  # VULNERABILITY: Anyone can use this key!
        }
        Action   = "kms:*"
        Resource = "*"
      }
    ]
  })
}
