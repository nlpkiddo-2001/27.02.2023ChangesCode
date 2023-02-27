//scalastyle:off
package com.zoho.crm.feature.entityparser.util.fuzzysearch

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._


trait Levenshtein extends Common with LevenshteinNgram{
  
  case class Candidate(start : Option[Int],subseq_index : Option[Int],dist : Option[Int])
  
  def find_near_matches_levenshtein(subsequence : String,sequence : String, max_l_dist : Int) : List[Match] = {
     if(subsequence.length() == 0)
      throw new IllegalArgumentException("Given subsequence is empty!")
     
     if(max_l_dist < 0)
       throw new IllegalArgumentException("Maximum Levenshtein distance must be >= 0!")
     
     if(max_l_dist == 0){
       search_exact(subsequence, sequence).map(e=>Match(Option(e),Option(e + subsequence.length()),Option(0)))
     }
     else if(subsequence.length() / (max_l_dist + 1) >= 3){
       find_near_matches_levenshtein_ngrams(subsequence, sequence, max_l_dist)
     }
     else{
       val matches = find_near_matches_levenshtein_linear_programming(subsequence, sequence, max_l_dist)
       val match_groups = group_matches(matches)
       val best_matches = match_groups.map(e=>get_best_match_in_group(e))
       best_matches
     }
        
  }
  def find_near_matches_levenshtein_linear_programming(subsequence : String, sequence : String,
                                                     max_l_dist : Int) : List[Match] = {
    
    if(subsequence.length() == 0)
      throw new IllegalArgumentException("Given subsequence is empty!")
    
    val char2first_subseq_index = make_char2first_subseq_index(subsequence,max_l_dist)
    val subseq_len = subsequence.length()
    
   
    val matches = ListBuffer[Match]()
    val candidates = sequence.zipWithIndex.foldLeft(List[Candidate]())((candidates,e)=>{
        
      val new_candidates = ListBuffer[Candidate]()
      val idx_in_subseq : Option[Int] = char2first_subseq_index.getOrElse(e._1, None)
      
      if(idx_in_subseq != None){
        if(idx_in_subseq.get + 1 == subseq_len){
          matches.+=(Match(Option(e._2),Option(e._2+1),idx_in_subseq))
        }else{
          new_candidates.+=(Candidate(Option(e._2),Option(idx_in_subseq.get+1),idx_in_subseq))
        }
      }
      
      candidates.foreach(cand=>{
        
        if(subsequence.charAt(cand.subseq_index.get) == e._1){
          if(cand.subseq_index.get + 1 == subseq_len){
            matches.+=(Match(cand.start,Option(e._2+1),cand.dist))
          }else{
            new_candidates.append(cand.copy(subseq_index=Option(cand.subseq_index.get + 1)))
          }
        }else{
          breakable{
            if(cand.dist.get == max_l_dist)
              break
          
          new_candidates.+=(cand.copy(dist=Option(cand.dist.get + 1)))
            
            if(e._2 + 1 < sequence.length() && cand.subseq_index.get + 1 < subseq_len){
              new_candidates.+=(
                cand.copy(dist=Option(cand.dist.get + 1),subseq_index=Option(cand.subseq_index.get + 1))    
              )
            }
            
            breakable {
              (1 until max_l_dist - cand.dist.get + 1).foreach(n_skipped=>{
                if(cand.subseq_index.get + n_skipped == subseq_len){
                  matches.+=(Match(cand.start,Option(e._2 + 1),Option(cand.dist.get + n_skipped)))
                  break
                }else if(subsequence(cand.subseq_index.get + n_skipped) == e._1){
                  if(cand.subseq_index.get + n_skipped + 1 == subseq_len){
                    matches.+=(Match(cand.start,Option(e._2 + 1),Option(cand.dist.get + n_skipped)))
                  }else{
                     new_candidates.+=(cand.copy(
                        dist= Option(cand.dist.get + n_skipped),
                        subseq_index= Option(cand.subseq_index.get + 1 + n_skipped)
                    ))
                  }
                  break
                }
              })
            }
        }
        }
      })
      new_candidates.toList
    })
//    sequence.zipWithIndex.foreach(e=>{
//        
//      val new_candidates = ListBuffer[Candidate]()
//      val idx_in_subseq : Option[Int] = char2first_subseq_index.getOrElse(e._1, None)
//      
//      if(idx_in_subseq != None){
//        if(idx_in_subseq.get + 1 == subseq_len){
//          matches.+=(Match(Option(e._2),Option(e._2+1),idx_in_subseq))
//        }else{
//          new_candidates.+=(Candidate(Option(e._2),Option(idx_in_subseq.get+1),idx_in_subseq))
//        }
//      }
//      
//      candidates.foreach(cand=>{
//        
//        if(subsequence.charAt(cand.subseq_index.get) == e._1){
//          if(cand.subseq_index.get + 1 == subseq_len){
//            matches.+=(Match(cand.start,Option(e._2+1),cand.dist))
//          }else{
//            new_candidates.append(cand.copy(subseq_index=Option(cand.subseq_index.get + 1)))
//          }
//        }else{
//          breakable{
//            if(cand.dist.get == max_l_dist)
//              break
//          
//          new_candidates.+=(cand.copy(dist=Option(cand.dist.get + 1)))
//            
//            if(e._2 + 1 < sequence.length() && cand.subseq_index.get + 1 < subseq_len){
//              new_candidates.+=(
//                cand.copy(dist=Option(cand.dist.get + 1),subseq_index=Option(cand.subseq_index.get + 1))    
//              )
//            }
//            
//            breakable {
//              (1 until max_l_dist - cand.dist.get + 1).foreach(n_skipped=>{
//                if(cand.subseq_index.get + n_skipped == subseq_len){
//                  matches.+=(Match(cand.start,Option(e._2 + 1),Option(cand.dist.get + n_skipped)))
//                  break
//                }else if(subsequence(cand.subseq_index.get + n_skipped) == e._1){
//                  if(cand.subseq_index.get + n_skipped + 1 == subseq_len){
//                    matches.+=(Match(cand.start,Option(e._2 + 1),Option(cand.dist.get + n_skipped)))
//                  }else{
//                     new_candidates.+=(cand.copy(
//                        dist= Option(cand.dist.get + n_skipped),
//                        subseq_index= Option(cand.subseq_index.get + 1 + n_skipped)
//                    ))
//                  }
//                  break
//                }
//              })
//            }
//        }
//        }
//      })
//      candidates = new_candidates
//    })
    
    for(cand <- candidates){
      val dist = cand.dist.get + subseq_len - cand.subseq_index.get
      if(dist <= max_l_dist)
        matches.+=(Match(cand.start,Option(sequence.length()),Option(dist)))
    }
    
    matches.toList
  }
  
  def make_char2first_subseq_index(subsequence : String, max_l_dist : Int) : Map[Char,Option[Int]] = {
    val str : String = subsequence.slice(0, max_l_dist + 1).toString()
    (str zipWithIndex).map(e=>(e._1,Option(e._2))).reverse.toMap
  }
}