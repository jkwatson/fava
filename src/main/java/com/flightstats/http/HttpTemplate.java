package com.flightstats.http;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.flightstats.http.HttpException.Details;
import static java.util.stream.Collectors.toList;

@AllArgsConstructor
public class HttpTemplate {
    public static final String APPLICATION_JSON = "application/json";
    public static final Logger logger = LoggerFactory.getLogger(HttpTemplate.class);

    private final HttpClient client;
    private final Gson gson;
    private final Retryer<Response> retryer;
    private final String contentType;
    private final String acceptType;

    @Inject
    public HttpTemplate(HttpClient client, Gson gson, Retryer<Response> retryer) {
        this.client = client;
        this.gson = gson;
        this.retryer = retryer;
        this.contentType = APPLICATION_JSON;
        this.acceptType = APPLICATION_JSON;
    }

    public <T> T get(String hostUrl, String path, Function<String, T> responseCreator, NameValuePair... queryParams) {
        URI uri;
        try {
            uri = new URIBuilder(hostUrl).setPath(path).setParameters(queryParams).build();
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
        return get(uri, responseCreator);
    }

    public <T> T get(String uri, Function<String, T> responseCreator) {
        return get(URI.create(uri), responseCreator);
    }

    public <T> T get(URI uri, Function<String, T> responseCreator) {
        AtomicReference<T> result = new AtomicReference<>();
        get(uri, (Response response) -> {
            if (isFailedStatusCode(response.getCode())) {
                throw new HttpException(new Details(response.getCode(), "Post failed to: " + uri + ". response: " + response));
            }
            result.set(responseCreator.apply(response.getBodyString()));
        });
        return result.get();
    }

    public int get(String uri, Consumer<Response> responseConsumer) {
        return get(URI.create(uri), responseConsumer).getCode();
    }

    public Response get(URI uri) {
        return get(uri, (Response c) -> {
        });
    }

    public Response get(URI uri, Consumer<Response> responseConsumer) {
        return get(uri, responseConsumer, Collections.emptyMap());
    }

    public Response get(URI uri, Consumer<Response> responseConsumer, Map<String,String> extraHeaders) {
        HttpGet request = new HttpGet(uri);
        extraHeaders.entrySet().stream().forEach(entry -> request.setHeader(entry.getKey(), entry.getValue()));
        return handleRequest(request, responseConsumer);
    }

    public Response head(URI uri) {
        return head(uri, true);
    }

    public Response head(String uri) {
        return head(uri, true);
    }

    public Response head(String uri, boolean followRedirects) {
        return head(URI.create(uri), followRedirects);
    }

    public Response head(URI uri, boolean followRedirects) {
        HttpHead httpHead = new HttpHead(uri);
        if (!followRedirects) {
            httpHead.setConfig(RequestConfig.custom().setRedirectsEnabled(false).build());
        }
        return handleRequest(httpHead, x -> {
        });
    }

    public <T> T get(URI uri, Type type) {
        return get(uri, (String body) -> gson.fromJson(body, type));
    }

    public String getSimple(String uri) {
        return get(uri, (Function<String, String>) s -> s);
    }

    private Response handleRequest(HttpRequestBase request, Consumer<Response> responseConsumer) {
        try {
            HttpResponse httpResponse = client.execute(request);
            Response response = convertHttpResponse(httpResponse);
            responseConsumer.accept(response);
            return response;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            request.reset();
        }
    }

    private Response convertHttpResponse(HttpResponse httpResponse) throws IOException {
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        byte[] body = new byte[0];
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            InputStream content = entity.getContent();
            body = ByteStreams.toByteArray(content);
        }
        return new Response(statusCode, body, mapHeaders(httpResponse));
    }

    private Multimap<String, String> mapHeaders(HttpResponse response) {
        Header[] headers = response.getAllHeaders();
        ListMultimap<String, Header> headersByName = Multimaps.index(Arrays.asList(headers), Header::getName);
        return Multimaps.transformValues(headersByName, Header::getValue);
    }

    public int postWithNoResponseCodeValidation(String fullUri, Object bodyToPost, Consumer<Response> responseConsumer) {
        try {
            String bodyEntity = convertBodyToString(bodyToPost, contentType);
            Response response = retryer.call(() -> executePost(fullUri, responseConsumer, contentType, new StringEntity(bodyEntity, Charsets.UTF_8)));
            return response.getCode();
        } catch (ExecutionException | RetryException e) {
            throw Throwables.propagate(e);
        }
    }

    private Response executePost(String fullUri, String contentType, HttpEntity entity) throws Exception {
        return executePost(fullUri, x -> {
        }, contentType, entity);
    }

    private Response executePost(String fullUri, Consumer<Response> responseConsumer, String contentType, HttpEntity entity) throws Exception {
        HttpPost httpPost = new HttpPost(fullUri);
        return execute(httpPost, responseConsumer, contentType, entity);
    }

