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
            classpath "com.github.thrive-framework:thrive-versioning-plugin:0.2.0-SNAPSHOT"
        }
    }

> There are some reported incidents where JitPack isn't working properly if you don't specify 
> repo name. It will probably work without it, but when in doubt, use it.

Use the plugin with old style convention:

    apply plugin: "com.github.thrive-versioning"

> New style of applying plugins isn't working properly. Stay tuned, there's 
> [a bug for that](https://github.com/thrive-framework/thrive-versioning-plugin/issues/1).

## Algorithm

The [rules](/src/main/groovy/com/github/thriveframework/plugin/ThriveVersioningPlugin.groovy)
implement following algorithm for determining version:
- find latest tag with name in format `x.y.z`
- calculate number of commits since that tag
- if we're on master branch:
  - if the tag is on this commit (number of commits since the tag = 0), then use name of the tag (`x.y.z`) as version
  - else, use version `x.y+1.z-RC` 
- if we're on branch named with format `a.b.c`:
  - **assert** that `a.b.c` is direct successor to `x.y.z` (see below)
  - use version `a.b.c-SNAPSHOT`
- else, use version `x.y+1.z-FEATURE+(haschode of branch name)_(number of commits since last tag)`

Besides setting `project.version`, this plugin also adds a property (via `project.ext`) `projectState` with one of following values:
- `RELEASE`
- `RC`
- `SNAPSHOT`
- `FEATURE`

### Limitations

Algorithm presented above assumes that most basic increments happens on minor part of the version.

This means that even if you work on major or patch version and merge to `master` without tagging,
it will result in minor increment (with `RC` state).

To give an example:

If your last `master` release was `0.1.0` and you were working on `0.1.1` branch, then, when 
merged to `master`without tagging, `master` commit will have `0.2.0-RC` version instead of
`0.1.1-RC`. Same happens with major increment (`0.8.0` on `master`, `1.0.0` on branch, after
merge it will be `0.9.0-RC` and not `1.0.0-RC`). 

This may lead to inconsistencies if you don't merge patches to `master`, as well as some weird
behaviour on major increments. 

> In the end, most of the projects are not Spring, with its backports and patches. This versioning
> model should suffice.

### Version successors

One of the steps above mentioned "direct succesor".

Direct successors to version `a.b.c` is one of following:
- `a+1.0.0`
- `a.b+1.0`
- `a.b.c+1`

When asserting (as mentioned above), it is assumed that there is no release tag (`-something` suffix) or metadata (`+something` suffix) in branch name.

**Long story short:** dev-like `a.b.c` branches should not have `-` nor `+` suffixes, and they
should increment version by some part, while zeroing less important parts. 

## Modified GitFlow

Use `master` as you've always used it.

Make tag-based releases from `master`.

**Make sure that there is at least one tag! If not, there will be error. Simplest practice is to tag first commit with `0.0.0`.**
> This is considered bug, but a minor one. Stay tuned, or submit a PR.

Instead of using `dev`/`develop`/`development` branch, use branch name in SemVer format `a.b.c`.
This name should be direct successor to last tagged release version (see above).

Merge from `a.b.c` to master to create releases. Do not remove these branches, to allow using 
past versions with JitPack.

> This project is using itself. It has several commits in history before release of 0.1.0,
> because it was initially developed as part of [thrive-service-plugin](https://github.com/thrive-framework/thrive-service-plugin)