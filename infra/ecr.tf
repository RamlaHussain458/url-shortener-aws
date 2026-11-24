# One private registry per image. Tags are immutable, so every pushed image needs a unique tag
# (for example a version number or a Git commit hash) and a tag can never be overwritten.
locals {
  ecr_repositories = toset(["backend", "web"])
}

resource "aws_ecr_repository" "app" {
  for_each = local.ecr_repositories

  name                 = "${var.project_name}-${each.key}"
  image_tag_mutability = "IMMUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }
}

resource "aws_ecr_lifecycle_policy" "app" {
  for_each = aws_ecr_repository.app

  repository = each.value.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep only the most recent images"

        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = var.ecr_images_to_keep
        }

        action = {
          type = "expire"
        }
      }
    ]
  })
}