    private Response executePut(String fullUri, Consumer<Response> responseConsumer, String contentType, HttpEntity entity) throws Exception {
        HttpPut httpPut = new HttpPut(fullUri);
        return execute(httpPut, responseConsumer, contentType, entity);
    }

    private Response execute(HttpEntityEnclosingRequestBase httpRequest, String contentType, HttpEntity entity) throws IOException {
        return execute(httpRequest, x -> {
        }, contentType, entity);
    }

    private Response execute(HttpEntityEnclosingRequestBase httpRequest, Consumer<Response> responseConsumer, String contentType, HttpEntity entity) throws IOException {
        try {
            httpRequest.setHeader("Content-Type", contentType);
            httpRequest.setHeader("Accept", acceptType);
            httpRequest.setEntity(entity);
            HttpResponse httpResponse = client.execute(httpRequest);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            byte[] body = ByteStreams.toByteArray(httpResponse.getEntity().getContent());
            if (isRetryableStatusCode(statusCode)) {
                logger.error("Internal server error, status code " + statusCode);
                throw new HttpException(new Details(statusCode, httpRequest.getMethod() + " failed to: " + httpRequest.getURI() + ".  Status = " + statusCode + ", message = " + new String(body)));
            }
            Response response = new Response(statusCode, body, mapHeaders(httpResponse));
            responseConsumer.accept(response);
            return response;
        } finally {
            httpRequest.reset();
        }
    }


    private boolean isFailedStatusCode(int responseStatusCode) {
        return (HttpStatus.SC_OK > responseStatusCode) || (responseStatusCode > HttpStatus.SC_NO_CONTENT);
    }

    private boolean isRetryableStatusCode(int responseStatusCode) {
        return 502 <= responseStatusCode && responseStatusCode <= 504;
    }

    private String convertBodyToString(Object bodyToPost, String contentType) {
        switch (contentType) {
            case APPLICATION_JSON:
                return gson.toJson(bodyToPost);
            default:
                return String.valueOf(bodyToPost);
        }
    }

    public Response post(URI uri, byte[] bytes, String contentType) {
        try {
            return executePost(uri.toString(), contentType, new ByteArrayEntity(bytes));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Fire &amp; Forget...don't care about the response at all.
     */
    public void post(String fullUri, Object bodyToPost) {
        postWithResponse(fullUri, bodyToPost, httpResponse -> {
        });
    }

    /**
     * Post, and return the response body as a String.
     */
    public String postSimple(String fullUri, Object bodyToPost) {
        return post(fullUri, bodyToPost, s -> s);
    }

    public <T> T post(String fullUri, Object bodyToPost, Function<String, T> responseConverter) {
        AtomicReference<T> result = new AtomicReference<>();
        postWithResponse(fullUri, bodyToPost, (Response response) -> result.set(responseConverter.apply(response.getBodyString())));
        return result.get();
    }

    public Response put(URI uri, byte[] bytes, String contentType) {
        try {
            HttpPut httpPut = new HttpPut(uri.toString());
            return execute(httpPut, contentType, new ByteArrayEntity(bytes));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * todo: this needs a better name, but different than "post", so it doesn't collide with the other one. I hate type erasure!
     */
    public Response postWithResponse(String fullUri, Object bodyToPost, Consumer<Response> responseConsumer) {
        try {
            String bodyEntity = convertBodyToString(bodyToPost, contentType);
            Response response = retryer.call(() -> executePost(fullUri, responseConsumer, contentType, new StringEntity(bodyEntity, Charsets.UTF_8)));
            if (isFailedStatusCode(response.getCode())) {
                throw new HttpException(new Details(response.getCode(), "Post failed to: " + fullUri + ". response: " + response));
            }
            return response;
        } catch (ExecutionException | RetryException e) {
            throw Throwables.propagate(e);
        }
    }

    public int postSimpleHtmlForm(String fullUri, Map<String, String> formValues) throws Exception {
        List<NameValuePair> nameValuePairs = formValues.entrySet().stream()
                .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
                .collect(toList());
        HttpPost httpPost = new HttpPost(fullUri);
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = client.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (isFailedStatusCode(statusCode)) {
                throw new HttpException(new Details(statusCode, "Post failed to: " + fullUri));
            }
            return statusCode;
        } finally {
            httpPost.reset();
        }
    }

    public Response delete(URI uri) {
        HttpDelete delete = new HttpDelete(uri);
        try {
            try {
                HttpResponse response = client.execute(delete);
                return convertHttpResponse(response);
            } catch (IOException e) {
                throw new UncheckedIOException("Error issuing DELETE against " + uri, e);
            }
        } finally {
            delete.reset();
        }

    }
}
