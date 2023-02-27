//scalastyle:off
package com.zoho.crm.feature.entityparser.util.humannameparser

import java.util.regex.Pattern

abstract class Name(actualName: String) {

  private var str = actualName

  def getStr(): String = {
    str
  }
  def chopWithRegex(regex: String, submatchIndex: Int): String = {
    var chopped = ""
    var pattern = Pattern.compile(regex)
    var matcher = pattern.matcher(this.str)

    // workdaround for numReplacements in Java
    var numReplacements = 0
    while (matcher.find()) {
      numReplacements += 1
    }

    // recreate or the groups are gone
    pattern = Pattern.compile(regex)
    matcher = pattern.matcher(str)
    if (matcher.find()) {

      var subset = matcher.groupCount() > submatchIndex
      if (subset) {
        this.str = this.str.replaceAll(regex, " ")
        if (numReplacements > 1) {
          "" //No I18N
        }
        this.norm()
        return matcher.group(submatchIndex).trim()
      }
    }
    return chopped
  }
  def flip(flipAroundChar: String) = {
    val parts = this.str.split(flipAroundChar);
    if (parts != null) {
      if (parts.length == 2) {
        this.str = String.format("%s %s", parts(1), parts(0)); //No I18N
        this.norm();
      }
    }
  }
  def norm() {
    this.str = this.str.trim();
    this.str = this.str.replaceAll("\\s+", " "); //No I18N
    this.str = this.str.replaceAll(",$", " "); //No I18N
  }
}
