## JBoss Class File Writer

JBoss Class File Writer is a byte code manipulating library used by EJB subsytem in [WildFly](https://github.com/wildfly/wildfly) project.

## Deprecated!

This project is deprecated. The preferred method for generating or parsing bytecode is
[the JDK classfile API](https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/classfile/package-summary.html)
which may be found in JDK 24 or later, or 
[the classfile API backport project](https://github.com/dmlloyd/jdk-classfile-backport)
which runs on JDK 17 or later.

## Building

Prerequisites:

* JDK 11 or newer - check `java -version`
* Maven 3.6.0 or newer - check `mvn -v`

To build with your own Maven installation:

    mvn install

## Issue tracker

All issues can be reported at https://github.com/jbossas/jboss-classfilewriter/issues

## Code

All code can be found at https://github.com/jbossas/jboss-classfilewriter

## License

All code distributed under [ASL 2.0](LICENSE.txt).
