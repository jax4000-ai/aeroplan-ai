output "frontend_bucket_name" {
  description = "S3 bucket holding the built frontend (frontend/dist is synced here)."
  value       = aws_s3_bucket.frontend.bucket
}

output "frontend_url" {
  description = "Public HTTP URL for the deployed frontend (S3 static website hosting)."
  value       = "http://${aws_s3_bucket_website_configuration.frontend.website_endpoint}"
}
