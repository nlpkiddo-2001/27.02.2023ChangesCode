//scalastyle:off
package com.zoho.crm.feature.entityparser.util.fuzzysearch

import scala.collection.mutable.ListBuffer


trait Common {
  private val CLASS_WITH_INDEX = ()
  private val CLASSES_WITH_FIND : (Byte,String) = null
  case class Match(start:Option[Int],end:Option[Int],dist:Option[Int])
  case class GroupOfMatches(map : Match){
  var matches = ListBuffer(map)
  var start = map.start.get
  var end = map.end.get
  def is_match_in_group(match_ : Match ) : Boolean = {
    !(match_.end.get <= start || match_.start.get >= end)
  }
  
  def add_match(match_ : Match) {
    matches.+=(match_)
    start = math.min(start,match_.start.get)
    end = math.min(end,match_.end.get)
  }
  
  
  
}
  def search_exact(subsequence : String, sequence: String, start_index : Int = 0, end_index_arg : Option[Int] = None) : List[Int] = {
    if(subsequence.isEmpty())
        throw new IllegalArgumentException("subsequence must not be empty")
    val end_index = if(end_index_arg == None) sequence.length() else end_index_arg.get
    val first_item  : Char = subsequence.charAt(0)
      val find_in_index_range = (index : Int) => {
        val indx = sequence.slice(index, end_index).mkString.indexOf(subsequence)
        if(indx != -1) indx + start_index else -1
    }
    
    
    val index_arr = {
      def whileLoop(index : Int,indexList : List[Int]) : List[Int] = {
        index match{
          case x if x>=0 => {
            indexList.::(index)
            whileLoop(find_in_index_range(index + 1),indexList)
          }
          case _ => indexList
        }
      }
      whileLoop(find_in_index_range(start_index),List[Int]())
    }
    index_arr

  }
  
  
  def count_differences_with_maximum(sequence1 : String, sequence2 : String, max_differences : Int) : Int = {
    val n_different = sequence1.zip(sequence2).count {
        case (charA, charB) => charA != charB
    }
    n_different
  }
       
  def group_matches(matches : List[Match]) : List[List[Match]] = { 
    val groups = ListBuffer[GroupOfMatches]()
    
    for(i <- matches){
      val overlapping_groups = groups.filter(_.is_match_in_group(i))
      if(overlapping_groups.length == 0)
        groups.+=(GroupOfMatches(i))
      else if(overlapping_groups.length == 1)
        overlapping_groups(0).add_match(i)
      else{
        val new_group = GroupOfMatches(i)
        for(group <- overlapping_groups){
            for(match_ <- group.matches){
                new_group.add_match(match_)
                val tmpGroups = groups.filter(overlapping_groups.contains(_))
                groups.clear()
                groups.++=(tmpGroups)
                groups.append(new_group)
            }
        }
      }
    }
    
    groups.map(_.matches.toList).toList
  }
  
  def get_best_match_in_group(group : List[Match]) : Match = {
    group.sortBy(e=> (e.dist.get, -(e.end.get - e.start.get))).apply(0)
  }
  
  case class LevenshteinSearchParams(max_substitutions : Option[Int] = None,
                 max_insertions : Option[Int] = None,
                 max_deletions : Option[Int] = None,
                 max_l_dist : Option[Int] = None){
  
  check_params_valid(this.max_substitutions,this.max_insertions,
                     this.max_deletions,this.max_l_dist)
                     
  val new_max_l_dist = Option(this.get_max_l_dist(max_substitutions, max_insertions,
                                             max_deletions, max_l_dist))
  def unpacked : (Option[Int],Option[Int],Option[Int],Option[Int]) = {
        (this.max_substitutions, this.max_insertions, 
            this.max_deletions, this.new_max_l_dist)
  }
  
  
  private def check_params_valid( max_substitutions : Option[Int], max_insertions : Option[Int],
                     max_deletions : Option[Int] , max_l_dist : Option[Int]){
//    if not all(x is None or isinstance(x, int)
//                   for x in
//                   [max_substitutions, max_insertions, max_deletions, max_l_dist])
    val params_arr = Array[Option[Any]](max_substitutions, max_insertions
                     , max_deletions, max_l_dist)
    if(params_arr.forall(e=> e == None || e.isInstanceOf[Int]))
      throw new IllegalArgumentException("All limits must be integers or None")
    
     if(max_l_dist == None){
       val n_limits = params_arr.filter(_ != None).length
   
  
       n_limits match {
         case 0 if n_limits == 0 => 
           throw new IllegalArgumentException("No limitations given!")
         case 1 if max_substitutions == None => 
           throw new IllegalArgumentException("# substitutions must be limited!")
         case 2 if max_insertions == None => 
           throw new IllegalArgumentException("# insertions must be limited!")
         case 3 if max_deletions == None => 
           throw new IllegalArgumentException("# deletions must be limited!")
         case _ => 
       }
         
     }
  }
  
  private def get_max_l_dist (max_substitutions : Option[Int], max_insertions : Option[Int],
                     max_deletions : Option[Int] , max_l_dist : Option[Int]) : Int = {
    val bignum = 1 << 29
    val params_arr = Array[Option[Int]](max_substitutions, max_insertions
                     , max_deletions, max_l_dist) 
    val maxes_sum : Int = params_arr.map(e=>e.getOrElse(bignum)).sum
    if(max_l_dist != None 
        && max_l_dist.get <= max_l_dist.get) max_l_dist.get else maxes_sum
  }
   
  
}
}



