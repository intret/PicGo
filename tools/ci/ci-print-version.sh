#!/usr/bin/env bash


#echo ${GIT_BRANCH}
cd ${0%/*}
# 脚本执行环境
cd ..
CURR_PATH=$(pwd)
SHELL_PARAMS=$*
#echo "CURR_PATH=$CURR_PATH"
if [[ ! -d ".ci" ]]; then
	#echo "[ERROR] There is not a '.ci' directory in dir : $CURR_PATH"
	exit 1
fi

# ------------------------------------------------------------------------------------------
# initialization
# ------------------------------------------------------------------------------------------

if [[ ! -d ".ci" ]]; then
	#echo "[ERROR] There is not a '.ci' directory in dir : $CURR_PATH"
	exit 1
fi

init_tool_dir()
{
	CI_TOOLS_DIR=./.ci/tools
	mkdir -p $CI_TOOLS_DIR

	# Get full path
	CI_TOOLS="$(cd "$(dirname "$CI_TOOLS_DIR")"; pwd)/$(basename "$CI_TOOLS_DIR")"
}
init_tool_dir

init_tool_path()
{
	export PATH=$CI_TOOLS:$PATH
	if [[ "$(uname)" = "Darwin" ]]; then
		export PATH=$CI_TOOLS/osx-amd64:$PATH
	elif [[ "$(uname)" = "Linux" ]]; then
		export PATH=$CI_TOOLS/linux-amd64:$PATH
	else
		echo "* 未知操作系统，不知道如何初始化工具"
		return 1
	fi
	#echo "PATH=$PATH"
}
init_tool_path

# $1 flavorName : string likes "googleplay", "ali", "yyb"
# $2 buildType : string, one of these : "debug", "release"
get_apk_name()
{
	jq ".[].path" $1
}

# ------------------------------------------------------------------------------------------
# main code
# ------------------------------------------------------------------------------------------

# $1 flavorName : like "googleplay","ali","yyb"
# $2 buildType : like "debug", "release"
# $3 target dir
print_apk_to_dir()
{
	outputJsonFile="app/build/outputs/apk/$1/$2/output.json"

	cpApkName=`get_apk_name $outputJsonFile`
	APP_FILE_NAME=$cpApkName
	# remove character " from begin and end of string.
	cpApkName="${cpApkName%\"}"
	cpApkName="${cpApkName#\"}"

	#echo "$cpApkName"

	
    APP_FILE_NAME=${APP_FILE_NAME/(/_}
    APP_FILE_NAME=${APP_FILE_NAME/)_/_}
    APP_FILE_NAME=${APP_FILE_NAME%\"}
    APP_FILE_NAME=${APP_FILE_NAME#\"}
    APP_FILE_NAME=${APP_FILE_NAME%.*}

    #git tag "$APP_FILE_NAME"
    echo $APP_FILE_NAME

	# cp "apk/$1/$2/$cpApkName" "$3"
	# if [[ $? -gt 0 ]]; then
	# 	return 1
	# fi
}

print_apk_to_dir production debug