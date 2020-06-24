#!/usr/bin/env bash

# Prepare ssh keys
eval "$(ssh-agent -s)"
chmod 600 ~/.ssh/dke-travis-git
ssh-add ~/.ssh/dke-travis-git

# deploy to DKE server
git fetch --unshallow origin
git checkout -b deploy-branch
git commit -am "Add updated version from Travis"
git remote add deploy ssh://git@$DEPLOY_IP:$DEPLOY_PORT$DEPLOY_DIR
git push deploy deploy-branch

echo "Successfully deployed to DKE server"