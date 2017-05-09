[![Circle CI](https://circleci.com/gh/dbarowy/AutoMan.svg?style=shield)](https://circleci.com/gh/dbarowy/AutoMan)

# AutoMan: Human-Computation Runtime

AutoMan [documentation is here](http://dbarowy.github.io/AutoMan/).

## What is AutoMan?

AutoMan is the first fully automatic _crowdprogramming_ system. AutoMan integrates human-based ("crowdsourced") computations into a standard programming language as ordinary function calls that can be intermixed freely with traditional functions. This abstraction lets  programmers focus on their programming logic. An AutoMan program specifies a _confidence level_ for the overall computation and a _budget_. The AutoMan runtime system then transparently manages all details necessary for scheduling, pricing, and quality control. AutoMan automatically schedules human tasks for each computation until it achieves the desired confidence level; monitors, reprices, and restarts human tasks as necessary; and maximizes parallelism across human workers while staying under budget.

AutoMan is available as a library for Scala.

## Getting AutoMan

The easiest way to get AutoMan is via the Maven Central Repository.  If you're using SBT:

    libraryDependencies += "edu.umass.cs" %% "automan" % "1.1.7"

_or_ if you're using Maven:

    <dependency>
      <groupId>edu.umass.cs</groupId>
      <artifactId>automan_2.11</artifactId>
      <version>1.1.7</version>
    </dependency>

Sorry, we no longer support Scala 2.10 as AutoMan requires Java 8.

## Latest Updates

Lastest changes include:

* Support for estimation queries and composed (i.e., "end-to-end") estimation queries.  Documentation coming soon!
* Bug fixes.

## Bug Reports.

Please report bugs using this repository's issue tracker.

## License

AutoMan is licensed under the GPLv2, Copyright (C) 2011-2016 The University of Massachusetts, Amherst.