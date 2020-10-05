#!/usr/bin/env bash

REVERSEMODE=0
while getopts r flag
do
  case "${flag}" in
    r) REVERSEMODE=1;;
    a*) echo "Unknown flag, use -r for reversed mode."
  esac
done

CONFIGFILEBASE="config/numerical_tests-base.properties"
CONFIGFILE="config/numerical_tests.properties"
SETFILES="config/together-lines.txt"

MYCAS="Mathematica"
MYCASMODE="-mathematica"

if (( $REVERSEMODE == 1 )); then
  MYCASMODE="${MYCASMODE} -reverse"
fi

export MAPLE="/opt/maple2019/"
export LD_LIBRARY_PATH="/opt/maple2019/bin.X86_64_LINUX:/opt/Wolfram/SystemFiles/Links/JLink/SystemFiles/Libraries/Linux-x86-64/"

NEWLINE=$'\n'
RESSTR="Results:$NEWLINE";

IFS=': '
while read line; do
  echo "Remove config file."
  rm -f $CONFIGFILE;

  echo "Create new config file."
  read -ra ADDR <<< $line;

# MathematicaNumericSymbolicSuccessful
  cat $CONFIGFILEBASE >> $CONFIGFILE;
  OUTFOLDER="${MYCAS}Numeric"
  if (( $REVERSEMODE == 1 )); then
    OUTFOLDER="${OUTFOLDER}SymbolicSuccessful"
  fi
  echo "Writing to folder ${OUTFOLDER}"

  echo "output=/home/andreg-p/data/Howard/Results/${OUTFOLDER}/${ADDR[0]}-numeric.txt" >> $CONFIGFILE;
  echo "symbolic_results_data=/home/andreg-p/data/Howard/Results/${MYCAS}Symbolic/${ADDR[0]}-symbolic.txt" >> $CONFIGFILE;

  echo "Done creating file for ${ADDR[0]}"
  echo "Start processing..."
  java -Xmx24g -Xss100M -jar ./bin/numeric-tester.jar $MYCASMODE;
  RESULTCODE=$?;
  echo "Done ${ADDR[0]}"

  RESSTR="${RESSTR}${ADDR[0]}: $RESULTCODE $NEWLINE"
done < $SETFILES

echo "$RESSTR"
exit 0
