---
layout: default
title: AutoMan Home
description: Domain-Specific Language for Crowdsourcing Tasks
isHome: true
---

[![Circle CI](https://circleci.com/gh/dbarowy/AutoMan.svg?style=shield)](https://circleci.com/gh/dbarowy/AutoMan)

<section class="bs-docs-section">
<h1 id="overview" class="page-header">What is AutoMan?</h1>
</section>
AutoMan is the first fully automatic _crowdprogramming_ system. AutoMan integrates human-based ("crowdsourced") computations into a standard programming language as ordinary function calls that can be intermixed freely with traditional functions. This abstraction lets  programmers focus on their programming logic. An AutoMan program specifies a _confidence level_ for the overall computation and a _budget_. The AutoMan runtime system then transparently manages all details necessary for scheduling, pricing, and quality control. AutoMan automatically schedules human tasks for each computation until it achieves the desired confidence level; monitors, reprices, and restarts human tasks as necessary; and maximizes parallelism across human workers while staying under budget.

AutoMan is available as a library for Scala.

### Where did it all come from?

AutoMan is actively being developed by researchers at the [PLASMA Laboratory](http://plasma.cs.umass.edu/) at the [University of Massachusetts Amherst](https://www.cics.umass.edu/), chiefly [Daniel Barowy](http://people.cs.umass.edu/~dbarowy/) and Professor [Emery Berger](https://emeryberger.com/).  VoxPL, a research project that extends AutoMan's expressiveness for estimates, was a collaboration with [Microsoft Research NYC](https://www.microsoft.com/en-us/research/lab/microsoft-research-new-york/).

<section class="bs-docs-section">
  <h1 id="downloading" class="page-header">Getting AutoMan</h1>
</section>

The easiest way to get AutoMan is via the Maven Central Repository.  If you're using SBT:

    libraryDependencies += "edu.umass.cs" %% "automan" % "1.1.7"

_or_ if you're using Maven:

    <dependency>
      <groupId>edu.umass.cs</groupId>
      <artifactId>automan_2.11</artifactId>
      <version>1.1.7</version>
    </dependency>

Sorry, we no longer support Scala 2.10 as AutoMan requires Java 8.

<section class="bs-docs-section">
  <h1 id="bugs" class="page-header">Reporting bugs</h1>
</section>

Please report bugs using this repository's [issue tracker](https://github.com/dbarowy/AutoMan/issues).

<section class="bs-docs-section">
  <h1 id="license" class="page-header">License</h1>
</section>

AutoMan is licensed under the GPLv2, Copyright (C) 2011-2016 The University of Massachusetts, Amherst.

<section class="bs-docs-section">
  <h1 id="using_automan" class="page-header">Using AutoMan</h1>
</section>

In your source file, import the Mechanical Turk adapter (Scala syntax):

    import edu.umass.cs.automan.adapters.mturk._

After that, initialize the AutoMan runtime with an MTurk config:

    implicit val mt = mturk (
      access_key_id = "my key",         // your MTurk "access key"
      secret_access_key = "my secret",  // your MTurk "secret key" 
      sandbox_mode = true               // if true, run on MTurk sandbox
    )

and then define your task:

    def which_one() = radio(
      budget = 8.00,
      text = "Which one of these does not belong?",
      options = (
        "Oscar the Grouch",
        "Kermit the Frog",
        "Spongebob Squarepants",
        "Cookie Monster",
        "The Count"
      )
    )

You may then call `which_one` just like an ordinary function (which it is).  Note that AutoMan functions immediately return an `Outcome`, but continue to execute asynchronously in the background.

To access return values, you must pattern-match on the `Outcome`'s `answer` field, e.g.,

    val outcome = which_one()
    
    // ... do some other stuff ...
    
    // then, when you want answers ...
    val answer = outcome.answer match {
      case Answer(value, _, _) => value
      case _ => throw new Exception("Oh no!")
    }

Other possible cases are `LowConfidenceAnswer` and `OverBudgetAnswer`.  If you run out of money during a computation, a `LowConfidenceAnswer` will let you access to lower-confidence results.  An `OverBudgetAnswer` signals that you didn't have enough money in your budget to begin with.

### Cleanup of AutoMan Resources

Note that, due to AutoMan's design, you must inform it when to shut down, otherwise it will continue to execute indefinitely and your program will hang:

    a.close()

Alternately, you may wrap your program in an `automan` statement, and cleanup will happen automatically.  This feature was [inspired](https://msdn.microsoft.com/en-us/library/vstudio/yh598w02%28v=vs.100%29.aspx) by the C# `using` statement:

    automan(a) {
      ... your program ...
    }

We will add more documentation to this site in the near future.  In the interim, please see the collection of sample programs in the `apps`
directory.

### Supported Question Types

|Question Type|Purpose|Quality-Controlled|Number of Answers Returned|
| --- | --- | --- | --- |
| `RadioButtonQuestion` | The user is asked to choose one of n options.|yes|1|
| `CheckboxQuestion` | The user is asked to choose one of m of n options, where m <= n. |yes|1|
| `FreeTextQuestion` | The user is asked to enter a textual response, such that the response conforms to a simple pattern (a "picture clause"). |yes|1|
| `EstimationQuestion` | The user is asked to enter a numeric (real-valued) response. |yes|1|
| `RadioButtonDistributionQuestion` | Same as `RadioButtonQuestion`. |no|user-defined|
| `CheckboxDistributionQuestion` | Same as `CheckboxQuestion`. |no|user-defined|
| `FreeTextDistributionQuestion` | Same as `FreeTextQuestion`. |no|user-defined|


### Using AutoMan with a Different Crowdsourcing Backend

We currently only support Amazon's Mechanical Turk.  However, AutoMan
was designed to accommodate arbitrary backends.  If you are interested
in seeing your crowdsourcing platform supported, please contact us.

### Memoization

AutoMan saves all intermediate human-computed results by default.  You may turn this feature off by setting `logging = LogConfig.NO_LOGGING` in your AutoMan config.  You may also set the location of the database with `database_path = "/path/to/your/database"`.  Note that the format of the database has changed from earlier versions of AutoMan from Apache Derby to H2.

<section class="bs-docs-section">
  <h1 id="building_automan" class="page-header">Building AutoMan</h1>
</section>

You do not need to build AutoMan yourself, as it is available via Maven as a JAR.  However, if you want to hack on AutoMan, or if you just like building stuff, the AutoMan source code includes an SBT build script.  The build script builds the AutoMan JAR for you, including downloading all of AutoMan's dependencies.  The build script can also build the sample applications that are located in the `apps` directory.  These applications are the ones used in our papers.

You can build the AutoMan JAR using the following commands:

    cd libautoman
    sbt pack

The AutoMan JAR plus all of its dependencies will then be found in the
`libautoman/target/pack/lib/` folder.

<section class="bs-docs-section">
  <h1 id="sample_apps" class="page-header">Sample Applications</h1>
</section>

Sample applications can be found in the `apps` directory.  Apps can also be built using `pack`.  E.g.,

    cd apps/simple_program
    sbt pack

Unix/DOS shell scripts for running the programs can then be found in `apps/[the app]/target/pack/bin/`.

<section class="bs-docs-section">
  <h1 id="acknowledgements" class="page-header">Acknowledgements</h1>
</section>

This material is based on work supported by National Science Foundation Grant Nos. CCF-1144520 and CCF-0953754 and DARPA Award N10AP2026.  Microsoft Research also generously supported research and development by funding experiments on Mechanical Turk.
