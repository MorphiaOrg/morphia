<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Untitled :: Morphia Docs</title>
    <meta name="generator" content="Antora 2.3.3">
    <link rel="stylesheet" href="../../../../../../_/css/javadoc.css">
<!--    <script type="text/javascript" src="../../../../../../_/js/vendor/script.js"></script> This one is GPL -->
    <script type="text/javascript" src="../../../../../../_/js/vendor/javadoc.js"></script>
<!--    <script type="text/javascript" src="../../../../../../_/_/js/vendor/javadoc/jquery/jszip-utils/dist/jszip-utils.js"></script>-->
    <!--[if IE]>
<!--    <script type="text/javascript" src="../../../../../../_/_/js/vendor/javadoc/jquery/jszip-utils/dist/jszip-utils-ie.js"></script>-->
    <![endif]-->
<!--    <script type="text/javascript" src="../../../../../../_/_/js/vendor/javadoc/jquery/jquery-3.3.1.js"></script>-->
<!--    <script type="text/javascript" src="../../../../../../_/_/js/vendor/javadoc/jquery/jquery-migrate-3.0.1.js"></script>-->
<!--    <script type="text/javascript" src="../../../../../../_/_/js/vendor/javadoc/jquery/jquery-ui.js"></script>-->
  </head>
  <body class="article">
<header class="header" role="banner">
    <nav class="navbar">
        <div class="navbar-brand">
            <img height="80%" src="../../../../../../_/img/logo.png">
        </div>
        <div class="navbar-brand">
            <div class="navbar-item">
                <a href="https://morphia.dev">Morphia</a>
                <span class="separator">//</span>
                <a href="../../../../../..">Docs</a>
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
    <h3 class="title"><a href="../../../../index.html">Morphia</a></h3>
<ul class="nav-list">
  <li class="nav-item" data-depth="0">
<ul class="nav-list">
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../../../../index.html">Getting Started</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../../../../quicktour.html">Quick Tour</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../../../../reference.html">Reference</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../../../../issues-help.html">Issues &amp; Support</a>
  </li>
  <li class="nav-item" data-depth="1">
    <a class="nav-link" href="../../../index.html">Javadoc</a>
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
          <a href="../../../../../../critter/index.html">master</a>
        </li>
        <li class="version">
          <a href="../../../../../../critter/4.0/index.html">4.0</a>
        </li>
      </ul>
    </li>
    <li class="component">
      <span class="title">Home</span>
      <ul class="versions">
        <li class="version is-latest">
          <a href="../../../../../../landing/index.html">landing</a>
        </li>
      </ul>
    </li>
    <li class="component is-current">
      <span class="title">Morphia</span>
      <ul class="versions">
        <li class="version is-current">
          <a href="../../../../index.html">2.2-SNAPSHOT</a>
        </li>
        <li class="version is-latest">
          <a href="../../../../../2.1/index.html">2.1</a>
        </li>
        <li class="version">
          <a href="../../../../../2.0/index.html">2.0</a>
        </li>
        <li class="version">
          <a href="../../../../../1.6/index.html">1.6</a>
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
  <a href="../../../../../../landing/index.html" class="home-link"></a>
<nav class="breadcrumbs" aria-label="breadcrumbs">
</nav>
<div class="page-versions">
  <button class="version-menu-toggle" title="Show other versions of page">2.2-SNAPSHOT</button>
  <div class="version-menu">
    <a class="version is-current" href="jszip-utils-ie.js">2.2-SNAPSHOT</a>
    <a class="version" href="../../../../../2.1/javadoc/jquery/jszip-utils/dist/jszip-utils-ie.js">2.1</a>
    <a class="version" href="../../../../../2.0/javadoc/jquery/jszip-utils/dist/jszip-utils-ie.js">2.0</a>
    <a class="version" href="../../../../../1.6/javadoc/jquery/jszip-utils/dist/jszip-utils-ie.js">1.6</a>
  </div>
</div>
</div>
    <article class="javadoc">
        /*!

JSZipUtils - A collection of cross-browser utilities to go along with JSZip.
<http://stuk.github.io/jszip-utils>

(c) 2014 Stuart Knightley, David Duponchel
Dual licenced under the MIT license or GPLv3. See https://raw.github.com/Stuk/jszip-utils/master/LICENSE.markdown.

*/
;(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);throw new Error("Cannot find module '"+o+"'")}var f=n[o]={exports:{}};t[o][0].call(f.exports,function(e){var n=t[o][1][e];return s(n?n:e)},f,f.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
var global=typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {};/* jshint evil: true, newcap: false */
/* global IEBinaryToArray_ByteStr, IEBinaryToArray_ByteStr_Last */
"use strict";

// Adapted from http://stackoverflow.com/questions/1095102/how-do-i-load-binary-image-data-using-javascript-and-xmlhttprequest
var IEBinaryToArray_ByteStr_Script =
    "<!-- IEBinaryToArray_ByteStr -->\r\n"+
    "<script type='text/vbscript'>\r\n"+
    "Function IEBinaryToArray_ByteStr(Binary)\r\n"+
    "   IEBinaryToArray_ByteStr = CStr(Binary)\r\n"+
    "End Function\r\n"+
    "Function IEBinaryToArray_ByteStr_Last(Binary)\r\n"+
    "   Dim lastIndex\r\n"+
    "   lastIndex = LenB(Binary)\r\n"+
    "   if lastIndex mod 2 Then\r\n"+
    "       IEBinaryToArray_ByteStr_Last = Chr( AscB( MidB( Binary, lastIndex, 1 ) ) )\r\n"+
    "   Else\r\n"+
    "       IEBinaryToArray_ByteStr_Last = "+'""'+"\r\n"+
    "   End If\r\n"+
    "End Function\r\n"+
    "</script>\r\n";

// inject VBScript
document.write(IEBinaryToArray_ByteStr_Script);

global.JSZipUtils._getBinaryFromXHR = function (xhr) {
    var binary = xhr.responseBody;
    var byteMapping = {};
    for ( var i = 0; i < 256; i++ ) {
        for ( var j = 0; j < 256; j++ ) {
            byteMapping[ String.fromCharCode( i + (j << 8) ) ] =
                String.fromCharCode(i) + String.fromCharCode(j);
        }
    }
    var rawBytes = IEBinaryToArray_ByteStr(binary);
    var lastChr = IEBinaryToArray_ByteStr_Last(binary);
    return rawBytes.replace(/[\s\S]/g, function( match ) {
        return byteMapping[match];
    }) + lastChr;
};

// enforcing Stuk's coding style
// vim: set shiftwidth=4 softtabstop=4:

},{}]},{},[1])
;

    </article>
</main>
</div>
<!--
<footer class="footer">
    <p>Copyright (C) 2020-2020
</footer>
-->
<script src="../../../../../../_/js/site.js"></script>
<script async src="../../../../../../_/js/vendor/highlight.js"></script>
  </body>
</html>
