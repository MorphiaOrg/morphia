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
<div class="nav-container" data-component="morphia" data-version="2.2">
  <aside class="nav">
    <div class="panels">
<div class="nav-panel-menu is-active" data-panel="menu">
  <nav class="nav-menu">
    <h3 class="title"><a href="../index.html">Morphia</a></h3>
<ul class="nav-list">
  <li class="nav-item" data-depth="0">
<ul class="nav-list">
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../index.html">Getting Started</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../quicktour.html">Quick Tour</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../reference.html">Reference</a>
  </li>
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
    <span class="version">2.2-SNAPSHOT</span>
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
        <li class="version is-current">
          <a href="../index.html">2.2-SNAPSHOT</a>
        </li>
        <li class="version is-latest">
          <a href="../../2.1/index.html">2.1</a>
        </li>
        <li class="version">
          <a href="../../2/index.html">2</a>
        </li>
        <li class="version">
          <a href="../../1.6/index.html">1.6</a>
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
  <button class="version-menu-toggle" title="Show other versions of page">2.2-SNAPSHOT</button>
  <div class="version-menu">
    <a class="version is-current" href="search.js">2.2-SNAPSHOT</a>
    <a class="version" href="../../2.1/javadoc/search.js">2.1</a>
    <a class="version" href="../../2/javadoc/search.js">2</a>
    <a class="version" href="../../1.6/javadoc/search.js">1.6</a>
  </div>
