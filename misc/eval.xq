declare namespace functx = "http://www.functx.com";
declare function functx:trim
  ( $arg as xs:string? )  as xs:string {

   replace(replace($arg,'\s+$',''),'^\s+','')
 } ;
declare function functx:replace-first
  ( $arg as xs:string? ,
    $pattern as xs:string ,
    $replacement as xs:string )  as xs:string {

   replace($arg, concat('(^.*?)', $pattern),
             concat('$1',$replacement))
 } ;
declare function functx:index-of-match-first
  ( $arg as xs:string? ,
    $pattern as xs:string )  as xs:integer? {

  if (matches($arg,$pattern))
  then string-length(tokenize($arg, $pattern)[1]) + 1
  else ()
 } ;
declare function functx:get-matches-and-non-matches
  ( $string as xs:string? ,
    $regex as xs:string )  as element()* {

   let $iomf := functx:index-of-match-first($string, $regex)
   return
   if (empty($iomf))
   then <non-match>{$string}</non-match>
   else
   if ($iomf > 1)
   then (<non-match>{substring($string,1,$iomf - 1)}</non-match>,
         functx:get-matches-and-non-matches(
            substring($string,$iomf),$regex))
   else
   let $length :=
      string-length($string) -
      string-length(functx:replace-first($string, $regex,''))
   return (<match>{substring($string,1,$length)}</match>,
           if (string-length($string) > $length)
           then functx:get-matches-and-non-matches(
              substring($string,$length + 1),$regex)
           else ())
 } ;
declare function functx:get-matches
  ( $string as xs:string? ,
    $regex as xs:string )  as xs:string* {

   functx:get-matches-and-non-matches($string,$regex)/
     string(self::match)
 } ;
for $entry in /map/entry
let $cnt := count($entry/map//string)
let $token := $entry/string/text()
let $chapters :=  $entry/map//entry
let $example :=  concat( "http://orion.cam.nist.gov/howardsdrmf/", replace(functx:trim(
<e>{functx:get-matches($chapters[1]/set/string[1],"\d+-\d+-E\d+.s.tex")}</e>),'-','.'))
where starts-with($token,'\')
order by $cnt descending
return substring($token,1,20) ||","|| $cnt ||","|| $example || ",("|| <c>{
  for $c in $chapters
    let $cf := count($c/set//*)
  return concat($c/int,'[',$cf,"]")}</c> || ")"