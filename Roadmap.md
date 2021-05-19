# Overview

Late in 2018, MongoDB contributed Morphia to the community to maintain and evolve the project.  In spite of certain obvious lags 
and gaps in activity, there's a fair bit going on.  In this road map, we'll lay out out the current vision of the release train and try 
to give a sense of where things are heading.  This road map should be treated as informative rather than definitive.  It represents the 
current thinking and planning but such things are always subject to change due to feedback and other external factors such as driver 
updates, etc.  If any questions or comments should arise from this road map, please file an 
 [issue](https://github.com/MorphiaOrg/morphia/issues) and we'll try to address things as they come up.  There are currently three 
 versions of note currently active and each will be discussed below.

_(Last updated:  27 Dec 2018)_

# 1.4.0

The [1.4.0 release](https://github.com/MorphiaOrg/morphia/releases/tag/r1.4.0) was largely a bookkeeping release. With the move to the
community organization, Morphia could no longer live under the same package structure. The primary goal of 1.4.0 was to update the package
names and the maven coordinates. This release should be mostly identical to 1.3.2. No further updates are planned for 1.4.mapsOfStuff.  
Any such updates should be released as part of the 1.5.mapsOfStuff cycle.

# 1.5.0

1.5.0 will be the first significant release of Morphia in some time. It will contain both bug fixes and API updates. If 1.4.0 was the
transition release to the community project, 1.5.0 can be considered the transition point to the 2.0.mapsOfStuff cycle. As such many changes
are planned to help users transition to 2.0.0 with as little fuss as possible. Many changes will also help slim down Morphia's API to reduce
both the burden of learning Morphia's capabilities and the burden of maintaining and updating the API going forward. These details and more
are laid out below:

* **Deprecating much of Datastore.**  Over the years many convenience methods have been added to Datastore to make certain operations 
slightly easier.  This profusion of methods have made adding overloads for things like WriteConcern and ReadPreference awkward.  We're 
left with the choice adding such overrides for each variation on the interface or omitting some updates leading to inconsistencies in 
the API.  The decision was bad to deprecate those methods leading to fewer methods which will be easier to both understand for new 
comers and require less work when updating in the future.
* **Deprecating Morphia's Option classes.**  When these classes were introduced, the goal was to shield Morphia users from changes in the 
driver APIs.  In practice this has not worked out as well as hoped.  It has, in fact, led to an increase in maintenance burden.  So 2.0 
will take the opportunity to correct this misfeature and will expose and use the driver options classes directly.  This gives us two 
huge benefits:
    1.  As the driver adds new options to queries, updates, etc., Morphia gets these for free.  Users will no longer need to wait on a 
    Morphia update to provided access to these updated features.
    2. There will be less need for maintenance as Morphia will no longer need to track these classes to provide Morphia-side analogs for
     these features.

  In practice, these options classes have very similar interfaces, by design, so migration should not be terribly problematic.
* **Deprecating internal Morphia artifacts.**  Because Java's accessibility controls are rather limited in certain use cases, various 
aspects of Morphia's internal APIs have been exposed to users.  These were never intended for external use but as with any API, once 
it's out there folks will use it.  1.5.0 will attempt to rectify some of this by explicitly marking these items as deprecated/internal. 
 Future versions may change or remove these items without warning.
* **No significant mapper fixes**  The primary feature update in 2.0 will be moving to the new `PojoCodec` API.  This update will fix many
 of the outstanding mapping bugs in Morphia by simple virtue of upgrading.  As such, there won't be much focus on cleaning up issues 
 with the current mapping code.
* **Release timeline**.  As with most software projects, it will release when it's ready.    Hopefully, this means the first month or two 
of 2019 if all goes well.  Development time is split between 1.5 and 2.0 so the ultimate timing may we vary.

# 2.0.0

Much of 1.5.0 development is intended to start the culling process of removing redundant/overlapping methods.  As a result, 2.0's API 
should be much leaner and much easier reason about.  The hope is that this leaner API will be easier to extend and maintain.
* **Java 8 minimum support**.  As of 2.0, Morphia will move to requiring Java8+.
* **Updated Mapper**.  The keystone feature of 2.0 is the updated mapper.  Using updates to the driver, Morphia will transition off its 
own mapping code and leverage the infrastructure now provided by the driver.  This means that many of the mapping bugs around generics 
will begin working, e.g.  However it also means that the shapes of documents in the database might end up looking different.  Work is 
underway to mitigate as much of that as possible but it might not be 100% effective.
* **Realignment against driver APIs.** Morphia will make better use of the driver API.  Sometimes this will mean replacing Morphia's 
Options classes with the driver's Options.  In other cases, this will mean return types from methods will change.  The hope is that such
 changes will result in a cleaner, more future-proof API.  These changes do not come without some breakage.  Efforts are being made to 
 mitigate much of that but, again, will not be 100% effective.
* **Removal of modules**.  As of 2.0 there will only be the core module.  Modules such as `entityscanner-plug` and `logging-slf4j` have 
not seen any updates or apparent use in quite some time.  The entity scanner code was only ever half implemented and the logging code is 
vestigial at best.  2.0 will leverage slf4j directly internally and users can choose whatever logging implementation they would like.  
Any dependence on `dev.morphia.logging.Logger` will need to be updated after 2.0.  Steps should be taken now to migrate away from these 
types now.  

# 2.1.0

* **Aggregation updates**.  It's a little early to speculate too much about what 2.1.0 will need but there's a sizable back log of 
aggregation updates to complete.  The current aggregation API was a mild experiment but was sadly not communicated as such as well as it
 should have been.  The updates will will include both new features and, hopefully, and improved more usable API.  This might come in 
 the form of a parallel, experimental API that evolves and eventually replaces the current API.
