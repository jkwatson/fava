Fava
====

NOTE: ***This project is no longer maintained***. The functionality within this small library is available from other better-maintained sources that we've switched to using. We are unlikely to respond to PRs, issues, or questions, and will likely delete this repository at some point.

====

Fava is Flightstats' contribution back to the open source java community. It's a bit of a kitchen sink, but it's a kitchen sink that we find to be generally useful.

General dependencies:
* java 8
* lombok
* commons-http
* commons-mime
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
            <version>0.2.0</version>
        </dependency>
```

Or with gradle:
```
        'com.flightstats:fava:0.2.0'
```
