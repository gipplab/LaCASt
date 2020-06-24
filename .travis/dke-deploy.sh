#!/usr/bin/env bash

# Prepare ssh keys
eval "$(ssh-agent -s)"
chmod 600 ~/.ssh/dke-travis-git
ssh-add ~/.ssh/dke-travis-git

# deploy to DKE server
git remote add deploy ssh://git@$DEPLOY_IP:$DEPLOY_PORT$DEPLOY_DIR
git push deploy master

echo "Successfully deployed to DKE server"