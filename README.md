# Oauth2 Server using Play!

Also have a look at OAuth2 client apps in sample-apps/ folder.

## Requirements

This project requires following tools

 * JDK7+
 * SBT 0.13+

## Build

Run tests

    $ sbt test


Create a distribution

    $ sbt dist


## Test Coverage

SBT Scoverage plugin generates the test coverage:

    $ sbt scoverage:test


The report is generated in XML and HTML format at `target/scala-2.10/scoverage-report/`

    $ cd target/scala-2.10/scoverage-report/
    $ firefox /index.html

## Deployment

[Read here](http://www.playframework.com/documentation/2.0/Production)

