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
<div class="nav-container" data-component="morphia" data-version="2.1.0">
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
</ul>
  </li>
  <li class="nav-item" data-depth="0">
<ul class="nav-list">
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../configuration.html">Configuration</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../mapping.html">Mapping</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../querying.html">Querying</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../querying-old.html">Querying (Deprecated)</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../textSearch.html">Text Search</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../updating.html">Updating</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../updating-old.html">Updating (Deprecated)</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../aggregation.html">Aggregation</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../transactions.html">Transactions</a>
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
</ul>
  </li>
  <li class="nav-item" data-depth="0">
<ul class="nav-list">
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../issues-help.html">Issues &amp; Support</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="index.html">Javadoc</a>
  </li>
</ul>
  </li>
</ul>
  </nav>
</div>
<div class="nav-panel-explore" data-panel="explore">
  <div class="context">
    <span class="title">Morphia</span>
    <span class="version">2.1.0</span>
  </div>
  <ul class="components">
    <li class="component">
      <span class="title">Critter</span>
      <ul class="versions">
        <li class="version is-latest">
          <a href="../../../critter/4.0.0/index.html">4.0.0</a>
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
        <li class="version is-current is-latest">
          <a href="../index.html">2.1.0</a>
        </li>
        <li class="version">
          <a href="../../2.0.2/index.html">2.0.2</a>
        </li>
        <li class="version">
          <a href="../../1.6.0/index.html">1.6.0</a>
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
  <button class="version-menu-toggle" title="Show other versions of page">2.1.0</button>
  <div class="version-menu">
    <a class="version" href="../../2.2/javadoc/type-search-index.js">2.2-SNAPSHOT</a>
    <a class="version is-current" href="type-search-index.js">2.1.0</a>
    <a class="version" href="../../2.0.2/javadoc/type-search-index.js">2.0.2</a>
    <a class="version" href="../../1.6.0/javadoc/type-search-index.js">1.6.0</a>
  </div>
