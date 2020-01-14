#!/usr/bin/env bash

BASEPATH="misc/Results"
MATHBASE="$BASEPATH/MathematicaSymbolic"
MATHP="$BASEPATH/MathematicaNumeric"

FILES=$(find $MATHP -type f | sort)

SUMTRANS=0
SUMSYMSUCC=0
SUMFAIL=0
SUMNUMSUCC=0
SUMNUMFAIL=0

for FILE in $FILES; do
  [[ $FILE =~ .*([0-9][0-9])\-([A-Z]+)-.* ]]

  num=${BASH_REMATCH[1]};
  id=${BASH_REMATCH[2]};

  refF="$MATHBASE/$num-$id-symbolic.txt";

  # SUCCESS: 26, SUCCESS_SYMB: 26, SUCCESS_TRANS: 103, FAILURE: 77, STARTED_TEST_CASES: 229, SKIPPED: 17, DEFINITIONS: 41, IGNORE: 400, MISSING: 103, ERROR: 23
  # Overall: [SUCCESS: 18, FAILURE: 48, LIMIT_SKIPS: 2, TESTED: 77, ERROR: 9]
  IFS=','
  read -r -a arrTrans <<< $(gawk 'match($0, /.*SYMB: ([0-9]+),.*TRANS: ([0-9]+).*/, arry) {print arry[1]","arry[2]}' $refF)
  read -r -a arrMath <<< $(gawk 'match($0, /.*SUCCESS: ([0-9]+),.*FAILURE: ([0-9]+),.*TESTED: ([0-9]+).*/, arry) {print arry[1]","arry[2]","arry[3]}' $FILE)

  symFAIL="${arrMath[2]}"
  avgSYMSUCC=$(bc <<< "scale=2; 100*${arrTrans[0]}/${arrTrans[1]}")
  avgNUMSUCC=$(bc <<< "scale=2; 100*${arrMath[0]}/${symFAIL}")
  avgFAILOFALL=$(bc <<< "scale=2; 100*${arrMath[1]}/${arrTrans[1]}")

  SUMTRANS=$((SUMTRANS + arrTrans[1]))
  SUMSYMSUCC=$((SUMSYMSUCC + arrTrans[0]))
  SUMFAIL=$((SUMFAIL + symFAIL))
  SUMNUMSUCC=$((SUMNUMSUCC + arrMath[0]))
  SUMNUMFAIL=$((SUMNUMFAIL + arrMath[1]))

  printf "\\TT\\TB \\\verb|%s| & %2d & %3d & %3d & (%4.1f\\%%) & %3d & %3d & (%4.1f\\%%) & %3d & (%4.1f\\%%) \\\\\\ \\hline\n" "${id}" "${num#0}" "${arrTrans[1]}" "${arrTrans[0]}" "${avgSYMSUCC}" "${symFAIL}" "${arrMath[0]}" "${avgNUMSUCC}" "${arrMath[1]}" "${avgFAILOFALL}"
done

avgSS=$(bc <<< "scale=2; 100*${SUMSYMSUCC}/${SUMTRANS}")
avgNUMS=$(bc <<< "scale=2; 100*${SUMNUMSUCC}/${SUMFAIL}")
avgREST=$(bc <<< "scale=2; 100*${SUMNUMFAIL}/${SUMTRANS}")

printf "\\multicolumn{2}{|c|}{$\Sigma$} & %'4d & %'4d & (%4.1f\\%%) & %'4d & %'4d & (%4.1f\\%%) & %'4d & (%4.1f\\%%)\n" "${SUMTRANS}" "${SUMSYMSUCC}" "${avgSS}" "${SUMFAIL}" "${SUMNUMSUCC}" "${avgNUMS}" "${SUMNUMFAIL}" "${avgREST}"