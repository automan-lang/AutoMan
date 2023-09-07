addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.13")
// Ye Shu note: sbt-assembly creates one fat jar, which is easier for distribution
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.4")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1")
