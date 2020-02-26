#!/usr/bin/env bash

JACOCO_FILE="target/jacoco-report/jacoco.xml"
JACOCO_REPL_FILE="target/jacoco-report/jacoco-fix.xml"

sed -e 's/gov\//src\/main\/java\/gov\//g' -e 's/<[\/]\?group[^>]*>//g' $JACOCO_FILE > $JACOCO_REPL_FILE
