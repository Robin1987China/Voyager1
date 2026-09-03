#!/bin/bash

# Mistakenly deleted !!!!!!!!!!!
# Init script templates for local build, local publish, script template, ssh publish, ssh command template and other related functions

user="$(id -un 2>/dev/null || true)"

if [ "$user" == 'root' ]; then
	rootProfiles=("/etc/profile" "/etc/bashrc")
	for element in "${rootProfiles[@]}"; do
		if [ -f "$element" ]; then
			# shellcheck disable=SC1090
			source "$element"
		fi
	done
fi

userProfiles=("$HOME/.bash_profile" "$HOME/.bashrc" "$HOME/.bash_login")
for element in "${userProfiles[@]}"; do
	if [ -f "$element" ]; then
		# shellcheck disable=SC1090
		source "$element"
	fi
done

# Do not delete the following content (leave at least one blank line)! ! ! ! !
