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

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SslUtils {
  private static final Logger logger = LoggerFactory.getLogger();

  private SslUtils() {}

  public static TrustManagerFactory getTrustManagerFactory(URL serverCertLocation)
      throws IOException, GeneralSecurityException {
    // Trusted keystore manager => so this client can trust the collector!
    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    InputStream caInput = serverCertLocation.openStream();

    Certificate certificate;
    try {
      certificate = certificateFactory.generateCertificate(caInput);
    } finally {
      caInput.close();
    }

    // trusted keystore
    KeyStore trustedKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());

    trustedKeyStore.load(null, null);
    trustedKeyStore.setCertificateEntry("ca", certificate);

    // Create a TrustManager that trusts the CAs in our KeyStore
    String algorithm = TrustManagerFactory.getDefaultAlgorithm();
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(algorithm);
    trustManagerFactory.init(trustedKeyStore);

    // end Trusted keystore manager
    return trustManagerFactory;
  }

  /**
   * Wrap the input trustManagers into a single manager, which simply iterate through the input
   * managers and check whether the cert presented has host name matches the `host` input parameter.
   *
   * <p>This is necessary for either java 6- that does not support
   * `SslParameters#setEndpointIdentificationAlgorithm("HTTPS")` or when there's cert host name
   * override
   *
   * @param trustManagers
   * @param host
   * @return an array of one manager, which wraps all the input trust managers
   */
  private static TrustManager[] explicitHostCheckManager(
      final TrustManager[] trustManagers, final String host) {
    X509TrustManager manager =
        new X509TrustManager() {
          @Override
          public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
              throws CertificateException {
            for (TrustManager trustManager : trustManagers) {
              ((X509TrustManager) trustManager).checkClientTrusted(x509Certificates, s);
            }
          }

          @Override
          public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
              throws CertificateException {
            for (TrustManager trustManager : trustManagers) {
              ((X509TrustManager) trustManager).checkServerTrusted(x509Certificates, s);
            }

            boolean hostnameMatch = false;
            for (X509Certificate x509Certificate : x509Certificates) {
              Collection<List<?>> alternativeNameCollection =
                  x509Certificate.getSubjectAlternativeNames();
              if (alternativeNameCollection != null) {
                for (List alternativeNames : alternativeNameCollection) {
                  if (alternativeNames.get(1).equals(host)) {
                    hostnameMatch = true;
                    break;
                  }
                }
              }
            }
            if (!hostnameMatch) {
              throw new CertificateException(
                  "Certificate hostname and requested hostname don't match");
            }
          }

          @Override
          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }
        };
    return new TrustManager[] {manager};
  }

  public static SSLContext getSslContext(URL serverCertLocation)
      throws IOException, GeneralSecurityException {
    return getSslContext(serverCertLocation, null);
  }

  public static SSLContext getSslContext(URL serverCertLocation, String explicitHostCheck)
      throws IOException, GeneralSecurityException {

    // Create an SSLContext that uses our TrustManager
    TrustManagerFactory factory = getTrustManagerFactory(serverCertLocation);
    TrustManager[] managers = factory.getTrustManagers();
    if (explicitHostCheck != null) {
      managers = explicitHostCheckManager(managers, explicitHostCheck);
    }

    return getSslContext(managers);
  }

  /**
   * Obtain a SSLContext, if the default of the JVM is TLSv1, it will try to see whether there's
   * support for TLSv1.2 or TLSv1.1 and return SSLContext with the newer version For JVM default
   * that is NOT TLSv1, it will just return SSLContext with the default SSL version
   *
   * @return SSLContext that prefers protocol version higher than TLSv1
   * @throws GeneralSecurityException
   */
  private static SSLContext getSslContext(TrustManager[] trustManagers)
      throws GeneralSecurityException {
    SSLContext context = SSLContext.getInstance("TLS");
    context.init(null, trustManagers, null);

    try {
      if (isDefaultTLSv1(context)) { // we should try higher version
        List<String> supportedProtocols =
            Arrays.asList(context.getSupportedSSLParameters().getProtocols());
        if (supportedProtocols.contains("TLSv1.2")) {
          context = SSLContext.getInstance("TLSv1.2");
          context.init(null, trustManagers, null);
          return context;
        } else if (supportedProtocols.contains("TLSv1.1")) {
          context = SSLContext.getInstance("TLSv1.1");
          context.init(null, trustManagers, null);
          return context;
        } else {
          logger.warn("SSL default protocol is TLSv1 and no TLSv1.1 nor TLSv1.2 support found");
          return context;
        }
      } else { // the default is NOT TLSv1. Just use whatever the default is
        return context;
      }
    } catch (NoSuchMethodError e) {
      logger.warn("Cannot check SSL protocol version as it's running JDK 1.6-");
      return context;
    }
  }

  /** Returns whether the only enabled SSL protocol is TLSv1 */
  private static boolean isDefaultTLSv1(SSLContext context) {
    List<String> enabledProtocols =
        new ArrayList<String>(
            Arrays.asList(
                context.getDefaultSSLParameters().getProtocols())); // the list has to be mutable

    // need to filter out protocols such as SSLv2Hello, SSLv3 etc
    enabledProtocols.removeIf(protocol -> !protocol.startsWith("TLSv"));
    return enabledProtocols.size() == 1 && "TLSv1".equals(enabledProtocols.get(0));
  }
}
