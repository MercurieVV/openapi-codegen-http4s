[![](https://jitpack.io/v/MercurieVV/swagger3-codegen-http4s.svg)](https://jitpack.io/#MercurieVV/swagger3-codegen-http4s)
# swagger3-codegen-http4s
Swagger codegen for http4s.
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