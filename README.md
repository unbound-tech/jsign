Jsign - Java implementation of Microsoft Authenticode
=====================================================

[![Build Status](https://secure.travis-ci.org/ebourg/jsign.svg)](http://travis-ci.org/ebourg/jsign)
[![Coverage Status](https://coveralls.io/repos/github/ebourg/jsign/badge.svg?branch=master)](https://coveralls.io/github/ebourg/jsign?branch=master)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/net.jsign/jsign.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22net.jsign%22)

Jsign is a Java implementation of Microsoft Authenticode that lets you sign
and timestamp executable files for Windows. Jsign is platform independent and
provides an alternative to native tools like signcode/signtool on Windows
or the Mono development tools on Unix systems.

Added support for Unbound-tech security provider.

See https://ebourg.github.com/jsign for more information.


## Build
Add the Unbound Java Security Provider Jar (ekm-java-provider-2.0) to root directory of the project and run `ant`.

## Usage
### cli 

`java -jar .\ub-jsgin-cli.jar --partition part1 --storetype unbound --alias key-alias file-name`

### ant
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project name="UbSignExe" basedir="." default="ubsign">
   <target name="ubsign">
      <taskdef name="UbSignExe" classname="net.jsign.PESignerTask" classpath="ub-jsgin-ant.jar" />
      <UbSignExe file="wineyes.exe" partition="p1" alias="test" storetype="unbound" />
   </target>
</project>
```
