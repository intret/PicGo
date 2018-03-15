#!/usr/bin/env bash

# http://argbash.readthedocs.io/en/stable/usage.html#template-generator
argbash-init --opt flavor --opt build-type > myci.m4

argbash -o myci.sh myci.m4

