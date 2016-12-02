# From LaTeX to CAS

# Rules
1. We are working with [Abdou's](https://github.com/abdouyoussef) JAR, which is not public yet. It is strictly **_prohibited to upload or share_**  this JAR with other people (even with other contributers). 
2. Only [Abdou](https://github.com/abdouyoussef) is allowed to handout the JAR. If you want to be a contributor contact [Howard Cohl](https://github.com/HowardCohl) and [Abdou Youssef](https://github.com/abdouyoussef) to ask for the permission.
3. Organize your issues in the [projects tab](https://github.com/TU-Berlin/latex-grammar/projects).
4. If you working on an open task, drag and drop this task to _"accepted tasks"_.

## Structure
1. [How to use our program](#howTo)
2. [Troubleshooting](#troubleshooting)
3. [The Team](#contributers)
4. [How to contribute](https://github.com/TU-Berlin/latex-grammar/edit/master/CONTRIBUTING.md)

## How to use our program<a name="howTo"></a>
You should found the `latex-grammar-<version_number>.zip` in the main directory of this repository. Download this zip and unzip it where ever you want. There are two jars in this zip file.
* `latex-converter.jar`: This jar translates given formulae in semantic LaTeX into a given computer algebra system. Per default it supports translations to Maple and Mathematica (make sure you use a the correct notation). It has also some features on board. Run `latex-converter.jar -h` gives you an overview. There is a flag called `-extra` that gives you further information about the translation process. If you don't set this flag, it will simply translates your expression without any other information. Additionally you can add the `-debug` flag to give a little more information about time and elements of the output. In general you don't need that.
* `lexicon-creator.jar`: This jar takes the CSV files in `libs/ReferenceData/CSVTables` and translate them to a lexicon file (the math language processors based on this lexicon files). You only have to add the CSV files for a CAS and not the DLMFMacro.csv file. This jar is only useful when you have any updates. For a detailed explanation how to add new translations or even support another computer algebra system take a look to the _[Update or add a new CAS to the translation process](https://github.com/TU-Berlin/latex-grammar/edit/master/CONTRIBUTING.md#howToUpdate)_ section in the contributing.md.

## Troubleshooting<a name="troubleshooting"></a>
When you want to contribute or just run our program it could happen to get some errors. Here are some tips to avoid that. When every you found an error which is not explained here and you don't know how to fix it by your own, feel free to contact [André Greiner-Petter](https://github.com/AndreG-P) (or some of the [other contributers](#contributers)).

1. You cannot translate your CSV file to our lexicon files. (typical exception like: MalformedInputException)
This could happen when our program cannot find out the encoding of your CSV file. It is strongly recommended to set the encoding to UTF-8 (with or without BOM) of your CSV file.

## Current contributers and their roles<a name="contributers"></a>
[Howard Cohl](https://github.com/HowardCohl) & [Moritz Shubotz](https://github.com/physikerwelt): Supervisor

[Abdou Youssef](https://github.com/abdouyoussef): Advisor

[André Greiner-Petter](https://github.com/AndreG-P): Developer (main task: translate semantic LaTeX to CAS)

[Claude](https://github.com/ClaudeZou) & [Jagan](https://github.com/notjagan): Developer (main task: translate generic LaTeX to semantic LaTeX)

## How to contribute?
Take a look to the [contributing guidelines](https://github.com/TU-Berlin/latex-grammar/edit/master/CONTRIBUTING.md)
