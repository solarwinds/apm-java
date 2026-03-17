/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solarwinds.joboe.sampling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

public class XtraceOptionsTest {

  @Test
  @SuppressWarnings("unchecked")
  public void testGetXTraceOptions() throws Exception {
    assertNull(XtraceOptions.getXTraceOptions(null, null));

    String swKeys = "lo:se";
    XtraceOptions options =
        XtraceOptions.getXTraceOptions(
            XtraceOption.TRIGGER_TRACE.getKey()
                + XtraceOptions.ENTRY_SEPARATOR
                + XtraceOption.SW_KEYS.getKey()
                + XtraceOptions.KEY_VALUE_SEPARATOR
                + swKeys
                + XtraceOptions.ENTRY_SEPARATOR
                + XtraceOption.CUSTOM_KV_PREFIX
                + "tag1"
                + XtraceOptions.KEY_VALUE_SEPARATOR
                + "v1"
                + XtraceOptions.ENTRY_SEPARATOR
                + XtraceOption.CUSTOM_KV_PREFIX
                + "tag2"
                + XtraceOptions.KEY_VALUE_SEPARATOR
                + "v2",
            null);
    assertEquals(
        XtraceOptions.AuthenticationStatus.NOT_AUTHENTICATED, options.getAuthenticationStatus());

    HashMap<XtraceOption<String>, String> expectedCustomKvs =
        new HashMap<XtraceOption<String>, String>();
    expectedCustomKvs.put(
        (XtraceOption<String>) XtraceOption.fromKey(XtraceOption.CUSTOM_KV_PREFIX + "tag1"), "v1");
    expectedCustomKvs.put(
        (XtraceOption<String>) XtraceOption.fromKey(XtraceOption.CUSTOM_KV_PREFIX + "tag2"), "v2");

    assertEquals(swKeys, options.getOptionValue(XtraceOption.SW_KEYS));
    assertEquals(Boolean.TRUE, options.getOptionValue(XtraceOption.TRIGGER_TRACE));
    assertEquals(expectedCustomKvs, options.getCustomKvs());
    assertEquals(Collections.emptyList(), options.getExceptions());

    // no trigger-trace option
    options =
        XtraceOptions.getXTraceOptions(
            XtraceOption.SW_KEYS.getKey()
                + XtraceOptions.KEY_VALUE_SEPARATOR
                + swKeys
                + XtraceOptions.ENTRY_SEPARATOR
                + XtraceOption.CUSTOM_KV_PREFIX
                + "tag1"
                + XtraceOptions.KEY_VALUE_SEPARATOR
                + "v1"
                + XtraceOptions.ENTRY_SEPARATOR
                + XtraceOption.CUSTOM_KV_PREFIX
                + "tag2"
                + XtraceOptions.KEY_VALUE_SEPARATOR
                + "v2",
            null);
    assertEquals(
        XtraceOptions.AuthenticationStatus.NOT_AUTHENTICATED, options.getAuthenticationStatus());
    assertEquals(Collections.emptyList(), options.getExceptions());
    assertEquals(swKeys, options.getOptionValue(XtraceOption.SW_KEYS));
    assertEquals(expectedCustomKvs, options.getCustomKvs());
  }

