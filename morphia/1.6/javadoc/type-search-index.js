<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Untitled :: Morphia Docs</title>
    <meta name="generator" content="Antora 2.3.3">
    <link rel="stylesheet" href="../../../_/css/javadoc.css">
<!--    <script type="text/javascript" src="../../../_/js/vendor/script.js"></script> This one is GPL -->
    <script type="text/javascript" src="../../../_/js/vendor/javadoc.js"></script>
<!--    <script type="text/javascript" src="../../../_/_/js/vendor/javadoc/jquery/jszip-utils/dist/jszip-utils.js"></script>-->
    <!--[if IE]>
<!--    <script type="text/javascript" src="../../../_/_/js/vendor/javadoc/jquery/jszip-utils/dist/jszip-utils-ie.js"></script>-->
    <![endif]-->
<!--    <script type="text/javascript" src="../../../_/_/js/vendor/javadoc/jquery/jquery-3.3.1.js"></script>-->
<!--    <script type="text/javascript" src="../../../_/_/js/vendor/javadoc/jquery/jquery-migrate-3.0.1.js"></script>-->
<!--    <script type="text/javascript" src="../../../_/_/js/vendor/javadoc/jquery/jquery-ui.js"></script>-->
  </head>
  <body class="article">
<header class="header" role="banner">
    <nav class="navbar">
        <div class="navbar-brand">
            <img height="80%" src="../../../_/img/logo.png">
        </div>
        <div class="navbar-brand">
            <div class="navbar-item">
                <a href="https://morphia.dev">Morphia</a>
                <span class="separator">//</span>
                <a href="../../..">Docs</a>
            </div>
            <button class="navbar-burger" data-target="topbar-nav">
                <span></span>
                <span></span>
                <span></span>
            </button>
        </div>
        <div id="topbar-nav" class="navbar-menu">
            <div class="navbar-end">
                <div class="navbar-item has-dropdown is-hoverable">
                    <div class="navbar-link">Projects</div>
                    <div class="navbar-dropdown">
                        <div class="navbar-item"><strong>Core</strong></div>
                        <a class="navbar-item" href="https://github.com/MorphiaOrg/morphia">Repository</a>
                        <a class="navbar-item" href="https://github.com/MorphiaOrg/morphia/issues">Issue Tracker</a>
                        <hr class="navbar-divider">
                        <div class="navbar-item"><strong>Critter</strong></div>
                        <a class="navbar-item" href="https://github.com/MorphiaOrg/critter">Repository</a>
                        <a class="navbar-item" href="https://github.com/MorphiaOrg/critter/issues">Issue Tracker</a>
                        <!--                        <hr class="navbar-divider">-->
                        <!--                        <a class="navbar-item" href="https://github.com/MorphiaOrg/morphia/blob/master/contributing.adoc">Contributing</a></div>-->
                    </div>
                </div>

                <div class="navbar-item has-dropdown is-hoverable">
                    <div class="navbar-link">Community</div>
                    <div class="navbar-dropdown is-right">
                        <a class="navbar-item" href="https://developer.mongodb.com/community/forums/c/drivers-odms/">Drivers &
                            ODMs chat</a>
                    </div>
                </div>

                <a class="navbar-item" href="https://twitter.com/evanchooly">
                    <span class="icon">
                        <svg aria-hidden="true" data-icon="twitter" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
                            <path fill="#57aaee"
                                  d="M459.37 151.716c.325 4.548.325 9.097.325 13.645 0 138.72-105.583 298.558-298.558 298.558-59.452 0-114.68-17.219-161.137-47.106 8.447.974 16.568 1.299 25.34 1.299 49.055 0 94.213-16.568 130.274-44.832-46.132-.975-84.792-31.188-98.112-72.772 6.498.974 12.995 1.624 19.818 1.624 9.421 0 18.843-1.3 27.614-3.573-48.081-9.747-84.143-51.98-84.143-102.985v-1.299c13.969 7.797 30.214 12.67 47.431 13.319-28.264-18.843-46.781-51.005-46.781-87.391 0-19.492 5.197-37.36 14.294-52.954 51.655 63.675 129.3 105.258 216.365 109.807-1.624-7.797-2.599-15.918-2.599-24.04 0-57.828 46.782-104.934 104.934-104.934 30.213 0 57.502 12.67 76.67 33.137 23.715-4.548 46.456-13.32 66.599-25.34-7.798 24.366-24.366 44.833-46.132 57.827 21.117-2.273 41.584-8.122 60.426-16.243-14.292 20.791-32.161 39.308-52.628 54.253z"></path>
                        </svg>
                    </span>
                </a>
            </div>
        </div>
    </nav>
