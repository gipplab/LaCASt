#!/usr/bin/env bash

REVERSEMODE=0
BASEPATH="/home/andreg-p/data/Howard/Results"
CAS="Mathematica"
CAS2="Maple"

while getopts p: flag
do
  case "${flag}" in
    p) BASEPATH=${OPTARG};;
    a*) echo "Unknown flag, use -r for reversed mode."
  esac
done

MATHBASE="$BASEPATH/${CAS}Symbolic"
MATHP="$BASEPATH/${CAS}Numeric"

MAPLEBASE="$BASEPATH/${CAS2}Symbolic"
MAPLEP="$BASEPATH/${CAS2}Numeric"

FILECOUNTER=0

FILES=$(find $MATHP -type f | sort)

for FILEMATH in $FILES; do
  [[ $FILEMATH =~ .*([0-9][0-9])\-([A-Z]+)-.* ]]

  num=${BASH_REMATCH[1]};
  id=${BASH_REMATCH[2]};

  refFMath="$MATHBASE/$num-$id-symbolic.txt";
  FILEMAPLE="$MAPLEP/$num-$id-numeric.txt";
  refFMaple="$MAPLEBASE/$num-$id-symbolic.txt";
  
  IFS=','
  read -r -a arrTransMath <<< $(gawk 'match($0, /.*STARTED_TEST_CASES: ([0-9]+),.*SUCCESS_TRANS: ([0-9]+),.*SUCCESS_SYMB: ([0-9]+).*,.*SUCCESS_UNDER_EXTRA_CONDITION: ([0-9]+).*/, arry) {print arry[3]","arry[2]","arry[4]","arry[1]}' $refFMath)
  read -r -a arrMathMath <<< $(gawk 'match($0, /.*STARTED_TEST_CASES: ([0-9]+),.*SUCCESS_NUM: ([0-9]+),.*FAILURE: ([0-9]+),.*ABORTED: ([0-9]+),.*ERROR: ([0-9]+).*/, arry) {print arry[2]","arry[3]","arry[1]","arry[4]","arry[5]}' $FILEMATH)
  read -r -a skippedMath <<< $(gawk 'match($0, /.*NO_TEST_VALUES: ([0-9]+).*/, arry) {print arry[1]}' $FILEMATH)
  read -r -a partFailedMath <<< $(gawk 'match($0, /.*Failed \[([0-9]+)\/([0-9]+)\]/, arr) {res[arr[1]<arr[2]]++; res[2]+=arr[1]; res[3]+=arr[2]}; END {print res[1]","res[0]","res[2]","res[3]}' $FILEMATH)
  read -r -a succTestsMath <<< $(gawk 'match($0, /.*Successful \[Tested: ([0-9]+)\]/, arr) {res[0]+=arr[1]}; END {print res[0];}' $FILEMATH)
  
  read -r -a arrTransMaple <<< $(gawk 'match($0, /.*SUCCESS_TRANS: ([0-9]+),.*SUCCESS_SYMB: ([0-9]+).*,.*SUCCESS_UNDER_EXTRA_CONDITION: ([0-9]+).*/, arry) {print arry[2]","arry[1]","arry[3]}' $refFMaple)
  read -r -a arrMathMaple <<< $(gawk 'match($0, /.*STARTED_TEST_CASES: ([0-9]+),.*SUCCESS_NUM: ([0-9]+),.*FAILURE: ([0-9]+),.*ABORTED: ([0-9]+),.*ERROR: ([0-9]+).*/, arry) {print arry[2]","arry[3]","arry[1]","arry[4]","arry[5]}' $FILEMAPLE)
  read -r -a skippedMaple <<< $(gawk 'match($0, /.*NO_TEST_VALUES: ([0-9]+).*/, arry) {print arry[1]}' $FILEMAPLE)
  read -r -a partFailedMaple <<< $(gawk 'match($0, /.*Failed \[([0-9]+)\/([0-9]+)\]/, arr) {res[arr[1]<arr[2]]++; res[2]+=arr[1]; res[3]+=arr[2]}; END {print res[1]","res[0]","res[2]","res[3]}' $FILEMAPLE)
  read -r -a succTestsMaple <<< $(gawk 'match($0, /.*Successful \[Tested: ([0-9]+)\]/, arr) {res[0]+=arr[1]}; END {print res[0];}' $FILEMAPLE)

  allSymbSuccMath=$((arrTransMath[0]+arrTransMath[2]))
  symFAILMath="${arrMathMath[2]}"
  avgSYMSUCCMath=$(bc <<< "scale=2; 100*${allSymbSuccMath}/${arrTransMath[1]}")
  avgNUMSUCCMath=$(bc <<< "scale=2; 100*${arrMathMath[0]}/${symFAILMath}")
  
  allSymbSuccMaple=$((arrTransMaple[0]+arrTransMaple[2]))
  symFAILMaple="${arrMathMaple[2]}"
  avgSYMSUCCMaple=$(bc <<< "scale=2; 100*${allSymbSuccMaple}/${arrTransMaple[1]}")
  avgNUMSUCCMaple=$(bc <<< "scale=2; 100*${arrMathMaple[0]}/${symFAILMaple}")
  
  # 1. AL || Trans Maple || Trans Math || Symb Succ Maple || Percent || Fail || Symb Succ Math || Percent || Fail || Num Succ || Percent || Fail || [P/T]
  printf "| %2d. [[Results of %s|%s]] || %3d || %3d || %3d || %3d || %4.1f%% || %3d || %3d || %4.1f%% || %3d || %3d || %4.1f%% || %3d || [%3d / %3d] || %3d || %3d || %3d || %4.1f%% || %3d || [%3d / %3d] || %3d || %3d \n|-\n" \
  	"${num#0}" "${id}" "${id}" "${arrTransMath[3]}"\
  	"${arrTransMaple[1]}" "${arrTransMath[1]}" \
  	"${allSymbSuccMaple}" "${avgSYMSUCCMaple}" "${symFAILMaple}" \
  	"${allSymbSuccMath}" "${avgSYMSUCCMath}" "${symFAILMath}" \
  	"${arrMathMaple[0]}" "${avgNUMSUCCMaple}" "${arrMathMaple[1]}" "${partFailedMaple[0]}" "${partFailedMaple[1]}" "${arrMathMaple[3]}" "${arrMathMaple[4]}" \
  	"${arrMathMath[0]}" "${avgNUMSUCCMath}" "${arrMathMath[1]}" "${partFailedMath[0]}" "${partFailedMath[1]}" "${arrMathMath[3]}" "${arrMathMath[4]}"
  	

  #printf "\\TT\\TB \\\verb|%s| & %2d & %3d & %3d & (%4.1f\\%%) & %3d & %3d & (%4.1f\\%%) & %3d & [%3d / %3d] & %3d & %3d \\\\\\ \\hline\n" \
  #	"${id}" "${num#0}" "${arrTransMath[1]}" "${allSymbSuccMath}" "${avgSYMSUCCMath}" "${symFAILMath}" \
  #	"${arrMathMath[0]}" "${avgNUMSUCCMath}" "${arrMathMath[1]}" "${partFailedMath[0]}" "${partFailedMath[1]}" "${arrMathMath[3]}" "${arrMathMath[4]}"

  FILECOUNTER=$((FILECOUNTER+1))

  if (( $FILECOUNTER == 20 )); then
    printf "| 21. [[Results of MT|MT]] || 0 || 0 || - || - || - || - || - || - || - || - || - || - || - || - || - || - || - || - || - || - || - \n|-\n"
    FILECOUNTER=$((FILECOUNTER+1))
  elif (( $FILECOUNTER == 34 )); then
    printf "| 35. [[Results of FM|FM]] || 0 || 0 || - || - || - || - || - || - || - || - || - || - || - || - || - || - || - || - || - || - || - \n|-\n"
    FILECOUNTER=$((FILECOUNTER+1))
  fi
done

