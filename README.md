LeVar
=====

Machine learning training and evaluation database.

Getting started
---------------

Start the SBT console:

    $ ./sbt
    >

## Developing the web application locally

You'll need to install Java 7 and Postgres. By default, the web application
expects a local Postgres DB named "levar". To create that:

    $ psql -c 'create database levar;'

We're using Play 2.3. Because we're using a multi-project build, you need
to load the `web` project explicitly in the SBT console.

To run locally:

    $ ./sbt
    > project web
    [levar-web] $ run
