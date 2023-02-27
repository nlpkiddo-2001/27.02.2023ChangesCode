//scalastyle:off
package com.zoho.crm.feature.entityparser.util.fuzzysearch

object FuzzySearch extends Levenshtein {
  
  def find_near_matches(subsequence : String, sequence : String,
                      max_substitutions : Option[Int] =None,
                      max_insertions : Option[Int] =None,
                      max_deletions : Option[Int] =None,
                      max_l_dist : Option[Int] =None) : List[Match] = {
    val search_params = LevenshteinSearchParams(max_substitutions,
                                            max_insertions,
                                            max_deletions,
                                            max_l_dist)
                                            
    val search_func : ((String,String,LevenshteinSearchParams) => List[Match]) = choose_search_func(search_params)
    
    search_func(subsequence, sequence, search_params)
  }
  
  def choose_search_func(search_params : LevenshteinSearchParams) : ((String,String,LevenshteinSearchParams) => List[Match]) = {
    val (max_substitutions, max_insertions, max_deletions, max_l_dist) = search_params.unpacked
    
       (subsequence : String, sequence : String, search_params : LevenshteinSearchParams) =>
            find_near_matches_levenshtein(subsequence, sequence, search_params.max_l_dist.get)

  }
}