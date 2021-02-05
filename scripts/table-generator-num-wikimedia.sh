#!/usr/bin/env bash

pageNames=(
  "Algebraic and Analytic Methods"
  "Asymptotic Approximations"
  "Numerical Methods"
  "Elementary Functions"
  "Gamma Function"
  "Exponential, Logarithmic, Sine, and Cosine Integrals"
  "Error Functions, Dawson’s and Fresnel Integrals"
  "Incomplete Gamma and Related Functions"
  "Airy and Related Functions"
  "Bessel Functions" #10
  "Struve and Related Functions"
  "Parabolic Cylinder Functions"
  "Confluent Hypergeometric Functions"
  "Legendre and Related Functions"
  "Hypergeometric Function"
  "Generalized Hypergeometric Functions and Meijer G-Function"
  "q-Hypergeometric and Related Functions"
  "Orthogonal Polynomials" # ====================== 18 ========================
  "Elliptic Integrals"
  "Theta Functions"
  "Multidimensional Theta Functions"
  "Jacobian Elliptic Functions"
  "Weierstrass Elliptic and Modular Functions"
  "Bernoulli and Euler Polynomials"
  "Zeta and Related Functions"
  "Combinatorial Analysis"
  "Functions of Number Theory"
  "Mathieu Functions and Hill’s Equation"
  "Lamé Functions"
  "Spheroidal Wave Functions"
  "Heun Functions"
  "Painlevé Transcendents"
  "Coulomb Functions"
  "3j,6j,9j Symbols"
  "Functions of Matrix Argument"
  "Integrals with Coalescing Saddles"
)

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

BASELINE="$BASEPATH/OldTrans"

FILECOUNTER=0

totForm=0
totTransBase=0
totTransMaple=0
totTransMath=0

totSymMapS=0
totSymMapF=0
totSymMathS=0
totSymMathF=0

totNumMapS=0
totNumMapF=0
totNumMapPT=0
totNumMapTP=0
totNumMapA=0
totNumMapE=0

totNumMathS=0
totNumMathF=0
totNumMathPT=0
totNumMathTP=0
totNumMathA=0
totNumMathE=0

FILES=$(find $MATHP -type f | sort)

