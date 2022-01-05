/**
 *  Copyright (c) 2020 - 2022 Henix, henix.fr
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/* This may grit Groovy purist's teeth, but way way less than those cabalistic "no such property messages for missing classes do grit mine. */
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission

def gitRepo = new File(".git/hooks")
def preCommitHook = new File("pre-commit", gitRepo)

def lines = []

def ensureExecutableHook = {
    def preCommitHookPath = Paths.get(preCommitHook.toURL().toURI())

    def permissions = Files.getPosixFilePermissions(preCommitHookPath);
    permissions.add(PosixFilePermission.OWNER_EXECUTE)
    permissions.add(PosixFilePermission.GROUP_EXECUTE)
    permissions.add(PosixFilePermission.OTHERS_EXECUTE)

    Files.setPosixFilePermissions(preCommitHookPath,permissions);
}

if (preCommitHook.exists()) {
    preCommitHook.eachLine { line -> lines << line }
}

containsPreCommit = lines.find { it ==~ /^\s*\. src\/main\/scripts\/precommit.sh\s*/ }

if (containsPreCommit) {
    println "precommit hook found, nothing to do"
    
    ensureExecutableHook()
    
    return
}

println "no precommit hook, will add hook definition"

def old = new File("pre-commit.old", gitRepo)
if(preCommitHook.exists()){
    old.withWriter { w -> lines.each { w.writeLine it } }
}

preCommitHook << ". src/main/scripts/precommit.sh\n"

ensureExecutableHook()
