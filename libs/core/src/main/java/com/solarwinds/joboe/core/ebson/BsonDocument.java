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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Immutable representation of a <a href="http://bsonspec.org/">BSON</a> document.
 *
 * <p><b>Notes:</b>
 *
 * <ul>
 *   <li>None of the {@linkplain Map map interface's} optional operations are supported.
 *   <li>Use the {@linkplain BsonDocuments documents utility class} to create new documents.
 * </ul>
 */
public interface BsonDocument extends Map<String, Object> {

  /**
   * Returns this document's size (the number of its keys).
   *
   * @return this document's size
   */
  @Override
  int size();

  /**
   * Returns <em>true</em> if this document contains no key-value pairs; <em>false</em> otherwise.
   *
   * @return <em>true</em> if this document contains no key-value pairs; <em>false</em> otherwise
   */
  @Override
  boolean isEmpty();

  /**
   * Returns an immutable view of this document's keys.
   *
   * @return an immutable view of this document's keys.
   */
  @Override
  Set<String> keySet();

  /**
   * Returns an immutable view of this document's values.
   *
   * @return an immutable view of this document's values
   */
  @Override
  Collection<Object> values();

  /**
   * Returns an immutable view of this document's key-value pairs.
   *
   * @return an immutable view of this document's key-value pairs
   */
  @Override
  Set<Entry<String, Object>> entrySet();

  /**
   * Returns the value associated with {@code key} in a type-safe manner.
   *
   * <p><b>Note:</b> {@code type} can and should be null only <em>iff</em> the value associated with
   * {@code key} is expected to be null.
   *
   * @param key the key whose associated value is to be returned
   * @param type the expected type of the value associated with {@code key}
   * @param <T> the expected type of the value associated with {@code key}
   * @return the value associated with {@code key} in a type-safe manner
   * @throws NullPointerException if {@code key} is null
   * @throws IllegalArgumentException if this document does not contain {@code key} or if the value
   *     associated with it is not assignment-compatible with {@code type}
   * @throws ClassCastException if {@code key} is not a string
   */
  @Nullable <T> T get(Object key, @Nullable Class<T> type);

  /**
   * Returns the value associated with {@code key}.
   *
   * @param key the key whose associated value is to be returned
   * @return the value associated with {@code key}
   * @throws NullPointerException if {@code key} is null
   * @throws IllegalArgumentException if this document does not contain {@code key}
   * @throws ClassCastException if {@code key} is not a string
   */
  @Override
  @CheckForNull
  Object get(Object key);

  /**
   * Returns <em>true</em> if {@code key} is contained by this document; <em>false</em> otherwise.
   *
   * @param key the key to be tested if it is contained by this document
   * @return <em>true</em> if {@code key} is contained by this document; <em>false</em> otherwise
   * @throws NullPointerException if {@code key} is null
   * @throws ClassCastException if {@code key} is not a string
   */
  @Override
  boolean containsKey(Object key);

  /**
   * Returns <em>true</em> if {@code value} is contained by this document; <em>false</em> otherwise.
   *
   * @param value the value to be tested if it is contained by this document
   * @return <em>true</em> if {@code value} is contained by this document; <em>false</em> otherwise
   */
  @Override
  boolean containsValue(@Nullable Object value);

  /**
   * Returns this document's hash code (calculated using its {@linkplain #entrySet() key-value
   * pairs}).
   *
   * @return this document's hash code
   */
  @Override
  int hashCode();

  /**
   * Returns <em>true</em> if {@code object} is the same as this document; <em>false</em> otherwise.
   *
   * <p>{@code object} is the same as this document <em>iff</em>:
   *
   * <ul>
   *   <li>{@code this == object} or
   *   <li>{@code object instanceof Map} <em>and</em>
   *   <li>{@code this.entrySet().equals(object.entrySet())}.
   * </ul>
   *
   * @param object the reference object with which to compare
   * @return <em>true</em> if {@code object} is the same as this document; <em>false</em> otherwise
   */
  @Override
  boolean equals(@CheckForNull Object object);

  /**
   * Returns this document's textual representation.
   *
   * <p>The general format is the following:
   *
   * <ul>
   *   <li>Empty: <code>{}</code>
   *   <li>Single key-value pair: <code>{key: value}</code>
   *   <li>Multiple key-value pair: <code>{key1: value1, key2: value2, ...}</code>
   *   <li>Embedded: <code>{key: {key: value}}</code>
   *   <li>...
   * </ul>
   *
   * @return this document's textual representation
   */
  @Override
  String toString();

  /**
   * Not supported.
   *
   * @param key not supported
   * @return not supported
   * @throws UnsupportedOperationException on every invocation of this method
   */
  @Override
  @Nullable Object remove(Object key);

  /**
   * Not supported.
   *
   * @throws UnsupportedOperationException on every invocation of this method
   */
  @Override
  void clear();

  /**
   * Not supported.
   *
   * @param map not supported
   * @throws UnsupportedOperationException on every invocation of this method
   */
  @Override
  void putAll(Map<? extends String, ? extends Object> map);

  /**
   * {@linkplain BsonDocument Document} builder.
   *
   * <p><b>Notes:</b>
   *
   * <ul>
   *   <li>Calling a builder's {@linkplain #build} method <em>does not clear its state</em>, however
   *       subsequent invocations of it returns new immutable documents.
   *   <li>Document builders can be acquired via the {@linkplain BsonDocuments documents utility
   *       class}.
   * </ul>
   */
  interface Builder {

    /**
     * Adds {@code key} and its associated {@code value} to the document being built.
     *
     * @param key the key to be added to the document being built
     * @param value the value associated with {@code key}
     * @return this builder
     * @throws NullPointerException if {@code key} is null
     * @throws IllegalArgumentException if {@code key} is already present
     */
    Builder put(String key, @Nullable Object value);

    /**
     * Adds {@code map}'s key-value pairs to the document being built.
     *
     * @param map the map whose key-value pairs are to be added to the document being built
     * @return this builder
     * @throws NullPointerException if {@code map} or any of its keys are null
     * @throws IllegalArgumentException if {@code map} contains keys that are already present
     */
    Builder putAll(Map<String, Object> map);

    /**
     * A patched method to allow same key with multiple values (MULTIVAL), this should only be used
     * for reading
     *
     * @param key
     * @param value
     * @return
     */
    Builder putAllowMultiVal(String key, @Nullable Object value);

    /**
     * Returns a new document using the contents of this builder.
     *
     * @return a new document using the contents of this builder
     */
    BsonDocument build();
  }
}
