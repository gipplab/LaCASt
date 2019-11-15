import csv


def main():
    count = 0
    maple_trans = []
    mathematica_trans = []
    with open("/home/rid/latex-grammar/libs/ReferenceData/CSVTables/CAS_Maple.csv") as maple:
        csv_reader = csv.reader(maple, delimiter=";")
        for row in csv_reader:
            if row[0] != "" and row[1] != "":
                count += 1
                maple_trans.append(row)
        #number of maple functions in the csv
        print("Maple translations: " + str(count))

    count = 0
    with open("/home/rid/latex-grammar/libs/ReferenceData/CSVTables/CAS_Mathematica.csv") as mathematica:
        csv_reader = csv.reader(mathematica, delimiter=";")
        for row in csv_reader:
            if row[0] != "" and row[1] != "":
                count += 1
                mathematica_trans.append(row)
        #number of mathematica functions in the csv
        print("Mathematica translations: " + str(count))

    count = 0
    count2 = 0
    for row in maple_trans:
        if count == 0:
            count += 1
            continue
        cond1 = "{" in row[1]
        if cond1:
          index = row[1].index("{")
          macro = row[1][0:index]
        if macro not in str(mathematica_trans):
            count2 += 1
            print(row)
    #number of functions in maple's csv that mathematica's csv does not have
    print("Diff: " + str(count2))

    count = 0
    with open("/home/rid/latex-grammar/libs/ReferenceData/Lexicons/DLMF-macros-lexicon.txt") as lexicon:
        for row in lexicon.readlines():
            if "Mathematica: " in row and len(row) > 14:
                count += 1
        #number of mathematica functions in the lexicon
        print("Mathematica: " + str(count))

    count2 = 0
    with open("/home/rid/latex-grammar/libs/ReferenceData/Lexicons/DLMF-macros-lexicon.txt") as lexicon:
        for row in lexicon.readlines():
            if "Maple: " in row and len(row) > 8:
                count2 += 1
        #number of maple functions in the lexicon
        print("Maple: " + str(count2))
        #number difference in the number of maple functions and mathematica functions in the lexicon
        print("Diff: " + str(count2 - count))


if __name__ == '__main__':
    main()