terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # No remote backend configured — this is a personal portfolio/demo project (see
  # docs/architecture-decisions.md ADR-004). State is local on purpose; a real team project would
  # use an S3 + DynamoDB-locking backend here instead.
}

provider "aws" {
  region = var.aws_region
}
