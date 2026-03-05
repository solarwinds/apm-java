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

package com.solarwinds.joboe.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.solarwinds.joboe.core.util.ServerHostInfoReader.DockerInfoReader;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DockerInfoReaderTest {
  private static final String TEST_FILE_PREFIX =
      "src/test/java/com/solarwinds/joboe/core/util/docker-cgroup-"; // using a rather static path.

  // Using
  // Class.getResourceAsStream
  // does not work in test (vs
  // main)

  @BeforeEach
  protected void tearDown() throws Exception {
    DockerInfoReader.getInstance()
        .initializeLinux(DockerInfoReader.DEFAULT_LINUX_DOCKER_FILE_LOCATION); // reset to default
  }

  @Test
  public void testReadDockerContainerId() throws IOException {
    DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "standard");
    assertEquals(
        "0531ff3c6395131175507ac7e94fdf387f2a2dea81961e6c96f6ac5ccd7ede3f",
        DockerInfoReader.getDockerId());

    DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "standard-2");
    assertEquals(
        "0531ff3c6395131175507ac7e94fdf387f2a2dea81961e6c96f6ac5ccd7ede3f",
        DockerInfoReader.getDockerId());

    DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "ce");
    assertEquals(
        "93d377d55070d2463493706ba7194d119c3efb1c2e7929f36da183ffe71d72a8",
        DockerInfoReader.getDockerId());

    DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "ecs");
    assertEquals(
        "93d377d55070d2463493706ba7194d119c3efb1c2e7929f36da183ffe71d72a8",
        DockerInfoReader.getDockerId());

    DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "cri-containerd");
    assertEquals(
        "0531ff3c6395131175507ac7e94fdf387f2a2dea81961e6c96f6ac5ccd7ede3f",
        DockerInfoReader.getDockerId());

    DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "kubepods");
    assertEquals(
        "0531ff3c6395131175507ac7e94fdf387f2a2dea81961e6c96f6ac5ccd7ede3f",
        DockerInfoReader.getDockerId());

    DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "empty");
    assertNull(DockerInfoReader.getDockerId());

    DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "non-docker");
    assertNull(DockerInfoReader.getDockerId());

    DockerInfoReader.getInstance().initializeLinux(TEST_FILE_PREFIX + "invalid");
    assertNull(DockerInfoReader.getDockerId());
  }
}
