resource "random_id" "frontend_suffix" {
  byte_length = 4
}

# NOTE: the original design (docs/infrastructure-spec.md) called for a private S3 bucket behind
# CloudFront with Origin Access Control. CloudFront creation on this AWS account currently returns
# "Your account must be verified before you can add new CloudFront resources" (common for newly
# active accounts until AWS Support/billing verification completes). Rather than block the whole
# demo on that, this falls back to plain S3 static website hosting (public bucket, HTTP only, no
# CDN/caching) for now. Once the account is verified, swap this file back to a CloudFront + OAC
# origin — the rest of the architecture (API Gateway, ECS, RDS) is unaffected either way.
resource "aws_s3_bucket" "frontend" {
  bucket = "${var.project_name}-frontend-${random_id.frontend_suffix.hex}"

  tags = {
    Project     = var.project_name
    Environment = var.environment
    Component   = "frontend"
  }
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_website_configuration" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  index_document {
    suffix = "index.html"
  }

  # Single-page app: unknown paths should still serve index.html rather than S3's default 404.
  error_document {
    key = "index.html"
  }
}

data "aws_iam_policy_document" "frontend_bucket_policy" {
  statement {
    sid       = "PublicReadGetObject"
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.frontend.arn}/*"]

    principals {
      type        = "*"
      identifiers = ["*"]
    }
  }
}

resource "aws_s3_bucket_policy" "frontend" {
  bucket     = aws_s3_bucket.frontend.id
  policy     = data.aws_iam_policy_document.frontend_bucket_policy.json
  depends_on = [aws_s3_bucket_public_access_block.frontend]
}
