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

package com.solarwinds.joboe.core.rpc.grpc;

import static com.solarwinds.joboe.core.settings.SettingsUtil.transformToLocalSettings;
import static com.solarwinds.joboe.core.util.HostInfoUtils.getHostId;
import static com.solarwinds.joboe.core.util.ServerHostInfoReader.setIfNotNull;

import com.google.protobuf.ByteString;
import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.ProxyConfig;
import com.solarwinds.joboe.core.BsonBufferException;
import com.solarwinds.joboe.core.Event;
import com.solarwinds.joboe.core.HostId;
import com.solarwinds.joboe.core.rpc.ClientException;
import com.solarwinds.joboe.core.rpc.ClientFatalException;
import com.solarwinds.joboe.core.rpc.ClientRecoverableException;
import com.solarwinds.joboe.core.rpc.ProtocolClient;
import com.solarwinds.joboe.core.rpc.ProtocolClientFactory;
import com.solarwinds.joboe.core.rpc.Result;
import com.solarwinds.joboe.core.rpc.ResultCode;
import com.solarwinds.joboe.core.rpc.SettingsResult;
import com.solarwinds.joboe.core.util.BsonUtils;
import com.solarwinds.joboe.core.util.HostInfoUtils;
import com.solarwinds.joboe.core.util.SslUtils;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.trace.ingestion.proto.Collector;
import com.solarwinds.trace.ingestion.proto.TraceCollectorGrpc;
import io.grpc.HttpConnectProxiedSocketAddress;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

/**
 * Collector protocol client that uses the gRPC protocol.
 *
 * <p>Wraps the gRPC client generated from the collector.proto definition
 */
public class GrpcClient implements ProtocolClient {
  private static final Logger logger = LoggerFactory.getLogger();
  private final TraceCollectorGrpc.TraceCollectorBlockingStub client;
  private final GrpcHostIdManager hostIdManager = new GrpcHostIdManager();

  private static final int INITIAL_MESSAGE_SIZE = 64 * 1024; // 64 kB

  private final int deadlineSeconds;