</div>
</div>
    <article class="javadoc">
        /*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

var noResult = {l: "No results found"};
var catModules = "Modules";
var catPackages = "Packages";
var catTypes = "Types";
var catMembers = "Members";
var catSearchTags = "SearchTags";
var highlight = "<span class=\"resultHighlight\">$&</span>";
var camelCaseRegexp = "";
var secondaryMatcher = "";
function getHighlightedText(item) {
    var ccMatcher = new RegExp(camelCaseRegexp);
    var label = item.replace(ccMatcher, highlight);
    if (label === item) {
        label = item.replace(secondaryMatcher, highlight);
    }
    return label;
}
function getURLPrefix(ui) {
    var urlPrefix="";
    if (useModuleDirectories) {
        var slash = "/";
        if (ui.item.category === catModules) {
            return ui.item.l + slash;
        } else if (ui.item.category === catPackages && ui.item.m) {
            return ui.item.m + slash;
        } else if ((ui.item.category === catTypes && ui.item.p) || ui.item.category === catMembers) {
            $.each(packageSearchIndex, function(index, item) {
                if (ui.item.p == item.l) {
                    urlPrefix = item.m + slash;
                }
            });
            return urlPrefix;
        } else {
            return urlPrefix;
        }
    }
    return urlPrefix;
}
var watermark = 'Search';
$(function() {
    $("#search").val('');
    $("#search").prop("disabled", false);
    $("#reset").prop("disabled", false);
    $("#search").val(watermark).addClass('watermark');
    $("#search").blur(function() {
        if ($(this).val().length == 0) {
            $(this).val(watermark).addClass('watermark');
        }
    });
    $("#search").on('click keydown', function() {
        if ($(this).val() == watermark) {
            $(this).val('').removeClass('watermark');
        }
    });
    $("#reset").click(function() {
        $("#search").val('');
        $("#search").focus();
    });
    $("#search").focus();
    $("#search")[0].setSelectionRange(0, 0);
});
$.widget("custom.catcomplete", $.ui.autocomplete, {
    _create: function() {
        this._super();
        this.widget().menu("option", "items", "> :not(.ui-autocomplete-category)");
    },
    _renderMenu: function(ul, items) {
        var rMenu = this,
                currentCategory = "";
        rMenu.menu.bindings = $();
        $.each(items, function(index, item) {
            var li;
            if (item.l !== noResult.l && item.category !== currentCategory) {
                ul.append("<li class=\"ui-autocomplete-category\">" + item.category + "</li>");
                currentCategory = item.category;
            }
            li = rMenu._renderItemData(ul, item);
            if (item.category) {
                li.attr("aria-label", item.category + " : " + item.l);
                li.attr("class", "resultItem");
            } else {
                li.attr("aria-label", item.l);
                li.attr("class", "resultItem");
            }
        });
    },
    _renderItem: function(ul, item) {
        var label = "";
        if (item.category === catModules) {
            label = getHighlightedText(item.l);
        } else if (item.category === catPackages) {
            label = (item.m)
                    ? getHighlightedText(item.m + "/" + item.l)
                    : getHighlightedText(item.l);
        } else if (item.category === catTypes) {
            label = (item.p)
                    ? getHighlightedText(item.p + "." + item.l)
                    : getHighlightedText(item.l);
        } else if (item.category === catMembers) {
            label = getHighlightedText(item.p + "." + (item.c + "." + item.l));
        } else if (item.category === catSearchTags) {
            label = getHighlightedText(item.l);
        } else {
            label = item.l;
        }
        var li = $("<li/>").appendTo(ul);
        var div = $("<div/>").appendTo(li);
        if (item.category === catSearchTags) {
            if (item.d) {
                div.html(label + "<span class=\"searchTagHolderResult\"> (" + item.h + ")</span><br><span class=\"searchTagDescResult\">"
                                + item.d + "</span><br>");
            } else {
                div.html(label + "<span class=\"searchTagHolderResult\"> (" + item.h + ")</span>");
            }
        } else {
            div.html(label);
        }
        return li;
    }
});
$(function() {
    $("#search").catcomplete({
        minLength: 1,
        delay: 100,
        source: function(request, response) {
            var result = new Array();
            var presult = new Array();
            var tresult = new Array();
            var mresult = new Array();
            var tgresult = new Array();
            var secondaryresult = new Array();
            var displayCount = 0;
            var exactMatcher = new RegExp("^" + $.ui.autocomplete.escapeRegex(request.term) + "$", "i");
            camelCaseRegexp = ($.ui.autocomplete.escapeRegex(request.term)).split(/(?=[A-Z])/).join("([a-z0-9_$]*?)");
            var camelCaseMatcher = new RegExp("^" + camelCaseRegexp);
            secondaryMatcher = new RegExp($.ui.autocomplete.escapeRegex(request.term), "i");

            // Return the nested innermost name from the specified object
            function nestedName(e) {
                return e.l.substring(e.l.lastIndexOf(".") + 1);
            }

            function concatResults(a1, a2) {
                a1 = a1.concat(a2);
                a2.length = 0;
                return a1;
            }

            if (moduleSearchIndex) {
                var mdleCount = 0;
                $.each(moduleSearchIndex, function(index, item) {
                    item.category = catModules;
                    if (exactMatcher.test(item.l)) {
                        result.push(item);
                        mdleCount++;
                    } else if (camelCaseMatcher.test(item.l)) {
                        result.push(item);
                    } else if (secondaryMatcher.test(item.l)) {
                        secondaryresult.push(item);
                    }
                });
                displayCount = mdleCount;
                result = concatResults(result, secondaryresult);
            }
            if (packageSearchIndex) {
                var pCount = 0;
                var pkg = "";
                $.each(packageSearchIndex, function(index, item) {
                    item.category = catPackages;
                    pkg = (item.m)
                            ? (item.m + "/" + item.l)
                            : item.l;
                    if (exactMatcher.test(item.l)) {
                        presult.push(item);
                        pCount++;
                    } else if (camelCaseMatcher.test(pkg)) {
                        presult.push(item);
                    } else if (secondaryMatcher.test(pkg)) {
                        secondaryresult.push(item);
                    }
                });
                result = result.concat(concatResults(presult, secondaryresult));
                displayCount = (pCount > displayCount) ? pCount : displayCount;
            }
            if (typeSearchIndex) {
                var tCount = 0;
                $.each(typeSearchIndex, function(index, item) {
                    item.category = catTypes;
                    var s = nestedName(item);
                    if (exactMatcher.test(s)) {
                        tresult.push(item);
                        tCount++;
                    } else if (camelCaseMatcher.test(s)) {
                        tresult.push(item);
                    } else if (secondaryMatcher.test(item.p + "." + item.l)) {
                        secondaryresult.push(item);
                    }
                });
                result = result.concat(concatResults(tresult, secondaryresult));
                displayCount = (tCount > displayCount) ? tCount : displayCount;
            }
            if (memberSearchIndex) {
                var mCount = 0;
                $.each(memberSearchIndex, function(index, item) {
                    item.category = catMembers;
                    var s = nestedName(item);
                    if (exactMatcher.test(s)) {
                        mresult.push(item);
                        mCount++;
                    } else if (camelCaseMatcher.test(s)) {
                        mresult.push(item);
                    } else if (secondaryMatcher.test(item.c + "." + item.l)) {
                        secondaryresult.push(item);
                    }
                });
                result = result.concat(concatResults(mresult, secondaryresult));
                displayCount = (mCount > displayCount) ? mCount : displayCount;
            }
            if (tagSearchIndex) {
                var tgCount = 0;
                $.each(tagSearchIndex, function(index, item) {
                    item.category = catSearchTags;
                    if (exactMatcher.test(item.l)) {
                        tgresult.push(item);
                        tgCount++;
                    } else if (secondaryMatcher.test(item.l)) {
                        secondaryresult.push(item);
                    }
                });
                result = result.concat(concatResults(tgresult, secondaryresult));
                displayCount = (tgCount > displayCount) ? tgCount : displayCount;
            }
            displayCount = (displayCount > 500) ? displayCount : 500;
            var counter = function() {
                var count = {Modules: 0, Packages: 0, Types: 0, Members: 0, SearchTags: 0};
                var f = function(item) {
                    count[item.category] += 1;
                    return (count[item.category] <= displayCount);
                };
                return f;
            }();
            response(result.filter(counter));
        },
        response: function(event, ui) {
            if (!ui.content.length) {
                ui.content.push(noResult);
            } else {
                $("#search").empty();
            }
        },
        autoFocus: true,
        position: {
            collision: "flip"
        },
        select: function(event, ui) {
            if (ui.item.l !== noResult.l) {
                var url = getURLPrefix(ui);
                if (ui.item.category === catModules) {
                    if (useModuleDirectories) {
                        url += "module-summary.html";
                    } else {
                        url = ui.item.l + "-summary.html";
                    }
                } else if (ui.item.category === catPackages) {
                    if (ui.item.url) {
                        url = ui.item.url;
                    } else {
                    url += ui.item.l.replace(/\./g, '/') + "/package-summary.html";
                    }
                } else if (ui.item.category === catTypes) {
                    if (ui.item.url) {
                        url = ui.item.url;
                    } else if (ui.item.p === "<Unnamed>") {
                        url += ui.item.l + ".html";
                    } else {
                        url += ui.item.p.replace(/\./g, '/') + "/" + ui.item.l + ".html";
                    }
                } else if (ui.item.category === catMembers) {
                    if (ui.item.p === "<Unnamed>") {
                        url += ui.item.c + ".html" + "#";
                    } else {
                        url += ui.item.p.replace(/\./g, '/') + "/" + ui.item.c + ".html" + "#";
                    }
                    if (ui.item.url) {
                        url += ui.item.url;
                    } else {
                        url += ui.item.l;
                    }
                } else if (ui.item.category === catSearchTags) {
                    url += ui.item.u;
                }
                if (top !== window) {
                    parent.classFrame.location = pathtoroot + url;
                } else {
                    window.location.href = pathtoroot + url;
                }
                $("#search").focus();
            }
        }
    });
});

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
