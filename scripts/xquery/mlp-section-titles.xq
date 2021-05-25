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

for $section in *
  for $toctitle in $section/toctitle
    let $tag := $toctitle/tag/text()
    let $title := string-join(local:makeTitle($toctitle), ' ')
    return $title
    
(:return <toc><tag>{$tag}</tag><title>{$title}</title></toc>:)