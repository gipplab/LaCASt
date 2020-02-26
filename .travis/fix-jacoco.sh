#!/usr/bin/env bash

JACOCO_FILE="target/jacoco-report/jacoco.xml"
JACOCO_REPL_FILE="target/jacoco-report/jacoco-fix.xml"

echo $(ls "target/jacoco-report")
echo "Start replacing..."
sed -e 's/gov\//src\/main\/java\/gov\//g' -e 's/<[\/]\?group[^>]*>//g' $JACOCO_FILE > $JACOCO_REPL_FILE
echo "Finished replacing"
echo $(ls "target/jacoco-report")
