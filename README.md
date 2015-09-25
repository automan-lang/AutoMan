AutoMan: Human-Computation Runtime v1.0
---------------------------------------

This is a major release of AutoMan.  Code written using earlier versions (< 1.0) will need to be rewritten as AutoMan's syntax has changed.  See below for details.

Major changes include:

* Support for non-quality-controlled question types in addition to the standard quality-controlled types.
* Per-question budgets.
* All Scala concurrency details are now hidden.  Instead of dealing with low-level Scala `Future` objects, instead you pattern-match on AutoMan `Outcome` objects.
* A an important but subtle bias toward accepting low-confidence answers has been eliminated by modifying AutoMan's core algorithm.  The details are discussed in our upcoming CACM Research Highlights article (stay tuned!).
* A much more capable record-and-replay engine. For example, if your program terminates abnormally, on restart AutoMan will continue as if nothing unusual happened.  Earlier versions of AutoMan only memoized answered MTurk HITs, which meant that restarts would schedule new work even if HITs had been completed during the interim. The new version records the complete state of a computation on MTurk.
* The replay engine can also be used to provide mocks for your unit tests.  We will be expanding on these capabilities in the near future--stay tuned!
* AutoMan now has a simple plugin architecture, designed to support a visual debugger [currently being developed](https://github.com/BartoszJanota/automan-debugger) for IntelliJ IDEA largely through the efforts of our excellent Google Summer of Code intern, Bartosz Janota.  This work was based on an [earlier web-based prototype](https://bitbucket.org/btamaskar/automan-debugger) from a talented undergrad working in our lab, Bianca Tamaskar.  If you would like to develop plugins for AutoMan, get in touch!
* Many changes to enhance reliability.

Please report any bugs you may find to the project maintainer, Dan Barowy <dbarowy@cs.umass.edu>.

License
-------

AutoMan is licensed under the GPLv2, Copyright (C) 2011-2015 The
University of Massachusetts, Amherst.

Building a JAR
----------------

This release incorporates an SBT build script that can build the AutoMan JAR
for you, including downloading all of AutoMan's dependencies.  The build
script can also build the sample applications that are located in the
`apps` directory.  These applications are the ones used in our paper.

You can build the AutoMan JAR using the following commands:

    cd libautoman
    sbt pack

The AutoMan JAR plus all of its dependencies will then be found in the
`libautoman/target/pack/lib/` folder.

Sample applications can be found in the `apps` directory.  Apps can also be built using `pack`.  E.g.,

    cd apps/simple_program
    sbt pack

Maven Artifact Coming Soon!
---------------------------

We have registered with SonaType as an organization and will be pushing
AutoMan to the Central Repository soon.  This means that you will simply
be able to list AutoMan as a dependency in your `build.sbt` and all
of its dependencies will be handled by SBT.  Stay tuned!

Using AutoMan in Your Project
-----------------------------

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

Cleanup of AutoMan Resources
----------------------------

Note that, due to AutoMan's design, you must inform it when to shut down, otherwise it will continue to execute indefinitely and your program will hang:

    a.close()

Alternately, you may wrap your program in an `automan` statement, and cleanup will happen automatically.  This feature was [inspired](https://msdn.microsoft.com/en-us/library/vstudio/yh598w02%28v=vs.100%29.aspx) by the C# `using` statement:

        automan(a) {
          ... your program ...
        }

We will add more documentation to this site in the near future.  In the interim, please see the collection of sample programs in the `apps`
directory.

Supported Question Types
------------------------

|Question Type|Purpose|Quality-Controlled|Number of Answers Returned|
| --- | --- | --- | --- |
| `RadioButtonQuestion` | The user is asked to choose one of n options.|yes|1|
| `CheckboxQuestion` | The user is asked to choose one of m of n options, where m <= n. |yes|1|
| `FreeTextQuestion` | The user is asked to enter a textual response, such that the response conforms to a simple pattern (a "picture clause"). |yes|1|
| `RadioButtonDistributionQuestion` | Same as `RadioButtonQuestion`. |no|user-defined|
| `CheckboxDistributionQuestion` | Same as `CheckboxQuestion`. |no|user-defined|
| `FreeTextDistributionQuestion` | Same as `FreeTextQuestion`. |no|user-defined|

Using AutoMan with a Different Crowdsourcing Backend
----------------------------------------------------

We currently only support Amazon's Mechanical Turk.  However, AutoMan
was designed to accommodate arbitrary backends.  If you are interested
in seeing your crowdsourcing platform supported, please contact us.

Memoization
-----------

AutoMan saves all intermediate human-computed results by default.  You may turn this feature off by setting `logging = LogConfig.NO_LOGGING` in your AutoMan config.  You may also set the location of the database with `database_path = "/path/to/your/database"`.  Note that the format of the database has changed from earlier versions of AutoMan from Apache Derby to H2.

More Information
----------------

More detailed information is available in the following paper,
published at OOPSLA 2012, included in the repo as AutoMan-OOPSLA2012.pdf.

  AutoMan: A Platform for Integrating Human-Based and Digital Computation
  Daniel W. Barowy, Charlie Curtsinger, Emery D. Berger, and Andrew McGregor

  http://www.cs.umass.edu/~emery/pubs/res0007-barowy.pdf

  The full citation is given below:

```
@inproceedings{Barowy:2012:API:2384616.2384663,
 author = {Barowy, Daniel W. and Curtsinger, Charlie and Berger, Emery D. and McGregor, Andrew},
 title = {{AutoMan}: a platform for integrating human-based and digital computation},
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

Change Log:
----------

|Version|Notes|
| --- | --- |
|1.0|Major release.|
|   |Syntax changes.|
|   |New question types.|
|   |Per-question budgets.|
|   |Bias due to multiple comparisons eliminated with Bonferroni correction.|
|   |New memo engine that allows MTurk computation to be resumed without additional cost (timeouts notwithstanding).|
|   |Support for mocks.|
|   |Plugin architecture.|
|   |Numerous other changes for better reliability.|
|0.4|Maintenance release.|
|   |Switch to SBT build system. Updates for Scala 2.10.|
|0.3|Maintenance release|
|   |Buildr Buildfile, including reorganization of project directory.|
|0.2.1|Maintenance release|
|   |Update to work with latest MTurk API (1.6.0).|
|   |Better log output, including scheduler object logging.|
|   |New Automatic Number Plate Recognition (ANPR) app.|
|0.2|Major rewrite to simplify syntax.|
|0.1|First release.|

Acknowledgements:
----------------
This material is based on work supported by National Science Foundation Grant Nos. CCF-1144520 and CCF-0953754 and DARPA Award N10AP2026.

