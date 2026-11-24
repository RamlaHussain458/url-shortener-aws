# Latest Amazon Linux 2023 image for standard x86 servers. x86 matches the images built on a
# normal PC and on GitHub Actions.
data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }
}

resource "aws_instance" "web" {
  ami                         = data.aws_ami.al2023.id
  instance_type               = var.instance_type
  subnet_id                   = aws_subnet.public[0].id
  vpc_security_group_ids      = [aws_security_group.web.id]
  iam_instance_profile        = aws_iam_instance_profile.web.name
  associate_public_ip_address = true

  user_data                   = file("${path.module}/user_data.sh")
  user_data_replace_on_change = true

  # IMDSv2 only: protects the instance credentials from simple request forgery attacks.
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  root_block_device {
    volume_type           = "gp3"
    volume_size           = 20
    encrypted             = true
    delete_on_termination = true
  }

  tags = {
    Name = "${var.project_name}-web"
  }

  # A newer image being published must not force the server to be rebuilt.
  lifecycle {
    ignore_changes = [ami]
  }
}

# A fixed public address that survives stopping and starting the server.
resource "aws_eip" "web" {
  domain     = "vpc"
  depends_on = [aws_internet_gateway.main]

  tags = {
    Name = "${var.project_name}-web-eip"
  }
}

resource "aws_eip_association" "web" {
  instance_id   = aws_instance.web.id
  allocation_id = aws_eip.web.id
}
