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

import com.solarwinds.joboe.core.HostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class K8sReaderTest {
  private ServerHostInfoReader.K8sReader tested;

  @BeforeEach
  protected void setUp() throws Exception {
    ServerHostInfoReader.K8sReader.NAMESPACE_FILE_LOC_LINUX =
        "src/test/java/com/solarwinds/joboe/core/util/namespace";

    ServerHostInfoReader.K8sReader.POD_UUID_FILE_LOC =
        "src/test/java/com/solarwinds/joboe/core/util/poduid";
    ServerHostInfoReader.osType = HostInfoUtils.OsType.LINUX;
    tested = new ServerHostInfoReader.K8sReader();
  }

  @Test
  public void testReadK8sMetadata() {
    HostId.K8sMetadata actual = tested.getK8sMetadata();
    HostId.K8sMetadata expected =
        HostId.K8sMetadata.builder()
            .namespace("o11y-platform")
            .podUid("9dcdb600-4156-4b7b-afcc-f8c06cb0e474")
            .podName(ServerHostInfoReader.INSTANCE.getHostName())
            .build();

    assertEquals(expected, actual);
  }
}
