#!/usr/bin/env bash

REVERSEMODE=0
BASEPATH="/home/andreg-p/data/Howard/Results"
while getopts rp: flag
do
  case "${flag}" in
    r) REVERSEMODE=1;;
    p) BASEPATH=${OPTARG};;
    a*) echo "Unknown flag, use -r for reversed mode."
  esac
done

MATHBASE="$BASEPATH/MathematicaSymbolic"
MATHP="$BASEPATH/MathematicaNumeric"

if (( $REVERSEMODE == 1 )); then
  MATHP="${MATHP}SymbolicSuccessful"
fi

FILES=$(find $MATHP -type f | sort)

SUMTRANS=0
SUMSYMSUCC=0
SUMFAIL=0
SUMNUMSUCC=0
SUMNUMFAIL=0
ABORTED=0
ERRS=0

TOTALTESTS=0
TOTALFAILTESTS=0
TOTALSUCCESSTESTS=0

PARTFAIL=0
TOTALFAIL=0

FILECOUNTER=0

for FILE in $FILES; do
  [[ $FILE =~ .*([0-9][0-9])\-([A-Z]+)-.* ]]

  num=${BASH_REMATCH[1]};
  id=${BASH_REMATCH[2]};

  refF="$MATHBASE/$num-$id-symbolic.txt";

  # SUCCESS: 26, SUCCESS_SYMB: 26, SUCCESS_TRANS: 103, FAILURE: 77, STARTED_TEST_CASES: 229, SKIPPED: 17, DEFINITIONS: 41, IGNORE: 400, MISSING: 103, ERROR: 23
  # Overall: [SUCCESS: 18, FAILURE: 48, LIMIT_SKIPS: 2, TESTED: 77, ERROR: 9]
  ####### NEW ######
  # Overall: [TOTAL: 406, SKIPPED: 373, DEFINITIONS: 0, STARTED_TEST_CASES: 33, ERROR_TRANS: 0, MISSING: 0, SUCCESS_TRANS: 33, SUCCESS_SYMB: 0, SUCCESS_NUM: 11, SUCCESS_UNDER_EXTRA_CONDITION: 0, FAILURE: 18, ABORTED: 4, ERROR: 0] for test expression: (#LHS)-(#RHS)
  
  IFS=','
  #read -r -a arrTrans <<< $(gawk 'match($0, /.*SYMB: ([0-9]+),.*TRANS: ([0-9]+).*/, arry) {print arry[1]","arry[2]}' $refF)
  #read -r -a arrMath <<< $(gawk 'match($0, /.*SUCCESS: ([0-9]+),.*FAILURE: ([0-9]+),.*TESTED: ([0-9]+).*/, arry) {print arry[1]","arry[2]","arry[3]}' $FILE)
  
  read -r -a arrTrans <<< $(gawk 'match($0, /.*SUCCESS_TRANS: ([0-9]+),.*SUCCESS_SYMB: ([0-9]+).*,.*SUCCESS_UNDER_EXTRA_CONDITION: ([0-9]+).*/, arry) {print arry[2]","arry[1]","arry[3]}' $refF)
  read -r -a arrMath <<< $(gawk 'match($0, /.*STARTED_TEST_CASES: ([0-9]+),.*SUCCESS_NUM: ([0-9]+),.*FAILURE: ([0-9]+).*,.*ABORTED: ([0-9]+),.*ERROR: ([0-9]+).*/, arry) {print arry[2]","arry[3]","arry[1]","arry[4]","arry[5]}' $FILE)

  read -r -a partFailed <<< $(gawk 'match($0, /.*Failed \[([0-9]+)\/([0-9]+)\]/, arr) {res[arr[1]<arr[2]]++; res[2]+=arr[1]; res[3]+=arr[2]}; END {print res[1]","res[0]","res[2]","res[3]}' $FILE)
  read -r -a succTests <<< $(gawk 'match($0, /.*Successful \[Tested: ([0-9]+)\]/, arr) {res[0]+=arr[1]}; END {print res[0];}' $FILE)

  allSymbSucc=$((arrTrans[0]+arrTrans[2]))
  symFAIL="${arrMath[2]}"
  avgSYMSUCC=$(bc <<< "scale=2; 100*${allSymbSucc}/${arrTrans[1]}")
  avgNUMSUCC=$(bc <<< "scale=2; 100*${arrMath[0]}/${symFAIL}")

