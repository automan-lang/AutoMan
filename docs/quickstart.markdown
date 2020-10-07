---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: page
title: Quick Start
---

### Prerequisites

<hr class="style12" />

Before you start, you must have a few things installed.

1. Install Java (version 11+ recommended, but 1.8+ should work).

   <a class="btn btn-green btn-cta" href="https://www.java.com/en/download/" target="_blank"><i class="fas fa-cloud-download-alt"></i> Download Java</a>
1. Install Scala (specifically, version 2.12; Scala 2.13 is not yet supported).

   <a class="btn btn-green btn-cta" href="https://www.scala-lang.org/download/" target="_blank"><i class="fas fa-cloud-download-alt"></i> Download Scala</a>
1. You are encouraged to install `sbt`&#8224;.
   
   Most Scala developers use `sbt`, and if you plan to run any of AutoMan's demo apps, or if you plan to hack on AutoMan itself, you will need it.

   <a class="btn btn-green btn-cta" href="https://www.scala-sbt.org/download.html" target="_blank"><i class="fas fa-cloud-download-alt"></i> Download sbt</a>

<hr/>

### Download AutoMan

<hr class="style12" />

If you use `sbt` to build your application, then you can simply list AutoMan as a dependency for your software.  Here is a [template `build.sbt`](https://github.com/automan-lang/AutoMan/blob/master/apps/simple/sbt_template/build.sbt) that you can use as a starter.

If you are already familiar with `sbt`, simply add the following dependency to your `libraryDependencies` section.

```
"org.automanlang" %% "automan" % "1.4.1"
```

`sbt` will download an AutoMan JAR and store it locally on your machine, along with all of AutoMan's dependencies.

Alternatively, if you do not want to use `sbt`, you may download the latest AutoMan JAR [directly from Maven](https://mvnrepository.com/artifact/org.automanlang/automan).  This JAR does not include AutoMan's dependencies, which you will also need to download.  See the section titled "Compile Dependencies" in the page linked above.  Note that dependencies may themselves have dependencies, which is why you are encouraged to use the approach described using `sbt` above.

<hr/>


&#8224; [Pronounced like this](https://www.youtube.com/watch?v=pQlPjUSj7no).&#8225;

&#8225; The `sbt` authors [insist "sbt" does not stand for "Scala build tool"](https://www.scala-sbt.org/1.x/docs/Faq.html), so... fair game.
