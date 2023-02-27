//scalastyle:off
package com.zoho.crm.feature.entityparser.util.fuzzysearch

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._


trait LevenshteinNgram extends Common{
  
  private def expand(subsequence : String,sequence : String,max_l_dist : Int) : (Option[Int],Option[Int]) = {
    if(subsequence.isEmpty())
      (Option(0),Option(0))
    else{
      var scores : ListBuffer[Int] = ListBuffer.range(0, max_l_dist+1) ++ ListBuffer.fill[Int]((subsequence.length() + 1) - (max_l_dist + 1))(-1)
    var new_scores = ListBuffer.fill[Int](subsequence.length() + 1)(-1)
    var min_score : Option[Int] = None
    var min_scores_idx : Option[Int] = None
    var subseq_index = 0
    sequence.zipWithIndex.foreach(e=>{
      new_scores(0) = scores(0) + 1
      for(subseq_index_ <- 0 until math.min(e._2 + max_l_dist,subsequence.length() - 1)){
        subseq_index = subseq_index_
        new_scores(subseq_index + 1) = math.min(
            scores(subseq_index) + (if(e._1 == subsequence(subseq_index)) 0 else 1),
            math.min(scores(subseq_index+1) + 1,
            new_scores(subseq_index) + 1)
        )
      }
      subseq_index = math.min(e._2 + max_l_dist,subsequence.length() - 1)
      new_scores(subseq_index + 1) = math.min(
          scores(subseq_index) + (if (e._1 == subsequence(subseq_index)) 0 else 1),
          new_scores(subseq_index) + 1
      )
      val last_score = new_scores(subseq_index + 1)
      if(subseq_index == subsequence.length() - 1 && (min_score == None || last_score <= min_score.get)){
        min_score = Option(last_score)
        min_scores_idx = Option(e._2)
      }
     val tmp_score = scores
     
     scores = new_scores
     
     new_scores = tmp_score
    
    })
    if(min_score != None && min_score.get <= max_l_dist ) (min_score,Option(min_scores_idx.get + 1)) else (None,None)
    }
    
  }
  def find_near_matches_levenshtein_ngrams(subsequence : String, sequence : String, max_l_dist : Int) : List[Match] = {
    val subseq_len = subsequence.length()
    val seq_len = sequence.length()

    val ngram_len = subseq_len / (max_l_dist + 1)
    val matches = ListBuffer[Match]()
    
    for (ngram_start <- 0 until (subseq_len - ngram_len + 1) by ngram_len){
      val ngram_end = ngram_start + ngram_len
      val subseq_before_reversed = subsequence.slice(0, ngram_start).reverse
      val subseq_after = subsequence.slice(ngram_end, subseq_len)
      val start_index = math.max(0,ngram_start - max_l_dist)
      val end_index = math.min(seq_len,seq_len - subseq_len + ngram_end + max_l_dist)
      search_exact(subsequence.slice(ngram_start, ngram_end), 
        sequence, start_index, Option(end_index)) foreach(index=>{
//        
//        val (dist_right,right_expand_size) : (Option[Int],Option[Int]) = expand(
//            subseq_after,
//            sequence.slice(index + ngram_len,index - ngram_start + subseq_len + max_l_dist),
//            max_l_dist
//        )
//        if(dist_right != None){
//          val (dist_left,left_expand_size) : (Option[Int],Option[Int]) = expand(
//            subseq_before_reversed,
//            sequence.slice(math.max(0,index-ngram_start-(max_l_dist -dist_right.get)), index).reverse,
//            max_l_dist - dist_right.get
//          )
//          if(dist_left != None){
//            matches.+=(Match(
//               Option(index - left_expand_size.get),
//               Option(index + ngram_len + right_expand_size.get),
//               Option(dist_left.get + dist_right.get)
//            ))
//          }
//        }
        
         breakable{
          val (dist_right,right_expand_size) : (Option[Int],Option[Int]) = expand(
            subseq_after,
            sequence.slice(index + ngram_len,index - ngram_start + subseq_len + max_l_dist),
            max_l_dist
        )
        if(dist_right == None)
          break
        
        val (dist_left,left_expand_size) : (Option[Int],Option[Int]) = expand(
            subseq_before_reversed,
            sequence.slice(math.max(0,index-ngram_start-(max_l_dist -dist_right.get)), index).reverse,
            max_l_dist - dist_right.get
          )
        
        if(dist_left.get + dist_right.get <= max_l_dist)
          throw new Exception("Match Error")
        
          if(dist_left == None)
          break
          
        matches.+=(Match(
             Option(index - left_expand_size.get),
             Option(index + ngram_len + right_expand_size.get),
             Option(dist_left.get + dist_right.get)
          ))
        }
        
      })
    }
    val match_groups : List[List[Match]] =  group_matches(matches.toList)
    val best_matches  = match_groups.map(get_best_match_in_group(_))
    best_matches
  }

}