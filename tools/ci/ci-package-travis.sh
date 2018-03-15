#!/usr/bin/env bash
# build apk and then copy .apk to specified directory for travis's uploading to Github Releases.
# --------------------------------------------------------------------------------------------------


# 1 Build 'beta' flavor of project
# 2 copy output .apk to 'app/build/outputs/ci-outputs/apk'


# the .apk files will be copied to app/build/outputs/ci-travis
chmod +x ./ci.sh
./ci.sh --build --flavor "production" --build-type "debug" --collect-apk --collect-apk-dir "ci-travis-apk"
#./ci.sh --collect-apk




#<The general help message of ci.sh>
#Usage: ./ci.sh [--(no-)build] [--flavor <arg>] [--build-type <arg>] [--(no-)collect-apk] [--collect-apk-dir <arg>] [-d|--(no-)debug] [-h|--help]
#	--build,--no-build: A bool value(on/off) indicated that whether the script will executes the gradle-building process (on by default)
#	--flavor: The flavor-name of gradle project you wanted to build, for example 'production' (default: 'all-flavors')
#	--build-type: The build-type of gradle project you wanted to build, for example 'debug','release' (default: 'all-types')
#	--collect-apk,--no-collect-apk: A bool value(on/off) indicated that whether the script will executes the collect-apk process (on by default)
#	--collect-apk-dir: The directory of all output .apk files (default: 'ci-outputs/apk')
#	-d,--debug,--no-debug: A bool value(on/off) indicated that whether the script will print the debug information (off by default)
#	-h,--help: Prints help