</div>
</div>
    <article class="javadoc">
        typeSearchIndex = [{"p":"dev.morphia.query","l":"AbstractCriteria"},{"p":"dev.morphia","l":"AbstractEntityInterceptor"},{"p":"dev.morphia.query","l":"AbstractQueryFactory"},{"p":"dev.morphia.aggregation","l":"Accumulator"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"Accumulator"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"AccumulatorExpression"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"AccumulatorExpressions"},{"p":"dev.morphia.aggregation.experimental.stages","l":"AddFields"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"AddFieldsCodec"},{"p":"dev.morphia.query.experimental.updates","l":"AddToSetOperator"},{"p":"dev.morphia","l":"AdvancedDatastore"},{"p":"dev.morphia.aggregation.experimental","l":"Aggregation"},{"p":"dev.morphia.aggregation.experimental.codecs","l":"AggregationCodecProvider"},{"p":"dev.morphia.aggregation.experimental","l":"AggregationException"},{"p":"dev.morphia.aggregation.experimental","l":"AggregationImpl"},{"p":"dev.morphia.aggregation.experimental","l":"AggregationOptions"},{"p":"dev.morphia.aggregation","l":"AggregationPipeline"},{"p":"dev.morphia.aggregation","l":"AggregationPipelineImpl"},{"l":"All Classes","url":"allclasses-index.html"},{"p":"dev.morphia.annotations","l":"AlsoLoad"},{"p":"dev.morphia.annotations","l":"AnnotationBuilder"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"ArrayExpression"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"ArrayExpressions"},{"p":"dev.morphia.mapping.codec","l":"ArrayFieldAccessor"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"ArrayFilterExpression"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"ArrayIndexExpression"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"ArrayLiteral"},{"p":"dev.morphia.query","l":"ArraySlice"},{"p":"dev.morphia.aggregation.experimental.stages","l":"AutoBucket"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"AutoBucketCodec"},{"p":"dev.morphia.experimental","l":"BaseMorphiaSession"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"BooleanExpressions"},{"p":"dev.morphia.mapping.codec","l":"BsonTypeMap"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Bucket"},{"p":"dev.morphia.query","l":"BucketAutoOptions"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"BucketCodec"},{"p":"dev.morphia.query","l":"BucketOptions"},{"p":"dev.morphia.mapping","l":"MapperOptions.Builder"},{"p":"dev.morphia.mapping.codec.pojo","l":"TypeData.Builder"},{"p":"dev.morphia.annotations","l":"CappedAt"},{"p":"dev.morphia.query","l":"Shape.Center"},{"p":"dev.morphia.mapping.codec","l":"ClassCodec"},{"p":"dev.morphia.mapping.validation","l":"ClassConstraint"},{"p":"dev.morphia.mapping.codec.pojo","l":"ClassMethodPair"},{"p":"dev.morphia.annotations","l":"Collation"},{"p":"dev.morphia.annotations","l":"CollationBuilder"},{"p":"dev.morphia.mapping.codec","l":"CollectionCodec"},{"p":"dev.morphia.mapping.experimental","l":"CollectionReference"},{"p":"dev.morphia.aggregation.experimental.stages","l":"CollectionStats"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"CollectionStatsCodec"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"ComparisonExpressions"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"ConditionalExpressions"},{"p":"dev.morphia.mapping.validation","l":"ConstraintViolation"},{"p":"dev.morphia.mapping.validation","l":"ConstraintViolationException"},{"p":"dev.morphia.annotations.experimental","l":"Constructor"},{"p":"dev.morphia.mapping.experimental","l":"ConstructorCreator"},{"p":"dev.morphia.mapping.validation","l":"ConstructorParameterNameConstraint"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"ContradictingFieldAnnotation"},{"p":"dev.morphia.mapping.codec","l":"Conversions"},{"p":"dev.morphia.annotations","l":"Converters"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"ConvertExpression"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"ConvertType"},{"p":"dev.morphia.geo","l":"CoordinateReferenceSystem"},{"p":"dev.morphia.geo","l":"CoordinateReferenceSystemType"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Count"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"CountCodec"},{"p":"dev.morphia.query","l":"CountOptions"},{"p":"dev.morphia.query","l":"Criteria"},{"p":"dev.morphia.query","l":"CriteriaContainer"},{"p":"dev.morphia.query","l":"CriteriaContainerImpl"},{"p":"dev.morphia.query","l":"CriteriaJoin"},{"p":"dev.morphia.query.experimental.updates","l":"CurrentDateOperator"},{"p":"dev.morphia.aggregation.experimental.stages","l":"CurrentOp"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"CurrentOpCodec"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"DataSizeExpressions"},{"p":"dev.morphia","l":"Datastore"},{"p":"dev.morphia","l":"DatastoreImpl"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"DateExpressions.DateExpression"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"DateExpressions"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"DateFromParts"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"DateFromString"},{"p":"dev.morphia.mapping","l":"DateStorage"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"DateToParts"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"DateToString"},{"p":"dev.morphia.query","l":"DefaultQueryFactory"},{"p":"dev.morphia","l":"DeleteOptions"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Sort.Direction"},{"p":"dev.morphia.mapping","l":"DiscriminatorFunction"},{"p":"dev.morphia.mapping","l":"DiscriminatorLookup"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"DocumentExpression"},{"p":"dev.morphia.mapping.codec.reader","l":"DocumentReader"},{"p":"dev.morphia.mapping.codec","l":"DocumentWriter"},{"p":"dev.morphia.mapping.validation.classrules","l":"DuplicatedAttributeNames"},{"p":"dev.morphia.annotations","l":"Embedded"},{"p":"dev.morphia.mapping.validation.classrules","l":"EmbeddedAndId"},{"p":"dev.morphia.mapping.validation.classrules","l":"EmbeddedAndValue"},{"p":"dev.morphia.annotations.experimental","l":"EmbeddedBuilder"},{"p":"dev.morphia.annotations","l":"Entity"},{"p":"dev.morphia.mapping.validation.classrules","l":"EntityAndEmbed"},{"p":"dev.morphia.mapping.validation.classrules","l":"EntityCannotBeMapOrIterable"},{"p":"dev.morphia.mapping.codec.pojo","l":"EntityDecoder"},{"p":"dev.morphia","l":"EntityInterceptor"},{"p":"dev.morphia.annotations","l":"EntityListeners"},{"p":"dev.morphia.mapping.codec.pojo","l":"EntityModel"},{"p":"dev.morphia.mapping.codec.pojo","l":"EntityModelBuilder"},{"p":"dev.morphia.mapping.validation.classrules","l":"EntityOrEmbed"},{"p":"dev.morphia.mapping.codec","l":"EnumCodec"},{"p":"dev.morphia.mapping.codec","l":"EnumCodecProvider"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"Expression"},{"p":"dev.morphia.aggregation.experimental.codecs","l":"ExpressionCodec"},{"p":"dev.morphia.aggregation.experimental.codecs","l":"ExpressionHelper"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"Expressions"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Facet"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"FacetCodec"},{"p":"dev.morphia.annotations","l":"Field"},{"p":"dev.morphia.mapping.codec","l":"FieldAccessor"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"FieldConstraint"},{"p":"dev.morphia.query","l":"FieldEnd"},{"p":"dev.morphia.query","l":"FieldEndImpl"},{"p":"dev.morphia.mapping.validation.classrules","l":"FieldEnumString"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"FieldHolder"},{"p":"dev.morphia.mapping.codec.pojo","l":"FieldModel"},{"p":"dev.morphia.mapping.codec.pojo","l":"FieldModelBuilder"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"Fields"},{"p":"dev.morphia.query.experimental.filters","l":"Filter"},{"p":"dev.morphia.query","l":"FilterOperator"},{"p":"dev.morphia.query.experimental.filters","l":"Filters"},{"p":"dev.morphia.query","l":"FindAndDeleteOptions"},{"p":"dev.morphia","l":"FindAndModifyOptions"},{"p":"dev.morphia.query","l":"FindOptions"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"FunctionExpression"},{"p":"dev.morphia.query.experimental.filters","l":"GeoIntersectsFilter"},{"p":"dev.morphia.geo","l":"GeoJson"},{"p":"dev.morphia.geo","l":"GeoJsonType"},{"p":"dev.morphia.geo","l":"Geometry"},{"p":"dev.morphia.geo","l":"GeometryCollection"},{"p":"dev.morphia.aggregation","l":"GeoNear"},{"p":"dev.morphia.aggregation.experimental.stages","l":"GeoNear"},{"p":"dev.morphia.aggregation","l":"GeoNear.GeoNearBuilder"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"GeoNearCodec"},{"p":"dev.morphia.query.experimental.filters","l":"GeoWithinFilter"},{"p":"dev.morphia.query","l":"BucketAutoOptions.Granularity"},{"p":"dev.morphia.aggregation.experimental.stages","l":"GraphLookup"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"GraphLookupCodec"},{"p":"dev.morphia.aggregation","l":"Group"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Group"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"GroupCodec"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Group.GroupId"},{"p":"dev.morphia.annotations","l":"Handler"},{"p":"dev.morphia.annotations","l":"Id"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"IdDoesNotMix"},{"p":"dev.morphia.annotations.experimental","l":"IdField"},{"p":"dev.morphia.annotations","l":"IdGetter"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"IfNull"},{"p":"dev.morphia.annotations","l":"Index"},{"p":"dev.morphia.annotations","l":"Indexed"},{"p":"dev.morphia.annotations","l":"Indexes"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"IndexExpression"},{"p":"dev.morphia.annotations","l":"IndexHelper"},{"p":"dev.morphia.annotations","l":"IndexOptions"},{"p":"dev.morphia.aggregation.experimental.stages","l":"IndexStats"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"IndexStatsCodec"},{"p":"dev.morphia","l":"InsertManyOptions"},{"p":"dev.morphia","l":"InsertOneOptions"},{"p":"dev.morphia","l":"InsertOptions"},{"p":"dev.morphia.mapping.codec","l":"InstanceCreator"},{"p":"dev.morphia.mapping","l":"InstanceCreatorFactory"},{"p":"dev.morphia.mapping","l":"InstanceCreatorFactoryImpl"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"IsoDates"},{"p":"dev.morphia","l":"Key"},{"p":"dev.morphia.mapping.codec","l":"KeyCodec"},{"p":"dev.morphia.mapping.lazy","l":"LazyFeatureDependencies"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"LazyReferenceMissingDependencies"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"LazyReferenceOnArray"},{"p":"dev.morphia.query","l":"LegacyQuery"},{"p":"dev.morphia.mapping.codec","l":"LegacyQueryCodec"},{"p":"dev.morphia.query","l":"LegacyQueryFactory"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"LetExpression"},{"p":"dev.morphia.mapping.validation","l":"ConstraintViolation.Level"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Limit"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"LimitCodec"},{"p":"dev.morphia.geo","l":"LineString"},{"p":"dev.morphia.mapping.experimental","l":"ListReference"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"LiteralExpression"},{"p":"dev.morphia.annotations","l":"LoadOnly"},{"p":"dev.morphia.mapping.codec","l":"LocaleCodec"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Lookup"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"LookupCodec"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"MapExpression"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"MapKeyTypeConstraint"},{"p":"dev.morphia.mapping","l":"Mapper"},{"p":"dev.morphia.mapping","l":"MapperOptions"},{"p":"dev.morphia.mapping","l":"MappingException"},{"p":"dev.morphia.mapping.validation","l":"MappingValidator"},{"p":"dev.morphia.mapping.experimental","l":"MapReference"},{"p":"dev.morphia.mapping.codec.reader","l":"Mark"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Match"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"MatchCodec"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"MathExpression"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"MathExpressions"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Merge"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"MergeCodec"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"ObjectExpressions.MergeObjects"},{"p":"dev.morphia.query","l":"Meta"},{"p":"dev.morphia.query","l":"Meta.MetaDataKeyword"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"MetaExpression"},{"p":"dev.morphia.query","l":"Modify"},{"p":"dev.morphia","l":"ModifyOptions"},{"p":"dev.morphia","l":"Morphia"},{"p":"dev.morphia.mapping.codec.pojo","l":"MorphiaCodec"},{"p":"dev.morphia.mapping.codec","l":"MorphiaCodecProvider"},{"p":"dev.morphia.mapping.codec","l":"MorphiaCollectionPropertyCodecProvider"},{"p":"dev.morphia.mapping","l":"MorphiaConvention"},{"p":"dev.morphia.mapping.codec","l":"MorphiaDateCodec"},{"p":"dev.morphia.mapping","l":"MorphiaDefaultsConvention"},{"p":"dev.morphia.mapping.codec","l":"MorphiaInstanceCreator"},{"p":"dev.morphia.mapping.codec","l":"MorphiaLocalDateTimeCodec"},{"p":"dev.morphia.mapping.codec","l":"MorphiaLocalTimeCodec"},{"p":"dev.morphia.mapping.codec","l":"MorphiaPropertySerialization"},{"p":"dev.morphia.mapping.codec.references","l":"MorphiaProxy"},{"p":"dev.morphia.query","l":"MorphiaQuery"},{"p":"dev.morphia.mapping.codec","l":"MorphiaQueryCodec"},{"p":"dev.morphia.mapping.experimental","l":"MorphiaReference"},{"p":"dev.morphia.mapping.experimental","l":"MorphiaReferenceCodec"},{"p":"dev.morphia.experimental","l":"MorphiaSession"},{"p":"dev.morphia.experimental","l":"MorphiaSessionImpl"},{"p":"dev.morphia.transactions.experimental","l":"MorphiaTransaction"},{"p":"dev.morphia.mapping.codec","l":"MorphiaTypesCodecProvider"},{"p":"dev.morphia.geo","l":"MultiLineString"},{"p":"dev.morphia.mapping.validation.classrules","l":"MultipleId"},{"p":"dev.morphia.mapping.validation.classrules","l":"MultipleVersions"},{"p":"dev.morphia.geo","l":"MultiPoint"},{"p":"dev.morphia.geo","l":"MultiPolygon"},{"p":"dev.morphia.annotations.experimental","l":"Name"},{"p":"dev.morphia.geo","l":"NamedCoordinateReferenceSystem"},{"p":"dev.morphia.geo","l":"NamedCoordinateReferenceSystemConverter"},{"p":"dev.morphia.mapping","l":"NamingStrategy"},{"p":"dev.morphia.query.experimental.filters","l":"NearFilter"},{"p":"dev.morphia.mapping","l":"NoArgCreator"},{"p":"dev.morphia.mapping.validation.classrules","l":"NoId"},{"p":"dev.morphia.annotations","l":"NotSaved"},{"p":"dev.morphia.mapping.codec","l":"ObjectCodec"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"ObjectExpressions"},{"p":"dev.morphia","l":"ObjectFactory"},{"p":"dev.morphia.query","l":"OperationTarget"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Out"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"OutCodec"},{"p":"dev.morphia.query","l":"BucketAutoOptions.OutputOperation"},{"p":"dev.morphia.query","l":"BucketOptions.OutputOperation"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"PipelineField"},{"p":"dev.morphia.aggregation.experimental.stages","l":"PlanCacheStats"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"PlanCacheStatsCodec"},{"p":"dev.morphia.geo","l":"Point"},{"p":"dev.morphia.geo","l":"PointBuilder"},{"p":"dev.morphia.geo","l":"Polygon"},{"p":"dev.morphia.query.experimental.updates","l":"PopOperator"},{"p":"dev.morphia.annotations","l":"PostLoad"},{"p":"dev.morphia.annotations","l":"PostPersist"},{"p":"dev.morphia.annotations","l":"PreLoad"},{"p":"dev.morphia.annotations","l":"PrePersist"},{"p":"dev.morphia.mapping.codec","l":"PrimitiveCodecRegistry"},{"p":"dev.morphia.aggregation","l":"Projection"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Projection"},{"p":"dev.morphia.query","l":"Projection"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"ProjectionCodec"},{"p":"dev.morphia.annotations","l":"Property"},{"p":"dev.morphia.mapping.codec","l":"PropertyCodec"},{"p":"dev.morphia.mapping.codec","l":"PropertyCodecRegistryImpl"},{"p":"dev.morphia.mapping.codec.pojo","l":"PropertyHandler"},{"p":"dev.morphia.query.experimental.updates","l":"PullOperator"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"Push"},{"p":"dev.morphia.query.experimental.updates","l":"PushOperator"},{"p":"dev.morphia.query","l":"PushOptions"},{"p":"dev.morphia.query","l":"Query"},{"p":"dev.morphia.query","l":"QueryException"},{"p":"dev.morphia.query","l":"QueryFactory"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"RangeExpression"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Redact"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"RedactCodec"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"ReduceExpression"},{"p":"dev.morphia.annotations","l":"Reference"},{"p":"dev.morphia.mapping.codec.references","l":"ReferenceCodec"},{"p":"dev.morphia.mapping.lazy.proxy","l":"ReferenceException"},{"p":"dev.morphia.mapping.codec.references","l":"ReferenceProxy"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"ReferenceToUnidentifiable"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"RegexExpression"},{"p":"dev.morphia.query.experimental.filters","l":"RegexFilter"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"ReplaceExpression"},{"p":"dev.morphia.aggregation.experimental.stages","l":"ReplaceRoot"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"ReplaceRootCodec"},{"p":"dev.morphia.aggregation.experimental.stages","l":"ReplaceWith"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"ReplaceWithCodec"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Sample"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"SampleCodec"},{"p":"dev.morphia.query.experimental.updates","l":"SetEntityOperator"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"SetExpressions"},{"p":"dev.morphia.query.experimental.updates","l":"SetOnInsertOperator"},{"p":"dev.morphia.mapping.experimental","l":"SetReference"},{"p":"dev.morphia.query","l":"Shape"},{"p":"dev.morphia.mapping.experimental","l":"SingleReference"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Skip"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"SkipCodec"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"SliceExpression"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Sort"},{"p":"dev.morphia.query","l":"Sort"},{"p":"dev.morphia.aggregation.experimental.stages","l":"SortByCount"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"SortByCountCodec"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"SortCodec"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Sort.SortType"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Stage"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"StageCodec"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"StringExpressions"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"SwitchExpression"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"SystemVariables"},{"p":"dev.morphia.annotations","l":"Text"},{"p":"dev.morphia.annotations","l":"TextBuilder"},{"p":"dev.morphia.query.experimental.filters","l":"TextSearchFilter"},{"p":"dev.morphia.annotations","l":"Transient"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"TrigonometryExpressions"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"TrimExpression"},{"p":"dev.morphia.query","l":"Type"},{"p":"dev.morphia.mapping.codec.pojo","l":"TypeData"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"TypeExpressions"},{"p":"dev.morphia.query.experimental.updates","l":"CurrentDateOperator.TypeSpecification"},{"p":"dev.morphia.aggregation.experimental.stages","l":"UnionWith"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"UnionWithCodec"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Unset"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"UnsetCodec"},{"p":"dev.morphia.query.experimental.updates","l":"UnsetOperator"},{"p":"dev.morphia.aggregation.experimental.stages","l":"Unwind"},{"p":"dev.morphia.aggregation.experimental.codecs.stages","l":"UnwindCodec"},{"p":"dev.morphia.query","l":"Update"},{"p":"dev.morphia.query","l":"UpdateBase"},{"p":"dev.morphia","l":"UpdateDocument"},{"p":"dev.morphia.query","l":"UpdateException"},{"p":"dev.morphia.query","l":"UpdateOperations"},{"p":"dev.morphia.query.experimental.updates","l":"UpdateOperator"},{"p":"dev.morphia.query.experimental.updates","l":"UpdateOperators"},{"p":"dev.morphia.query","l":"UpdateOpsImpl"},{"p":"dev.morphia","l":"UpdateOptions"},{"p":"dev.morphia.query","l":"Updates"},{"p":"dev.morphia.mapping.codec","l":"URICodec"},{"p":"dev.morphia.annotations","l":"Validation"},{"p":"dev.morphia.annotations","l":"ValidationBuilder"},{"p":"dev.morphia.query","l":"ValidationException"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"ValueExpression"},{"p":"dev.morphia.aggregation.experimental.expressions","l":"VariableExpressions"},{"p":"dev.morphia.annotations","l":"Version"},{"p":"dev.morphia.mapping.validation.fieldrules","l":"VersionMisuse"},{"p":"dev.morphia.query","l":"WhereCriteria"},{"p":"dev.morphia.aggregation.experimental.expressions.impls","l":"ZipExpression"}]
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