  @Test
  public void testFormatting() {
    XtraceOptions options;
    String swKeys = "lo:se";
    // leading and trailing whitespace
    options =
        XtraceOptions.getXTraceOptions(
            "      "
                + XtraceOption.TRIGGER_TRACE.getKey()
                + XtraceOptions.ENTRY_SEPARATOR
                + XtraceOption.SW_KEYS.getKey()
                + XtraceOptions.KEY_VALUE_SEPARATOR
                + swKeys
                + "      ",
            null);
    assertEquals(swKeys, options.getOptionValue(XtraceOption.SW_KEYS));
    assertEquals(Boolean.TRUE, options.getOptionValue(XtraceOption.TRIGGER_TRACE));

    // space in between kv pairs are trimmed
    // leading and trailing whitespace
    options =
        XtraceOptions.getXTraceOptions(
            XtraceOption.TRIGGER_TRACE.getKey()
                + " "
                + XtraceOptions.ENTRY_SEPARATOR
                + " "
                + XtraceOption.SW_KEYS.getKey()
                + " "
                + XtraceOptions.KEY_VALUE_SEPARATOR
                + " "
                + swKeys,
            null);
    assertEquals(swKeys, options.getOptionValue(XtraceOption.SW_KEYS));
    assertEquals(Boolean.TRUE, options.getOptionValue(XtraceOption.TRIGGER_TRACE));

    // space in key is considered invalid
    options = XtraceOptions.getXTraceOptions("trigger trace", null);
    assertEquals(Boolean.FALSE, options.getOptionValue(XtraceOption.TRIGGER_TRACE));
    assertEquals(
        "trigger trace",
        ((XtraceOptions.UnknownXTraceOptionException) options.getExceptions().get(0))
            .getInvalidOptionKey());

    // key/value separator (=) in value is okay
    String customKey = XtraceOption.CUSTOM_KV_PREFIX + "1";
    String customValue = "foo" + XtraceOptions.KEY_VALUE_SEPARATOR + "5";
    options =
        XtraceOptions.getXTraceOptions(
            customKey + XtraceOptions.KEY_VALUE_SEPARATOR + customValue, null);
    assertEquals(0, options.getExceptions().size());
    assertEquals(1, options.getCustomKvs().size());
    assertEquals(customKey, options.getCustomKvs().keySet().iterator().next().getKey());
    assertEquals(customValue, options.getCustomKvs().values().iterator().next());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testDuplicatedOption() {
    XtraceOptions options =
        XtraceOptions.getXTraceOptions(
            XtraceOption.SW_KEYS.getKey()
                + XtraceOptions.KEY_VALUE_SEPARATOR
                + "p1"
                + XtraceOptions.ENTRY_SEPARATOR
                + XtraceOption.SW_KEYS.getKey()
                + XtraceOptions.KEY_VALUE_SEPARATOR
                + "p2"
                + XtraceOptions.ENTRY_SEPARATOR
                + XtraceOption.CUSTOM_KV_PREFIX
                + "tag1"
                + XtraceOptions.KEY_VALUE_SEPARATOR
                + "v1"
                + XtraceOptions.ENTRY_SEPARATOR
                + XtraceOption.CUSTOM_KV_PREFIX
                + "tag1"
                + XtraceOptions.KEY_VALUE_SEPARATOR
                + "v2",
            null);
    HashMap<XtraceOption<String>, String> expectedCustomKvs =
        new HashMap<XtraceOption<String>, String>();
    expectedCustomKvs.put(
        (XtraceOption<String>) XtraceOption.fromKey(XtraceOption.CUSTOM_KV_PREFIX + "tag1"),
        "v1"); // take the first value only

    assertEquals("p1", options.getOptionValue(XtraceOption.SW_KEYS));
    assertEquals(expectedCustomKvs, options.getCustomKvs());
  }

  @Test
  public void testGetXTraceOptionsExceptions() throws Exception {
    XtraceOptions options =
        XtraceOptions.getXTraceOptions(
            XtraceOption.TRIGGER_TRACE.getKey()
                + XtraceOptions.ENTRY_SEPARATOR
                + "unknown-tag1"
                + XtraceOptions.KEY_VALUE_SEPARATOR
                + "v1"
                + XtraceOptions.ENTRY_SEPARATOR
                + "unknown-tag2",
            null);

    assertEquals(Boolean.TRUE, options.getOptionValue(XtraceOption.TRIGGER_TRACE));
    assertEquals(2, options.getExceptions().size());
    assertEquals(
        "unknown-tag1",
        ((XtraceOptions.UnknownXTraceOptionException) options.getExceptions().get(0))
            .getInvalidOptionKey());
    assertEquals(
        "unknown-tag2",
        ((XtraceOptions.UnknownXTraceOptionException) options.getExceptions().get(1))
            .getInvalidOptionKey());

    // test invalid format
    options =
        XtraceOptions.getXTraceOptions(
            XtraceOption.TRIGGER_TRACE.getKey()
                + XtraceOptions.KEY_VALUE_SEPARATOR
                + "1"
                + XtraceOptions.ENTRY_SEPARATOR
                + XtraceOption.CUSTOM_KV_PREFIX
                + "1",
            null); // trigger trace should not have value, custom kv should have a value
    assertEquals(Boolean.FALSE, options.getOptionValue(XtraceOption.TRIGGER_TRACE));
    assertEquals(
        XtraceOption.TRIGGER_TRACE.getKey(),
        ((XtraceOptions.InvalidFormatXTraceOptionException) options.getExceptions().get(0))
            .getInvalidOptionKey());
    assertEquals(
        XtraceOption.CUSTOM_KV_PREFIX + "1",
        ((XtraceOptions.InvalidFormatXTraceOptionException) options.getExceptions().get(1))
            .getInvalidOptionKey());

    // test invalid value
    options =
        XtraceOptions.getXTraceOptions(
            XtraceOption.TS.getKey() + XtraceOptions.KEY_VALUE_SEPARATOR + "abc",
            null); // ts should be a long
    assertEquals(
        XtraceOption.TS.getKey(),
        ((XtraceOptions.InvalidValueXTraceOptionException) options.getExceptions().get(0))
            .getInvalidOptionKey());

    // parse some options either though others are bad
    options =
        XtraceOptions.getXTraceOptions("trigger-trace;custom-foo=' bar;bar' ;custom-bar=foo", null);
    assertEquals(Boolean.TRUE, options.getOptionValue(XtraceOption.TRIGGER_TRACE));
    assertEquals(2, options.getCustomKvs().size());
    Iterator<XtraceOption<String>> customKeyIterator = options.getCustomKvs().keySet().iterator();
    Iterator<String> customValueIterator = options.getCustomKvs().values().iterator();
    assertEquals("custom-foo", customKeyIterator.next().getKey());
    assertEquals("' bar", customValueIterator.next());

    assertEquals("custom-bar", customKeyIterator.next().getKey());
    assertEquals("foo", customValueIterator.next());

    assertEquals(
        "bar'",
        ((XtraceOptions.UnknownXTraceOptionException) options.getExceptions().get(0))
            .getInvalidOptionKey());

    options =
        XtraceOptions.getXTraceOptions(
            ";trigger-trace;custom-something=value_thing;sw-keys=02973r70:9wqj21,0d9j1;1;2;=custom-key=val?;=",
            null);
    assertEquals(Boolean.TRUE, options.getOptionValue(XtraceOption.TRIGGER_TRACE));
    assertEquals("02973r70:9wqj21,0d9j1", options.getOptionValue(XtraceOption.SW_KEYS));
    assertEquals(1, options.getCustomKvs().size());
    customKeyIterator = options.getCustomKvs().keySet().iterator();
    customValueIterator = options.getCustomKvs().values().iterator();
    assertEquals("custom-something", customKeyIterator.next().getKey());
    assertEquals("value_thing", customValueIterator.next());
    assertEquals(
        2,
        options
            .getExceptions()
            .size()); // should only flag exception for 1 and 2, the last two entry starts with '='
    // will be ignored
    assertEquals(
        "1",
        ((XtraceOptions.UnknownXTraceOptionException) options.getExceptions().get(0))
            .getInvalidOptionKey());
    assertEquals(
        "2",
        ((XtraceOptions.UnknownXTraceOptionException) options.getExceptions().get(1))
            .getInvalidOptionKey());

    // skip sequel ;
    options =
        XtraceOptions.getXTraceOptions(
            "custom-something=value_thing;sw-keys=02973r70;;;;custom-key=val", null);
    assertEquals("02973r70", options.getOptionValue(XtraceOption.SW_KEYS));
    assertEquals(2, options.getCustomKvs().size());
    customKeyIterator = options.getCustomKvs().keySet().iterator();
    customValueIterator = options.getCustomKvs().values().iterator();
    assertEquals("custom-something", customKeyIterator.next().getKey());
    assertEquals("value_thing", customValueIterator.next());
    assertEquals("custom-key", customKeyIterator.next().getKey());
    assertEquals("val", customValueIterator.next());

    // case sensitive
    options = XtraceOptions.getXTraceOptions("Trigger-Trace;Custom-something=value_thing", null);
    assertEquals(Boolean.FALSE, options.getOptionValue(XtraceOption.TRIGGER_TRACE));
    assertEquals(2, options.getExceptions().size());
    assertEquals(
        "Trigger-Trace",
        ((XtraceOptions.UnknownXTraceOptionException) options.getExceptions().get(0))
            .getInvalidOptionKey());
    assertEquals(
        "Custom-something",
        ((XtraceOptions.UnknownXTraceOptionException) options.getExceptions().get(1))
            .getInvalidOptionKey());

    // no X-Trace-Options but has signature
    options = XtraceOptions.getXTraceOptions(null, "abc");
    assertNull(options);
  }

  @Test
  public void testHmacAuthenticator() throws Exception {
    byte[] content =
        Files.readAllBytes(Paths.get(new File("src/test/resources/hmac-signature.txt").getPath()));
    XtraceOptions.HmacSignatureAuthenticator authenticator =
        new XtraceOptions.HmacSignatureAuthenticator(content);

    assertTrue(
        authenticator.authenticate(
            "trigger-trace;sw-keys=lo:se,check-id:123;ts=1564597681",
            "26e33ce58c52afc507c5c1e9feff4ac5562c9e1c"));
    assertFalse(
        authenticator.authenticate(
            "trigger-trace;sw-keys=lo:se,check-id:123;ts=1564597681",
            "2c1c398c3e6be898f47f74bf74f035903b48baaa"));
  }

  @Test
  public void testAuthenticate() throws IOException {
    byte[] content =
        Files.readAllBytes(Paths.get(new File("src/test/resources/hmac-signature.txt").getPath()));
    XtraceOptions.HmacSignatureAuthenticator authenticator =
        new XtraceOptions.HmacSignatureAuthenticator(content);

    // missing ts
    assertEquals(
        XtraceOptions.AuthenticationStatus.failure("bad-timestamp"),
        XtraceOptions.authenticate(
            "trigger-trace;sw-keys=lo:se,check-id:123",
            null,
            "2c1c398c3e6be898f47f74bf74f035903b48b59c",
            authenticator));

    long outOfRangeTimestamp =
        System.currentTimeMillis() / 1000 - (XtraceOptions.TIMESTAMP_MAX_DELTA + 1);
    // timestamp out of range
    assertEquals(
        XtraceOptions.AuthenticationStatus.failure("bad-timestamp"),
        XtraceOptions.authenticate(
            "trigger-trace;sw-keys=lo:se,check-id:123;ts=" + outOfRangeTimestamp,
            outOfRangeTimestamp,
            "2c1c398c3e6be898f47f74bf74f035903b48b59c",
            authenticator));

    // no signature
    assertEquals(
        XtraceOptions.AuthenticationStatus.NOT_AUTHENTICATED,
        XtraceOptions.authenticate(
            "trigger-trace;sw-keys=lo:se,check-id:123", null, null, authenticator));

    // valid signature - using a mock up authenticator here to bypass the signature check - which is
    // verify in testHmacAuthenticator
    long goodTimestamp = System.currentTimeMillis() / 1000;
    assertEquals(
        XtraceOptions.AuthenticationStatus.OK,
        XtraceOptions.authenticate(
            "trigger-trace;sw-keys=lo:se,check-id:123;ts=" + goodTimestamp,
            goodTimestamp,
            "2c1c398c3e6be898f47f74bf74f035903b48b59c",
            ((optionsString, signature) -> true)));

    // authenticator not ready
    assertEquals(
        XtraceOptions.AuthenticationStatus.failure("authenticator-unavailable"),
        XtraceOptions.authenticate(
            "trigger-trace;sw-keys=lo:se,check-id:123;ts=" + goodTimestamp,
            goodTimestamp,
            "2c1c398c3e6be898f47f74bf74f035903b48b59c",
            null));
  }
}
