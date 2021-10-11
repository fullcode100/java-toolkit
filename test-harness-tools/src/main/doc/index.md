<!--

     Copyright (c) 2020 - 2021 Henix, henix.fr

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.

-->
# OpenTestFactory Integration test harness

Main principle : the harness is based on the Junit framework. It uses [MockServer](https://www.mock-server.com/mock_server/getting_started.html#request_properties_matchers)
to setup a mock listening for callbacks from the system under test.
As an outline, an integration test case consists in :

1. Setting up expected answers using the ExpectedOutputReceiver instance provided by the test class
2. Sending one or more (most of the time only one, seeing as you're trying to test a well defined test case) messages to the SUT
    using one of the dedicated sendTestMessage/sendTemplatedTestMessage test class methods.
5. Asking the test harness to check that received request conform to expectations.

Sent and expected messages are referenced as test resource names pointing to text resources from the src/test/resource test.
These test resources  are expected to be json payloads.

## Contents

[//]: # (As there is no automatic TOC in markdown, manually maintained :'( - this is a comment by the way !)

* [Getting started](#getting-started)
* [Anatomy of a failure](#anatomy-of-a-failure) 
* [HOWTO](#howto)
   * [Using variable mappings](#using-variable-mappings)
   * [Checking that a request is received from the SUT](#checking-that-a-request-is-received-from-the-sut)
   * [Ignoring uncontrolled parts of response messages](#ignoring-uncontrolled-parts-of-response-messages)
   * [Checking that a message has NOT been received](#checking-that-a-message-has-not-been-received)
   * [Sending a test message](#sending-a-test-message)
   * [Checking HTTP status when sending](#checking-http-status-when-sending)

<a name="getting-started" />

## Getting started

Most tests will be derived from the org.opentestfactory.test.it.AbstractMicroserviceIntegrationTest class, and will look as follows :

```java

package org.opentestfactory.test.it;

import org.junit.Test;
import org.opentestfactory.test.harness.AbstractMicroserviceIntegrationTest;
import org.opentestfactory.test.harness.ExpectedOutputReceiver;
import org.opentestfactory.test.harness.TestConfiguration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

/**
 * This test checks that actions in an incoming workflow trigger
 * the required Report events.
 */
public class StepExecutionIntegrationTest extends AbstractMicroserviceIntegrationTest {

    /* 
     * Port used by the receiver to check SUT reponse messages.
     * Each test class should use a different one as much as possible, because the SUT event bus is NOT
     * reinitialized after test cases or test classes completion, making AssertionFailed harder to read
     * when many tests send messages to the same receiver (**it won't break passing tests, though**).
     * NB : choose port numbers >=1025, as the 1-1024 range is reserved to standard protocols and requires admin privileges.
     */
    public static final int RECEIVER_PORT = 1091;

    public StepExecutionIntegrationTest(){
        super(RECEIVER_PORT, 
            /* 
             *  Under here, entry point definition and authentication token references for the SUT - for 'real' integration tests this will be the event bus,
             *  but isolated microservice tests may target the microservice in itself. 
             *  In that case, the microservice endpoint and authentication token will have to be added to the test configuration file and referenced using 
             *   - respectively - TestConfiguration.VALUES.getServiceBaseURL("my.service.url") and TestConfiguration.VALUES.getServiceAuthToken("my.service.auth-token").
             *  The ".url" and ".auth-token" suffixes are defined respectively as the URL_KEY_SUFFIX and AUTH_TOKEN_KEY_SUFFIX constants.
             */
            TestConfiguration.VALUES.getServiceBaseURL(EVENTBUS_BASE_KEY+URL_KEY_SUFFIX),
            TestConfiguration.VALUES.getServiceAuthToken(EVENTBUS_BASE_KEY+AUTH_TOKEN_KEY_SUFFIX),
             /*
             * Here comes a (limited in most use cases) list of resource names pointing to json sbuscription messages to automatically subscribe the mock receiver to the SUT event bus.
             */
                "/it/runsteps/executionReportBusSubscription.json"
        );
    }

    /* How individual test cases are laid out : */

    @Test //typical junit4 unit test signature here
    public void getExecutionReportFromDummyEnvForExecutionCommand() throws IOException, InterruptedException, URISyntaxException {

        ExpectedOutputReceiver receiver=getExpectedOutputReceiver()
                .withVariableMapping("workflow_uuid", UUID.randomUUID().toString()) //each call to this adds a key-value mapping to substitute in message templates
                .withExpectedRequestTemplate( /* This means the message to expectd is defined as a message templates where place holder keys will be
                                               * replaced with the values value defined above.*/
                        "/publications", //This path is the same as the path used in the relevant subscription test resource
                        useTestResource("/it/runsteps/expected/directRunStepExecutionReport1.json") //This is the expected message resource name
                );

        /*
         * Send a message to the SUT (in many tests this will be a tailored workflow targetting the test case) by resource name.
         * The message is a template where placeholders will be replaced using the same mappings as the recevier (hence the receiver.mappings() parameter)
         */
        sendTemplatedTestMessage("/it/runsteps/input/directRunStepsWorkflow.json",receiver.mappings())
            .thenExpectHttpOkResponseCode();

         /*
          * Here we ask the receiver to check received messages. 
          * As these tests targets external processes with asynchrounous reponses, the receiver will wait before checking. The delay is defined as a parameter.
        receiver.waitAndVerifyExpectedCall(DELAY_BEFORE_ASYNC_REQUEST_SEQUENCE_VERIFY);
    }
}

```

When using the ExpectedOutputReceiver, remember that it is a immutable object, so you must chain all calls as chown in the sample above. 

<a name="anatomy-of-a-failure" />

## Anatomy of a failure

When An assertion fails, the message will look as in the example below. The stacktrace at the end is irrelevant.
It shows where the AssertionError was thrown from in the test code, but you already know that from the test name.

The important parts are ``expected<   here is your expected json >`` and ``but was < zero or more json payloads >``

* If the actual part reads ``but was <>``, it means no reponse was detected. Either you got the expected pathWrong
(remember, it must match the path from the subscription envelope), or no response was received. In the latter case,
you have to check microservice logs to look for error traces or clues as to what prevented your expected event from being
fired.

* If the actual part contains one or more json trees, events where received but did not match the expectations.
From there, you need to compare them with the expected payload.

```log
java.lang.AssertionError: Expected request payload not found : expected<{
  "apiVersion": "opentestfactory.org/v1alpha1",
  "kind": "Workflow",
  "metadata": {
    "name": "Simple Workflow",
    "workflow_id": "${json-unit.ignore}",
    "annotations": "${json-unit.ignore-element}",
    "creationTimestamp": "${json-unit.ignore}"
  },
  "thisIsNowhereToBeFoundInREALmessages": "so it fails...",
  "jobs": {
    "explicitJob": {
      "runs-on": "dummy",
      "steps": [
        {"run":"mvn clean test"}
      ]
    }
  }
}
> but was <{"apiVersion": "opentestfactory.org/v1alpha1", "kind": "Workflow", "metadata": {"name": "Simple Workflow", "annotations": {"opentestfactory.org/ordering": [["explicitJob"]]}, "creationTimestamp": "2020-11-23T18:47:03.919307", "workflow_id": "00663bec-e5ad-45c3-a67d-97edb975592d"}, "jobs": {"explicitJob": {"runs-on": "dummy", "steps": [{"run": "mvn clean test"}]}}}
{"apiVersion": "opentestfactory.org/v1alpha1", "kind": "Workflow", "metadata": {"name": "Simple Workflow", "annotations": {"opentestfactory.org/ordering": [["explicitJob"]]}, "creationTimestamp": "2020-11-23T18:47:03.919307", "workflow_id": "00663bec-e5ad-45c3-a67d-97edb975592d"}, "jobs": {"explicitJob": {"runs-on": "dummy", "steps": [{"run": "mvn clean test"}]}}}
>

	at org.opentestfactory.test.harness.ExpectedOutputReceiver.checkIfExpectedRequestWasReceived(ExpectedOutputReceiver.java:213)
	at org.opentestfactory.test.harness.ExpectedOutputReceiver.waitAndVerifyExpectedCall(ExpectedOutputReceiver.java:161)
	at org.opentestfactory.test.ReceptionistTest.testBasicJsonWorkflow(ReceptionistTest.java:38)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:566)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
	at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
	at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:68)
	at com.intellij.rt.execution.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:47)
	at com.intellij.rt.execution.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:242)
	at com.intellij.rt.execution.junit.JUnitStarter.main(JUnitStarter.java:70)


```

<a name="howto" />

## HOWTO

<a name="using-variable-mappings" />

### Using variable mappings

Variable mappings are used to insert computed values from the junit test code into test resources.

To use a variable mapping, you need to do as follows :
1. Insert a pace holder in the test resource.
As of 2020/11/23, variable may only be used to desfine a whole json string or integer attroibute value.

The placeholder is written as such:
``#{key}`` 

Here is an example of a varibilized json payload :

```json
{
  "apiVersion": "opentestfactory.org/v1alpha1",
  "kind": "Workflow",
  "metadata": {
    "workflow_id": #{workflow_uuid}
  },
  jobs:{
  
  }
}
```
**Please note** that the place holder has no quotes. It must replace the **whole** attribute value.

2. Add the key/value mapping to the registered mappings in your test

```java
       ExpectedOutputReceiver receiver=getExpectedOutputReceiver()
                .withVariableMapping("workflow_uuid", UUID.randomUUID().toString())
```
**Please note** you must add mappings **first**. 

  * Adding a mapping after the first expected payload will trigger an error.
  * Placeholders with no matching mappings will be left alone - and trigger a waining 
     and of course validation errors. 

3. Use the templated version of methods when defining the message :
  * For expected payloads:
```java
               .withExpectedRequestTemplate(
                        "/publications",
                        useTestResource("/it/runsteps/expected/directRunStepExecutionReport1.json")
```

  * For sent messages:
```java
        sendTemplatedTestMessage("/it/runsteps/input/directRunStepsWorkflow.json",receiver.mappings())
            .thenExpectHttpOkResponseCode();
```
**Please note:** In this method we take the mappings for the receiver, so that mappings are consistent 
in both sent messages and expected messages.

<a name="checking-that-a-request-is-received-from-the-sut" />

### Checking that a request is received from the SUT
This is done using the receiver using its withExpectedXXX methods as below:

**Fixed content version**
```java
                .withExpectedRequests(
                        requestMatcher("/messageIn","/eventbus/messages/basicMessage.json"),
                        requestMatcher("/messageIn","/eventbus/messages/messageWithMoreFields.json")
                )
```
**Please note:** This method is varags, so you may include as many requetMatcher calls as needed.
It may also be chained as many times as wanted.

**Variabilized template version**
```java
        .withExpectedRequestTemplate(
                        "/publications",
                        useTestResource("/it/runsteps/expected/directRunStepExecutionReport1.json")
```
**Please note:** This method only defines a single expectation. However calls may be chained aqs needed.

The path (first argument) is the path used in the subscription message (if the TestSuite subscribes to two
kinds of events, the path must the one from the relevant subscription).

<a name="ignoring-uncontrolled-parts-of-response-messages" />

### Ignoring uncontrolled parts of response messages

Messages are expected exactly as specified. Missing, different AND unexpected attributes will trigger AssertionError.
Some attributes are generated by the SUT and may not be predicted. To ignore such contents, you may use one of these two features
defined by the json-unit library :

1. Ignoring an attribute value

Use the ``"${json-unit.ignore}"`` special value for this attribute :

```json
{
  "apiVersion": "opentestfactory.org/v1alpha1",
  "kind": "WorkflowCompleted",
  "metadata": {
    "name": "RobotFramework Example",
    "workflow_id": #{workflow_uuid},
    "creationTimestamp": "${json-unit.ignore}"
  }
}
```

2. Ignore an element alltogether

Use the ``"${json-unit.ignore-element}"``

```json
{
  "apiVersion": "opentestfactory.org/v1alpha1",
  "kind": "ProviderCommand",
  "metadata": {
    "name": "RobotFramework Example",
    "workflow_id": #{workflow_uuid},
    "job_origin":[],
    "step_id":"${json-unit.ignore}",
    "step_origin":[],
    "job_id": "${json-unit.ignore}",
    "labels": {
      "opentestfactory.org/category": "checkout",
      "opentestfactory.org/categoryPrefix": "action",
      "opentestfactory.org/categoryVersion": "v2"
    },
    "creationTimestamp": "${json-unit.ignore}"
  },
  "step": {
    "with":{
      "repository":"https://github.com/robotframework/RobotDemo.git"
    },
    "uses":"action/checkout@v2",
    "id": "${json-unit.ignore}",
    "metadata": {
      "step_origin":[]
    }
  },
  "job": "${json-unit.ignore-element}"
}
```

<a name="checking-that-a-message-has-not-been-received" />

### Checking that a message has NOT been received

Some tests will also want to check that a given mesage was NOT emitted. This is done using the

``withUnwantedRequests`` receiver method :

```java
                ).withUnwantedRequests(
                        requestMatcher(pathOfMock, "/eventbus/messages/unWantedKindMessage.json")
                )
```

This method takes as many requestMatcher arguments as needed. It may also be chained several times.

<a name="sending-a-test-message" />

### Sending a test message

Use the ``sendTestMessage(String testMessageResourceName)`` or ``sendTemplatedTestMessage(String testMessageResourceName,JsonVariableMappings variableMappings)``
method as shown below.

```java
    sendTestMessage("/eventbus/messages/basicMessage.json").thenExpectHttpOkResponseCode();
```

```java
    sendTemplatedTestMessage(
            "/it/runsteps/input/directRunStepsWorkflow.json",
            receiver.mappings()
            )
            .thenExpectHttpOkResponseCode();   
```

<a name="checking-http-status-when-sending" />

### Checking HTTP status when sending

Use the ``thenExpectHttpXXX`` methods, chained with the sendXXX call. There are three methods :
* ``thenExpectHttpResponseCode(int reponseCode)`` may be used for any HTTP status code.
* ``thenExpectHttpOkResponseCode()`` expects HTTP 200 (OK)
* ``thenExpectHttpCreateResponseCode()`` expects HTTP 201 (Created)

Ex :
```java
        sendTemplatedTestMessage("/it/runsteps/input/directRunStepsWorkflow.json",receiver.mappings())
            .thenExpectHttpOkResponseCode();
```