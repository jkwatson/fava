Fava
====

Fava is Flightstats' contribution back to the open source java community. It's a bit of a kitchen sink, but it's a kitchen sink that we find to be generally useful.

General dependencies:
* java 8
* lombok
* commons-http
* guava
* guava-retrying
* gson
* amazon AWS client

Functionality:

* HttpTemplate - An easy to use wrapper on top of apache's HttpClient, which makes doing basic HTTP calls simple and painless.
* FileSystem - An abstraction on top of both a standard local filesystem, or S3, which makes using S3 as a file storage mechanism simple.


Available via maven central:
```
        <dependency>
            <groupId>com.flightstats</groupId>
            <artifactId>fava</artifactId>
            <version>0.1.9</version>
        </dependency>
```

Or with gradle:
```
        'com.flightstats:fava:0.1.9'
```
