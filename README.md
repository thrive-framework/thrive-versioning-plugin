# thrive-versioning-plugin

This trivial plugin encapsulates config for [org.unbroken-dome.gitversion](https://plugins.gradle.org/plugin/org.unbroken-dome.gitversion) plugin.

It is specifically intended to be used with [JitPack](https://jitpack.io) in modified 
[GitFlow](https://nvie.com/posts/a-successful-git-branching-model/) (see below) and [SemVer](https://semver.org/).

Optionally, it can do some repetetive configuration for publishing and publishing to JitPack
specifically.

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
            //fixme as soon as 0.2.0 is out, update this
            classpath "com.github.thrive-framework:thrive-versioning-plugin:0.2.0-SNAPSHOT"
        }
    }

> There are some reported incidents where JitPack isn't working properly if you don't specify 
> repo name. It will probably work without it, but when in doubt, use it.

Use the plugin with old style convention:

    apply plugin: "com.github.thrive-versioning"

> New style of applying plugins isn't working properly. Stay tuned, there's 
> [a bug for that](https://github.com/thrive-framework/thrive-versioning-plugin/issues/1).

## Configure and let configure

This plugin does not only configure versioning rules, but also can configure `maven-publish` 
module and ensure that the project is properly configured for JitPack.

Let's start with this plugins extension (defaults are shown in the snippet):

    thriveVersioning {
        configurePublishing = true
        configureForJitpack = true //note that "pack" is lowercase
    } 

If `configurePublishing` is `true`, then:
- apply `maven-publish` plugin (if not applied yet)
- set source and target compatibility to 1.8
- define `sourcesJar` task with classifier `sources`
- define `javadocJar` task with classifier `javadoc`
- configure `publishing` section by defining a publication (named `main`) that has compiled code, sources
  and javadoc in scope

Additionally, if `configureForJitpack` is `true` (as well as `configurePublishing`), then:
- make `publishMainPublicationToMavenLocal` depend on `build` (because sometimes JitPack can complain about missing JARs)
- for every non-root project change group to 
  [JitPack convention](https://jitpack.io/docs/BUILDING/#multi-module-projects)
  > There may be a bug for multi-project builds with deep nesting (anything deeper than root and subprojects below)
  > but it is not confirmed yet;
  > manually setting project name to something else than repo name may also cause a bug.
  > If you encounter any of these, please submit them. I don't want to do that before confirming them, and I have other
  > priorities in scope of Thrive.
  >
  > Welp, there is a bug, but even bigger than I thought. See #7

## Algorithm for determining version

The [rules](/src/main/groovy/com/github/thriveframework/plugin/ThriveVersioningPlugin.groovy)
implement following algorithm for determining version:
- find latest tag with name in format `x.y.z`; if there is no tag of that form, use 
`x.y.z = 0.0.0` 
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

> First of all, feature branches are not configured in a way that is aligned with JitPack.
> That is intentional, but there is a
>
> TODO When using configureForJitpack, change the strategy for feature branches (if possible)
>
> Same issue may impact RC versions

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

Instead of using `dev`/`develop`/`development` branch, use branch name in SemVer format `a.b.c`.
This name should be direct successor to last tagged release version (see above).

Merge from `a.b.c` to master to create releases. Do not remove these branches, to allow using 
past versions with JitPack.

> This project is using itself. It has several commits in history before release of 0.1.0,
> because it was initially developed as part of [thrive-service-plugin](https://github.com/thrive-framework/thrive-service-plugin)


------------------

> Note to people browsing the repo history:
>
> If you're wondering why this repo has tag `0.0.0` - when this project started using itself, it
> was version `0.1.0-SNAPSHOT`. `0.1.0` was released with a bug that required at least one tag.
>
> That bug was fixed in `0.2.0`, in one of the first `SNAPSHOT`s (see 
> [this issue](https://github.com/thrive-framework/thrive-versioning-plugin/issues/2)).
> 
> To make earliest versions available with JitPack, I'm gonna leave the redundant tag in place.
>
> This doesn't (I repeat: `DOESN'T`) impact your versioning. If you're using `0.2.0-SNAPSHOT` and
> further, it won't impact you. 