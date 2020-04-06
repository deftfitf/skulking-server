// Resolvers
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.0")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.1")
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.11-0.6")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.30")
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler-sjs06" % "0.16.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")