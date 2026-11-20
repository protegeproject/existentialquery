existentialquery
================

A plugin for Protege that allows the implied fillers of existential restrictions to be queried.

The 3.x series targets Protégé 6 and OWL API 5.5.1, with Java 11 or later.
Use the 2.x series with Protégé 5.

Build and test with `mvn --batch-mode clean verify`. The tests use HermiT 1.5.0
to check named and anonymous subclass expressions, equivalent fillers,
most-specific filler filtering, property chains, and updated results after
ontology changes and reasoner synchronization.

To test in Protégé 6, copy `target/existentialquery-3.0.0.jar` into the
application's `plugins` directory and restart. Add the **Existential Query**
view from **Window → Views → Query views**, select HermiT, and start the reasoner.
Enter a subclass expression and one object property, or a comma-separated chain
of properties, then click **Execute**.

For an ontology containing `Subject SubClassOf r some Filler` and
`Filler SubClassOf Parent`, querying `Subject` along `r` should return `Filler`,
`Parent`, and `owl:Thing`. Selecting **Return most specific fillers** should
leave only `Filler`. The anonymous expression `Subject and Parent` should also
return `Filler` in most-specific mode. After changing an existential restriction,
synchronize the reasoner and execute again to check the updated results.

Release and distribution integration are tracked in
[Protégé issue #1362](https://github.com/protegeproject/protege/issues/1362).
Only released versions that have been verified interactively should be added
to the Protégé 6 plugin registry.

To publish a release, set the POM version to the release version and push a tag
that exactly matches it, such as `3.0.0`. The publishing workflow tests, signs,
and publishes the artifacts through the Maven Central Portal. Manual workflow
runs are reserved for snapshot versions. Publishing uses the organization's
existing GPG and Maven Central secrets.
