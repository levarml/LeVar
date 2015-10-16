---
layout: page
title: This is LeVar
---

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

## License

LeVar is open source and licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Acknowledgements

Developed with love at [People Pattern Corporation](https://peoplepattern.com)

[![People Pattern logo]({{ site.baseurl }}/public/pp.png)](https://peoplepattern.com)
