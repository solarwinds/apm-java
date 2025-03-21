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

package com.solarwinds.containers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

public class PostgresContainer {

  private static final Logger logger = LoggerFactory.getLogger(PostgresContainer.class);
  public static final String PASSWORD = "petclinic";
  public static final String USERNAME = "petclinic";
  public static final String DATABASE_NAME = "petclinic";

  private final Network network;

  public PostgresContainer(Network network) {
    this.network = network;
  }

  public PostgreSQLContainer<?> build() throws Exception {
    return new PostgreSQLContainer<>("postgres:9.6.22")
        .withNetwork(network)
        .withNetworkAliases("postgres")
        .withLogConsumer(new Slf4jLogConsumer(logger))
        .withUsername(USERNAME)
        .withPassword(PASSWORD)
        .withDatabaseName(DATABASE_NAME)
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("initDB.sql"),
            "/docker-entrypoint-initdb.d/initDB.sql")
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("populateDB.sql"),
            "/docker-entrypoint-initdb.d/populateDB.sql")
        .withReuse(false);
  }
}
