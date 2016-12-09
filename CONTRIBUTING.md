# Contributing to LaTeX-Grammar

# Rules
1. We are working with [Abdou's](https://github.com/abdouyoussef) JAR, which is not public yet. It is strictly **_prohibited to upload or share_**  this JAR with other people (even with other contributers). 
2. Only [Abdou](https://github.com/abdouyoussef) is allowed to handout the JAR. If you want to be a contributor contact [Howard Cohl](https://github.com/HowardCohl) and [Abdou Youssef](https://github.com/abdouyoussef) to ask for the permission.
3. Organize your issues in the [projects tab](https://github.com/TU-Berlin/latex-grammar/projects).
4. If you working on an open task, drag and drop this task to _"accepted tasks"_.

## Structure
1. [What is our goal?](#goal)
2. [What should I know before I start?](#start)
3. [The program structure and important main classes](#program)
4. [Update or add a new CAS to the translation process](#howToUpdate)
5. [Troubleshooting](#troubleshooting)

## What is our goal?<a name="goal"></a>
We want to create a software that translates generic LaTeX to a given computer algebra system (called CAS). To get a rough overview, take a look at our [usecase for the Jacobi polynomial](https://github.com/TU-Berlin/latex-grammar/blob/master/LaTeX2CAS.pdf).

## What should I know before I start?<a name="start"></a>
#### The IDE
We usually working with IntelliJ. That's why you can find an .idea directory in the project. When you clone this repo to your local machine it should be possible to simple open the project folder (latex-grammar) with IntelliJ and everything is already setup. If something is wrong, you can create the normal project structure by maven. You should start with interpreter.core, since this is the main module of all other modules. To give it a try, you can nevigate to `gov.nist.drmf.interpreter.examlples` package in `interpreter.core` and run the `MLP.java`. This is an example usage of Abdou's math language processor.

#### Math Language Processor (MLP)
Abdou has uploaded a MLP.zip file. You can find it in [`libs/MLP/`](https://github.com/TU-Berlin/latex-grammar/tree/master/libs/MLP). The most important file is the MLP.jar in the same directory. Make sure your idea has included this jar. Abdou has also written a [_"Software usage guide"_](https://github.com/TU-Berlin/latex-grammar/tree/master/libs/MLP) and created a documentation for the MLP.jar [starts with the index.html](https://github.com/TU-Berlin/latex-grammar/tree/master/libs/MLP/javadoc). If you have any questions about the MLP, ask [Abdou Youssef](https://github.com/abdouyoussef) or [André Greiner-Petter](https://github.com/AndreG-P).

#### How to compile
When you open the project in IntelliJ you only have to start `Build -> Build Artifacts... -> All Artifacts`. This will creat two jars (namely `latex-converter.jar` and `lexicon-creator.jar`).

#### Orginzing tasks via issues
For precise tasks, take a look to the [issues](https://github.com/TU-Berlin/latex-grammar/issues) and [projects](https://github.com/TU-Berlin/latex-grammar/projects). When ever you working on our project, please create issues and organize them in the projects overview. That makes it much easier for everybody to see what is going on.

## The program structure and important main classes<a name="program"></a>
There are a couple of main classes in the project.
* `interpreter.core -> ...interpreter.examples.MLP.java`: This is an example of Abdou's math language processors. It runs a hard-coded example of the analyzing process.
* `interpreter.cas  -> ...interpreter.cas.SemanticToCASInterpreter.java`: That's the main class to translate formulae to a computer algebra system.
* `interpreter.cas  -> ...interpreter.cas.mlp.CSVtoLexiconConverter.java`: This class translates given CSV files to lexicon files. You only have to add the translation CSV files and not the DLMFMacro.csv itself to create a correct lexicon.

## Update or add a new CAS to the translation process<a name="howToUpdate"></a>
Since this project is still in progress, there are a lot of translations and information about these missing. Here is a list of all extendable files and how to use them. If you want to support a whole new computer algebra system, take a look at the sub section [add a whole new CAS](#newCAS). You can find all of the following files in the `libs/ReferenceData` directory.

#### Add some information or translations to already supported CAS
First of all there are some basic functions in the system which has a special role in the program. These functions are supported in LaTeX by default and don't have to have an extra DLMF macro. `\sqrt` and `\frac` are famous examples for these functions. On the other hand there are some symbols with special meanings (like the exclamation mark ! for factorial). These functions and symbols are defined in `BasicFunctions.json`.
* _Basic Functions_: When you take a look into `BasicFunctions.json` you see four categories in two groups. The first group is for the functions. There is a node with all languages. If you want to support a complete new CAS you have to add the name of your CAS here as well and add a translation for each of the existing elements. If you only want to support an additional function add a new node under `Functions`.
* _Symbols_: This is similar to the _Basic Functions_ section. This translation became necessary after we figured out that it is necessary to support translations even for simple symbols (like a sign for multiplication). That's why you can find an element for _`General Multiplication`_ in it. For Maple it is absolutely necessary to add an asterisk between each factor, meanwhile in Mathematica we can add space between each factor (which is better to read).

Besides that we have a JSON file for Greek letters and constants as well. It's called `GreekLettersAndConstants.json` and uses the same structure like `BasicFunctions.json`.
* _Greek Letters_: Again add a Greek letter that is missing to the _`Greek Letters`_ node.
* _Constants_: Constants are a bit more tricky. First you have to add the translation for your new constant here as well (like a Greek letter). But it could happen that our program cannot find this constant. It has to be defined in the lexicon as well. To extend the lexicon take a look to the following section [Add new translations to the lexicon](#lexiconAddOn).

#### Add new translations to the lexicon<a name="lexiconAddOn"></a>
Besides those simple JSON files we have an Excel file for all DLMF macros and all translations of the special functions. The main file is the `LaTeX and Maple.xlsx`. The first sheet gives an overview, which is useful for humans but hard to understand for a computer. That's why we have several sheets for each CAS and one sheet for each DLMF macro.
* `Macro-Information sheet`: This sheet contains all of the DLMF and DRMF macros and provides some further information.
* `Macro2<CAS> sheets`: As you can see the first column contains again each DLMF/DRMF macro which is necessary to match the translation correctly. When ever you want to support a new CAS the columns must have the same structure (but the the order can be differenct, excepts for the first column). There must be a column called like your CAS, one with `<CAS>-Comment` and so on. We use placeholders like `$0` to define a position of a variable, parameter. For instance, the `\JacobiP` macro uses another order in Maple and Mathematica (the subscript comes before the superscript). The numbering starts at zero. `$0` refers to the first parameter, variable.

After you add a new entry or change an existing entry follow these instructions to add the changes to our program:
1. Export the sheet you changed to a CSV file (separated by semicolons ;) and put them into `libs/ReferenceData/CSVTables/`. The name for the DLMF/DRMF macros file is fixed. It has to be `DLMFMacro.csv`. The names for the other CSV files are up to you. You have to add these files manually in step 3.

2. Make sure each CSV file is encoded in UTF-8 (with ot without BOM) because there are some entries possible with illegal letters like an `ö` in `Möbius`.

3. Run the `lexicon-creator.jar` with `java -jar lexicon-creator.jar`. It will ask you to add your CAS specific CSV files. Add them (one CSV file per line, hit enter). When you are done, enter -end and hit enter. Make sure you have to add the already supported CAS as well. Otherwise these translations are lost in the lexicon file. You can also add all CSV files directly behind the jar. For our default program (Maple and Mathematica) it looks like `java -jar lexicon-creator.jar CAS_Maple.csv CAS_Mathematica.csv`. The program will could show you some problems during the translation process. You should at least solve all severe errors.

4. Done, you can start `latex-converter.jar` now to test your changes.

#### Add a whole new CAS<a name="newCAS"></a>
To add a new whole computer algebra system you have to add the name of your CAS to all of the JSON files `BasicFunctions.json` and `GreekLettersAndConstants.json` and add a translation for to each already existing entry for your CAS as well. If one is missing, the latex-converter cannot start correctly. This is the minimum precedure to make the latex-converter runnable again. To support your new CAS correctly you have to create a complete new sheet for the CSV files. You can find a description in [Add new translations to the lexicon](#lexiconAddOn) section above.

## Troubleshooting<a name="troubleshooting"></a>
When you want to contribute or just run our program it could happen to get some errors. Here are some tips to avoid that. When ever you found an error which is not explained here and you don't know how to fix it by your own, feel free to contact [André Greiner-Petter](https://github.com/AndreG-P) (or some of the [other contributers](#contributers)).

1. You cannot translate your CSV file to our lexicon files. (typical exception: MalformedInputException)
This could happen when our program cannot find out the encoding of your CSV file. It is strongly recommended to set the encoding to UTF-8 (with or without BOM) of your CSV file.
