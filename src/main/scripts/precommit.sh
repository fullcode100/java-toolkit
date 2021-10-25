#!/bin/sh
#
#  Copyright (c) 2020 - 2021 Henix, henix.fr
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

echo pre-commit checks - license headers

echo ...license check
log=$(mktemp)
mvn -o -DskipTests org.codehaus.mojo:build-helper-maven-plugin:timestamp-property@timestamp-property license:check license:check@Modified-by license:check@OriginalCopyright>$log
status=$?

if [ $status -ne 0 ]; then
	echo "*** Failed: some license headers are missing or non-conformant ***"
	grep Missing $log| cut -d"]" -f 2
	if [ $(grep Missing $log | wc -l) -eq 0 ]; then
		echo "*** No missing header found, see log in target/precommit.log"
		mkdir -p target
		mv $log target/precommit.log
	else
		echo "To draft license editing, please run the following:"
		echo mvn org.codehaus.mojo:build-helper-maven-plugin:timestamp-property@timestamp-property license:format license:format@Modified-by license:format@OriginalCopyright
		rm $log
	fi
	exit 1
fi

echo "All checked files have conformant headers"
rm $log

src/main/scripts/google-code-check.sh
status=$?

if [ $status -ne 0 ]; then
    echo Code format check failed. To draft formatting changes, please run the following :
    echo ./src/main/scripts/google-code-format.sh
    exit 1
fi

echo "All checked files comply with the required code format."
