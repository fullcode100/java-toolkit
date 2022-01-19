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

if [ ! -r target/google-java-format-1.11.0-all-deps.jar ]; then
	mkdir -p target
	mvn dependency:copy -Dartifact=com.google.googlejavaformat:google-java-format:1.11.0:jar:all-deps -DoutputDirectory=target
fi

find . -type f -name "*.java" > target/google-format-files
java -jar target/google-java-format-1.11.0-all-deps.jar --set-exit-if-changed --dry-run @target/google-format-files 