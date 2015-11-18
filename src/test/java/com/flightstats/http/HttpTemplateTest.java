package com.flightstats.http;


import com.flightstats.util.Part;
import com.flightstats.util.UUIDGenerator;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class HttpTemplateTest {

    @Test(expected = IllegalStateException.class)
    public void testGsonRequiresCorrectContentType() throws Exception {
        final HttpTemplate httpTemplate = new HttpTemplate(mock(HttpClient.class), null, "*/*", "*/*");
        httpTemplate.get(URI.create("foo"), String.class);
    }

    @Test
    public void testNonDefaultPostContentTypeHeader() throws Exception {
        // given
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        HttpEntity entity = new StringEntity("bar", "UTF-8");

        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(entity);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[] { new BasicHeader("Content-Type", "text/plain") });

        AtomicReference<HttpPost> post = new AtomicReference<>();

        when(httpClient.execute(any(HttpPost.class)))
                .thenAnswer( (a) -> {
                    post.set((HttpPost) a.getArguments()[0]);
                    return httpResponse;
                });

        HttpTemplate testClass = new HttpTemplate(httpClient, dummyRetryer(), "application/json", "application/json");
        testClass.post(URI.create("foo"), "foo".getBytes(), "*/*");

        Header[] contentType = post.get().getHeaders("Content-Type");
        assertEquals(1, contentType.length);
        assertEquals("Content-Type: */*", contentType[0].toString());
    }


    @Test
    public void testPostWithResponse_successOnSingleAttempt() throws Exception {
        // given
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse httpResponse = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        StatusLine statusLine = mock(StatusLine.class);

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity().getContent()).thenReturn(new ByteArrayInputStream("hello".getBytes()));
        when(statusLine.getStatusCode()).thenReturn(201);

        Gson gson = new GsonBuilder().create();
        HttpTemplate httpTemplate = new HttpTemplate(httpClient, gson, buildRetryer(), null);

        // when
        Response result = httpTemplate.postWithResponse("/some/uri", new Object(), mock(Consumer.class));

        // then
        assertEquals(201, result.getCode());
        verify(httpClient).execute(any(HttpPost.class));
    }

    private Retryer<Response> buildRetryer() {
        return RetryerBuilder.<Response>newBuilder()
                .retryIfExceptionOfType(HttpException.class)
                .withStopStrategy(StopStrategies.stopAfterAttempt(5))
                .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.MILLISECONDS))
                .build();
    }

    @Test
    public void testPostWithResponse_successfulRetryAfterSomeErrors() throws Exception {
        // given
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse httpResponse = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        StatusLine statusLine = mock(StatusLine.class);

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity().getContent()).thenReturn(new ByteArrayInputStream("hello".getBytes()));
        when(statusLine.getStatusCode()).thenReturn(502).thenReturn(503).thenReturn(201);

        Gson gson = new GsonBuilder().create();
        HttpTemplate httpTemplate = new HttpTemplate(httpClient, gson, buildRetryer(), null);

        // when
        Response result = httpTemplate.postWithResponse("/some/uri", new Object(), mock(Consumer.class));

        // then
        assertEquals(201, result.getCode());
        verify(httpClient, times(3)).execute(any(HttpPost.class));
    }

    @Test(expected = HttpException.class)
    public void testPostWithResponse_unsuccessful400WithNoRetrying() throws Exception {
        // given
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);

        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(400);

        HttpEntity httpEntity = mock(HttpEntity.class);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("some output".getBytes("UTF-8")));

        Gson gson = new GsonBuilder().create();
        HttpTemplate httpTemplate = new HttpTemplate(httpClient, gson, buildRetryer(), null);

        // when
        httpTemplate.postWithResponse("/some_uri", new Object(), mock(Consumer.class));

        // then exception
    }

    @Test(expected = HttpException.class)
    public void testGetWithResponse_failedAsException() throws Exception {
        // given
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);

        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(400);

        HttpEntity httpEntity = mock(HttpEntity.class);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("some output".getBytes("UTF-8")));

        Gson gson = new GsonBuilder().create();
        HttpTemplate testClass = new HttpTemplate(httpClient, gson, dummyRetryer(), null);

        // when/then
        testClass.get("/some_uri", (String s) -> null);
    }

    @Test
    public void testGetWithResponse_failedStatusCode() throws Exception {
        // given
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);

        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(400);

        HttpEntity httpEntity = mock(HttpEntity.class);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("some output".getBytes("UTF-8")));

        Gson gson = new GsonBuilder().create();
        HttpTemplate testClass = new HttpTemplate(httpClient, gson, dummyRetryer(), null);

        // when
        int statusCode = testClass.get("/some_uri", mock(Consumer.class));

        assertEquals(400, statusCode);
    }

    @Test
    public void testDelete() throws Exception {
        //GIVEN
        String body = "any old body";
        Multimap<String, String> expectedHeaders = LinkedListMultimap.create();
        expectedHeaders.put("foo", "bar");
        expectedHeaders.put("bar", "baz");

        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse httpResponse = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        HttpEntity entity = mock(HttpEntity.class);

        when(httpClient.execute(isA(HttpDelete.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes()));
        when(httpResponse.getEntity()).thenReturn(entity);
        when(httpResponse.getAllHeaders()).thenReturn(new Header[]{new BasicHeader("foo", "bar"), new BasicHeader("bar", "baz")});

        HttpTemplate testClass = new HttpTemplate(httpClient, null, dummyRetryer(), null);

        //WHEN
        Response result = testClass.delete(URI.create("http://lmgtfy.com"));

        //THEN
        assertEquals(new Response(200, body.getBytes(), expectedHeaders), result);
    }

    private Retryer<Response> dummyRetryer() {
        return new Retryer<>(StopStrategies.neverStop(), WaitStrategies.noWait(), a -> false);
    }

    @Test
    public void testGetWithExtraHeaders() throws Exception {
        //GIVEN
        String body = "result body text";
        Response expected = new Response(200, body.getBytes(), LinkedListMultimap.create());

        HttpResponse response = mock(HttpResponse.class, RETURNS_DEEP_STUBS);
        HttpClient httpClient = mock(HttpClient.class);
        StatusLine statusLine = mock(StatusLine.class);

        AtomicReference<HttpRequest> seenRequest = new AtomicReference<>();
        when(httpClient.execute(isA(HttpGet.class))).thenAnswer(invocation -> {
            HttpRequest httpRequest = (HttpRequest) invocation.getArguments()[0];
            seenRequest.set(httpRequest);
            return response;
        });
        when(response.getAllHeaders()).thenReturn(new Header[0]);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity().getContent()).thenReturn(new ByteArrayInputStream(body.getBytes()));
        when(statusLine.getStatusCode()).thenReturn(200);

        HttpTemplate testClass = new HttpTemplate(httpClient, null, dummyRetryer(), null);

        //WHEN
        URI uri = URI.create("http://service.com/ftw");
        Map<String, String> headers = new HashMap<>();
        headers.put("foo", "bar");
        Response result = testClass.get(uri, x -> {
        }, headers);

        //THEN
        assertEquals(expected, result);
        assertEquals("bar", seenRequest.get().getFirstHeader("foo").getValue());
        assertEquals("application/json", seenRequest.get().getFirstHeader("Accept").getValue());
    }

    @Test
    public void testPostWithExtraHeaders() throws Exception {
        //GIVEN
        Response expected = new Response(200, "body response".getBytes(), ArrayListMultimap.create());
        URI uri = URI.create("http://the-post-target.com");
        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("SOMETHING", "I'm extra");

        HttpClient client = mock(HttpClient.class);
        HttpResponse httpResponse = mock(HttpResponse.class, RETURNS_DEEP_STUBS);

        AtomicReference<HttpPost> seenPost = new AtomicReference<>();
        when(client.execute(any(HttpPost.class))).thenAnswer(invocation -> {
            seenPost.set((HttpPost) invocation.getArguments()[0]);
            return httpResponse;
        });
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.getEntity().getContent()).thenReturn(new ByteArrayInputStream(expected.getBody()));
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);

        HttpTemplate testClass = new HttpTemplate(client, null, dummyRetryer(), null);

        //WHEN
        Response result = testClass.post(uri, "body message".getBytes(), "text/plain", extraHeaders);

        //THEN
        assertEquals(expected, result);
        assertEquals("I'm extra", seenPost.get().getFirstHeader("SOMETHING").getValue());
    }

    @Test
    public void testMultipartPost() throws Exception {
        //GIVEN
        String uri = "http://it.will.work.com";
        String separator = "I_Separate";
        Part firstPart = new Part("firstPart", ContentType.TEXT_PLAIN.getMimeType(), "This is the firstPart");
        Part secondPart = new Part("secondPart", ContentType.TEXT_PLAIN.getMimeType(), "This is the secondPart");
        List<Part> parts = Arrays.asList(firstPart, secondPart);

        Response expected = new Response(200, "it finished".getBytes(), ArrayListMultimap.create());

        UUIDGenerator uuidGenerator = mock(UUIDGenerator.class);
        HttpClient client = mock(HttpClient.class);
        HttpResponse httpResponse = mock(HttpResponse.class, RETURNS_DEEP_STUBS);

        AtomicReference<HttpPost> seenPost = new AtomicReference<>();
        when(client.execute(any(HttpPost.class))).thenAnswer(invocation -> {
            seenPost.set((HttpPost) invocation.getArguments()[0]);
            return httpResponse;
        });
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.getEntity().getContent()).thenReturn(new ByteArrayInputStream(expected.getBody()));
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);

        HttpTemplate testClass = new HttpTemplate(client, null, dummyRetryer(), uuidGenerator);

        //WHEN
        Response result = testClass.postMultipart(uri, parts, Optional.of(separator));

        //THEN
        assertEquals(expected, result);
        assertNotNull(seenPost.get());
        assertEquals("multipart/form-data; boundary=I_Separate", seenPost.get().getEntity().getContentType().getValue());
    }

    @Test
    public void testMultipartPost_defaultSeparator_defaultContentType() throws Exception {
        //GIVEN
        String uri = "http://it.will.still.work.com";
        Part firstPart = new Part("firstPart", ContentType.TEXT_PLAIN.getMimeType(), "This is the firstPart");
        Part secondPart = new Part("secondPart", ContentType.TEXT_PLAIN.getMimeType(), "This is the secondPart");
        List<Part> parts = Arrays.asList(firstPart, secondPart);

        Response expected = new Response(200, "it finished".getBytes(), ArrayListMultimap.create());

        UUIDGenerator uuidGenerator = mock(UUIDGenerator.class);
        HttpClient client = mock(HttpClient.class);
        HttpResponse httpResponse = mock(HttpResponse.class, RETURNS_DEEP_STUBS);

        when(uuidGenerator.generateUUID()).thenReturn(new UUID(43L, 42L));
        AtomicReference<HttpPost> seenPost = new AtomicReference<>();
        when(client.execute(any(HttpPost.class))).thenAnswer(invocation -> {
            seenPost.set((HttpPost) invocation.getArguments()[0]);
            return httpResponse;
        });
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.getEntity().getContent()).thenReturn(new ByteArrayInputStream(expected.getBody()));
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);

        HttpTemplate testClass = new HttpTemplate(client, null, dummyRetryer(), uuidGenerator);

        //WHEN
        Response result = testClass.postMultipart(uri, parts);

        //THEN
        assertEquals(expected, result);
        assertNotNull(seenPost.get());
        assertEquals("multipart/form-data; boundary=fava_00000000-0000-002b-0000-00000000002a", seenPost.get().getEntity().getContentType().getValue());
    }

    @Test
    public void testMultipartPost_setContent() throws Exception {
        //GIVEN
        String uri = "http://it.will.even.still.work.com";
        Part firstPart = new Part("firstPart", ContentType.TEXT_PLAIN.getMimeType(), "This is the firstPart");
        Part secondPart = new Part("secondPart", ContentType.TEXT_PLAIN.getMimeType(), "This is the secondPart");
        List<Part> parts = Arrays.asList(firstPart, secondPart);

        Response expected = new Response(200, "it finished".getBytes(), ArrayListMultimap.create());

        UUIDGenerator uuidGenerator = mock(UUIDGenerator.class);
        HttpClient client = mock(HttpClient.class);
        HttpResponse httpResponse = mock(HttpResponse.class, RETURNS_DEEP_STUBS);

        when(uuidGenerator.generateUUID()).thenReturn(new UUID(43L, 42L));
        AtomicReference<HttpPost> seenPost = new AtomicReference<>();
        when(client.execute(any(HttpPost.class))).thenAnswer(invocation -> {
            seenPost.set((HttpPost) invocation.getArguments()[0]);
            return httpResponse;
        });
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(httpResponse.getEntity().getContent()).thenReturn(new ByteArrayInputStream(expected.getBody()));
        when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);

        HttpTemplate testClass = new HttpTemplate(client, null, dummyRetryer(), uuidGenerator);

        //WHEN
        Response result = testClass.postMultipart(uri, parts, Optional.<String>empty(), Optional.of(HttpTemplate.MULTIPART_MIXED));

        //THEN
        assertEquals(expected, result);
        assertNotNull(seenPost.get());
        assertEquals("multipart/mixed; boundary=fava_00000000-0000-002b-0000-00000000002a; charset=UTF-8", seenPost.get().getFirstHeader("Content-type").getValue());
    }
}
