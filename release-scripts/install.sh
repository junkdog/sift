#!/usr/bin/env bash

# note: this scripts is packaged with release artifacts: https://github.com/junkdog/sift/releases

# exit on script failure
set -e

# ANSI color and style codes
BOLD="\033[1m"
CYAN="\033[0;36m"
GREEN="\033[0;32m"
RED="\033[0;31m"
YELLOW="\033[0;33m"
RESET="\033[0m"

# ensure user is not root
if [ "$(id -u)" -eq 0 ]; then
    echo -e "${RED}Error:${RESET} This script should not be run as root."
    exit 1
fi

# require 1 argument
if [[ $# -ne 1 ]]; then
    echo -e "${RED}Usage:${RESET} $0 ${BOLD}<zsh|sh>${RESET}"
    exit 1
fi

# validate input
if [[ $1 != "zsh" ]] && [[ $1 != "sh" ]]; then
    echo -e "${RED}Error:${RESET} Invalid argument. Please pass either ${BOLD}'zsh'${RESET} or ${BOLD}'sh'${RESET}."
    exit 1
fi

echo -e "Installing ${CYAN}sift${RESET} for ${BOLD}$1${RESET} ..."
echo "Creating directories ..."
mkdir -vp ~/.local/share/sift/{bin,templates}

echo "Copying the sift binary and the appropriate shell script ..."
if [ -f ./sift-cli.jar ]; then
    cp -v sift-cli.jar ~/.local/share/sift/bin
fi
if [ -f ./sift ]; then
    cp -v sift ~/.local/share/sift/bin
fi
cp -v "sift.$1" ~/.local/share/sift/bin/



echo "Creating 'sift' symlink in ~/.local/bin ..."
mkdir -vp ~/.local/bin
ln -sf ~/.local/share/sift/bin/"sift.$1" ~/.local/bin/sift

echo -e "${GREEN}sift has been installed for $1.${RESET}"

if ! echo "$PATH" | grep -q $HOME/.local/bin; then
    echo -e "${YELLOW}It seems that ~/.local/bin is not in your PATH. Add the following line to your shell profile:${RESET}"
    echo -e 'export PATH="${BOLD}$HOME/.local/bin:$PATH${RESET}"'
fi
