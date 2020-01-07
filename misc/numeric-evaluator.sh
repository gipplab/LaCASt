#!/usr/bin/env bash

CONFIGFILEBASE="config/numerical_tests-base.properties"
CONFIGFILE="config/numerical_tests.properties"
SETFILES="config/together-lines.txt"

MYCAS="Math"
MYCASMODE="-mathematica"

export MAPLE="/opt/maple2016/"
export LD_LIBRARY_PATH="/opt/maple2016/bin.X86_64_LINUX:/opt/Wolfram/SystemFiles/Links/JLink/SystemFiles/Libraries/Linux-x86-64/"

NEWLINE=$'\n'
RESSTR="Results:$NEWLINE";

IFS=': '
while read line; do
  echo "Remove config file."
  rm -f $CONFIGFILE;

  echo "Create new config file."
  read -ra ADDR <<< $line;

  cat $CONFIGFILEBASE >> $CONFIGFILE;
  echo "output=/home/andreg-p/Howard/Results/${MYCAS}Numeric/${ADDR[0]}-numeric.txt" >> $CONFIGFILE;
  echo "symbolic_results_data=/home/andreg-p/Howard/Results/Auto${MYCAS}/${ADDR[0]}-symbolic.txt" >> $CONFIGFILE;

  echo "Done creating file for ${ADDR[0]}"
  echo "Start processing..."
  java -Xmx10g -jar ./bin/numeric-tester.jar $MYCASMODE;
  echo "Done ${ADDR[0]}"

  RESSTR="${RESSTR}${ADDR[0]}: $? $NEWLINE"
done < $SETFILES

echo "$RESSTR"
exit 0
