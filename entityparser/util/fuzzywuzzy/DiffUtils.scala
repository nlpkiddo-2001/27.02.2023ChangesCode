//scalastyle:off
package com.zoho.crm.feature.entityparser.util.fuzzywuzzy

object DiffUtils {
  
  
  
   def levEditDistance(s1 : String, s2 : String, xcost : Int) : Int = {

        var i  = 0
        var half = 0

       var c1 = s1.toCharArray();
        var c2 = s2.toCharArray();

        var str1 = 0;
        var str2 = 0;

        var len1 = s1.length();
        var len2 = s2.length();

        /* strip common prefix */
        while (len1 > 0 && len2 > 0 && c1(str1) == c2(str2)) {

            len1-=1
            len2-=1
            str1+=1
            str2+=1

        }

        /* strip common suffix */
        while (len1 > 0 && len2 > 0 && c1(str1 + len1 - 1) == c2(str2 + len2 - 1)) {
            len1-=1
            len2-=1
        }

          /* catch trivial cases */
        if (len1 == 0)
            return len2;
        if (len2 == 0)
            return len1;

        /* make the inner cycle (i.e. str2) the longer one */
        if (len1 > len2) {

            var nx = len1;
            var temp = str1;

            len1 = len2;
            len2 = nx;

            str1 = str2;
            str2 = temp;

            var t = c2;
            c2 = c1;
            c1 = t;

        }

        /* check len1 == 1 separately */
        if (len1 == 1) {
            if (xcost != 0) {
                return len2 + 1 - 2 * memchr(c2, str2, c1(str1), len2);
            } else {
                return len2 - memchr(c2, str2, c1(str1), len2);
            }
        }

        len1+=1
        len2+=1
        half = len1 >> 1

        var row = Array.fill(len2)(0)
        var end = len2 - 1;

        for (i <- 0 until (len2 - (if (xcost != 0) 0 else half)))
            row(i) = i


        /* go through the matrix and compute the costs.  yes, this is an extremely
         * obfuscated version, but also extremely memory-conservative and relatively
         * fast.  */

        if (xcost != 0) {

            for (i <- 1 until len1) {

                var p = 1;

                var ch1 = c1(str1 + i - 1)
                var c2p = str2

                var D = i
                var x = i

                while (p <= end) {

                    if (ch1 == c2(c2p)) {
                        x = D
                        D-=1
                    } else {
                        x+=1
                    }
                    c2p+=1
                    D = row(p)
                    D+=1

                    if (x > D)
                        x = D;
                    row(p) = x
                    p+=1

                }

            }

        } else {

            /* in this case we don't have to scan two corner triangles (of size len1/2)
             * in the matrix because no best path can go throught them. note this
             * breaks when len1 == len2 == 2 so the memchr() special case above is
             * necessary */

            row(0) = len1 - half - 1;
            for (i <- 1 until len1) {
                var p = 1

                var ch1 = c1(str1 + i - 1)
                var c2p = 1

                var D, x = 0

                /* skip the upper triangle */
                if (i >= len1 - half) {
                    var offset = i - (len1 - half);
                    var c3 = 0

                    c2p = str2 + offset;
                    p = offset;
                    c3 = row(p) + (if (ch1 != c2(c2p)) 1 else 0)
                    p+=1
                    c2p+=1
                    x = row(p)
                    x+=1
                    D = x
                    if (x > c3) {
                        x = c3;
                    }
                    row(p) = x
                    p+=1
                } else {
                    p = 1;
                    c2p = str2;
                    D = i
                    x = i;
                }
                /* skip the lower triangle */
                if (i <= half + 1)
                    end = len2 + i - half - 2;
                /* main */
                while (p <= end) {
                   D-=1
                    var c3 = D + (if (ch1 != c2(c2p))  1 else 0)
                    c2p+=1
                    x+=1;
                    if (x > c3) {
                        x = c3;
                    }
                    D = row(p);
                    D+=1
                    if (x > D)
                        x = D;
                    row(p) = x
                    p+=1

                }

                /* lower triangle sentinel */
                if (i <= half) {
                  D+=1
                    var c3 = D + (if(ch1 != c2(c2p)) 1 else 0)
                    x+=1
                    if (x > c3) {
                        x = c3;
                    }
                    row(p) = x;
                }
            }
        }

        i = row(end)

        i

    }

   private def memchr(haystack : Array[Char],offset : Int,needle : Char,num : Int) : Int =  {
       var num1 = num
        if (num != 0) {
            var p = 0;

            do {

                if (haystack(offset + p) == needle)
                    return 1

                p+=1
                num1-=1
            } while (num1 != 0);

        }
        return 0

    }

  def getRatio(s1 : String , s2 : String ) : Double = {

        val len1 = s1.length();
        val len2 = s2.length();
        val lensum = len1 + len2;

        val editDistance = levEditDistance(s1, s2, 1);

        (lensum - editDistance) / lensum.toDouble

    }
  
}