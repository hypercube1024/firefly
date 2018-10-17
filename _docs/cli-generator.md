---

category : docs
title: CLI generator

---

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Installation](#installation)
- [Create a Kotlin web project](#create-a-kotlin-web-project)
- [Create a simple Kotlin project](#create-a-simple-kotlin-project)

<!-- /TOC -->

# Installation
Run command (macOS):
```bash
cd /usr/local/lib
sudo curl -O https://oss.sonatype.org/content/groups/public/com/fireflysource/firefly-boot/{{ site.data.global.releaseVersion }}/firefly-boot-{{ site.data.global.releaseVersion }}-jar-with-dependencies.jar
alias fireflyCli='java -jar /usr/local/lib/firefly-boot-{{ site.data.global.releaseVersion }}-jar-with-dependencies.jar'
```

Run fireflyCli:
```bash
fireflyCli --version
```

The terminal print:
```
The fireflyCli version is {{ site.data.global.releaseVersion }}
Usage: fireflyCli [options]
  Options:
  * --groupId, -g
      The project group id.
  * --artifactId, -a
      The project artifact id.
  * --packageName, -p
      The project package name, e.g., com.xxx.yyy .
  * --domainName, -d
      The project domain name, e.g., yyy.xxx.com .
  * --jarName, -j
      The project jar name.
    --template, -t
      The scaffold template name, the value is firefly-web-seed or
      firefly-simple-seed
      Default: firefly-web-seed
    --outputPath, -o
      The project output path, current path is default.
      Default: .
    --fireflyVersion, -f
      The firefly version.
      Default: {{ site.data.global.releaseVersion }}
    --buildTool, -b
      The build tool name, the value is maven or gradle.
      Default: maven
    --help, -h
      Show the firefly cli usage.
    --version, -v
      Show the firefly cli version.
```

# Create a Kotlin web project
Run command:
```bash
fireflyCli -g com.test.abc -a abc-haha -p com.test.abc -d www.abc.com -j www.abc.com
```

This command creates a Kotlin web project in the current path. Import abc-haha project in your IDE.

# Create a simple Kotlin project
Add parameter `-t firefly-simple-seed`, it specifies a simple Kotlin project template, run command:
```bash
fireflyCli -g com.test.abc -a abc-simple -p com.test.abc -d www.abc.com -j www.abc.com -t firefly-simple-seed
```
