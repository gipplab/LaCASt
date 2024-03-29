#!/usr/bin/env bash

# Prepare ssh keys
eval "$(ssh-agent -s)"
chmod 600 ~/.ssh/dke-travis-git
ssh-add ~/.ssh/dke-travis-git

# deploy to DKE server
# If you need a working copy on the server, you need to add
# git config --local receive.denyCurrentBranch updateInstead
# otherwise you cannot commit to the current branch (option added with Git 2.3)
git fetch --unshallow origin || true
git commit -am "Add updated version from Travis"
git remote add deploy ssh://git@$DEPLOY_IP:$DEPLOY_PORT$DEPLOY_DIR
if git push -f deploy master; then
  echo "Successfully deployed to DKE server"
else
  echo "Unable to deploy to DKE server"
  exit 1
fi
