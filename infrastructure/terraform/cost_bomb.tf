# ⚠️ VULNERABLE BY DESIGN - DO NOT USE IN PRODUCTION ⚠️
#
# Terraform Cost Bomb - No Budget Limits
# This configuration can generate MILLIONS in cloud bills!
#
# Real-world incidents:
# - Startup $72,000 overnight bill (Lambda + DynamoDB loop)
# - $50,000 NAT Gateway bill (forgot to delete)
# - $30,000 EBS snapshots (no lifecycle policy)

# ============================================================
# VULNERABILITY: No Budget Alerts or Limits
# ============================================================
# Missing:
# - AWS Budgets
# - Cost anomaly detection
# - Billing alarms
# - Service quotas

# Should have:
# resource "aws_budgets_budget" "monthly" {
#   budget_type  = "COST"
#   limit_amount = "1000"
#   limit_unit   = "USD"
#   ...
# }

# ============================================================
# COST BOMB #1: Auto-scaling with NO LIMITS
# ============================================================

resource "aws_autoscaling_group" "unlimited_scaling" {
  name = "death-cocktail-asg"

  # VULNERABILITY: No maximum limit!
  min_size         = 1
  max_size         = 10000  # Can scale to 10,000 instances!
  desired_capacity = 1

  launch_template {
    id      = aws_launch_template.expensive.id
    version = "$Latest"
  }

  # VULNERABILITY: Aggressive scaling policy
  # Small CPU spike = massive scale-out
}

resource "aws_autoscaling_policy" "scale_up_aggressive" {
  name                   = "aggressive-scale-up"
  autoscaling_group_name = aws_autoscaling_group.unlimited_scaling.name
  policy_type            = "SimpleScaling"
  adjustment_type        = "ChangeInCapacity"

  # VULNERABILITY: Scale up by 100 instances at a time!
  scaling_adjustment = 100
  cooldown           = 60  # Only 1 minute cooldown
}

resource "aws_launch_template" "expensive" {
  name_prefix   = "expensive-"
  image_id      = "ami-12345678"

  # VULNERABILITY: Most expensive instance type
  instance_type = "x1e.32xlarge"  # $26.688/hour per instance!
  # 10,000 instances * $26.688 = $266,880/hour = $6.4M/day!

  # VULNERABILITY: Large root volume
  block_device_mappings {
    device_name = "/dev/sda1"
    ebs {
      volume_size           = 16000  # 16TB per instance!
      volume_type           = "io2"  # Most expensive EBS type
      iops                  = 64000  # Maximum IOPS = maximum cost
      delete_on_termination = false  # Volumes persist after termination!
    }
  }
}

# ============================================================
# COST BOMB #2: Lambda Infinite Loop
# ============================================================

resource "aws_lambda_function" "infinite_loop" {
  filename         = "lambda.zip"
  function_name    = "cost-bomb-lambda"
  role            = aws_iam_role.lambda_role.arn
  handler         = "index.handler"
  runtime         = "nodejs18.x"

  # VULNERABILITY: Maximum memory and timeout
  memory_size = 10240  # Maximum memory = maximum cost
  timeout     = 900    # 15 minutes = maximum timeout

  # VULNERABILITY: No concurrency limit!
  # reserved_concurrent_executions = -1  # Unlimited!

  environment {
    variables = {
      # This Lambda calls itself - infinite loop!
      SELF_INVOKE = "true"
      INVOKE_COUNT = "1000"  # Invokes itself 1000 times per execution
    }
  }
}

# VULNERABILITY: Lambda can invoke itself without limit
resource "aws_lambda_permission" "allow_self_invoke" {
  statement_id  = "AllowSelfInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.infinite_loop.function_name
  principal     = "lambda.amazonaws.com"
}

# ============================================================
# COST BOMB #3: NAT Gateway for Everything
# ============================================================

resource "aws_nat_gateway" "expensive_nat" {
  count = 100  # VULNERABILITY: 100 NAT Gateways!
  # Each NAT Gateway = ~$32/month + $0.045/GB data processing

  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public.id
}

resource "aws_eip" "nat" {
  count = 100  # 100 Elastic IPs
  # Each unused EIP = $3.60/month
}

resource "aws_route" "nat_route" {
  route_table_id         = aws_route_table.private.id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.expensive_nat[0].id

  # VULNERABILITY: ALL traffic goes through NAT
  # Downloading 1TB through NAT = $45 in data processing fees
}

# ============================================================
# COST BOMB #4: DynamoDB On-Demand without Limits
# ============================================================

resource "aws_dynamodb_table" "unlimited_dynamo" {
  name         = "death-cocktail-table"
  billing_mode = "PAY_PER_REQUEST"  # On-demand = unlimited cost!

  # VULNERABILITY: No read/write capacity limits
  # A script could perform millions of operations
  # $1.25 per million writes, $0.25 per million reads

  hash_key  = "pk"
  range_key = "sk"

  attribute {
    name = "pk"
    type = "S"
  }

  attribute {
    name = "sk"
    type = "S"
  }

  # VULNERABILITY: Global tables in ALL regions
  replica {
    region_name = "us-west-2"
  }
  replica {
    region_name = "eu-west-1"
  }
  replica {
    region_name = "ap-northeast-1"
  }
  replica {
    region_name = "ap-southeast-1"
  }
  # Multiplies all costs by 5!
}

# ============================================================
# COST BOMB #5: S3 without Lifecycle Policies
# ============================================================

resource "aws_s3_bucket" "unlimited_storage" {
  bucket = "death-cocktail-unlimited-storage"

  # VULNERABILITY: No lifecycle policy
  # Data never expires or transitions to cheaper storage
  # 1PB of data = $23,000/month
}

