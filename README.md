# Jar Shaker

Similar to Javascript Tree-shaking, let's find classes that are not in-use for a project and their dependencies and repackages it.

Support for reflection is TBD. Obviously there are some hard constraints here, but I'll try to mirror what ahead-of-time compilation (i.e. GraalVM) does in this regard. Perhaps add some ways to force include (via globs) classes for reflection reasons if the user of this project knows them ahead of time. I'll most likely support "reflect-config.json" as well as force-inclusions via gradle config (can be sufficient for simple SPI).

Performance is also a concern. I know the popular shadow plugin for shaded jars has minify support; I want to be faster and potentially not support features to meet this goal.

I care about security here and minimizing alerts flagged by CVE scanners. Often people include dependencies and only use some subset of the code. Dead code still gets picked up by popular scanners like XRay or Trivvy (TODO research deeply how these things work) and this imposes an operational burden on maintainers and organizations. Less code == faster scans & less CVEs.

This is a work in progress.