</header>

<div class="body">
<div class="nav-container" data-component="morphia" data-version="1.6">
  <aside class="nav">
    <div class="panels">
<div class="nav-panel-menu is-active" data-panel="menu">
  <nav class="nav-menu">
    <h3 class="title"><a href="../index.html">Morphia</a></h3>
<ul class="nav-list">
  <li class="nav-item" data-depth="0">
<ul class="nav-list">
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../getting-started.html">Getting Started</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../quick-tour.html">Quick Tour</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../migration.html">Migrating to 2.0</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../querying.html">Querying</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../updating.html">Updating</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../aggregation.html">Aggregation</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../annotations.html">Annotations</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../indexing.html">Indexing</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../lifeCycleMethods.html">Life Cycle Methods</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../issues-help.html">Issues &amp; Support</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="index.html">Javadoc</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../jrebel.html">JRebel</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../validationExtension.html">Validation Extension</a>
  </li>
</ul>
  </li>
</ul>
  </nav>
</div>
<div class="nav-panel-explore" data-panel="explore">
  <div class="context">
    <span class="title">Morphia</span>
    <span class="version">1.6</span>
  </div>
  <ul class="components">
    <li class="component">
      <span class="title">Critter</span>
      <ul class="versions">
        <li class="version is-latest">
          <a href="../../../critter/4/index.html">4</a>
        </li>
      </ul>
    </li>
    <li class="component">
      <span class="title">Home</span>
      <ul class="versions">
        <li class="version is-latest">
          <a href="../../../landing/index.html">landing</a>
        </li>
      </ul>
    </li>
    <li class="component is-current">
      <span class="title">Morphia</span>
      <ul class="versions">
        <li class="version">
          <a href="../../2.2/index.html">2.2-SNAPSHOT</a>
        </li>
        <li class="version is-latest">
          <a href="../../2.1/index.html">2.1</a>
        </li>
        <li class="version">
          <a href="../../2/index.html">2</a>
        </li>
        <li class="version is-current">
          <a href="../index.html">1.6</a>
        </li>
      </ul>
    </li>
  </ul>
</div>
    </div>
  </aside>
</div>
<main>
<div class="toolbar" role="navigation">
<button class="nav-toggle"></button>
  <a href="../../../landing/index.html" class="home-link"></a>
<nav class="breadcrumbs" aria-label="breadcrumbs">
</nav>
<div class="page-versions">
  <button class="version-menu-toggle" title="Show other versions of page">1.6</button>
  <div class="version-menu">
    <a class="version" href="../../2.2/javadoc/type-search-index.js">2.2-SNAPSHOT</a>
    <a class="version" href="../../2.1/javadoc/type-search-index.js">2.1</a>
    <a class="version" href="../../2/javadoc/type-search-index.js">2</a>
    <a class="version is-current" href="type-search-index.js">1.6</a>
  </div>
