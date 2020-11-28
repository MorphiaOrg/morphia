/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

//Adapted from the Oracle source by fixing lint problems and by
//removing the loading code, replaced by async scripts in the header.

//Note that Oracle does not supply a copy of the GNU General Public License version
// 2 with generated javadoc, so we don't either.

/*eslint-env browser */
/* global tabs, tableTab, activeTableTab, rowColor, altColor, data, $ */

// eslint-disable-next-line no-unused-vars
var moduleSearchIndex
// eslint-disable-next-line no-unused-vars
var packageSearchIndex
// eslint-disable-next-line no-unused-vars
var typeSearchIndex
// eslint-disable-next-line no-unused-vars
var memberSearchIndex
// eslint-disable-next-line no-unused-vars
var tagSearchIndex

// Modified to rely on the *.js version of the data loaded by hard-coded header scripts.
// eslint-disable-next-line no-unused-vars
function loadScripts (doc, tag) {
  $(window).resize(function () {
    $('.navPadding').css('padding-top', $('.fixedNav').css('height'))
  })
}

// eslint-disable-next-line no-unused-vars
function show (type) {
  var count = 0
  for (var key in data) {
    var row = document.getElementById(key)
    if ((data[key] & type) !== 0) {
      row.style.display = ''
      row.className = count++ % 2 ? rowColor : altColor
    } else {
      row.style.display = 'none'
    }
  }
  updateTabs(type)
}

function updateTabs (type) {
  for (var value in tabs) {
    var sNode = document.getElementById(tabs[value][0])
    var spanNode = sNode.firstChild
    if (value === type) {
      sNode.className = activeTableTab
      spanNode.innerHTML = tabs[value][1]
    } else {
      sNode.className = tableTab
      spanNode.innerHTML = '<a href="javascript:show(' + value + ');">' + tabs[value][1] + '</a>'
    }
  }
}

// eslint-disable-next-line no-unused-vars
function updateModuleFrame (pFrame, cFrame) {
  top.packageFrame.location = pFrame
  top.classFrame.location = cFrame
}
