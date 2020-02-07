[![Stable releaases in the Maven store](https://img.shields.io/maven-metadata/v/https/repo1.maven.org/maven2/com/github/mercurievv/openapi-codegen-http4s/maven-metadata.xml.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.github.mercurievv%22%20AND%20a%3A%22openapi-codegen-http4s%22)
![Java CI](https://github.com/MercurieVV/openapi-codegen-http4s/workflows/Java%20CI/badge.svg)
# openapi-codegen-http4s
OpenAPI(Swagger) v3 codegen for http4s.
SBT
project/plugins.sbt
```
addSbtPlugin("com.github.mercurievv" % "sbt-openapi-generator-plugin" % "1.0.0")
libraryDependencies += "com.github.mercurievv" % "openapi-codegen-http4s" % "1.0.7"
```
build.sbt
```
  .enablePlugins(SwaggerGeneratorPlugin)
  .settings(
    language := "com.github.mercurievv.openapi.codegen.scala.http4s.Http4sCodegen", //Server
    //language := "com.github.mercurievv.openapi.codegen.scala.http4s.Http4sClientCodegen", //Client
  )
```

After that you will have generated code like:
```scala

```