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

import static com.solarwinds.joboe.core.Constants.XTR_UDP_HOST;
import static com.solarwinds.joboe.core.Constants.XTR_UDP_PORT;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPReporter implements EventReporter {
  private static final Logger logger = LoggerFactory.getLogger();
  private DatagramSocket socket;

  private InetAddress addr;
  private int port;

  /**
   * Creates UDP reporter with default destination Host and Port.
   *
   * <p>WARNING: this is ONLY for internal usage and will be removed from public access later on In
   * order to build UDP reporter, please use {@link ReporterFactory} instead
   *
   * @throws IOException
   */
  public UDPReporter() throws IOException {
    this(XTR_UDP_HOST, XTR_UDP_PORT);
  }

  /**
   * Creates UDP reporter with provided destination Host and Port
   *
   * <p>WARNING: this is ONLY for internal usage and will be removed from public access later on In
   * order to build UDP reporter, please use {@link ReporterFactory} instead
   *
   * @param host
   * @param port
   * @throws IOException
   */
  public UDPReporter(String host, int port) throws IOException {
    this(host, port, null, null);
  }

  /**
   * Create UDP report with provided destination Host and Port. Also bind the socket to the local
   * address and port provided
   *
   * @param host
   * @param port
   * @param datagramLocalAddress
   * @param datagramLocalPort
   * @throws IOException
   */
  UDPReporter(String host, int port, String datagramLocalAddress, Integer datagramLocalPort)
      throws IOException {
    init(host, port, datagramLocalAddress, datagramLocalPort);
  }

  protected void init(String host, int port, String datagramLocalAddress, Integer datagramLocalPort)
      throws IOException {
    if (datagramLocalAddress != null && datagramLocalPort != null) {
      socket = new DatagramSocket(datagramLocalPort, InetAddress.getByName(datagramLocalAddress));
    } else {
      socket = new DatagramSocket();
    }
    addr = InetAddress.getByName(host);
    this.port = port;
  }

  @Override
  public void send(Event event) throws EventReporterException {
    byte[] buf;
    try {
      buf = event.toBytes();
    } catch (BsonBufferException e) {
      logger.error("Failed to get value from event : " + e.getMessage(), e);
      throw new EventReporterException(e.getMessage(), e);
    }
    DatagramPacket pkt = new DatagramPacket(buf, buf.length, addr, port);
    try {
      socket.send(pkt);
    } catch (IOException e) {
      throw new EventReporterException(e.getMessage(), e);
    }
  }

  @Override
  public EventReporterStats consumeStats() {
    return new EventReporterStats(0, 0, 0, 0, 0); // not implemented
  }

  @Override
  public void close() {}
}
