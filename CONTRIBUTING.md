# Contributing to LaTeX-Grammar

# General Rules
1. It is strictly prohibited to share any files from this repository without permission!
2. Do not push to the master branch of the project. Please checkout a different branch and create a pull request!

## Structure
1. [Setup Project](#start)
2. [Goals for the summer extensions](#summer-extensions)
3. [Update or add a new CAS to the translation process](#howToUpdate)
4. [The program structure and important main classes](#program)
5. [Troubleshooting](#troubleshooting)

## Setup Project<a name="start"></a>
#### 1. Setup SSH for git
* These steps only need to be done once
* If you don't have a private-public ssh key authentication setup then generate a private key and upload it to GitHub
* Run <code>ssh-keygen</code>
* Display the public key by executing <code>cat ~/.ssh/id_rsa.pub</code>
* Copy all of the output and paste it to your GitHub -> Settings -> SSH keys, namely https://github.com/settings/keys. Do this by hitting <code>New SSH key</code> button.

#### 2. Setup Git
* Make sure to do this before you do your first commit otherwise you will have problems
* Run <code>git config --global user.name "YOUR NAME"</code> to set the author's name
* Run <code>git config --global user.email "YOUR_EMAIL@example.com"</code> to set the author's email
* Run <code>git config --global core.editor "vim"</code> to set the author's editor

#### 3. Download the project
* Run <code>git clone git@github.com:ag-gipp/latex-grammar.git</code>
* cd into the folder you downloaded `cd latex-grammer`
* Run `git fetch` to fetch remote branches to your local machine
* Run `git checkout -b extensions` to create a new branch with name `extensions`. Do not work on the `master` branch!

#### 4. Setup Maven
The project use maven as a build tool, maven 3 in particular. This means we use maven to organize external packages.
* Make sure you have maven 3 (`mvn --version`)

#### 5. Setup IntelliJ
We are working with IntelliJ. If you download the project open the project via IntelliJ in the following way:
* Launch IntelliJ
* Hit `Import Project`
* Select the main directory of our repo (`latex-grammar`)

#### 6. Push changes
When you make changes you have to commit them to git. First lets check if there are unstaged changes
```bash
git status
```
Unstaged changes have to be added before we can commit them. You can do this via
```bash
git add file_name
```
Instead of adding single files, you can add all files directly with `git add .`.
Once you added the files you can commit them
```bash
git commit -m "your commit message"
``` 
The `git commit` command has an extra flag `-a` which automatically adds all unstaged files.
Thus you don't need to use the `git add` command if you want to commit all changes at once.
Please use reasonable commit messages for your changes. Also, keep your commits small. This helps
everybody to track your changes and give tips and feedback.

A commit does not push the changes to GitHub. You have to push your commits via
```bash
git push
```

#### 7. Create a pull request
We can easy track your changes when you create a pull request for your branch. To do so, go to the
GitHub repository and click on `Pull requests`. Here you can hit `New pull request`. The base should
be the `master` branch and you want to compare with your working branch `summer-extensions`. If you
created your own sub-branches of `summer-extensions` you should request a merge with `summer-extensions`
instead of the `master` branch. In this case the base will be `summer-extensions` and the compare branch
is your own specified branch.

#### 8. Organizing tasks via issues
We organize the work via issues in [issues](https://github.com/TU-Berlin/latex-grammar/issues).
So please use issues if you have questions or problems. And also use them to define your next tasks.


## Goals for the summer extensions<a name="summer-extensions"></a>
We have two main goals.
1. Support Wronskians and prime symbols (see #74)
2. Support `lim` and similar macros generic macros (see #73)

## Update or add a new CAS to the translation process<a name="howToUpdate"></a>
All translations are organized in `libs/ReferenceData/CSVTables` directory. Here you can find CSV files (semicolon separated) that keep
track of the supported translations. You can use any editor you prefer to update the CSV files. Here are the most important files
1. `DLMFMacro.csv` this file provides all information about the DLMF macros. There are no translations defined here.
2. `DLMF_<CAS>.csv` these files define the _forward_ translations from DLMF macros to a CAS. For example, `DLMF_Maple.csv` defines
the translations from DLMF to the CAS Maple. Always follow this naming convention if you add new CAS.
3. `CAS_<name>` these files define the _backward_ translations from a CAS (`<name>`) back to the DLMF macros. For example, `CAS_Mathematica.csv`
defines the backward translations from Mathematica back to the DLMF.

#### Add new translations to the lexicon<a name="lexiconAddOn"></a>
After you add a new entry or change an existing entry follow these instructions to add the changes to our program:
1. Export the sheet you changed to a CSV file (separated by semicolons ;) and put them into `libs/ReferenceData/CSVTables/`. The name for the DLMF/DRMF macros file is fixed. It has to be `DLMFMacro.csv`. The names for the other CSV files are up to you. You have to add these files manually in step 3.

2. Make sure each CSV file is encoded in UTF-8 (with ot without BOM) because there are some entries possible with illegal letters like an `ö` in `Möbius`.

3. Run the `lexicon-creator.jar` with `java -jar lexicon-creator.jar`. 
It will ask you to add your CAS specific CSV files. Add them (one CSV file per 
line, hit enter). When you are done, enter -end and hit enter. Make sure you 
have to add the already supported CAS as well. Otherwise these translations are 
lost in the lexicon file. You can also add all CSV files directly behind the jar. 
For our default program (Maple and Mathematica) it looks like 
`java -jar lexicon-creator.jar CAS_Maple.csv CAS_Mathematica.csv`. 
The program my show you some errors. You must solve all severe errors, if any.

4. Done, the translator supports your changes now.

## The program structure and important main classes<a name="program"></a>
There are a couple of main classes in the project.
* `interpreter.common  -> ...interpreter.examples.MLP.java`: This is an example of Abdou's math language processors. It runs a hard-coded example of the analyzing process.
* `interpreter.lacast   -> ...interpreter.cas.SemanticToCASInterpreter.java`: That's the main class to translate formulae to a computer algebra system.
* `interpreter.lacast   -> ...interpreter.cas.mlp.CSVtoLexiconConverter.java`: This class translates given CSV files to lexicon files. You only have to add the translation CSV files and not the DLMFMacro.csv itself to create a correct lexicon.
* `interpreter.maple -> ...interpreter.maple.MapleToSemanticInterpreter`: This class translates Maple expressions back to semantic LaTeX.

## Troubleshooting<a name="troubleshooting"></a>
When you want to contribute or just run our program it could happen to get some errors. Here are some tips to avoid that. When ever you found an error which is not explained here and you don't know how to fix it by your own, feel free to contact [André Greiner-Petter](https://github.com/AndreG-P) (or some of the [other contributers](#contributers)).

1. You cannot translate your CSV file to our lexicon files. (typical exception: MalformedInputException)
This could happen when our program cannot find out the encoding of your CSV file. It is strongly recommended to set the encoding to UTF-8 (with or without BOM) of your CSV file.
