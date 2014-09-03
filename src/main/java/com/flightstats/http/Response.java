package com.flightstats.http;


import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import lombok.Value;

import java.util.Collection;

@Value
public class Response {
    int code;
    String body;
    Multimap<String, String> headers;

    public String getHeader(String location) {
        Collection<String> headers = this.headers.get(location);
        return headers == null ? null : Iterables.getFirst(headers, null);
    }

    public Collection<String> getHeaders(String location) {
        return headers.get(location);
    }
}