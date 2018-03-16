#!/usr/bin/env bash
# Call node tools 'git-changelog' (https://www.npmjs.com/package/git-changelog)
# to generate the change log of project which used 'ANGULAR JS commit message conventions',
# see
# (https://conventionalcommits.org/)
# or
# ([Karma - Git Commit Msg](http://karma-runner.github.io/2.0/dev/git-commit-msg.html))


# install it
npm install -g git-changelog
git-changelog -t false

# or
# git-changelog -t picgo_0.1.dev_1
# git-changelog -t false





