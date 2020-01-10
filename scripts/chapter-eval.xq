let $chapters := distinct-values( /map//int)
for $c in $chapters
    let $cnt:=count(//entry[./int=$c])
order by $cnt
return $c || ',' || $cnt