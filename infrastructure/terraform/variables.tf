variable "aws_region" {
  description = "AWS region for all resources."
  type        = string
  default     = "us-east-2"
}

variable "aws_profile" {
  description = "Local AWS CLI profile used for authentication (not used by CI, which assumes a role)."
  type        = string
  default     = "blue-origin-demo"
}

variable "project_name" {
  description = "Short name used as a prefix for resource names."
  type        = string
  default     = "aeroplan"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "demo"
}
