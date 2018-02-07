# LeVar

[![Join the chat at https://gitter.im/levarml/LeVar](https://badges.gitter.im/levarml/LeVar.svg)](https://gitter.im/levarml/LeVar?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Machine learning training and evaluation database.

[![Build Status](https://travis-ci.org/levarml/LeVar.svg?branch=master)](https://travis-ci.org/levarml/LeVar)
[![Coverage Status](https://coveralls.io/repos/peoplepattern/LeVar/badge.svg?branch=master&service=github)](https://coveralls.io/github/peoplepattern/LeVar?branch=master)
[![Join the chat at https://gitter.im/peoplepattern/LeVar](https://badges.gitter.im/peoplepattern/LeVar.svg)](https://gitter.im/peoplepattern/LeVar?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## About

This is a database for storing and organizing your machine learning evaluation data.
It currently supports basic classification and regression datasets. You can store
your evaluation datasets, upload experiments against those datasets and view results.

Why would you need such a thing? A lot of data scientists do this: for *every* machine
learning project, you rewrite the same script to calculate precision, recall,
root-mean-squared-error, etc. You gather lots of good evaluation data, but then keep
it all of the place in `tmp` directories, lose track of what's where, and ultimately
it's a challenge to remember how your latest result relates to a benchmark you haven't
looked at in months. Moreover, even *if* you've responsibly kept a good lab notebook,
and you can see how your overall numbers compare on the same dataset across months,
you may not have the actual item-for-item predictions around to facilitate
comparative error analysis.

This project aims to solve all of those problems.

* It provides a database for storing evaluation datasets and experiments run against
  them, so that over time you can keep track of how your methods are doing against
  static, known data inputs.
* It provides a simple and clear CLI for uploading and downloading data to the DB,
  natively using the universal TSV format for data management.
* It provides a Scala API to the database, enabling programmatic access to datasets,
  to facilitate rapid model development, while preserving experimental results for
  the long term.
* Finally, the database is exposed as a RESTful Web service using JSON, so other
  applications can access it using standard Web service clients

### The name

LeVar is sort of, but not really, an amalgam of "Model Eval". Also, if you grew up
in North America in the 1980s, or even if you didn't, you should know that
[LeVar Burton](https://twitter.com/levarburton) is awesome for so many reasons.

## Develop, build and deploy

You'll need Java 7+ and [SBT](http://www.scala-sbt.org/) installed to develop this
project. The easiest way to install SBT is to use
[sbt-extras](https://github.com/paulp/sbt-extras). On a Mac or Unix system, do this:

    $ curl -s https://raw.githubusercontent.com/paulp/sbt-extras/master/sbt > ~/bin/sbt \
        && chmod 0755 ~/bin/sbt

### Run the server locally

To develop or run the DB locally, you'll need to install Postgresql. By default,
the web application expects a local Postgres DB named "levar". To create that:

    $ psql -c 'create database levar;'

We're using Play 2.4. Because we're using a multi-project build, you need
to load the `web` project explicitly in the SBT console.

To run locally:

    $ sbt stage
    $ levar-web/target/universal/stage/bin/levar-web

Startup output should look like this:

    $ levar-web/target/universal/stage/bin/levar-web
    Play server process ID is 8167
    [info] application - {"context":"global","status":"starting","action":"loading_db"}
    [info] application - {"context":"impl","status":"starting","action":"setup_db"}
    [info] application - {"context":"impl","status":"done","action":"setup_db"}
    [info] application - {"context":"global","status":"done","action":"loading_db"}
    [info] play - Application started (Prod)
    [info] play - Listening for HTTP on /0:0:0:0:0:0:0:0:9000

For rapid development, it can be run using Play's `run` directive in SBT:

    $ sbt
    > project web
    [levar-web] $ run

See [Play 2.3 docs](https://playframework.com/documentation/2.3.x/Home) for details.

### Deploy to Heroku

LeVar server is configured to be deployed to [Heroku](https://www.heroku.com/),
using the [sbt-heroku](https://github.com/heroku/sbt-heroku) plugin.

Quickstart, install the [Heroku toolbelt](https://toolbelt.heroku.com/) and do this:

    $ heroku apps:create
    $ heroku addons:create heroku-postgresql

The sbt-heroku plugin will figure out which app to deploy to from the Heroku/git
configuration, but a small hack is necessary to get this to work in the LeVar
multi-project build:

    $ cd levar-web
    $ ln -s ../.git .git
    $ cd ..

Then to deploy:

    $ sbt stage web/deployHeroku

### Use the client library

We have not published to the client library yet, so to use it you should publish
it locally first:

    $ sbt client/publishLocal

Then in your application code elsewhere, include this dependency:

    libraryDependencies += "com.peoplepattern" %% "levar-client" % "0.1-SNAPSHOT"

### Build and install the CLI

To build the CLI, simply do this:

    $ sbt stage

To install in, say, the `bin` directory of your $HOME, do this:

    $ mkdir -p ~/bin ~/lib
    $ cp levar-cli/target/universal/stage/* ~/

## Set up the server

The database API Web service does not have an admin Web UI yet, but is configurable
from the console.

Locally, start the console with:

    $ sbt web/console

Running on Heroku, run the console with:

    $ heroku run console

Inside the console application, there's a variety of admin tasks; here are
some of the ones you'll need to set up the the server.

Set up the DB (though starting the Web server does this too):

    scala> console.dbSetUp

Create a user:

    scala> console.createUser("your-user-name", "your-password")

Add an organization:

    scala> console.createOrg("your-org-name", Seq("your-user-name"))

Add other users to an organization:

    scala> console.addToOrg("your-org-name", Seq("another-user"))

## Use the CLI

To view help:

    $ levar-cli help

Set up your client to match your server credentials:

    $ levar-cli config
    URL:
    Username:
    Password:
    Organization:

This saves your credentials into `~/.levar/config.json`.

To upload a dataset:

    $ levar-cli upload dataset winequality-red.tsv

By default this creates a dataset with the name `winequality-red.tsv`; override this
with a custom name:

    $ levar-cli upload dataset --name wine winequality-red.tsv

Show what datasets you've uploaded:

    $ levar-cli list datasets

View details about an individual dataset:

    $ levar-cli view dataset wine

Dataset TSV files must have a special column for the correct (gold) value
of the records -- the value an evaluated system must predict. For classification
datasets, the column must be named `class`, for regression datasets this column
must be named `score`.

To run an experiment a dataset, you must download the dataset from the server --
the server creates an `id` field that is necessary to process uploaded experiment.

    $ levar-cli download dataset wine > wine-test.tsv

By default, a downloaded dataset does *not* have the gold-standard `score`
or `class` field.

To upload an experiment, you must specify the dataset the experiment is
against:

    $ lever-cli upload experiment wine wine-test-output.tsv

Similarly, a user-specified identifier can be added to the experiment:

$ lever-cli upload experiment --name wine-test-01 wine wine-test-output.tsv    

An experiment upload *must* include the `id` column and *must* append a
`score` column or a `class` column, depending on what kind of dataset is
being used. To view experiments associated with a dataset:

    $ levar-cli list experiments wine

To view experiment evaluation details, provide both the dataset and the experiment
IDs:

    $ levar-cli view experiment wine/wine-test-01

## More to come.

See the [issues](https://github.com/peoplepattern/LeVar/issues) for detailed
roadmap, but at a high level here are some of the things planned:

* Label or comment on any dataset, experiment, dataset item, experiment prediction
  in the database; list experiments and datasets by label
* Error analysis and viewing individual experiment predictions
* Periodic runs of experiments against a resource (like an API)

## License

LeVar is open source and licensed under the [Apache License 2.0](LICENSE.txt).

## Acknowledgements

Developed with love at [People Pattern Corporation](https://peoplepattern.com)

[![People Pattern logo](img/pp.png)](https://peoplepattern.com)