#  avgPARTFAIL=$(bc <<< "scale=2; 100*${partFailed[0]}/(${arrMath[0]}+${arrMath[1]})")
#  avgTOTALFAIL=$(bc <<< "scale=2; 100*${partFailed[1]}/(${arrMath[0]}+${arrMath[1]})")

  SUMTRANS=$((SUMTRANS + arrTrans[1]))
  SUMSYMSUCC=$((SUMSYMSUCC + allSymbSucc))
  SUMFAIL=$((SUMFAIL + symFAIL))
  SUMNUMSUCC=$((SUMNUMSUCC + arrMath[0]))
  SUMNUMFAIL=$((SUMNUMFAIL + arrMath[1]))
  ABORTED=$((ABORTED + arrMath[3]))
  ERRS=$((ERRS + arrMath[4]))

  PARTFAIL=$((PARTFAIL + partFailed[0]))
  TOTALFAIL=$((TOTALFAIL + partFailed[1]))
  TOTALFAILTESTS=$((TOTALFAILTESTS + partFailed[2]))
  TOTALTESTS=$((TOTALTESTS + succTests[0] + partFailed[3]))
  TOTALSUCCESSTESTS=$((TOTALSUCCESSTESTS + succTests[0] + (partFailed[3] - partFailed[2])))

  printf "\\TT\\TB \\\verb|%s| & %2d & %3d & %3d & (%4.1f\\%%) & %3d & %3d & (%4.1f\\%%) & %3d & [%3d / %3d] & %3d & %3d \\\\\\ \\hline\n" "${id}" "${num#0}" "${arrTrans[1]}" "${allSymbSucc}" "${avgSYMSUCC}" "${symFAIL}" "${arrMath[0]}" "${avgNUMSUCC}" "${arrMath[1]}" "${partFailed[0]}" "${partFailed[1]}" "${arrMath[3]}" "${arrMath[4]}"

  FILECOUNTER=$((FILECOUNTER+1))

  if (( $FILECOUNTER == 20 )); then
    printf "\\TT\\TB \\\verb|MT| & 21 &   0 & \\\multicolumn{2}{c|}{-} & - & \\\multicolumn{2}{c|}{-} & \\\multicolumn{2}{c|}{-} & - & - \\\\\\ \\hline\n"
    FILECOUNTER=$((FILECOUNTER+1))
  elif (( $FILECOUNTER == 34 )); then
    printf "\\TT\\TB \\\verb|FM| & 35 &   0 & \\\multicolumn{2}{c|}{-} & - & \\\multicolumn{2}{c|}{-} & \\\multicolumn{2}{c|}{-} & - & - \\\\\\ \\hline\n"
    FILECOUNTER=$((FILECOUNTER+1))
  fi
done

avgSS=$(bc <<< "scale=2; 100*${SUMSYMSUCC}/${SUMTRANS}")
avgNUMS=$(bc <<< "scale=2; 100*${SUMNUMSUCC}/${SUMFAIL}")
avgREST=$(bc <<< "scale=2; 100*${SUMNUMFAIL}/${SUMTRANS}")
#avgPFAIL=$(bc <<< "scale=2; 100*${SUMNUMFAIL}/${PARTFAIL}")
#avgTFAIL=$(bc <<< "scale=2; 100*${SUMNUMFAIL}/${TOTALFAIL}")

printf "\\hline\n"
printf "\\multicolumn{2}{|c|}{$\Sigma$} & %'4d & %'4d & (%4.1f\\%%) & %'4d & %'4d & (%4.1f\\%%) & %'4d & [%'4d / %'4d] & %'4d & %'4d \\\\\\ \\hline\n" "${SUMTRANS}" "${SUMSYMSUCC}" "${avgSS}" "${SUMFAIL}" "${SUMNUMSUCC}" "${avgNUMS}" "${SUMNUMFAIL}" "${PARTFAIL}" "${TOTALFAIL}" "${ABORTED}" "${ERRS}"

printf "Total test cases: %'d [Total Fail: %'d | Total Success: %'d]\n" "${TOTALTESTS}" "${TOTALFAILTESTS}" "${TOTALSUCCESSTESTS}"
