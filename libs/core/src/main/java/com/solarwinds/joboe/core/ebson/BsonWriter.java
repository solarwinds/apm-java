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
 * Writes Java object(s) to {@linkplain ByteBuffer buffers}, serialized to bytes as specified by the
 * <a href="http://bsonspec.org/">BSON</a> specification.
 *
 * <p><b>Note:</b> buffers supplied to {@linkplain #writeTo} must use little-endian byte ordering.
 */
public interface BsonWriter {

  /**
   * Writes {@code reference} to {@code buffer}.
   *
   * @param buffer the buffer {@code reference} will be written to
   * @param reference the reference to be written into {@code buffer}
   * @throws NullPointerException if {@code buffer} is null
   * @throws IllegalArgumentException if {@code buffer} is not using little-endian byte ordering
   */
  void writeTo(ByteBuffer buffer, @Nullable Object reference);
}
