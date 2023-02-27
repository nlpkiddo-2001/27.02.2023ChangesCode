//scalastyle:off
package com.zoho.crm.feature.entityparser.util.fuzzywuzzy



object FuzzySearch {
  def ratio(s1 : String,s2 : String) : Int = {
    new SimpleRatio().apply(s1, s2)
  }
}