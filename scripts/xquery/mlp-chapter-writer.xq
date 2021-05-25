declare namespace m = "http://www.w3.org/1998/Math/MathML";
declare namespace ltxx = "http://dlmf.nist.gov/LaTeXML/Extra";
declare default element namespace "http://dlmf.nist.gov/LaTeXML";

declare function local:makeTitle( $tocref as node() ) {
  let $elements := $tocref/node()
  for $element in $elements
    return typeswitch($element)
    case element(Math) return if ($element/@content-tex) then
        "<math>" || $element/@content-tex/string() || "</math>"
      else
        "<math>" || $element/@tex/string() || "</math>"
    case text() return $element
    default return $element/text()
};

declare function local:writeItemList(
  $tocentry as node() 
){
  for $subentry in $tocentry/toclist/tocentry/ref/text
    let $tag := $subentry/tag/text()
    let $title := $subentry/text()
    let $linktitle := string-join(local:makeTitle($subentry), ' ')
    let $str := "[[" || $tag || "|" || $linktitle || "]]" || "<br>"
    return ($str)
};

declare function local:writeItemSectionList(
  $toclist as node() 
){
  for $tocentry in $toclist/tocentry
    let $header := string-join(local:makeTitle($tocentry/text), ' ')
    let $str := "; " || $header || " : "
    return ($str, local:writeItemList($tocentry), '&#xa;')
};

declare function local:writeChapterToc(
  $chapter as node()
) {
  for $singletoc in $chapter/toclist
    let $text := local:writeItemSectionList($singletoc)
    return $text
};

declare function local:writeToc(
  $chapters as node()*
) {
  for $chapter in $chapters
    let $chapterId := $chapter/@xml:id
    let $chapterText := local:writeChapterToc($chapter)
    let $path := "/home/andreg-p/Projects/LaCASt/misc/Mediawiki/" || $chapterId || ".txt"
    let $content := '<div style="-moz-column-count:2; column-count:2;">&#xa;' || string-join($chapterText,'') || '</div>'
    return if ($chapterText != "") then
      file:write-text($path, $content)
};

local:writeToc(chapter)

(:
writes text files:
local:writeToc(chapter)


some other stuff
let $tmp := chapter[@xml:id="C16"]/toclist//tocentry//tag[text()="16.10"]/../..
return string-join(local:makeTitle($tmp), ' ')

let $path := "/home/andreg-p/Projects/LaCASt/misc/Mediawiki/" || $chapterId || ".txt"
file:write($path, $chapterText)

<chapter id="{$chapterId}">{$chapterText}</chapter>

chapter/toclist
chapter[@xml:id="C16"]/toclist
local:writeToc(chapter/toclist)
:)