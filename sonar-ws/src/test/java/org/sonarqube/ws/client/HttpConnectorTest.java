/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarqube.ws.client;

import com.squareup.okhttp.ConnectionSpec;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import java.io.File;
import java.util.List;
import javax.net.ssl.SSLSocketFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.ws.MediaTypes;

import static com.squareup.okhttp.Credentials.basic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpConnectorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  HttpConnector.JavaVersion javaVersion = mock(HttpConnector.JavaVersion.class);
  MockWebServer server;
  String serverUrl;

  @Before
  public void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    serverUrl = server.url("").url().toString();
  }

  @Test
  public void test_default_settings() throws Exception {
    answerHelloWorld();
    HttpConnector underTest = new HttpConnector.Builder().url(serverUrl).build();
    assertThat(underTest.baseUrl()).isEqualTo(serverUrl);
    GetRequest request = new GetRequest("api/issues/search").setMediaType(MediaTypes.PROTOBUF);
    WsResponse response = underTest.call(request);

    // verify default timeouts on client
    assertThat(underTest.okHttpClient().getConnectTimeout()).isEqualTo(HttpConnector.DEFAULT_CONNECT_TIMEOUT_MILLISECONDS);
    assertThat(underTest.okHttpClient().getReadTimeout()).isEqualTo(HttpConnector.DEFAULT_READ_TIMEOUT_MILLISECONDS);

    // verify response
    assertThat(response.hasContent()).isTrue();
    assertThat(response.content()).isEqualTo("hello, world!");

    // verify the request received by server
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod()).isEqualTo("GET");
    assertThat(recordedRequest.getPath()).isEqualTo("/api/issues/search");
    assertThat(recordedRequest.getHeader("Accept")).isEqualTo(MediaTypes.PROTOBUF);
    assertThat(recordedRequest.getHeader("Accept-Charset")).isEqualTo("UTF-8");
    assertThat(recordedRequest.getHeader("User-Agent")).startsWith("okhttp/");
    // compression is handled by OkHttp
    assertThat(recordedRequest.getHeader("Accept-Encoding")).isEqualTo("gzip");
  }

  @Test
  public void use_basic_authentication() throws Exception {
    answerHelloWorld();
    HttpConnector underTest = new HttpConnector.Builder()
      .url(serverUrl)
      .credentials("theLogin", "thePassword")
      .build();

    GetRequest request = new GetRequest("api/issues/search");
    underTest.call(request);

    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getHeader("Authorization")).isEqualTo(basic("theLogin", "thePassword"));
  }

  @Test
  public void use_basic_authentication_with_null_password() throws Exception {
    answerHelloWorld();
    HttpConnector underTest = new HttpConnector.Builder()
      .url(serverUrl)
      .credentials("theLogin", null)
      .build();

    GetRequest request = new GetRequest("api/issues/search");
    underTest.call(request);

    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getHeader("Authorization")).isEqualTo(basic("theLogin", ""));
  }

  /**
   * Access token replaces the couple {login,password} and is sent through
   * the login field
   */
  @Test
  public void use_access_token() throws Exception {
    answerHelloWorld();
    HttpConnector underTest = new HttpConnector.Builder()
      .url(serverUrl)
      .token("theToken")
      .build();

    GetRequest request = new GetRequest("api/issues/search");
    underTest.call(request);

    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getHeader("Authorization")).isEqualTo(basic("theToken", ""));
  }

  @Test
  public void use_proxy_authentication() throws Exception {
    answerHelloWorld();
    HttpConnector underTest = new HttpConnector.Builder()
      .url(serverUrl)
      .proxyCredentials("theProxyLogin", "theProxyPassword")
      .build();

    GetRequest request = new GetRequest("api/issues/search");
    underTest.call(request);

    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getHeader("Proxy-Authorization")).isEqualTo(basic("theProxyLogin", "theProxyPassword"));
  }

  @Test
  public void override_timeouts() {
    HttpConnector underTest = new HttpConnector.Builder()
      .url(serverUrl)
      .readTimeoutMilliseconds(42)
      .connectTimeoutMilliseconds(74)
      .build();

    assertThat(underTest.okHttpClient().getReadTimeout()).isEqualTo(42);
    assertThat(underTest.okHttpClient().getConnectTimeout()).isEqualTo(74);
  }

  @Test
  public void send_user_agent() throws Exception {
    answerHelloWorld();
    HttpConnector underTest = new HttpConnector.Builder()
      .url(serverUrl)
      .userAgent("Maven Plugin/2.3")
      .build();

    underTest.call(new GetRequest("api/issues/search"));

    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getHeader("User-Agent")).isEqualTo("Maven Plugin/2.3");
  }

  @Test
  public void fail_if_unknown_implementation_of_request() {
    HttpConnector underTest = new HttpConnector.Builder().url(serverUrl).build();
    try {
      underTest.call(mock(WsRequest.class));
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageContaining("Unsupported implementation: ");
    }
  }

  @Test
  public void send_post_request() throws Exception {
    answerHelloWorld();
    PostRequest request = new PostRequest("api/issues/search")
      .setParam("severity", "MAJOR")
      .setMediaType(MediaTypes.PROTOBUF);

    HttpConnector underTest = new HttpConnector.Builder().url(serverUrl).build();
    WsResponse response = underTest.call(request);

    // verify response
    assertThat(response.hasContent()).isTrue();
    assertThat(response.content()).isEqualTo("hello, world!");

    // verify the request received by server
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    assertThat(recordedRequest.getPath()).isEqualTo("/api/issues/search?severity=MAJOR");
  }

  @Test
  public void upload_file() throws Exception {
    answerHelloWorld();
    File reportFile = temp.newFile();
    FileUtils.write(reportFile, "the report content");
    PostRequest request = new PostRequest("api/report/upload")
      .setParam("project", "theKey")
      .setPart("report", new PostRequest.Part(MediaTypes.TXT, reportFile))
      .setMediaType(MediaTypes.PROTOBUF);

    HttpConnector underTest = new HttpConnector.Builder().url(serverUrl).build();
    WsResponse response = underTest.call(request);

    assertThat(response.hasContent()).isTrue();
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    assertThat(recordedRequest.getPath()).isEqualTo("/api/report/upload?project=theKey");
    String body = IOUtils.toString(recordedRequest.getBody().inputStream());
    assertThat(body)
      .contains("Content-Disposition: form-data; name=\"report\"")
      .contains("Content-Type: text/plain")
      .contains("the report content");
  }

  @Test
  public void http_error() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(404));
    PostRequest request = new PostRequest("api/issues/search");
    HttpConnector underTest = new HttpConnector.Builder().url(serverUrl).build();

    WsResponse wsResponse = underTest.call(request);
    assertThat(wsResponse.code()).isEqualTo(404);
  }

  @Test
  public void support_base_url_ending_with_slash() throws Exception {
    assertThat(serverUrl).endsWith("/");
    HttpConnector underTest = new HttpConnector.Builder().url(StringUtils.removeEnd(serverUrl, "/")).build();
    GetRequest request = new GetRequest("api/issues/search");

    answerHelloWorld();
    WsResponse response = underTest.call(request);

    assertThat(response.hasContent()).isTrue();
  }

  @Test
  public void support_base_url_with_context() {
    // just to be sure
    assertThat(serverUrl).endsWith("/");
    HttpConnector underTest = new HttpConnector.Builder().url(serverUrl + "sonar").build();

    GetRequest request = new GetRequest("api/issues/search");
    answerHelloWorld();
    assertThat(underTest.call(request).requestUrl()).isEqualTo(serverUrl + "sonar/api/issues/search");

    request = new GetRequest("/api/issues/search");
    answerHelloWorld();
    assertThat(underTest.call(request).requestUrl()).isEqualTo(serverUrl + "sonar/api/issues/search");
  }

  @Test
  public void support_tls_1_2_on_java7() {
    when(javaVersion.isJava7()).thenReturn(true);
    HttpConnector underTest = new HttpConnector.Builder().url(serverUrl).build(javaVersion);

    assertTlsAndClearTextSpecifications(underTest);
    // enable TLS 1.0, 1.1 and 1.2
    assertThat(underTest.okHttpClient().getSslSocketFactory()).isNotNull().isInstanceOf(Tls12Java7SocketFactory.class);
  }

  @Test
  public void support_tls_versions_of_java8() {
    when(javaVersion.isJava7()).thenReturn(false);
    HttpConnector underTest = new HttpConnector.Builder().url(serverUrl).build(javaVersion);

    assertTlsAndClearTextSpecifications(underTest);
    assertThat(underTest.okHttpClient().getSslSocketFactory()).isInstanceOf(SSLSocketFactory.getDefault().getClass());
  }

  private void assertTlsAndClearTextSpecifications(HttpConnector underTest) {
    List<ConnectionSpec> connectionSpecs = underTest.okHttpClient().getConnectionSpecs();
    assertThat(connectionSpecs).hasSize(2);

    // TLS. tlsVersions()==null means all TLS versions
    assertThat(connectionSpecs.get(0).tlsVersions()).isNull();
    assertThat(connectionSpecs.get(0).isTls()).isTrue();

    // HTTP
    assertThat(connectionSpecs.get(1).tlsVersions()).isNull();
    assertThat(connectionSpecs.get(1).isTls()).isFalse();
  }

  private void answerHelloWorld() {
    server.enqueue(new MockResponse().setBody("hello, world!"));
  }
}
