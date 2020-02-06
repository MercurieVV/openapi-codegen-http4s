![Java CI](https://github.com/MercurieVV/openapi-codegen-http4s/workflows/Java%20CI/badge.svg)
# openapi-codegen-http4s
OpenAPI(Swagger) v3 codegen for http4s.
Unfortunatelly at the moment I dont know (have time) to show code example with SBT. If you could send me example I would be very appreciate.

Gradle
```buildscript {
       repositories {
           maven { url 'https://jitpack.io' }
       }
   
       dependencies {
           classpath ('org.hidetake.swagger.generator:org.hidetake.swagger.generator.gradle.plugin:${swagger3version}')
           classpath 'com.github.MercurieVV:swagger3-codegen-http4s:${swaggerHttp4sVersion}'
       }
   }
dependencies {
    swaggerCodegen 'com.github.MercurieVV:swagger3-codegen-http4s:${swaggerHttp4sVersion}'
}
```

```swaggerSources {
       coolServer {
           code {
               language = "com.github.mercurievv.swagger3.codegen.scala.http4s.Http4sCodegen"
            }
            ...
        }
}
```

After that you will have generated code like:
```scala

```