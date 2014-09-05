package com.flightstats.http;


import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class HttpTemplateTest {

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
        HttpTemplate httpTemplate = new HttpTemplate(httpClient, gson, buildRetryer());

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
        HttpTemplate httpTemplate = new HttpTemplate(httpClient, gson, buildRetryer());

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
        HttpTemplate httpTemplate = new HttpTemplate(httpClient, gson, buildRetryer());

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
        HttpTemplate httpTemplate = new HttpTemplate(httpClient, gson, null);

        // when/then
        httpTemplate.get("/some_uri", (String s) -> {
            return null;
        });
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
        HttpTemplate httpTemplate = new HttpTemplate(httpClient, gson, null);

        // when
        int statusCode = httpTemplate.get("/some_uri", mock(Consumer.class));

        assertEquals(400, statusCode);
    }
}
