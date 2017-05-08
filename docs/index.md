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

    val a = MTurkAdapter { mt =>
      mt.access_key_id = "my key"
      mt.secret_access_key = "my secret"
      mt.sandbox_mode = true
    }

and then define your tasks:

    def which_one() = a.RadioButtonQuestion { q =>
      q.budget = 8.00
      q.text = "Which one of these does not belong?"
      q.options = List(
        a.Option('oscar, "Oscar the Grouch"),
        a.Option('kermit, "Kermit the Frog"),
        a.Option('spongebob, "Spongebob Squarepants"),
        a.Option('cookie, "Cookie Monster"),
        a.Option('count, "The Count")
      )
    }

You may then call `which_one` just like an ordinary function (which it is).  Note that AutoMan functions immediately return an `Outcome`, but continue to execute asynchronously in the background.

To access return values, you must pattern-match on the `Outcome`, e.g.,

    val outcome = which_one()
    
    // ... do some other stuff ...
    
    // then, when you want answers ...
    val answer = outcome.answer match {
      case Answer(value, _, _) => value
      case _ => throw new Exception("Oh no!")
    }

Other possible `AbstractAnswer` types are `LowConfidenceAnswer` if you run out of money during a computation (which gives you access to lower-confidence results), or `OverBudgetAnswer` in case even low-confidence answers are not possible because you didn't have enough money in your budget to begin with.

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
  <h1 id="more_info" class="page-header">Research and More Information</h1>
</section>

More detailed information is available in our papers:

  * [VoxPL: Programming with the Wisdom of the Crowd](http://barowy.net/papers/voxpl-chi.pdf)  
  Daniel W. Barowy, Emery D. Berger, Daniel G. Goldstein, and Siddharth Suri
  * [AutoMan: A Platform for Integrating Human-Based and Digital Computation (Research Highlight)](http://dl.acm.org/citation.cfm?id=2927928)  
  Daniel W. Barowy, Charlie Curtsinger, Emery D. Berger, and Andrew McGregor
  * [AutoMan: A Platform for Integrating Human-Based and Digital Computation](http://www.cs.umass.edu/~emery/pubs/res0007-barowy.pdf)  
  Daniel W. Barowy, Charlie Curtsinger, Emery D. Berger, and Andrew McGregor

  There are two versions of the original AutoMan paper, a shortened Research Highlight that appeared in the June 2016 issue of the Communications of the ACM and a longer version that appeared at OOPSLA '12.  You should probably start with the CACM version which has a bit more polish and some updated results.

  Full citations are given below:

```
@inproceedings{Barowy:2017:VPW:3025453.3026025,
 author = {Barowy, Daniel W. and Berger, Emery D. and Goldstein, Daniel G. and Suri, Siddharth},
 title = {VoxPL: Programming with the Wisdom of the Crowd},
 booktitle = {Proceedings of the 2017 CHI Conference on Human Factors in Computing Systems},
 series = {CHI '17},
 year = {2017},
 isbn = {978-1-4503-4655-9},
 location = {Denver, Colorado, USA},
 pages = {2347--2358},
 numpages = {12},
 url = {http://doi.acm.org/10.1145/3025453.3026025},
 doi = {10.1145/3025453.3026025},
 acmid = {3026025},
 publisher = {ACM},
 address = {New York, NY, USA},
 keywords = {crowdprogramming, crowdsourcing, domain-specific languages, quality control, scalability, wisdom of the crowd},
}
```

```
@article{Barowy:2016:API:2942427.2927928,
 author = {Barowy, Daniel W. and Curtsinger, Charlie and Berger, Emery D. and McGregor, Andrew},
 title = {AutoMan: A Platform for Integrating Human-based and Digital Computation},
 journal = {Commun. ACM},
 issue_date = {June 2016},
 volume = {59},
 number = {6},
 month = may,
 year = {2016},
 issn = {0001-0782},
 pages = {102--109},
 numpages = {8},
 url = {http://doi.acm.org/10.1145/2927928},
 doi = {10.1145/2927928},
 acmid = {2927928},
 publisher = {ACM},
 address = {New York, NY, USA},
}
```

```
@inproceedings{Barowy:2012:API:2384616.2384663,
 author = {Barowy, Daniel W. and Curtsinger, Charlie and Berger, Emery D. and McGregor, Andrew},
 title = {% raw %} {{AutoMan} {% endraw %}: a platform for integrating human-based and digital computation},
 booktitle = {Proceedings of the ACM International Conference on Object-Oriented Programming Systems Languages and Applications},
 series = {OOPSLA '12},
 year = {2012},
 isbn = {978-1-4503-1561-6},
 location = {Tucson, Arizona, USA},
 pages = {639--654},
 numpages = {16},
 url = {http://doi.acm.org/10.1145/2384616.2384663},
 doi = {10.1145/2384616.2384663},
 acmid = {2384663},
 publisher = {ACM},
 address = {New York, NY, USA},
 keywords = {crowdsourcing, programming languages, quality control},
}
```

Contact information:

  Dan Barowy, dbarowy@cs.umass.edu
  Emery Berger, emery@cs.umass.edu

<section class="bs-docs-section">
  <h1 id="acknowledgements" class="page-header">Acknowledgements</h1>
</section>

This material is based on work supported by National Science Foundation Grant Nos. CCF-1144520 and CCF-0953754 and DARPA Award N10AP2026.
