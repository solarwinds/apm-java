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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TokenBucketTest {
  @Test
  public void testCapacity1() throws Exception {

    TokenBucket bucket = new TokenBucket(100.0, 0.0);

    for (int i = 0; i < 100; i++) {
      assertTrue(bucket.consume());
    }
    assertFalse(bucket.consume());
  }

  @Test
  public void testCapacity2() {
    // default capacity
    TokenBucket bucket = new TokenBucket(0, 0);

    assertFalse(bucket.consume());
  }

  @Test
  public void testReset1() {
    TokenBucket bucket = new TokenBucket(0.0, 0.0);

    assertFalse(bucket.consume());

    // now set it to higher capacity, it should not affect available tokens
    bucket.setCapacity(100);

    assertFalse(bucket.consume());
  }

  @Test
  public void testReset2() {
    TokenBucket bucket = new TokenBucket(100.0, 0.0);

    assertTrue(bucket.consume());

    // now set it to lower capacity, the existing available token should be lowered accordingly
    bucket.setCapacity(0);

    assertFalse(bucket.consume());
  }

  @Test
  public void testReset3() {
    TokenBucket bucket = new TokenBucket(1.0, 0.0);

    assertTrue(bucket.consume());

    bucket.setCapacity(1.0); // same capacity, does not affect the available tokens

    assertFalse(bucket.consume());
  }

  @Test
  public void testRate1() throws InterruptedException {
    TokenBucket bucket = new TokenBucket(1000.0, 1.0);

    final int maxTry =
        2000; // avoid infinity loop if there's any problem on the TokenBucket instrumentation
    int counter = 0;
    while (bucket.consume() && counter < maxTry) { // should quickly deplete the available tokens
      counter++;
    }

    if (counter
        == maxTry) { // should not reach this unless TokenBucket is not functioning correctly
      fail("TokenBucket is expected to be exhausted but it was not!");
    }

    // now wait for a while for the bucket to be replenished
    Thread.sleep(1500);
    assertTrue(bucket.consume());
  }

  @Test
  public void testRate2() throws InterruptedException {
    TokenBucket bucket = new TokenBucket(10.0, 1000.0);

    final int maxTry = 1000;
    int counter = 0;
    while (bucket.consume() && counter < maxTry) {
      counter++;
      Thread.sleep(2); // replenish rate is faster than deplete rate
    }

    if (counter < maxTry) { // should reach this unless TokenBucket is not functioning correctly
      fail(
          "TokenBucket is NOT expected to be exhausted but it actually ran out of available token!");
    }
  }

  @Test
  public void testRate3() throws InterruptedException {
    TokenBucket bucket = new TokenBucket(10.0, 10.0);

    for (int i = 0; i < 100; i++) {
      assertTrue(bucket.consume());
      Thread.sleep(100); // roughly 1000/100 = 10 request per second which is the same as rate
    }
  }

  @Test
  public void testConsume() {
    TokenBucket bucket = new TokenBucket(3.00, 0.00);

    assertTrue(bucket.consume()); // OK, 2 left
    assertFalse(bucket.consume(3)); // only 2 left, not enough token
    assertTrue(bucket.consume(2)); // OK, 0 left
    assertFalse(bucket.consume()); // none left
  }
}
