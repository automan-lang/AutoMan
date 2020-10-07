---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: page
title: Quick Start
---

## Prerequisites

Before you start, you need a few things installed.

1. Install Java (version 11+ recommended, but 1.8+ should work).

   <a class="btn btn-green btn-cta" href="https://www.java.com/en/download/" target="_blank"><i class="fas fa-cloud-download-alt"></i> Download Java</a>
1. Install Scala (specifically version 2.12; Scala has yet to be ported to Scala 2.13).

   <a class="btn btn-green btn-cta" href="https://www.scala-lang.org/download/" target="_blank"><i class="fas fa-cloud-download-alt"></i> Download Scala</a>
1. You are encouraged to install `sbt` ([which the authors insist does not stand for "Scala build tool"](https://www.scala-sbt.org/1.x/docs/Faq.html); consequently, I pronounce it [like this](https://www.youtube.com/watch?v=pQlPjUSj7no)).  Most Scala developers use `sbt`, and if you plan to run any of AutoMan's demo apps, or if you plan to hack on AutoMan itself, you will need it.

   <a class="btn btn-green btn-cta" href="https://www.scala-sbt.org/download.html" target="_blank"><i class="fas fa-cloud-download-alt"></i> Download sbt</a>

<hr/>

## Download AutoMan

There are two ways to obtain AutoMan: the hard way and the easy way.

### The easy way

If you use `sbt` to build your application, then you can simply list AutoMan as a dependency for your software.  [Here is a template build.sbt](https://github.com/automan-lang/AutoMan/blob/master/apps/simple/sbt_tempalte/build.sbt) that you can use as a starter.

If you are already familiar with `sbt`, use the dependency:

```
"org.automanlang" %% "automan" % "1.4.1"
```

`sbt` will download an AutoMan JAR and store it locally on your machine, along with all of AutoMan's dependencies.

### The hard way

You may download an AutoMan JAR [directly from Maven](https://mvnrepository.com/artifact/org.automanlang/automan_2.12/1.4.1).  This JAR does not include AutoMan's dependencies, which you will also need to download.  See the section titled "Compile Dependencies" in the page linked above.  Note that dependencies may themselves have dependencies, which is why you are encouraged to use the approach described in "The easy way" above.

<hr/>

