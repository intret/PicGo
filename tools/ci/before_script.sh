#!/usr/bin/env bash
cd ${0%/*}

# 脚本执行环境
cd ..
CURR_PATH=$(pwd)
SHELL_PARAMS=$*

# ------------------------------------------------------------------------------
# Init command-line tools for CI :
# - ossutil, a tool of aliyun os
# - jq, a tool for parsing .json file, extract infomation from Gradle outputs)
# ------------------------------------------------------------------------------

init_tool_dir()
{
	CI_TOOLS_DIR=./.ci/tools
	mkdir -p $CI_TOOLS_DIR

	# Get full path
	CI_TOOLS="$(cd "$(dirname "$CI_TOOLS_DIR")"; pwd)/$(basename "$CI_TOOLS_DIR")"
}


# [jq](https://stedolan.github.io/jq/)
# https://stedolan.github.io/jq/download/
init_jq()
{
	#! /bin/bash
	if [[ "$(uname)" = "Darwin" ]]; then
		if [[ ! -f "$CI_TOOLS/osx-amd64/jq" ]]; then
			echo "Download jq for macOS"
			wget "-O$CI_TOOLS/osx-amd64/jq" https://github.com/stedolan/jq/releases/download/jq-1.5/jq-osx-amd64
		else
			echo "Use '$CI_TOOLS/osx-amd64/jq'"
		fi

	elif [[ "$(uname)" = "Linux" ]]; then

		if type jq >/dev/null 2>&1; then
		  echo 'exists jq command'
		else
		  echo 'no exists jq command'

		  echo "Install jq for Linux 64"
		  apt-get --quiet install --yes jq --no-install-recommends
		fi
	else
		echo "* 未知操作系统，不知道如何下载 jq"
		return 1
	fi
}

init_ossutil()
{
	OSS_UTIL=$CI_TOOLS/ossutil

	if [[ ! -x "$CI_TOOLS/ossutil" ]]; then

		echo "Download Aliyun ossutil64 ..."

		if [[ "$(uname)" = "Darwin" ]]; then
			echo "Download ossutil64 for macOS"
			curl http://docs-aliyun.cn-hangzhou.oss.aliyun-inc.com/assets/attach/50452/cn_zh/1510149324862/ossutilmac64 > $CI_TOOLS/ossutil
		elif [[ "$(uname)" = "Linux" ]]; then
			echo "Download ossutil64 for Linux 64"
			curl http://docs-aliyun.cn-hangzhou.oss.aliyun-inc.com/assets/attach/50452/cn_zh/1506525299111/ossutil64 > $CI_TOOLS/ossutil
		else
			echo "* 未知操作系统，不知道如何下载 ossutil"
			return 1
		fi

		if [[ ! -f $CI_TOOLS/ossutil ]]; then
			echo "* 下载 ossutil 失败"
			return 1
		fi

	    # cp $CI_TOOLS/ossutil /usr/bin/ossutil
	else
		echo "Use '$CI_TOOLS/ossutil'"
	fi

	chmod +x $CI_TOOLS/ossutil
}

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
}

init_gradle()
{
	chmod +x ./gradlew
}

main()
{
	init_tool_dir
	init_jq
	init_ossutil
	init_tool_path

	echo "Config Aliyun ossutil ..."
	ossutil config -e oss.aliyuncs.com -i LTAIPX1yJlJWiWhU -k o5wkrc8G1RbThla3cWq5RXu0iquCAR
}
main
