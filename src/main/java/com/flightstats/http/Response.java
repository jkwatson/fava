package com.flightstats.http;


import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import lombok.Value;

import java.nio.charset.Charset;
import java.util.Collection;

@Value
public class Response {
    int code;
    byte[] body;
    Multimap<String, String> headers;

    public String getHeader(String location) {
        Collection<String> headers = this.headers.get(location);
        return headers == null ? null : Iterables.getFirst(headers, null);
    }

    public Collection<String> getHeaders(String location) {
        return headers.get(location);
    }

    public String getBodyString() {
        return getBodyString(Charset.defaultCharset());
    }

    public String getBodyString(Charset charset) {
        return new String(body, charset);
    }
}