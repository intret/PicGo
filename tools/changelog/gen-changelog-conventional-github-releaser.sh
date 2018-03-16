#!/usr/bin/env bash
# --------------
# Tool conventional-github-releaser
#
# Official url:
# [conventional-changelog/conventional-github-releaser: Make a new GitHub release from git metadata]
# (https://github.com/conventional-changelog/conventional-github-releaser)

# [conventional-changelog/packages/conventional-changelog-cli at master · conventional-changelog/conventional-changelog]
# (https://github.com/conventional-changelog/conventional-changelog/tree/master/packages/conventional-changelog-cli#conventional-changelog-cli)


# [Prerequisites]
#
# - npm install -g conventional-changelog-cli
# - has a tag which name followed convention of [Semantic Versioning 2.0.0 | Semantic Versioning](https://semver.org/)

# [Show help]
# conventional-changelog --help


cd ../..
conventional-changelog -p angular -i CHANGELOG.md -s -r 0