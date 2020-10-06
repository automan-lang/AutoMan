---
layout: default
title: Changelog
---

<div id="releases">
  <div id="spinner" style="text-align:center; color: #888; font-size:100px;">
    <span class="spinner"></span>
  </div>
</div>

<script src="https://cdn.jsdelivr.net/marked/0.3.5/marked.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.18.1/moment.min.js"></script>

<script>
  var releasesAPI = 'https://api.github.com/repos/{{site.id}}/releases';
  var issuesBase = 'https://github.com/{{site.id}}/issues/';
</script>

<script src="{{site.github.url}}/assets/js/changelog.js"></script>

|Version|Date|Notes|
| --- | --- | --- |
|1.4.1|2020-10-06|Point release. Bugfix that caused crash for checkbox distribution questions.|
|1.4.0|2020-10-05|Major release. Support for latest Amazon MTurk SDK (credit: [Emmie Hine](https://www.linkedin.com/in/emmie-hine/)).|
|1.2.0||New DSL syntax|
|     ||AutoMan now has a simplified syntax that does away with some Scala idiosyncrasies and simplifies imports.|
|1.1.7||Point release.|
|     ||Fix a bug in the qualification cleanup routine.  See [issue #28](https://github.com/dbarowy/AutoMan/issues/28).
|1.1.6||Major release.|
|     ||Now includes support for quality-controlled estimation queries!|
|     ||Preliminary estimation composition support.|
|     ||Exponential backoff to mitigate MTurk rate-limiting.|
|     ||Numerous bigfixes and performance improvements.|
|1.0.1||Maintenance release.|
|     ||Bugfix for timeout policy calculation.|
|     ||Renamed misleading distribution answer type name.|
|1.0.0||Major release.|
|     ||Syntax changes.|
|     ||New question types.|
|     ||Per-question budgets.|
|     ||Bias due to multiple comparisons eliminated with Bonferroni correction.|
|     ||New memo engine that allows MTurk computation to be resumed without additional cost (timeouts notwithstanding).|
|     ||Support for mocks.|
|     ||Plugin architecture.|
|     ||Numerous other changes for better reliability.|
|0.4.0||Maintenance release.|
|     ||Switch to SBT build system. Updates for Scala 2.10.|
|0.3.0||Maintenance release|
|     ||Buildr Buildfile, including reorganization of project directory.|
|0.2.1||Maintenance release|
|     ||Update to work with latest MTurk API (1.6.0).|
|     ||Better log output, including scheduler object logging.|
|     ||New Automatic Number Plate Recognition (ANPR) app.|
|0.2.0||Major rewrite to simplify syntax.|
|0.1.0||First release.|
