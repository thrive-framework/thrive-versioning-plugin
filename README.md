# thrive-versioning-plugin

This trivial plugin encapsulates config for [org.unbroken-dome.gitversion](https://plugins.gradle.org/plugin/org.unbroken-dome.gitversion) plugin.

It is specifically intended to be used with [JitPack](https://jitpack.io) in modified 
[GitFlow](https://nvie.com/posts/a-successful-git-branching-model/) (see below) and [SemVer](https://semver.org/).

## Usage

Add JitPack repo to buildscript dependencies and specify dependency on this plugin:

    buildscript {
        repositories {
            maven {
                name "jitpack"
                url "https://jitpack.io"
            }
        }
        dependencies {
            classpath "com.github.thrive-framework:thrive-versioning-plugin:0.1.0"
        }
    }

> There are some reported incidents where JitPack isn't working properly if you don't specify repo name.
> It will probably work without it, but when in doubt, use it.

Use the plugin with old style convention:

    apply plugin: "com.github.thrive-versioning"

or with new style:

    plugins {
        id "com.github.thrive-versioning" version "0.1.0"
    }

## Algorithm

The [rules](/src/main/groovy/com/github/thriveframework/plugin/ThriveVersioningPlugin.groovy)
implement following algorithm for determining version:
- find latest tag with name in format `x.y.z`
- calculate number of commits since that tag
- if we're on master branch:
  - if the tag is on this commit (number of commits since the tag = 0), then use name of commit (`x.y.z`) as version
  - else, use version `x.y+1.z-RC` 
- if we're on branch named with format `a.b.c`:
  - increment minor part of tag (`y`), **assert that branch name and version after incrementing is equal (assert `a.b.c == x.y+1.z`**
  > this is subject to change, to allow for major and patch version changes
  - use version `a.b.c-SNAPSHOT`
- else, use version `x.y+1.z-FEATURE`

Besides setting `project.version`, this plugin also adds a property (via `project.ext`) `projectState` with one of following values:
- `RELEASE`
- `RC`
- `SNAPSHOT`
- `FEATURE`

## Modified GitFlow

Use `master` as you've always used it.

Make tag-based releases from `master`.

**Make sure that there is at least one tag! If not, there will be error. Simplest practice is to tag first commit with `0.0.0`.**
> This is considered bug, but a minor one. Stay tuned, or submit a PR.

Instead of using `dev`/`develop`/`development` branch, use branch name in SemVer format `a.b.c`. This name should be same as last release tag, but with minor part (`b`) incremented.

Merge from `a.b.c` to master to create releases. Do not remove these branches, to allow using past versions with JitPack.

> This project is using itself. It has several commits in history before release of 0.1.0,
> because it was initially developed as part of [thrive-service-plugin](https://github.com/thrive-framework/thrive-service-plugin)