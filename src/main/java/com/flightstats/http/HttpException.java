package com.flightstats.http;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.Delegate;

import static java.net.HttpURLConnection.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class HttpException extends RuntimeException {

    @Delegate
    Details details;

    public HttpException(Details details, Exception e) {
        super(e);
        this.details = details;
    }

    public HttpException(Details details) {
        this.details = details;
    }

    public static HttpException invalidRequest(String message) {
        return new HttpException(new Details(HTTP_BAD_REQUEST, message));
    }

    public static HttpException unprocessableEntity(String message) {
        return new HttpException(new Details(422, message));
    }

    public static HttpException conflict(String message) {
        return new HttpException(new Details(HTTP_CONFLICT, message));
    }

    public static HttpException notFound(String message) {
        return new HttpException(new Details(HTTP_NOT_FOUND, message));
    }

    public static HttpException invalidRequest(String message, Exception e) {
        return new HttpException(new Details(HTTP_BAD_REQUEST, message), e);
    }

    public static HttpException forbidden(String message) {
        return new HttpException(new Details(HTTP_FORBIDDEN, message));
    }

    public static HttpException preconditionFailed(String message) {
        return new HttpException(new Details(HTTP_PRECON_FAILED, message));
    }

    @Value
    public static class Details {
        int statusCode;
        String message;
    }
}