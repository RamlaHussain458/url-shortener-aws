data "aws_caller_identity" "current" {}

# The web server gets an IAM role instead of access keys. The role allows only what it needs:
# Session Manager (shell access without SSH), pulling the two images, and reading this project's parameters.
data "aws_iam_policy_document" "ec2_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "web" {
  name               = "${var.project_name}-web-role"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume_role.json
}

resource "aws_iam_role_policy_attachment" "web_ssm" {
  role       = aws_iam_role.web.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_iam_policy_document" "web_permissions" {
  statement {
    sid       = "EcrLogin"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid       = "EcrPull"
    actions   = ["ecr:BatchCheckLayerAvailability", "ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer"]
    resources = [for repo in aws_ecr_repository.app : repo.arn]
  }

  statement {
    sid       = "ReadProjectParameters"
    actions   = ["ssm:GetParameter", "ssm:GetParameters", "ssm:GetParametersByPath"]
    resources = ["arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter/${var.project_name}/*"]
  }
}

resource "aws_iam_role_policy" "web" {
  name   = "${var.project_name}-web-access"
  role   = aws_iam_role.web.id
  policy = data.aws_iam_policy_document.web_permissions.json
}

resource "aws_iam_instance_profile" "web" {
  name = "${var.project_name}-web-profile"
  role = aws_iam_role.web.name
}
