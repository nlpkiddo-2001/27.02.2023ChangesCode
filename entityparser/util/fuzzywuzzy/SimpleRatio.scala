//scalastyle:off
package com.zoho.crm.feature.entityparser.util.fuzzywuzzy


class SimpleRatio {
  
  def apply(s1 : String,s2 : String) : Int = {

        Math.round(100 * DiffUtils.getRatio(s1, s2)).toInt

    }
}