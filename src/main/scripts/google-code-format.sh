#!/bin/sh -xe
#
#  Copyright (c) 2020 - 2022 Henix, henix.fr
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

GOOGLE_CODE_VERSION=1.11.0

GOOGLE_CODE_FILE=target/google-java-format-${GOOGLE_CODE_VERSION}-all-deps.jar

if [ ! -r ${GOOGLE_CODE_FILE} ]; then
	mkdir -p target
	mvn dependency:copy -Dartifact=com.google.googlejavaformat:google-java-format:${GOOGLE_CODE_VERSION}:jar:all-deps -DoutputDirectory=target
fi

find . -type f -name "*.java" > target/google-format-files
java -jar ${GOOGLE_CODE_FILE} -i @target/google-format-files 
