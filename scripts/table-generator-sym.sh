#!/usr/bin/env bash

BASEPATH="/home/andreg-p/data/Howard/Results"
SYMBOLIC="$BASEPATH/MathematicaSymbolic"
BASEOLDT="$BASEPATH/OldTrans"

FILES=$(find $SYMBOLIC -type f -name "*symbolic*" | sort)

SUMALL=0;
SUMOT=0;
SUMMAP=0;
SUMMATH=0;
SUMMISMAP=0;
SUMMISMATH=0;
SUMERR=0;

for FILE in $FILES; do
  [[ $FILE =~ .*([0-9][0-9])\-([A-Z]+)-.* ]]

  num=${BASH_REMATCH[1]};
  id=${BASH_REMATCH[2]};

  mapleF="$BASEPATH/MapleSymbolic/$num-$id-symbolic.txt";
  oldT="$BASEOLDT/$num-$id-symbolic.txt"

  # SUCCESS: 26, SUCCESS_SYMB: 26, SUCCESS_TRANS: 103, FAILURE: 77, STARTED_TEST_CASES: 229, SKIPPED: 17, DEFINITIONS: 41, IGNORE: 400, MISSING: 103, ERROR: 23
  # TOTAL: 688, SKIPPED: 414, DEFINITIONS: 40, STARTED_TEST_CASES: 234, ERROR_TRANS: 51, MISSING: 70, SUCCESS_TRANS: 113, SUCCESS_SYMB: 30, SUCCESS_NUM: 0, SUCCESS_UNDER_EXTRA_CONDITION: 1, FAILURE: 72, ABORTED: 10, ERROR: 0
  IFS=','
  #read -r -a arrMath <<< $(gawk 'match($0, /.*SYMB: ([0-9]+),.*TRANS: ([0-9]+),.*FAILURE: ([0-9]+),.*CASES: ([0-9]+),.*MISSING: ([0-9]+).*/, arry) {print arry[1]","arry[2]","arry[3]","arry[4]","arry[5]}' $FILE)
  read -r -a arrMath <<< $(gawk 'match($0, /.*STARTED_TEST_CASES: ([0-9]+),.*MISSING: ([0-9]+),.*SUCCESS_TRANS: ([0-9]+),.*SUCCESS_SYMB: ([0-9]+),.*SUCCESS_UNDER_EXTRA_CONDITION: ([0-9]+).*,.*FAILURE: ([0-9]+).*/, arry) {print (arry[4]+arry[5])","arry[3]","arry[6]","arry[1]","arry[2]}' $FILE)
  #read -r -a arrMaple <<< $(gawk 'match($0, /.*SYMB: ([0-9]+),.*TRANS: ([0-9]+),.*FAILURE: ([0-9]+),.*CASES: ([0-9]+),.*MISSING: ([0-9]+).*/, arry) {print arry[1]","arry[2]","arry[3]","arry[4]","arry[5]}' $mapleF)
  read -r -a arrMaple <<< $(gawk 'match($0, /.*STARTED_TEST_CASES: ([0-9]+),.*MISSING: ([0-9]+),.*SUCCESS_TRANS: ([0-9]+),.*SUCCESS_SYMB: ([0-9]+),.*SUCCESS_UNDER_EXTRA_CONDITION: ([0-9]+).*,.*FAILURE: ([0-9]+).*/, arry) {print (arry[4]+arry[5])","arry[3]","arry[6]","arry[1]","arry[2]}' $mapleF)
  read -r -a oldTrans <<< $(gawk 'match($0, /.*TRANS: ([0-9]+).*/, arry) {print arry[1]}' $oldT)

  avgOT=$(bc <<< "scale=2; 100*${oldTrans}/${arrMath[3]}")
  avgMAP=$(bc <<< "scale=2; 100*${arrMaple[1]}/${arrMaple[3]}")
  avgMath=$(bc <<< "scale=2; 100*${arrMath[1]}/${arrMath[3]}")
  err=$(bc <<< "${arrMath[3]} - ${arrMath[1]} - ${arrMath[4]}")

  SUMALL=$((SUMALL + arrMath[3]))
  SUMOT=$((SUMOT + oldTrans))
  SUMMAP=$((SUMMAP + arrMaple[1]))
  SUMMATH=$((SUMMATH + arrMath[1]))
  SUMMISMAP=$((SUMMISMAP + arrMaple[4]))
  SUMMISMATH=$((SUMMISMATH + arrMath[4]))
  SUMERR=$((SUMERR + err))

  printf "\\TT\\TB \\\verb|%s| & %2d & %3d & %3d & (%4.1f\\%%) & %3d & (%4.1f\\%%) & %3d & (%4.1f\\%%) & %3d & %3d & %2d \\\\\\ \\hline\n" "${id}" "${num#0}" "${arrMath[3]}" "${oldTrans}" "${avgOT}" "${arrMaple[1]}" "${avgMAP}" "${arrMath[1]}" "${avgMath}" "${arrMaple[4]}" "${arrMath[4]}" "${err}"
done

avgOT=$(bc <<< "scale=2; 100*${SUMOT}/${SUMALL}")
avgMAP=$(bc <<< "scale=2; 100*${SUMMAP}/${SUMALL}")
avgMath=$(bc <<< "scale=2; 100*${SUMMATH}/${SUMALL}")

printf "\\multicolumn{2}{|c|}{$\Sigma$} & %'4d & %'4d & (%4.1f\\%%) & %'4d & (%4.1f\\%%) & %'4d & (%4.1f\\%%) & %'4d & %'4d & %3d\n" "${SUMALL}" "${SUMOT}" "${avgOT}" "${SUMMAP}" "${avgMAP}" "${SUMMATH}" "${avgMath}" "${SUMMISMAP}" "${SUMMISMATH}" "${SUMERR}"
