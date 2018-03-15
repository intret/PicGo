#!/usr/bin/env bash
# (You can call this shell script without comment line parameters)
# update ci.sh file by using argbash(https://argbash.io)
#
# http://argbash.readthedocs.io/en/stable/usage.html#template-generator


#argbash-init --opt flavor --opt build-type > myci.m4

# update ci.sh
argbash -o myci.sh myci.sh

# run output script
ci.sh --no-build --flavor production --build-type release --collect-apk