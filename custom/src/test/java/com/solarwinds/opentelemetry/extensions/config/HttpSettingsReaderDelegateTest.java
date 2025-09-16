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

package com.solarwinds.opentelemetry.extensions.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.sampling.Settings;
import io.opentelemetry.api.internal.InstrumentationUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpSettingsReaderDelegateTest {

  @Mock private HttpURLConnection mockConnection;

  @Mock private URL mockUrl;

  private HttpSettingsReaderDelegate tested;

  private static final String TEST_URL_STRING = "https://example.com/settings";
  private static final String TEST_AUTH_HEADER = "Bearer token123";
  private static final String TEST_JSON_RESPONSE =
      "{\n"
          + "    \"value\": 1000000,\n"
          + "    \"flags\": \"SAMPLE_START,SAMPLE_THROUGH_ALWAYS,SAMPLE_BUCKET_ENABLED,TRIGGER_TRACE\",\n"
          + "    \"timestamp\": 1741301987,\n"
          + "    \"ttl\": 120,\n"
          + "    \"arguments\": {\n"
          + "        \"BucketCapacity\": 2,\n"
          + "        \"BucketRate\": 1,\n"
          + "        \"TriggerRelaxedBucketCapacity\": 20,\n"
          + "        \"TriggerRelaxedBucketRate\": 1,\n"
          + "        \"TriggerStrictBucketCapacity\": 6,\n"
          + "        \"TriggerStrictBucketRate\": 0.1,\n"
          + "        \"SignatureKey\": \"<your-signature-key>\"\n"
          + "    }\n"
          + "}";

  @BeforeEach
  void setup() {
    tested = spy(new HttpSettingsReaderDelegate());
  }

  @Test
  void testFetchSettings_Success() throws IOException {
    InputStream inputStream = new ByteArrayInputStream(TEST_JSON_RESPONSE.getBytes());

    doReturn(mockConnection)
        .when(tested)
        .getHttpUrlConnection(any(URL.class), eq(TEST_AUTH_HEADER));
    when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(mockConnection.getInputStream()).thenReturn(inputStream);

    try (MockedStatic<InstrumentationUtil> instrumentationUtil =
        mockStatic(InstrumentationUtil.class)) {
      instrumentationUtil
          .when(() -> InstrumentationUtil.suppressInstrumentation(any(Runnable.class)))
          .thenAnswer(
              invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
              });

      Settings result = tested.fetchSettings(TEST_URL_STRING, TEST_AUTH_HEADER);

      assertEquals(1741301987, result.getTimestamp());
      assertEquals(1000000, result.getValue());

      verify(tested).getHttpUrlConnection(any(URL.class), eq(TEST_AUTH_HEADER));
      verify(mockConnection).disconnect();
    }
  }

  @Test
  void testFetchSettings_HttpError() throws IOException {
    String errorMessage = "Bad Request - Invalid parameters";
    InputStream errorStream = new ByteArrayInputStream(errorMessage.getBytes());

    doReturn(mockConnection)
        .when(tested)
        .getHttpUrlConnection(any(URL.class), eq(TEST_AUTH_HEADER));
    when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
    when(mockConnection.getErrorStream()).thenReturn(errorStream);

    try (MockedStatic<InstrumentationUtil> instrumentationUtil =
        mockStatic(InstrumentationUtil.class)) {
      instrumentationUtil
          .when(() -> InstrumentationUtil.suppressInstrumentation(any(Runnable.class)))
          .thenAnswer(
              invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
              });

      Settings result = tested.fetchSettings(TEST_URL_STRING, TEST_AUTH_HEADER);

      assertNull(result);
      verify(mockConnection).disconnect();
    }
  }

  @Test
  void testFetchSettings_IOException() throws IOException {
    doThrow(new IOException("Connection failed"))
        .when(tested)
        .getHttpUrlConnection(any(URL.class), eq(TEST_AUTH_HEADER));

    try (MockedStatic<InstrumentationUtil> instrumentationUtil =
        mockStatic(InstrumentationUtil.class)) {
      instrumentationUtil
          .when(() -> InstrumentationUtil.suppressInstrumentation(any(Runnable.class)))
          .thenAnswer(
              invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
              });

      Settings result = tested.fetchSettings(TEST_URL_STRING, TEST_AUTH_HEADER);

      assertNull(result);
    }
  }

  @Test
  void testFetchSettings_RuntimeException() throws IOException {
    doReturn(mockConnection)
        .when(tested)
        .getHttpUrlConnection(any(URL.class), eq(TEST_AUTH_HEADER));
    when(mockConnection.getResponseCode()).thenThrow(new RuntimeException("Unexpected error"));

    try (MockedStatic<InstrumentationUtil> instrumentationUtil =
        mockStatic(InstrumentationUtil.class)) {
      instrumentationUtil
          .when(() -> InstrumentationUtil.suppressInstrumentation(any(Runnable.class)))
          .thenAnswer(
              invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
              });

      Settings result = tested.fetchSettings(TEST_URL_STRING, TEST_AUTH_HEADER);

      assertNull(result);
      verify(mockConnection).disconnect();
    }
  }

  @Test
  void testGetHttpUrlConnection_Configuration() throws IOException {
    when(mockUrl.openConnection()).thenReturn(mockConnection);

    HttpURLConnection result = tested.getHttpUrlConnection(mockUrl, TEST_AUTH_HEADER);

    assertEquals(mockConnection, result);

    verify(mockConnection).setRequestMethod("GET");
    verify(mockConnection).setRequestProperty("Authorization", TEST_AUTH_HEADER);
    verify(mockConnection).setRequestProperty("Content-Type", "application/json");

    verify(mockConnection).setRequestProperty("Accept", "application/json");
    verify(mockConnection).setConnectTimeout(10000);
    verify(mockConnection).setReadTimeout(10000);
  }
}
