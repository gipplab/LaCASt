#!/usr/bin/env bash

# avoid to run the setup and git tag twice
if ! [ "$TRAVIS_TAG" ]; then
  # setup version number and travis tag
  VERSION="$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)"
  export VERSION
  export TRAVIS_TAG=LaCASt-$VERSION-r$TRAVIS_BUILD_NUMBER

  # setup git
  git config --global user.email "builds@travis-ci.com"
  git config --global user.name "Travis CI"

  # remove the logs from the tests and builds
  rm -rf libs/logs

  # zip LaCASt
  zip -r -9 ${TRAVIS_TAG}.zip bin/* config/* libs/* scripts/* README.md CONTRIBUTING.md

  # and finally tag it
  echo "Git tag with tag $TRAVIS_TAG"
  git tag "$TRAVIS_TAG"
fi