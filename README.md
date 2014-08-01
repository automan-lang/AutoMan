AutoMan: Human-Computation Runtime v0.4
---------------------------------------

This is a maintenance release of AutoMan.  Please report any bugs you
may find to the project maintainer, Dan Barowy <dbarowy@cs.umass.edu>.

License
-------

AutoMan is licensed under the GPLv2, Copyright (C) 2011-2014 The
University of Massachusetts, Amherst.

Building a JAR
----------------

This release incorporates an SBT build script that can build the AutoMan JAR
for you, including downloading all of AutoMan's dependencies.  The build
script can also build the sample applications that are located in the
`apps` directory.  These applications are the ones used in our paper.

You can build the AutoMan JAR using the following command:

    sbt pack

The AutoMan JAR plus all of its dependencies will then be found in the
`libautoman/target/pack/lib/` folder.

If you want to see a list of other targets, e.g., sample applications,
just type `sbt projects`.

Maven Artifact Coming Soon!
---------------------------

We have registered with SonaType as an organization and will be pushing
AutoMan to the Central Repository soon.  This means that you will simply
be able to list AutoMan as a dependency in your `build.sbt` and all
of its dependencies will be handled by SBT.  Stay tuned!

Using AutoMan in Your Project
-----------------------------

In your source file, import the Mechanical Turk adapter (Scala syntax):

    import edu.umass.cs.automan.adapters.MTurk.MTurkAdapter

Due to a change in Scala's concurrency libraries, you should also add:

    import scala.concurrent._
    import scala.concurrent.duration._

After that, initialize the AutoMan runtime with an MTurk config:

    val a = MTurkAdapter { mt =>
      mt.access_key_id = "my key"
      mt.secret_access_key = "my secret"
      mt.sandbox_mode = true
    }

and then define your tasks:

    def tootsie() = a.RadioButtonQuestion { q =>
      q.budget = 8.00
      q.text = "How licks does it take to get to the Tootsie " +
               "Roll center of a Toosie Pop?"
      q.options = List(
        a.Option('one, "One"),
        a.Option('two, "A-Two"),
        a.Option('three, "Three! Three!")
      )
    }

You may then call those tasks like regular functions.  Note that
these functions return immediately, as calling a human function execute
asynchronously in a separate background thread.

In order to access data returned by the function, since that data is
wrapped in a Scala `Future`, you must call the blocking `Await.result`
to unbox the `Answer` object returned by the function and then access
the `value` field in the `Answer` object.  E.g.,

    val future_answer = tootsie()
    val licks = Await.result(future_answer, Duration.Inf).value

Note: We plan to hide these concurrency operations in a future release.

Please see the collection of sample programs in the `apps`
directory. This folder contains the programs we used in the technical
report (see below).  You can build any one of them by running:

    sbt "project [appname]" pack

Then look in the `apps/[appname]/target/pack/bin` folder for the
appropriate runner script.  For example, if you want to run the
`SimpleProgram` application, do:

    sbt "project SimpleProgram" pack
    apps/simple_program/target/pack/bin/simple_program

Using AutoMan with a Different Crowdsourcing Backend
----------------------------------------------------

We currently only support Amazon's Mechanical Turk.  However, AutoMan
was designed to accommodate arbitrary backends.  If you are interested
in seeing your backend supported, please contact Dan Barowy.

However, we are happy to work with you to ensure that you have all of
the information you need to write your adapter library. We will also fix
any compatibility problems with the AutoMan runtime that you might
encounter along the way.

Memoization
-----------

AutoMan saves all intermediate human-computed results.  In the event
of a program exception or unexpected termination, the programmer may
restart AutoMan and it will reuse any previously-obtained results.  If
you want to discard these intermediate results, delete the
AutomanMemoDB file.  The format of this database is Apache Derby 10.8.

More Information
----------------

More detailed information is available in the following paper,
published at OOPSLA 2012, included in the repo as AutoMan-OOPSLA2012.pdf.

  AutoMan: A Platform for Integrating Human-Based and Digital Computation
  Daniel W. Barowy, Charlie Curtsinger, Emery D. Berger, and Andrew McGregor

  http://www.cs.umass.edu/~emery/pubs/res0007-barowy.pdf

  The full citation is given below:

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

Contact information:

  Emery Berger, emery@cs.umass.edu
  Dan Barowy, dbarowy@cs.umass.edu
  
CHANGELOG:
----------
|0.4|Maintenance release.|
|   |*Switch to SBT build system. Updates for Scala 2.10.|
|0.3|Maintenance release|
|   |*Buildr Buildfile, including reorganization of project directory.|
|0.2.1|Maintenance release|
|   |*Update to work with latest MTurk API (1.6.0).|
|   |*Better log output, including scheduler object logging.|
|   |*New Automatic Number Plate Recognition (ANPR) app.|
|0.2|Major rewrite to simplify syntax.|
|0.1|First release.|

ACKNOWLEDGMENTS:
----------------
This material is based upon work supported by the National Science
Foundation under Grant No. CCF-1144520.

