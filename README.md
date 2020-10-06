# AutoMan: Human-Computation Runtime

AutoMan [documentation is here](https://automan-lang.github.io).

## What is AutoMan?

AutoMan is the first fully automatic _crowdprogramming_ system. AutoMan integrates human-based ("crowdsourced") computations into a standard programming language as ordinary function calls that can be intermixed freely with traditional functions. This abstraction lets  programmers focus on their programming logic. An AutoMan program specifies a _confidence level_ for the overall computation and a _budget_. The AutoMan runtime system then transparently manages all details necessary for scheduling, pricing, and quality control. AutoMan automatically schedules human tasks for each computation until it achieves the desired confidence level; monitors, reprices, and restarts human tasks as necessary; and maximizes parallelism across human workers while staying under budget.

AutoMan is available as a library for Scala.

## Getting AutoMan

The easiest way to get AutoMan is via the Maven Central Repository.  If you're using SBT:

    libraryDependencies += "org.automanlang" %% "automan" % "1.4.1"

_or_ if you're using Maven:

    <dependency>
      <groupId>org.automanlang</groupId>
      <artifactId>automan_2.12</artifactId>
      <version>1.4.1</version>
    </dependency>

AutoMan 1.4.0+ requires Scala 2.12.

## Latest Updates

Lastest changes include:

* 2020-10-06: Bugfix that caused crash for checkbox distribution questions.
* 2020-10-05: Support for latest Amazon MTurk SDK (mostly due to [Emmie Hine](https://www.linkedin.com/in/emmie-hine/)!)

## Bug Reports.

Please report bugs using this repository's issue tracker.

## License

AutoMan is licensed under the GPLv2, Copyright (C) 2011-2020 The University of Massachusetts, Amherst / Williams College.