# VULNERABILITY: Versioning without expiration = infinite growth
resource "aws_s3_bucket_versioning" "enabled" {
  bucket = aws_s3_bucket.unlimited_storage.id
  versioning_configuration {
    status = "Enabled"
  }
  # No lifecycle rule to expire old versions!
  # Every update creates a new version forever
}

# VULNERABILITY: Requester pays disabled = you pay for attackers' downloads
# Missing: requester_pays = true

# ============================================================
# COST BOMB #6: CloudWatch without Limits
# ============================================================

resource "aws_cloudwatch_log_group" "verbose_logging" {
  name              = "/death-cocktail/verbose"
  retention_in_days = 0  # VULNERABILITY: Never expires!

  # Ingesting 1TB of logs/day = $500/day = $15,000/month
  # Storing logs forever = unlimited growth
}

resource "aws_cloudwatch_metric_alarm" "everything" {
  count = 5000  # VULNERABILITY: 5000 alarms!
  # First 10 alarms free, then $0.10/alarm/month
  # 5000 alarms = $500/month just for alarms

  alarm_name          = "alarm-${count.index}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = "60"  # 1-minute granularity = more cost
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "Alarm ${count.index}"
}

# ============================================================
# COST BOMB #7: EBS Snapshots without Cleanup
# ============================================================

resource "aws_ebs_snapshot" "daily_snapshot" {
  volume_id   = aws_ebs_volume.large_volume.id
  description = "Daily snapshot"

  # VULNERABILITY: No lifecycle - snapshots accumulate forever
  # 16TB snapshot = ~$800/month
  # 1 year of daily snapshots = 365 * $800 = $292,000
}

resource "aws_ebs_volume" "large_volume" {
  availability_zone = "us-east-1a"
  size              = 16000  # 16TB
  type              = "io2"
  iops              = 64000  # Maximum IOPS

  # VULNERABILITY: No delete_on_termination
  # Volume persists even after instance terminated
}

# ============================================================
# COST BOMB #8: API Gateway without Throttling
# ============================================================

resource "aws_api_gateway_rest_api" "unlimited_api" {
  name = "death-cocktail-api"

  # VULNERABILITY: No usage plan or throttling
  # Attacker can make unlimited API calls
  # $3.50 per million requests
}

# Missing rate limiting:
# resource "aws_api_gateway_usage_plan" "limited" {
#   throttle_settings {
#     rate_limit  = 100
#     burst_limit = 200
#   }
#   quota_settings {
#     limit  = 10000
#     period = "DAY"
#   }
# }

# ============================================================
# COST BOMB #9: Data Transfer Nightmare
# ============================================================

resource "aws_vpc_peering_connection" "cross_region" {
  # VULNERABILITY: Cross-region VPC peering
  # All traffic between VPCs = $0.01/GB
  # 1PB of traffic = $10,000

  vpc_id        = aws_vpc.main.id
  peer_vpc_id   = "vpc-peer-id"
  peer_region   = "ap-southeast-1"  # Far away = more latency, same cost

  # No traffic monitoring or limits
}

# ============================================================
# COST BOMB #10: RDS with Maximum Everything
# ============================================================

resource "aws_db_instance" "overkill" {
  identifier = "death-cocktail-db"

  # VULNERABILITY: Most expensive RDS instance
  instance_class = "db.r5.24xlarge"  # $9.29/hour = $6,700/month

  # VULNERABILITY: Maximum storage
  allocated_storage     = 65536  # 64TB
  max_allocated_storage = 65536
  storage_type          = "io1"
  iops                  = 80000  # Maximum IOPS

  # VULNERABILITY: Multi-AZ (doubles the cost)
  multi_az = true

  # VULNERABILITY: No auto-pause for dev environments
  # Running 24/7 even when not in use

  # VULNERABILITY: Long backup retention
  backup_retention_period = 35  # Maximum days

  # VULNERABILITY: Performance Insights (additional cost)
  performance_insights_enabled          = true
  performance_insights_retention_period = 731  # Maximum retention

  engine         = "postgres"
  engine_version = "13.4"
  username       = "admin"
  password       = "password123"

  skip_final_snapshot = true
}

# ============================================================
# VULNERABILITY SUMMARY:
# ============================================================
#
# Missing Budget Controls:
# - No AWS Budgets configured
# - No billing alerts
# - No anomaly detection
# - No service quotas
#
# Resource Explosions:
# - Unlimited auto-scaling (to 10,000 instances)
# - Lambda without concurrency limits
# - DynamoDB on-demand without caps
# - No API throttling
#
# Storage Accumulation:
# - S3 without lifecycle policies
# - EBS snapshots without expiration
# - CloudWatch logs without retention
#
# Expensive Configurations:
# - Largest instance types everywhere
# - Maximum IOPS on all storage
# - Multi-region replication
# - Cross-region data transfer
#
# ESTIMATED MAXIMUM MONTHLY BILL: $10,000,000+
# ============================================================

# ============================================================
# What SHOULD exist but doesn't:
# ============================================================

# resource "aws_budgets_budget" "monthly_limit" {
#   name         = "monthly-cost-budget"
#   budget_type  = "COST"
#   limit_amount = "10000"  # $10,000 limit
#   limit_unit   = "USD"
#   time_unit    = "MONTHLY"
#
#   notification {
#     comparison_operator = "GREATER_THAN"
#     threshold           = 80  # Alert at 80%
#     threshold_type      = "PERCENTAGE"
#     notification_type   = "ACTUAL"
#     subscriber_email_addresses = ["ops@company.com"]
#   }
# }

# resource "aws_ce_anomaly_monitor" "cost_anomaly" {
#   name              = "cost-anomaly-monitor"
#   monitor_type      = "DIMENSIONAL"
#   monitor_dimension = "SERVICE"
# }