for FILEMATH in $FILES; do
  [[ $FILEMATH =~ .*([0-9][0-9])\-([A-Z]+)-.* ]]

  num=${BASH_REMATCH[1]};
  id=${BASH_REMATCH[2]};

  refFMath="$MATHBASE/$num-$id-symbolic.txt";
  FILEMAPLE="$MAPLEP/$num-$id-numeric.txt";
  refFMaple="$MAPLEBASE/$num-$id-symbolic.txt";

  baselineFile="$BASELINE/$num-$id-symbolic.txt"
  
  IFS=','
  read -r -a baselineTrans <<< $(gawk 'match($0, /.*SUCCESS_SYMB: ([0-9]+),.*SUCCESS_TRANS: ([0-9]+),.*/, arry) {print arry[1]","arry[2]}' $baselineFile)

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

  succBaselineTrans=$((baselineTrans[1]))

  allSymbSuccMath=$((arrTransMath[0]+arrTransMath[2]))
  symFAILMath="${arrMathMath[2]}"
  avgSYMSUCCMath=$(bc <<< "scale=2; 100*${allSymbSuccMath}/${arrTransMath[1]}")
  avgNUMSUCCMath=$(bc <<< "scale=2; 100*${arrMathMath[0]}/${symFAILMath}")
  
  allSymbSuccMaple=$((arrTransMaple[0]+arrTransMaple[2]))
  symFAILMaple="${arrMathMaple[2]}"
  avgSYMSUCCMaple=$(bc <<< "scale=2; 100*${allSymbSuccMaple}/${arrTransMaple[1]}")
  avgNUMSUCCMaple=$(bc <<< "scale=2; 100*${arrMathMaple[0]}/${symFAILMaple}")


  link="[[Results of ${pageNames[FILECOUNTER]}|${id}]]"
  if (( $FILECOUNTER == 9 )); then
    link="BS [[Results of Bessel Functions I|I]] & [[Results of Bessel Functions II|II]]"
  elif (( $FILECOUNTER == 18 )); then
    link="EL [[Results of Elliptic Integrals I|I]] & [[Results of Elliptic Integrals II|II]]"
  fi
  
  # 1. AL || Trans Maple || Trans Math || Symb Succ Maple || Percent || Fail || Symb Succ Math || Percent || Fail || Num Succ || Percent || Fail || [P/T]
  printf "! scope=\"row\" style=\"text-align: left; border-right: solid 1px #000\"| %2d. %s \n| style=\"border-right: solid 1px #000;\" | %3d \n|| %3d || %3d \n| style=\"border-right: solid 1px #000;\" | %3d \n|| %3d || %4.1f%% \n| style=\"border-right: solid 2px #aaa;\" | %3d \n|| %3d || %4.1f%% \n| style=\"border-right: solid 1px #000;\" | %3d \n|| %3d || %4.1f%% || %3d || [%3d / %3d] || %3d \n| style=\"border-right: solid 2px #aaa;\" | %3d \n|| %3d || %4.1f%% || %3d || [%3d / %3d] || %3d || %3d \n|-\n" \
  	"${num#0}" "${link}" "${arrTransMath[3]}"\
  	"${succBaselineTrans}" "${arrTransMaple[1]}" "${arrTransMath[1]}" \
  	"${allSymbSuccMaple}" "${avgSYMSUCCMaple}" "${symFAILMaple}" \
  	"${allSymbSuccMath}" "${avgSYMSUCCMath}" "${symFAILMath}" \
  	"${arrMathMaple[0]}" "${avgNUMSUCCMaple}" "${arrMathMaple[1]}" "${partFailedMaple[0]}" "${partFailedMaple[1]}" "${arrMathMaple[3]}" "${arrMathMaple[4]}" \
  	"${arrMathMath[0]}" "${avgNUMSUCCMath}" "${arrMathMath[1]}" "${partFailedMath[0]}" "${partFailedMath[1]}" "${arrMathMath[3]}" "${arrMathMath[4]}"

  totForm=$((totForm+arrTransMath[3]))
  totTransBase=$((totTransBase+succBaselineTrans))
  totTransMaple=$((totTransMaple+arrTransMaple[1]))
  totTransMath=$((totTransMath+arrTransMath[1]))

  totSymMapS=$((totSymMapS+allSymbSuccMaple))
  totSymMapF=$((totSymMapF+symFAILMaple))
  totSymMathS=$((totSymMathS+allSymbSuccMath))
  totSymMathF=$((totSymMathF+symFAILMath))

  totNumMapS=$((totNumMapS+arrMathMaple[0]))
  totNumMapF=$((totNumMapF+arrMathMaple[1]))
  totNumMapPT=$((totNumMapPT+partFailedMaple[0]))
  totNumMapTP=$((totNumMapTP+partFailedMaple[1]))
  totNumMapA=$((totNumMapA+arrMathMaple[3]))
  totNumMapE=$((totNumMapE+arrMathMaple[4]))

  totNumMathS=$((totNumMathS+arrMathMath[0]))
  totNumMathF=$((totNumMathF+arrMathMath[1]))
  totNumMathPT=$((totNumMathPT+partFailedMath[0]))
  totNumMathTP=$((totNumMathTP+partFailedMath[1]))
  totNumMathA=$((totNumMathA+arrMathMath[3]))
  totNumMathE=$((totNumMathE+arrMathMath[4]))

  #printf "\\TT\\TB \\\verb|%s| & %2d & %3d & %3d & (%4.1f\\%%) & %3d & %3d & (%4.1f\\%%) & %3d & [%3d / %3d] & %3d & %3d \\\\\\ \\hline\n" \
  #	"${id}" "${num#0}" "${arrTransMath[1]}" "${allSymbSuccMath}" "${avgSYMSUCCMath}" "${symFAILMath}" \
  #	"${arrMathMath[0]}" "${avgNUMSUCCMath}" "${arrMathMath[1]}" "${partFailedMath[0]}" "${partFailedMath[1]}" "${arrMathMath[3]}" "${arrMathMath[4]}"

  FILECOUNTER=$((FILECOUNTER+1))

  if (( $FILECOUNTER == 20 )); then
    printf "! scope=\"row\" style=\"text-align: left; border-right: solid 1px #000\"| 21. [[Results of Multidimensional Theta Functions|MT]] \n|style=\"border-right: solid 1px #000;\" | - \n|| - || - \n| style=\"border-right: solid 1px #000;\" | - \n|| - || - \n| style=\"border-right: solid 2px #aaa;\" | - \n|| - || - \n| style=\"border-right: solid 1px #000;\" | - \n|| - || - || - || - || - \n| style=\"border-right: solid 2px #aaa;\" | - \n|| - || - || - || - || - || - \n|-\n"
    FILECOUNTER=$((FILECOUNTER+1))
  elif (( $FILECOUNTER == 34 )); then
    printf "! scope=\"row\" style=\"text-align: left; border-right: solid 1px #000\"| 35. [[Results of Functions of Matrix Argument|FM]] \n|style=\"border-right: solid 1px #000;\" | - \n|| - || - \n| style=\"border-right: solid 1px #000;\" | - \n|| - || - \n| style=\"border-right: solid 2px #aaa;\" | - \n|| - || - \n| style=\"border-right: solid 1px #000;\" | - \n|| - || - || - || - || - \n| style=\"border-right: solid 2px #aaa;\" | - \n|| - || - || - || - || - || - \n|-\n"
    FILECOUNTER=$((FILECOUNTER+1))
  fi
done

avgSYMSUCCMath=$(bc <<< "scale=2; 100*${totSymMathS}/${totTransMath}")
avgNUMSUCCMath=$(bc <<< "scale=2; 100*${totNumMathS}/${totSymMathF}")
avgSYMSUCCMaple=$(bc <<< "scale=2; 100*${totSymMapS}/${totTransMaple}")
avgNUMSUCCMaple=$(bc <<< "scale=2; 100*${totNumMapS}/${totSymMapF}")

printf "! <math>\sum</math> || %3d || %3d || %3d || %3d || %3d || %4.1f%% || %3d || %3d || %4.1f%% || %3d || %3d || %4.1f%% || %3d || [%3d / %3d] || %3d || %3d || %3d || %4.1f%% || %3d || [%3d / %3d] || %3d || %3d \n|-\n" \
  	"${totForm}"\
  	"${totTransBase}" "${totTransMaple}" "${totTransMath}" \
  	"${totSymMapS}" "${avgSYMSUCCMaple}" "${totSymMapF}" \
  	"${totSymMathS}" "${avgSYMSUCCMath}" "${totSymMathF}" \
  	"${totNumMapS}" "${avgNUMSUCCMaple}" "${totNumMapF}" "${totNumMapPT}" "${totNumMapTP}" "${totNumMapA}" "${totNumMapE}" \
  	"${totNumMathS}" "${avgNUMSUCCMath}" "${totNumMathF}" "${totNumMathPT}" "${totNumMathTP}" "${totNumMathA}" "${totNumMathE}"