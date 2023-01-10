/**
 *  Copyright (c) 2020 - 2023 Henix, henix.fr
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
package org.squashtest.ta.local.process.library.process

import java.util.concurrent.TimeoutException;

import org.squashtest.ta.core.tools.io.SimpleLinesData
import org.squashtest.ta.local.process.library.process.LocalProcessClient;
import org.squashtest.ta.local.process.library.shell.ShellResult;
import org.squashtest.ta.framework.exception.InstructionRuntimeException;
import org.squashtest.ta.framework.tools.TempDir;

import spock.lang.Specification

class LocalProcessClientTest extends Specification {
	LocalProcessClient testee
	
	def setup(){
		testee=new LocalProcessClient()
	}
	
	def "should execute with no timeout"(){
		given:
			def command = "ping"
			int timeout = 0
			int streamlength = 100
		when:
			def result = testee.runLocalProcessCommand(command, null, timeout, streamlength)
		then:
			result instanceof ShellResult
			result.getExitValue() > 0
	}
	
	def "should execute with parametrized timeout"(){
		given:
			def command = "ping localhost"
			int timeout = 30
			int streamlength = 100
		when:
			def result = testee.runLocalProcessCommand(command, null, timeout, streamlength)
		then:
			thrown(TimeoutException);
	}
	
	def "should transmit command correctly"(){
		given:
			def command = "ping"
			int timeout = 0
			int streamlength = 100
		when:
			def result = testee.runLocalProcessCommand(command, null, timeout, streamlength)
		then:
			result instanceof ShellResult
			result.getCommand() == command
	}
	
	def "should construct an outputstream"(){
		given:
			def command = "java -version"
			int timeout = 0
			int streamlength = 100
		when:
			def result = testee.runLocalProcessCommand(command, null, timeout, streamlength)
		then:
                        result.getExitValue() != 127
			def length
			if(result.getStdout().length() > 0 ){
				length =  result.getStdout().length()
			}else{
				length =  result.getStderr().length()
			}
			
			length > 0
	}
	
	def "wrong command should rant"(){
		given:
			def command = "wrong"
			int timeout = 0
			int streamlength = 100
		when:
			def result = testee.runLocalProcessCommand(command, null, timeout, streamlength)
		then:
			thrown(IOException)
	}
	
}
