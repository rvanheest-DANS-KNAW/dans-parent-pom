DANS Maven Parents
==================
[![Build Status](https://travis-ci.org/DANS-KNAW/dans-parent-pom.png?branch=master)](https://travis-ci.org/DANS-KNAW/dans-parent-pom)

Parents for DANS Maven based projects.


SYNOPSIS
--------

    <parent>
       <groupId>nl.knaw.dans.shared</groupId>
       <artifactId>dans-java-project</artifactId>
       <version>...</version>
    </parent>

or:

    <parent>
       <groupId>nl.knaw.dans.shared</groupId>
       <artifactId>dans-scala-[(app|service)-]project</artifactId>
       <version>...</version>
    </parent>

(Fill in the current version.)

DESCRIPTION
-----------
This module contains the main build for the projects that define parent POMs for use in DANS
Maven-based projects.

### Goals
* Define default versions and scopes for commonly used dependencies. This is done by declaring
  managed dependencies in the base modules. The inheriting project then only needs to declare the
  dependency using the `groupId` and `artifactId` to automatically use the defaults, while it can
  still override those, if necessary.
* Define default versions and configurations for commonly used plug-ins. This is done by declaring
  managed plug-ins, which work the same as managed dependencies. This saves even more space in the inheriting
  project, as plug-in configurations can be rather long.
* Declare a few dependencies and plug-ins that are (almost) always used in DANS projects. However, this
  is only done in the sub-modules lowest in the hierarchy, so if you really do not need those dependencies
  you can inherit from a parent higher in the tree.

### Deploying artifacts

#### Default deploy behavior
In Maven-speak *deploying* an artifact means publishing it to a repository for distribution. This process is supported
by the `maven-deploy-plugin`. 

The `maven-release-plugin` supports creating releases, which is subdivided into two steps:

1. **Preparing** the release: creating a non-snapshot version, committing and tagging it and and pushing it to GitHub. A `release.properties` file
   containing the details of the release will also be created; it is required for the next step (see below). 
   Command line: `mvn release:clean release:prepare`.
2. **Performing** the release: cloning the git repo to `target/checkout`, checking out the release tag, building 
   that commit and deploying it to the `repository` specified in the POM's `<distributionManagement>`. 
   Command line: `mvn release:perform`. Note that this will invoke the `maven-deploy-plugin` in the last step.
   
If you call the `maven-deploy-plugin` directly it will build the current git working directory. It will create a snapshot or a release
depending on whether the version in the POM is a snapshot version or a release version. Then it will be published to
the appropriate Maven repo configured in the `<distributionManagement>`  element, so to `<snapshotRepository>` for snapshots
and to `<repository>` for releases.

Command line: `mvn deploy` (`deploy` is actually [a Maven lifecycle phase](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html), so it
will cause Maven to execute all the phases leading up to it first).   
 
#### Overrides for deploying RPMs
The distribution repositories are usually Maven-repositories. However, we want to distribute our RPM-packages through 
YUM. To support this, projects can override the default `maven-deploy-plugin`. This is done by activating `maven-antrun-plugin`
and `exec-maven-plugin`. These have been configured in `dans-mvn-plugin-defaults` to do the following during the deploy 
phase:

* Look under `target` for one RPM file.
* `PUT` that file to `${rpm-snapshot-repository}` or `${rpm-release-repository}`, depending on the version of the project
   being a snapshot version or not.
   
These overrides are active for all non-POM-descendants of `dans-scala-app-project`. They have also been copied to certain
legacy projects. Note that they do not use the `<distributionManagement>` element.

#### Mixed Maven/RPM modules
What if an RPM-module *also* needs to be published as a library to Maven? Actually, a project that requires this should be
refactored to have two submodules: one for the lib and one for the RPM. For now, the default deploy behavior can be restored
by activating the `lib-deploy` profile (defined in `dans-scala-app-project`). To avoid deploying RPMs to Maven, which is a
waste of storage space, this should be combined with deactivating the `rpm` profile:

`mvn -P'!rpm' -Plib-deploy deploy`

### Design
As of writing this, Maven is unfortunately still rather low in composability. This means that to split up a
POM you do not have a whole lot of options. If you have to use the managed dependencies you are basically stuck with
splitting up over a single-inheritance hierarchy, so that is what we have done here. This hierarchy is subdivided
as follows:

                                    dans-mvn-base
                                         |
                                dans-mvn-plugin-defaults
                                         |
                                 dans-mvn-lib-defaults
                                         |
                                  -------------------
                                  |                 |
                            dans-java-project     dans-scala-project
                                                    |
                                                 dans-scala-app-project
                                                    |
                                                 dans-scala-service-project


POM                          | Description
-----------------------------|-------------------------------------------------------------
`dans-mvn-base`              | Only basic facilities needed by all projects.
`dans-mvn-plugin-defaults`   | Only managed plug-in configurations.
`dans-mvn-lib-defaults`      | Only managed dependency configurations.
`dans-java-project`          | The basic dependencies and plug-ins needed for any DANS Java project.
`dans-scala-project`         | The basic dependencies and plug-ins needed for any DANS Scala project.
`dans-scala-app-project`     | The basic dependencies and plug-ins needed for a Scala based application or service including support for distributing to YUM.

INSTALLATION AND CONFIGURATION
------------------------------
To use these parents you need to add two thing to you POM file:

* The parent project you want to inherit from. In most cases this should be `dans-scala-app-project` or `dans-java-project`. However, you can also use one of the parents
  higher up in the inheritance tree.
* The DANS Maven repositories.

This will look like the following. Note that you must first fill in the appropriate version.

    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
        <parent>
            <groupId>nl.knaw.dans.shared</groupId>
            <artifactId>dans-scala-app-project</artifactId>
            <version>...</version>
        </parent>
        <!-- ... -->
        <repositories>
            <repository>
                <id>dans-releases</id>
                <releases>
                    <enabled>true</enabled>
                </releases>
                <snapshots>
                    <enabled>false</enabled>
                </snapshots>
                <url>https://maven.dans.knaw.nl/releases/</url>
            </repository>
            <repository>
                <id>dans-snapshots</id>
                <releases>
                    <enabled>false</enabled>
                </releases>
                <snapshots>
                    <enabled>true</enabled>
                </snapshots>
                <url>https://maven.dans.knaw.nl/snapshots/</url>
            </repository>
         </repositories>
        <!-- ... -->
    </project>

You may in some cases want to extend the plug-in configurations that you inherited. You can often selectively override or append to the configuration
declared in the parent, using the `combine.*` attributes. (See [the Maven POM reference](https://maven.apache.org/pom.html); search for the word "combine".)

Currently, the DANS repositories don't host any plug-ins. Not entirely true: for a few legacy projects we host patched third-party plug-ins. In those
cases the above `<repositories>`-element must be copied and adjusted to a `<pluginRepositories>` element as well.

HANDLING CIRCULAR DEPENDENCIES
------------------------------
It can happen that a dependency is included in one of the parent POMs, that also inherits from the same parent pom. Currently this is the case for `dans-scala-lib` and `dans-bag-lib`. To avoid redundant deploys, these circular dependencies should be handled as follows:

1. Test that the `SNAPSHOT` version of the parent POM with reference to the latest `SNAPSHOT` version of the dependency is behaving as expected.
2. In the parent POM, change the version number for the dependency to the next (not yet deployed) release version.
3. Release `dans-parent-pom` and deploy it to the Maven repository.
4. In the dependency's POM, change the parent POM version number to the newly deployed version of `dans-parent-pom`.
5. Build and deploy the inheriting project.

BUILDING FROM SOURCE
--------------------
Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher

Steps:

    git clone -o blessed https://github.com/DANS-KNAW/dans-parent-prom.git
    cd dans-parent-pom
    mvn install
