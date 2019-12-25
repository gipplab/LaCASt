#!/usr/bin/env bash

CONFIGFILEBASE="config/symbolic_tests-base.properties"
CONFIGFILE="config/symbolic_tests.properties"
SETFILES="config/together-lines.txt"

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
  echo "output=/home/andreg-p/Howard/Results/AutoMath/${ADDR[0]}-symbolic.txt" >> $CONFIGFILE;
  echo "missing_macro_output=/home/andreg-p/Howard/Results/AutoMath/${ADDR[0]}-missing.txt" >> $CONFIGFILE;
  echo "subset_tests=${ADDR[1]}" >> $CONFIGFILE;

  echo "Done creating file for ${ADDR[0]}"
  echo "Start processing..."
  java -Xmx6g -jar ./bin/symbolic-tester.jar -mathematica;
  echo "Done ${ADDR[0]}"

  RESSTR="${RESSTR}${ADDR[0]}: $? $NEWLINE"
done < $SETFILES

echo "$RESSTR"
exit 0
