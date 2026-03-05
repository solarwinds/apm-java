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

package com.solarwinds.joboe.core.ebson;

import java.nio.ByteBuffer;
import javax.annotation.Nullable;

/**
 * Reads Java object(s) from {@linkplain ByteBuffer buffers}, deserialized from bytes as specified
 * by the <a href="http://bsonspec.org/">BSON</a> specification.
 *
 * <p><b>Note:</b> buffers supplied to {@linkplain #readFrom} must use little-endian byte ordering.
 */
public interface BsonReader {

  /**
   * Reads an arbitrary amount of bytes from {@code buffer} and constructs a new object from what
   * was read.
   *
   * @param buffer the buffer to read from
   * @return a new object from the bytes read from {@code buffer}
   * @throws NullPointerException if {@code buffer} is null
   * @throws IllegalArgumentException if {@code buffer} is not using little-endian byte ordering
   */
  @Nullable Object readFrom(ByteBuffer buffer);
}
