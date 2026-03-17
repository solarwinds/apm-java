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

package com.solarwinds.joboe.core;

/** Constants used throughout jboe code (from oboe.h) */
public class Constants {

  public static final int TASK_ID_LEN = 16,
      OP_ID_LEN = 8,
      MAX_METADATA_PACK_LEN = 512,
      MASK_TASK_ID_LEN = 0x03,
      MASK_OP_ID_LEN = 0x08,
      MASK_HAS_OPTIONS = 0x04, // unused?

      //            MAX_UDP_PKT_SZ = 65507, // (65535 max IP packet size - 20 IPv4 header - 8 UDP
      // header)
      MAX_EVENT_BUFFER_SIZE =
          512 * 1024, // 512kB. This should not be bound by UDP size anymore with SSL reporting,
      // though we still want to have some limit
      MAX_BACK_TRACE_TOP_LINE_COUNT = 100,
      MAX_BACK_TRACE_BOTTOM_LINE_COUNT = 20,
      MAX_BACK_TRACE_LINE_COUNT = MAX_BACK_TRACE_TOP_LINE_COUNT + MAX_BACK_TRACE_BOTTOM_LINE_COUNT,
      XTR_UDP_PORT = 7831;
  public static final String SW_W3C_KEY_PREFIX = "sw.",
      XTR_ASYNC_KEY = "Async",
      XTR_EDGE_KEY = SW_W3C_KEY_PREFIX + "parent_span_id",
      XTR_AO_EDGE_KEY = "Edge",
      XTR_THREAD_ID_KEY = "TID",
      XTR_HOSTNAME_KEY = "Hostname",
      XTR_METADATA_KEY = SW_W3C_KEY_PREFIX + "trace_context",
      XTR_XTRACE = "X-Trace",
      XTR_PROCESS_ID_KEY = "PID",
      XTR_TIMESTAMP_U_KEY = "Timestamp_u",
      XTR_UDP_HOST = "127.0.0.1";
}
