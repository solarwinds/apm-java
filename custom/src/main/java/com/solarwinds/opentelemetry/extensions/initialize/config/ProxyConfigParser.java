package com.solarwinds.opentelemetry.extensions.initialize.config;

import com.solarwinds.joboe.core.config.ConfigParser;
import com.solarwinds.joboe.core.config.InvalidConfigException;
import com.solarwinds.joboe.core.config.ProxyConfig;
import java.net.MalformedURLException;
import java.net.URL;

public class ProxyConfigParser implements ConfigParser<String, ProxyConfig> {
  public static final ProxyConfigParser INSTANCE = new ProxyConfigParser();

  private ProxyConfigParser() {}

  @Override
  public ProxyConfig convert(String proxyString) throws InvalidConfigException {
    URL proxyUrl;
    try {
      proxyUrl = new URL(proxyString);
    } catch (MalformedURLException e) {
      throw new InvalidConfigException(
          "Failed to parse proxy string value ["
              + proxyString
              + "] Error message is ["
              + e.getMessage()
              + "]",
          e);
    }

    // only support http:// for now
    if (!"http".equals(proxyUrl.getProtocol())) {
      throw new InvalidConfigException(
          "Failed to parse proxy string value ["
              + proxyString
              + "]. Unsupported protocol ["
              + proxyUrl.getProtocol()
              + "]. Currently support only proxying via a HTTP proxy server");
    }

    if (proxyUrl.getPort() == -1) {
      throw new InvalidConfigException(
          "Failed to parse proxy string value [" + proxyString + "]. Missing/Invalid port number.");
    }

    String userInfo = proxyUrl.getUserInfo();
    if (userInfo != null) {
      String[] tokens = userInfo.split(":", 2);
      if (tokens.length != 2) {
        throw new InvalidConfigException(
            "Failed to parse proxy config's user/password ["
                + userInfo
                + "], it should be in username:password format");
      }

      return new ProxyConfig(proxyUrl.getHost(), proxyUrl.getPort(), tokens[0], tokens[1]);
    } else {
      return new ProxyConfig(proxyUrl.getHost(), proxyUrl.getPort(), null, null);
    }
  }
}