  GrpcClient(TraceCollectorGrpc.TraceCollectorBlockingStub blockingStub) {
    this.client = blockingStub;
    Integer configuredDeadLine =
        (Integer) ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR_TIMEOUT);
    this.deadlineSeconds = configuredDeadLine == null ? 10 : configuredDeadLine;
  }

  @Override
  public void shutdown() {
    if (client != null && client.getChannel() instanceof ManagedChannel) {
      ((ManagedChannel) client.getChannel()).shutdown();
    }
  }

  @Override
  public Result doPostEvents(String serviceKey, List<Event> events) throws ClientException {
    return postInBatch(serviceKey, events, EVENT_SERIALIZER, POST_EVENTS_ACTION);
  }

  @Override
  public Result doPostMetrics(String serviceKey, List<Map<String, Object>> messages)
      throws ClientException {
    return postInBatch(serviceKey, messages, KEY_VALUE_MAP_SERIALIZER, POST_METRICS_ACTION);
  }

  @Override
  public Result doPostStatus(String serviceKey, List<Map<String, Object>> messages)
      throws ClientException {
    return postInBatch(serviceKey, messages, KEY_VALUE_MAP_SERIALIZER, POST_STATUS_ACTION);
  }

  @Override
  public SettingsResult doGetSettings(String serviceKey, String version) throws ClientException {
    // For getSettings call, we decided to fill in `hostname` only for `HostID` for consistency with
    // other agent implementation
    // Unlike other language agent, this change does not give any performance boost to java agent
    Collector.SettingsRequest request =
        Collector.SettingsRequest.newBuilder()
            .setApiKey(serviceKey)
            .setClientVersion(version)
            .setIdentity(hostIdManager.getHostnameOnlyHostID())
            .build();
    try {
      Collector.SettingsResult result =
          client.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS).getSettings(request);
      return transformToLocalSettings(result);

    } catch (StatusRuntimeException e) {
      throw new ClientRecoverableException(
          "gRPC Operation failed : [get settings] status [" + e.getStatus() + "]", e);
    }
  }

  @Override
  public void doPing(String serviceKey) throws ClientException {
    try {
      client.ping(Collector.PingRequest.newBuilder().setApiKey(serviceKey).build());
    } catch (StatusRuntimeException e) {
      throw new ClientRecoverableException(
          "gRPC Operation failed : [ping] status [" + e.getStatus() + "]", e);
    }
  }

  private <T> Result postInBatch(
      String serviceKey, List<T> items, Serializer<T> serializer, PostAction postAction)
      throws ClientException {
    List<List<ByteString>> itemsByCalls = new ArrayList<List<ByteString>>();
    List<ByteString> byteStrings = new ArrayList<ByteString>();
    itemsByCalls.add(byteStrings);

    long callSize = 0;
    for (T item : items) {
      try {
        ByteString itemByteString = serializer.serialize(item);
        int size = itemByteString.size();
        if (callSize + size > MAX_CALL_SIZE) {
          byteStrings = new ArrayList<ByteString>();
          itemsByCalls.add(byteStrings);
          callSize = size;
        } else {
          callSize += size;
        }
        byteStrings.add(itemByteString);

      } catch (BsonBufferException e) {
        logger.warn(
            "Failed to perform action ["
                + postAction.getDescription()
                + "] due to buffer exception : "
                + e.getMessage(),
            e);
        throw new ClientFatalException(e);
      }
    }

    Collector.MessageResult resultMessage = null;
    for (List<ByteString> itemsAsByteString : itemsByCalls) {
      Collector.HostID hostId = hostIdManager.getHostID();
      Collector.MessageRequest.Builder builder =
          Collector.MessageRequest.newBuilder()
              .setApiKey(serviceKey)
              .setIdentity(hostId)
              .setEncoding(Collector.EncodingType.BSON);
      logger.debug(
          postAction.getDescription()
              + " "
              + itemsAsByteString.size()
              + " item(s) using gRPC client hostId="
              + hostId);
      try {
        builder.addAllMessages(itemsAsByteString);
        resultMessage =
            postAction.post(
                client.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS), builder.build());
      } catch (StatusRuntimeException e) {
        if (e.getStatus().getCode() == Status.RESOURCE_EXHAUSTED.getCode()) {
          throw new ClientFatalException(
              "gRPC Operation failed : [post events] status ["
                  + e.getStatus()
                  + "]. This is not recoverable due to exhausted resource.",
              e);
        } else {
          throw new ClientRecoverableException(
              "gRPC Operation failed : [post events] status [" + e.getStatus() + "]", e);
        }
      }
    }

    if (resultMessage == null) {
      return new Result(null, null, null);
    }

    return new Result(
        ResultCode.valueOf(resultMessage.getResult().name()),
        resultMessage.getArg(),
        resultMessage.getWarning());
  }

  interface Serializer<T> {
    ByteString serialize(T item) throws BsonBufferException;
  }

  interface PostAction {
    Collector.MessageResult post(
        TraceCollectorGrpc.TraceCollectorBlockingStub client,
        Collector.MessageRequest messageRequest)
        throws StatusRuntimeException;

    String getDescription();
  }

  private static final Serializer<Map<String, Object>> KEY_VALUE_MAP_SERIALIZER =
      item ->
          ByteString.copyFrom(
              BsonUtils.convertMapToBson(item, INITIAL_MESSAGE_SIZE, MAX_MESSAGE_SIZE));

  private static final Serializer<Event> EVENT_SERIALIZER =
      event -> ByteString.copyFrom(event.toBytes());

  private static final PostAction POST_EVENTS_ACTION =
      new PostAction() {
        @Override
        public Collector.MessageResult post(
            TraceCollectorGrpc.TraceCollectorBlockingStub client,
            Collector.MessageRequest request) {
          return client.postEvents(request);
        }

        @Override
        public String getDescription() {
          return "Post Events";
        }
      };

  private static final PostAction POST_STATUS_ACTION =
      new PostAction() {
        @Override
        public Collector.MessageResult post(
            TraceCollectorGrpc.TraceCollectorBlockingStub client,
            Collector.MessageRequest request) {
          return client.postStatus(request);
        }

        @Override
        public String getDescription() {
          return "Post Status Message";
        }
      };

  private static final PostAction POST_METRICS_ACTION =
      new PostAction() {
        @Override
        public Collector.MessageResult post(
            TraceCollectorGrpc.TraceCollectorBlockingStub client,
            Collector.MessageRequest request) {
          return client.postMetrics(request);
        }

        @Override
        public String getDescription() {
          return "Post Metrics";
        }
      };

  static class GrpcProtocolClientFactory implements ProtocolClientFactory<ProtocolClient> {
    private final TrustManagerFactory trustManagerFactory;
    private final ProxyConfig proxyConfig =
        (ProxyConfig) ConfigManager.getConfig(ConfigProperty.AGENT_PROXY);
    private static final String compression;
    private static final String DEFAULT_COMPRESSION = "gzip";

    static {
      String compressionString =
          ((String) ConfigManager.getConfig(ConfigProperty.AGENT_GRPC_COMPRESSION));
      compression =
          compressionString != null ? compressionString.toLowerCase() : DEFAULT_COMPRESSION;
      logger.debug("Using compression " + compression + " for gRPC client");
    }

    GrpcProtocolClientFactory(URL serverCertLocation) throws IOException, GeneralSecurityException {
      TrustManagerFactory factory = null;
      if (serverCertLocation != null) {
        //                factory = InsecureTrustManagerFactory.INSTANCE; //for testing
        try {
          factory = SslUtils.getTrustManagerFactory(serverCertLocation);
        } catch (GeneralSecurityException e) {
          logger.warn(
              "Failed to initialize trust manager factory for GRPC client: " + e.getMessage(), e);
        } catch (IOException e) {
          logger.warn(
              "Failed to initialize trust manager factory for GRPC client: " + e.getMessage(), e);
        }
      }

      this.trustManagerFactory = factory;
    }

    @Override
    public ProtocolClient buildClient(String host, int port) throws ClientFatalException {
      // netty
      NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(host, port);

      if (proxyConfig != null) {
        channelBuilder =
            channelBuilder.proxyDetector(
                targetServerAddress -> {
                  HttpConnectProxiedSocketAddress.Builder builder =
                      HttpConnectProxiedSocketAddress.newBuilder()
                          .setProxyAddress(
                              new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort()))
                          .setTargetAddress((InetSocketAddress) targetServerAddress);
                  if (proxyConfig.getUsername() != null) {
                    builder = builder.setUsername(proxyConfig.getUsername());
                  }
                  if (proxyConfig.getPassword() != null) {
                    builder = builder.setPassword(proxyConfig.getPassword());
                  }
                  return builder.build();
                });
      }

      if (trustManagerFactory != null) {
        try {
          channelBuilder =
              channelBuilder.sslContext(
                  GrpcSslContexts.forClient().trustManager(trustManagerFactory).build());
        } catch (SSLException e) {
          throw new ClientFatalException("Failed to init client SSL");
        }
      }
      ManagedChannel channel = channelBuilder.build();

      TraceCollectorGrpc.TraceCollectorBlockingStub stub =
          TraceCollectorGrpc.newBlockingStub(channel);
      if (!"none".equals(compression)) {
        stub = stub.withCompression(compression);
      }

      return new GrpcClient(stub);
    }
  }

  private static class GrpcHostIdManager {
    private HostId localHostId;
    private Collector.HostID grpcHostID;
    private Collector.HostID grpcHostnameOnlyHostID;
    private String localHostname;

    private GrpcHostIdManager() {}

    private Collector.HostID getHostID() {
      HostId hostId = getHostId();
      boolean loadGrpcHostId;
      if (hostId == localHostId || hostId.equals(localHostId)) {
        loadGrpcHostId = grpcHostID == null;
      } else {
        localHostId = hostId;
        loadGrpcHostId = true;
      }

      if (loadGrpcHostId) {
        grpcHostID = toGrpcHostID(localHostId);
      }

      return grpcHostID;
    }

    private Collector.HostID getHostnameOnlyHostID() {
      String hostname = HostInfoUtils.getHostName();
      boolean loadGrpcHostId;
      if (hostname.equals(localHostname)) {
        loadGrpcHostId = grpcHostnameOnlyHostID == null;
      } else {
        localHostname = hostname;
        loadGrpcHostId = true;
      }

      if (loadGrpcHostId) {
        grpcHostnameOnlyHostID = toGrpcHostnameOnlyHostID(localHostname);
      }
      return grpcHostnameOnlyHostID;
    }

    private static Collector.HostID toGrpcHostID(HostId hostId) {
      Collector.HostID.Builder builder = Collector.HostID.newBuilder();
      setIfNotNull(builder::setHostname, hostId.getHostname());
      setIfNotNull(builder::setEc2InstanceID, hostId.getEc2InstanceId());

      setIfNotNull(builder::setEc2AvailabilityZone, hostId.getEc2AvailabilityZone());
      setIfNotNull(builder::setDockerContainerID, hostId.getDockerContainerId());
      setIfNotNull(builder::setHerokuDynoID, hostId.getHerokuDynoId());

      setIfNotNull(builder::setAzAppServiceInstanceID, hostId.getAzureAppServiceInstanceId());
      setIfNotNull(builder::setUamsClientID, hostId.getUamsClientId());
      setIfNotNull(builder::setUuid, hostId.getUuid());

      if (hostId.getAzureVmMetadata() != null) {
        builder.setAzureMetadata(hostId.getAzureVmMetadata().toGrpc());
      }

      if (hostId.getAwsMetadata() != null) {
        builder.setAwsMetadata(hostId.getAwsMetadata().toGrpc());
      }

      if (hostId.getK8sMetadata() != null) {
        builder.setK8SMetadata(hostId.getK8sMetadata().toGrpc());
      }

      return builder
          .setHostType(Collector.HostType.valueOf(hostId.getHostType().name()))
          .addAllMacAddresses(hostId.getMacAddresses())
          .setPid(hostId.getPid())
          .build();
    }

    /**
     * For getSettings call, we decided to fill in `hostname` only for `HostID` for consistency with
     * other agent implementation.
     *
     * <p>Unlike other language agent, this change does not give any performance boost to java
     * agent.
     *
     * @return
     */
    private static Collector.HostID toGrpcHostnameOnlyHostID(String hostname) {
      return Collector.HostID.newBuilder().setHostname(hostname).build();
    }
  }
}
