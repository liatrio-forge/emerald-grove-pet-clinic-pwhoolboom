data "terraform_remote_state" "platform" {
  backend = "s3"
  config = {
    bucket = var.platform_state_bucket
    key    = var.platform_state_key
    region = var.platform_state_region
  }
}

locals {
  data_subnet_ids = data.terraform_remote_state.platform.outputs.data_subnet_ids
  vpc_id          = data.terraform_remote_state.platform.outputs.vpc_id
}