</div>
</div>
    <article class="javadoc">
        typeSearchIndex = [{"p":"dev.morphia.query","l":"AbstractCriteria"},{"p":"dev.morphia","l":"AbstractEntityInterceptor"},{"p":"dev.morphia.query","l":"AbstractQueryFactory"},{"p":"dev.morphia.mapping.lazy.proxy","l":"AbstractReference"},{"p":"dev.morphia.aggregation","l":"Accumulator"},{"p":"dev.morphia","l":"AdvancedDatastore"},{"p":"dev.morphia.aggregation","l":"AggregationPipeline"},{"p":"dev.morphia.aggregation","l":"AggregationPipelineImpl"},{"l":"All Classes","url":"allclasses-index.html"},{"p":"dev.morphia.query.validation","l":"AllOperationValidator"},{"p":"dev.morphia.annotations","l":"AlsoLoad"},{"p":"dev.morphia.query","l":"ArraySlice"},{"p":"dev.morphia","l":"AuthenticationException"},{"p":"dev.morphia.dao","l":"BasicDAO"},{"p":"dev.morphia.converters","l":"BigDecimalConverter"},{"p":"dev.morphia.converters","l":"BooleanConverter"},{"p":"dev.morphia.query","l":"BucketAutoOptions"},{"p":"dev.morphia.query","l":"BucketOptions"},{"p":"dev.morphia.mapping","l":"MapperOptions.Builder"},{"p":"dev.morphia.converters","l":"ByteConverter"},{"p":"dev.morphia.annotations","l":"CappedAt"},{"p":"dev.morphia.mapping.lazy","l":"CGLibLazyProxyFactory"},{"p":"dev.morphia.converters","l":"CharacterConverter"},{"p":"dev.morphia.converters","l":"CharArrayConverter"},{"p":"dev.morphia.mapping.validation","l":"ClassConstraint"},{"p":"dev.morphia.converters","l":"ClassConverter"},{"p":"dev.morphia.annotations","l":"Collation"},{"p":"dev.morphia.mapping.lazy.proxy","l":"CollectionObjectReference"},{"p":"dev.morphia.mapping.experimental","l":"CollectionReference"},{"p":"dev.morphia.mapping.validation","l":"ConstraintViolation"},{"p":"dev.morphia.mapping.validation","l":"ConstraintViolationException"},{"p":"dev.morphia.annotations","l":"ConstructorArgs"},{"p":"dev.morphia.mapping.validation.classrules","l":"ContainsEmbeddedWithId"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"ContradictingFieldAnnotation"},{"p":"dev.morphia.converters","l":"ConverterException"},{"p":"dev.morphia.converters","l":"ConverterNotFoundException"},{"p":"dev.morphia.annotations","l":"Converters"},{"p":"dev.morphia.converters","l":"Converters"},{"p":"dev.morphia.geo","l":"CoordinateReferenceSystem"},{"p":"dev.morphia.geo","l":"CoordinateReferenceSystemType"},{"p":"dev.morphia.query","l":"CountOptions"},{"p":"dev.morphia.query","l":"Criteria"},{"p":"dev.morphia.query","l":"CriteriaContainer"},{"p":"dev.morphia.query","l":"CriteriaContainerImpl"},{"p":"dev.morphia.query","l":"CriteriaJoin"},{"p":"dev.morphia.converters","l":"CurrencyConverter"},{"p":"dev.morphia.converters","l":"CustomConverters"},{"p":"dev.morphia.mapping","l":"CustomMapper"},{"p":"dev.morphia","l":"DAO"},{"p":"dev.morphia.dao","l":"DAO"},{"p":"dev.morphia","l":"Datastore"},{"p":"dev.morphia","l":"DatastoreImpl"},{"p":"dev.morphia.mapping.lazy","l":"DatastoreProvider"},{"p":"dev.morphia.converters","l":"DateConverter"},{"p":"dev.morphia.mapping","l":"DateStorage"},{"p":"dev.morphia.converters","l":"DefaultConverters"},{"p":"dev.morphia.mapping","l":"DefaultCreator"},{"p":"dev.morphia.mapping.cache","l":"DefaultEntityCache"},{"p":"dev.morphia.mapping.cache","l":"DefaultEntityCacheFactory"},{"p":"relocated.morphia.org.apache.commons.collections","l":"DefaultMapEntry"},{"p":"dev.morphia.query","l":"DefaultQueryFactory"},{"p":"dev.morphia.query.validation","l":"DefaultTypeValidator"},{"p":"dev.morphia","l":"DeleteOptions"},{"p":"dev.morphia.converters","l":"DoubleConverter"},{"p":"dev.morphia.query.validation","l":"DoubleTypeValidator"},{"p":"dev.morphia.mapping.validation.classrules","l":"DuplicatedAttributeNames"},{"p":"dev.morphia.annotations","l":"Embedded"},{"p":"dev.morphia.mapping.validation.classrules","l":"EmbeddedAndId"},{"p":"dev.morphia.mapping.validation.classrules","l":"EmbeddedAndValue"},{"p":"dev.morphia.annotations","l":"Entity"},{"p":"dev.morphia.mapping.validation.classrules","l":"EntityAndEmbed"},{"p":"dev.morphia.query.validation","l":"EntityAnnotatedValueValidator"},{"p":"dev.morphia.mapping.cache","l":"EntityCache"},{"p":"dev.morphia.mapping.cache","l":"EntityCacheFactory"},{"p":"dev.morphia.mapping.cache","l":"EntityCacheStatistics"},{"p":"dev.morphia.mapping.validation.classrules","l":"EntityCannotBeMapOrIterable"},{"p":"dev.morphia","l":"EntityInterceptor"},{"p":"dev.morphia.annotations","l":"EntityListeners"},{"p":"dev.morphia.mapping.lazy.proxy","l":"EntityObjectReference"},{"p":"dev.morphia.query.validation","l":"EntityTypeAndIdValueValidator"},{"p":"dev.morphia.converters","l":"EnumConverter"},{"p":"dev.morphia.converters","l":"EnumSetConverter"},{"p":"dev.morphia.mapping","l":"EphemeralMappedField"},{"p":"dev.morphia.query.validation","l":"ExistsOperationValidator"},{"p":"dev.morphia.logging.jdk","l":"FasterJDKLogger"},{"p":"dev.morphia.logging.jdk","l":"FastestJDKLogger"},{"p":"dev.morphia.annotations","l":"Field"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"FieldConstraint"},{"p":"dev.morphia.query","l":"FieldEnd"},{"p":"dev.morphia.query","l":"FieldEndImpl"},{"p":"dev.morphia.mapping.validation.classrules","l":"FieldEnumString"},{"p":"dev.morphia.query","l":"FilterOperator"},{"p":"dev.morphia","l":"FindAndModifyOptions"},{"p":"dev.morphia.query","l":"FindOptions"},{"p":"dev.morphia.converters","l":"FloatConverter"},{"p":"dev.morphia.geo","l":"GeoJson"},{"p":"dev.morphia.geo","l":"GeoJsonType"},{"p":"dev.morphia.geo","l":"Geometry"},{"p":"dev.morphia.geo","l":"GeometryCollection"},{"p":"dev.morphia.geo","l":"GeometryConverter"},{"p":"dev.morphia.geo","l":"GeometryQueryConverter"},{"p":"dev.morphia.geo","l":"GeometryShapeConverter"},{"p":"dev.morphia.aggregation","l":"GeoNear"},{"p":"dev.morphia.aggregation","l":"GeoNear.GeoNearBuilder"},{"p":"dev.morphia.query.validation","l":"GeoWithinOperationValidator"},{"p":"dev.morphia.query","l":"BucketAutoOptions.Granularity"},{"p":"dev.morphia.aggregation","l":"Group"},{"p":"dev.morphia.annotations","l":"Id"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"IdDoesNotMix"},{"p":"dev.morphia.converters","l":"IdentityConverter"},{"p":"dev.morphia.annotations","l":"IdGetter"},{"p":"dev.morphia.annotations","l":"Index"},{"p":"dev.morphia.annotations","l":"Indexed"},{"p":"dev.morphia.annotations","l":"Indexes"},{"p":"dev.morphia.annotations","l":"IndexOptions"},{"p":"dev.morphia.query.validation","l":"InOperationValidator"},{"p":"dev.morphia","l":"InsertOptions"},{"p":"dev.morphia.converters","l":"InstantConverter"},{"p":"dev.morphia.converters","l":"IntegerConverter"},{"p":"dev.morphia.query.validation","l":"IntegerTypeValidator"},{"p":"dev.morphia.converters","l":"IterableConverter"},{"p":"dev.morphia.logging.jdk","l":"JDKLogger"},{"p":"dev.morphia.logging.jdk","l":"JDKLoggerFactory"},{"p":"dev.morphia","l":"Key"},{"p":"dev.morphia.converters","l":"KeyConverter"},{"p":"dev.morphia.query.validation","l":"KeyValueTypeValidator"},{"p":"dev.morphia.mapping.lazy","l":"LazyFeatureDependencies"},{"p":"dev.morphia.mapping.lazy","l":"LazyProxyFactory"},{"p":"dev.morphia.mapping.lazy.proxy","l":"LazyReferenceFetchingException"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"LazyReferenceMissingDependencies"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"LazyReferenceOnArray"},{"p":"dev.morphia.mapping.validation","l":"ConstraintViolation.Level"},{"p":"dev.morphia.geo","l":"LineString"},{"p":"dev.morphia.geo","l":"GeometryShapeConverter.LineStringConverter"},{"p":"dev.morphia.query.validation","l":"ListValueValidator"},{"p":"dev.morphia.converters","l":"LocalDateConverter"},{"p":"dev.morphia.converters","l":"LocalDateTimeConverter"},{"p":"dev.morphia.converters","l":"LocaleConverter"},{"p":"dev.morphia.converters","l":"LocalTimeConverter"},{"p":"dev.morphia.logging","l":"Logger"},{"p":"dev.morphia.logging","l":"LoggerFactory"},{"p":"dev.morphia.logging","l":"Logr"},{"p":"dev.morphia.logging","l":"LogrFactory"},{"p":"dev.morphia.converters","l":"LongConverter"},{"p":"dev.morphia.query.validation","l":"LongTypeValidator"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"MapKeyDifferentFromString"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"MapNotSerializable"},{"p":"dev.morphia.mapping.lazy.proxy","l":"MapObjectReference"},{"p":"dev.morphia.converters","l":"MapOfValuesConverter"},{"p":"dev.morphia.mapping","l":"MappedClass"},{"p":"dev.morphia.mapping","l":"MappedField"},{"p":"dev.morphia.mapping","l":"Mapper"},{"p":"dev.morphia.mapping","l":"MapperOptions"},{"p":"dev.morphia.mapping","l":"MappingException"},{"p":"dev.morphia.query.internal","l":"MappingIterable"},{"p":"dev.morphia.mapping.validation","l":"MappingValidator"},{"p":"dev.morphia","l":"MapReduceOptions"},{"p":"dev.morphia","l":"MapreduceResults"},{"p":"dev.morphia","l":"MapreduceType"},{"p":"dev.morphia.mapping.experimental","l":"MapReference"},{"p":"dev.morphia.query","l":"Meta"},{"p":"dev.morphia.query","l":"Meta.MetaDataKeyword"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"MisplacedProperty"},{"p":"dev.morphia.query.validation","l":"ModOperationValidator"},{"p":"dev.morphia","l":"Morphia"},{"p":"dev.morphia.query.internal","l":"MorphiaCursor"},{"p":"dev.morphia.query","l":"MorphiaIterator"},{"p":"dev.morphia.query.internal","l":"MorphiaKeyCursor"},{"p":"dev.morphia.query","l":"MorphiaKeyIterator"},{"p":"dev.morphia.logging","l":"MorphiaLoggerFactory"},{"p":"dev.morphia.mapping.experimental","l":"MorphiaReference"},{"p":"dev.morphia.internal","l":"MorphiaUtils"},{"p":"dev.morphia.geo","l":"MultiLineString"},{"p":"dev.morphia.geo","l":"GeometryShapeConverter.MultiLineStringConverter"},{"p":"dev.morphia.mapping.validation.classrules","l":"MultipleId"},{"p":"dev.morphia.mapping.validation.classrules","l":"MultipleVersions"},{"p":"dev.morphia.geo","l":"MultiPoint"},{"p":"dev.morphia.geo","l":"GeometryShapeConverter.MultiPointConverter"},{"p":"dev.morphia.geo","l":"MultiPolygon"},{"p":"dev.morphia.geo","l":"GeometryShapeConverter.MultiPolygonConverter"},{"p":"dev.morphia.geo","l":"NamedCoordinateReferenceSystem"},{"p":"dev.morphia.geo","l":"NamedCoordinateReferenceSystemConverter"},{"p":"dev.morphia.mapping.validation.classrules","l":"NoId"},{"p":"dev.morphia.query.validation","l":"NotInOperationValidator"},{"p":"dev.morphia.annotations","l":"NotSaved"},{"p":"dev.morphia","l":"ObjectFactory"},{"p":"dev.morphia.converters","l":"ObjectIdConverter"},{"p":"dev.morphia.query.validation","l":"OperationValidator"},{"p":"dev.morphia.query","l":"BucketAutoOptions.OutputOperation"},{"p":"dev.morphia.query","l":"BucketOptions.OutputOperation"},{"p":"dev.morphia.internal","l":"PathTarget"},{"p":"dev.morphia.query.validation","l":"PatternValueValidator"},{"p":"dev.morphia.geo","l":"Point"},{"p":"dev.morphia.query","l":"Shape.Point"},{"p":"dev.morphia.geo","l":"PointBuilder"},{"p":"dev.morphia.geo","l":"GeometryShapeConverter.PointConverter"},{"p":"dev.morphia.geo","l":"Polygon"},{"p":"dev.morphia.geo","l":"GeometryShapeConverter.PolygonConverter"},{"p":"dev.morphia.annotations","l":"Polymorphic"},{"p":"dev.morphia.annotations","l":"PostLoad"},{"p":"dev.morphia.annotations","l":"PostPersist"},{"p":"dev.morphia.annotations","l":"PreLoad"},{"p":"dev.morphia.annotations","l":"PrePersist"},{"p":"dev.morphia.annotations","l":"PreSave"},{"p":"dev.morphia.aggregation","l":"Projection"},{"p":"dev.morphia.annotations","l":"Property"},{"p":"dev.morphia.mapping.lazy.proxy","l":"ProxiedEntityReference"},{"p":"dev.morphia.mapping.lazy.proxy","l":"ProxiedEntityReferenceList"},{"p":"dev.morphia.mapping.lazy.proxy","l":"ProxiedEntityReferenceMap"},{"p":"dev.morphia.mapping.lazy.proxy","l":"ProxiedReference"},{"p":"dev.morphia.mapping.lazy.proxy","l":"ProxyHelper"},{"p":"dev.morphia.query","l":"PushOptions"},{"p":"dev.morphia.query","l":"Query"},{"p":"dev.morphia.query","l":"QueryException"},{"p":"dev.morphia.query","l":"QueryFactory"},{"p":"dev.morphia.query","l":"QueryImpl"},{"p":"dev.morphia.query","l":"QueryResults"},{"p":"dev.morphia.annotations","l":"Reference"},{"p":"dev.morphia.converters.experimental","l":"ReferenceConverter"},{"p":"relocated.morphia.org.apache.commons.collections","l":"ReferenceMap"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"ReferenceToUnidentifiable"},{"p":"dev.morphia.annotations","l":"Serialized"},{"p":"dev.morphia.converters","l":"SerializedObjectConverter"},{"p":"dev.morphia.mapping","l":"Serializer"},{"p":"dev.morphia.query","l":"Shape"},{"p":"dev.morphia.converters","l":"ShortConverter"},{"p":"dev.morphia.logging.jdk","l":"ShortFormatter"},{"p":"dev.morphia.logging","l":"SilentLogger"},{"p":"dev.morphia.converters","l":"SimpleValueConverter"},{"p":"dev.morphia.mapping.experimental","l":"SingleReference"},{"p":"dev.morphia.query.validation","l":"SizeOperationValidator"},{"p":"dev.morphia.aggregation","l":"Sort"},{"p":"dev.morphia.query","l":"Sort"},{"p":"dev.morphia","l":"MapreduceResults.Stats"},{"p":"dev.morphia.converters","l":"StringConverter"},{"p":"dev.morphia.annotations","l":"Text"},{"p":"dev.morphia.converters","l":"TimestampConverter"},{"p":"dev.morphia.annotations","l":"Transient"},{"p":"dev.morphia.query","l":"Type"},{"p":"dev.morphia.converters","l":"TypeConverter"},{"p":"dev.morphia.query.validation","l":"TypeValidator"},{"p":"dev.morphia.query","l":"UpdateException"},{"p":"dev.morphia.query","l":"UpdateOperations"},{"p":"dev.morphia.query","l":"UpdateOperator"},{"p":"dev.morphia.query","l":"UpdateOpsImpl"},{"p":"dev.morphia","l":"UpdateOptions"},{"p":"dev.morphia.query","l":"UpdateResults"},{"p":"dev.morphia.converters","l":"URIConverter"},{"p":"dev.morphia.converters","l":"UUIDConverter"},{"p":"dev.morphia.annotations","l":"Validation"},{"p":"dev.morphia","l":"ValidationBuilder"},{"p":"dev.morphia.query","l":"ValidationException"},{"p":"dev.morphia.query.validation","l":"ValidationFailure"},{"p":"dev.morphia.query.validation","l":"Validator"},{"p":"dev.morphia.query.validation","l":"ValueValidator"},{"p":"dev.morphia.annotations","l":"Version"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"VersionMisuse"},{"p":"dev.morphia.query","l":"WhereCriteria"}]
    </article>
</main>
</div>
<!--
<footer class="footer">
    <p>Copyright (C) 2020-2020
</footer>
-->
<script src="../../../_/js/site.js"></script>
<script async src="../../../_/js/vendor/highlight.js"></script>
  </body>
</html>
