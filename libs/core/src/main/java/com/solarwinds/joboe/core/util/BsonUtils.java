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

import com.solarwinds.joboe.core.BsonBufferException;
import com.solarwinds.joboe.core.ebson.BsonDocument;
import com.solarwinds.joboe.core.ebson.BsonDocuments;
import com.solarwinds.joboe.core.ebson.BsonToken;
import com.solarwinds.joboe.core.ebson.BsonWriter;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

public class BsonUtils {
  private BsonUtils() {}

  public static ByteBuffer convertMapToBson(
      Map<String, Object> map, int bufferSize, int maxBufferSize) throws BsonBufferException {
    BsonDocument doc = BsonDocuments.copyOf(map);
    BsonWriter writer = BsonToken.DOCUMENT.writer();
    ByteBuffer buffer = ByteBuffer.allocate(bufferSize).order(ByteOrder.LITTLE_ENDIAN);

    RuntimeException bufferException;
    try {
      writer.writeTo(buffer, doc);
      buffer.flip(); // cast for JDK 8- runtime compatibility
      return buffer;
    } catch (BufferOverflowException e) { // cannot use multi-catch due to 1.6 compatibility
      bufferException = e;
    } catch (
        IllegalArgumentException
            e) { // IllegalArumgnentException could be thrown from BsonWriter.DOCUMENT.writeto if
      // buffer position is greater than limit - 4
      bufferException = e;
    }

    if (bufferException != null) {
      buffer.clear(); // cast for JDK 8- runtime compatibility
      if (bufferSize * 2 <= maxBufferSize) {
        return convertMapToBson(map, bufferSize * 2, maxBufferSize);
      } else {
        throw new BsonBufferException(bufferException);
      }
    } else {
      // shouldn't really run to here base on current logic flow, but just to play safe
      return buffer;
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> convertBsonToMap(ByteBuffer byteBuffer) {
    Object o = BsonToken.DOCUMENT.reader().readFrom(byteBuffer);

    return (Map<String, Object>) o;
  }
}
