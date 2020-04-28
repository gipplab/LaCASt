# Contributing to LaTeX-Grammar

1. It is strictly prohibited to share any files from this repository without permission!
2. Do not push to the master branch of the project. Please checkout a different branch and create a pull request!

## Structure
1. [Setup Project](#start)
2. [Generate Jars](#jars)
3. [Test Coverage](#test-coverage)
4. [Update or add a new CAS to the translation process](#howToUpdate)
5. [The program structure and important main classes](#program)
6. [Troubleshooting](#troubleshooting)

## Setup Project<a name="start"></a>

If you have any trouble with the following steps please contact [André Greiner-Petter](https://github.com/AndreG-P) [andre.greiner-petter@t-online.de]. If you are familar with Git and Maven, you may just check the steps 6 and 8. However, if something does not work as you expected, make sure you follow all steps before you contact André and before you create any issues!

<details><summary><strong>1. Setup SSH for git</strong></summary>
  
* These steps only need to be done once
* If you don't have a private-public ssh key authentication setup then generate a private key and upload it to GitHub
* Run <code>ssh-keygen</code>
* Display the public key by executing <code>cat ~/.ssh/id_rsa.pub</code>
* Copy all of the output and paste it to your GitHub -> Settings -> SSH keys, namely https://github.com/settings/keys. Do this by hitting <code>New SSH key</code> button.

</details>

<details><summary><strong>2. Setup Git</strong></summary>
  
* Make sure to do this before you do your first commit otherwise you will have problems
* Run <code>git config --global user.name "YOUR NAME"</code> to set the author's name
* Run <code>git config --global user.email "YOUR_EMAIL@example.com"</code> to set the author's email
* Run <code>git config --global core.editor "vim"</code> to set the author's editor
</details>

<details><summary><strong>3. Install Java 11</strong></summary>

Make sure you have JDK 11 installed. There should be no difference between Oracle's JDK and OpenJDK anymore in Java 11. However, if you are unsure, take OpenJDK. Please look up installation by your own, since the installation is system dependent.
</details>

<details><summary><strong>4. Download the project sources</strong></summary>
  
* Run <code>git clone git@github.com:ag-gipp/LaCASt.git</code>
* cd into the folder you downloaded `cd latex-grammer`
* Run `git fetch` to fetch remote branches to your local machine
* Run `git checkout -b extensions` to create a new branch with name `extensions`. You do not have permissions to push any changes to the master branch, so it's recommended to create a new branch right at the beginning.
</details>

<details><summary><strong>5. Setup Maven</strong></summary>
  
The project use maven as a build tool, maven 3 in particular. Please install at least maven 3.6.1, otherwise there might be trouble with Java 11. After that, you can install dependencies and run tests.

* Make sure you have maven 3.6.1 or higher (`mvn --version`)
* Run `mvn clean install -DskipTests`
* Run `mvn test`

If these commands does not finish succesfully, please contact [André Greiner-Petter](https://github.com/AndreG-P) [andre.greiner-petter@t-online.de] and create an issue about it on [https://github.com/ag-gipp/LaCASt/issues].
</details>

<details><summary><strong>6. Setup Maple and Mathematica (optional)</strong></summary>

This is only necessary if you work on the backward translations or the evaluation engine.

* Change the properties in `pom.xml`
```xml
<properties>
    <maple.installation.dir>/opt/maple2019</maple.installation.dir>
    <mathematica.installation.dir>/opt/Wolfram</mathematica.installation.dir>
</properties>
```

* For Mathematica, update `config/mathematica_config.properties`
```properties
mathematica_math=/opt/Wolfram/Executables/math
```

* Verify everything works. Run again `mvn clean install -DskipTests`
* If you run the tests again, you should see more test cases now: `mvn test`

</details>

<details><summary><strong>7. Setup IntelliJ</strong></summary>
  
We are working with IntelliJ. If you download the project open the project via IntelliJ in the following way:
* Launch IntelliJ
* Hit `Import Project`
* Select the main directory of our repo (`LaCASt`)
* Select `Import project from external model` and select `maven`
* Next, see the settings in the image:
![Maven Setup](https://github.com/ag-gipp/LaCASt/blob/restructure/misc/setupmaven.png)
* Intellij should show you one module `gov.nist.drmf.interpreter:nterpreter:2.1-SNAPSHOT`. Only select this, hit next until you finish.
</details>

<details><summary><strong>8. Build and Run in IntelliJ</strong></summary>
  
**IMPORTANT:** For all classes or test cases you start in IntelliJ, make sure Intellij uses the root directory `LaCASt` as the `Working Directory`. You can set this up in `Run/Debug Configurations` via `Run -> Edit Configurations...`.

* You can run maven within IntelliJ. Open Maven and run `Semantic Translator (root)` -> `install`.
* To test if the forward translation works, go to `interpreter.lacast -> src -> main -> java -> gov.nist.drmf.interpreter.cas` and run `SemanticToCASInterpreter.java`. 
* To test the backward translation, e.g., from Maple, go to `interpreter.maple -> src -> main -> java -> gov.nist.drmf.interpreter.maple` and run `MapleToSemanticInterpreter.java`
    * You will (most likely) see an error. You have to tell IntelliJ the right settings.
    * Open `Run/Debug Configurations` via `Run -> Edit Configurations...`
    * Make sure you set the right working directory (see important note above)
    * Set the environment variables (click on the `$` sign in the end of the line
    * Add `MAPLE=/opt/maple2019` and `LD_LIBRARY_PATH=/opt/maple2019/bin.X86_64_LINUX` (of coursel, the paths might be different on your system, so update it accordingly)
    * Add the VM option: `-Xss50M`, otherwise the heap space is too small for Maple 2019
    
**Tip:** You can change the default `working directory` in Intellij under 
`Run -> Edit Configurations... -> Templates (left list)`. Here you choose `Application` and `JUnit` and set the 
`Working Directory` value to `$ProjectFileDir$`.
</details>

<details><summary><strong>9. Push changes</strong></summary>
  
When you make changes you have to commit them to git. First lets check if there are unstaged changes
```shell script
git status
```
Unstaged changes have to be added before we can commit them. You can do this via
```shell script
git add file_name
```
Instead of adding single files, you can add all files directly with `git add .`.
Once you added the files you can commit them
```shell script
git commit -m "your commit message"
``` 
The `git commit` command has an extra flag `-a` which automatically adds all unstaged files.
Thus you don't need to use the `git add` command if you want to commit all changes at once.
Please use reasonable commit messages for your changes. Also, keep your commits small. This helps
everybody to track your changes and give tips and feedback.

A commit does not push the changes to GitHub. You have to push your commits via
```shell script
git push
```
</details>

<details><summary><strong>10. Create a pull request</strong></summary>
  
We can easy track your changes and discuss/comment code when you create a pull request for your branch. To do so, go to the
GitHub repository and click on `Pull requests`. Here you can hit `New pull request`. The base should
be the `master` branch and you want to compare with your working branch `summer-extensions`. If you
created your own sub-branches of `summer-extensions` you should request a merge with `summer-extensions`
instead of the `master` branch. In this case the base will be `summer-extensions` and the compare branch
is your own specified branch.
</details>

<details><summary><strong>11. Organizing tasks via issues</strong></summary>
  
We organize the work via issues in [issues](https://github.com/TU-Berlin/LaCASt/issues).
So please use issues if you have questions or problems. And also use them to define your next tasks.
</details>

## Generate Jars<a name="jars"></a>
The general maven install cycle will not generate the jars in `bin` by default because their sizes
were slowing down the git workflow. Instead, the jars should only updated when needed 
(e.g., DRMF server require the new translator or when a new evaluation session is planned) or when
we reached another milestone.

To trigger the process to generate/update the jars, you have to add `-DgenerateJars` flag to the `install`
phase of maven
```shell script
mvn install -DgenerateJars
```

## Test Coverage<a name="test-coverage"></a>
We use Maven with Jacoco to create test coverage reports. Due to the fact that the program rely on third party tools
that cannot be shipped with its sources (e.g., the CAS Maple and Mathematica) the test coverage system has two modes.

1. **Full Coverage:** This mode covers all tests and sources regardless of any absence of required tools. You can
activate this mode by adding `-DjacocoReport=full` to Maven in the command line.
2. **Remote Coverage:** Covers only sources that run also in absence of third party tools (Maple and Mathematica).
You can activate this mode by adding `-DjacocoReport=remote` to Maven.

To trigger the test coverage, you have to add either full or remote coverage. Let's say on your machine everything
is setup correctly, use
```shell script
mvn test -DjacocoReport=full
```
The results can be found in `target/jacoco-report/`. Open the `index.html` to get a website view of the coverage report.

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
* `interpreter.maple -> ...interpreter.maple.MapleToSemanticInterpreter.java`: This class translates Maple expressions back to semantic LaTeX.

## Troubleshooting<a name="troubleshooting"></a>
When you want to contribute or just run our program it could happen to get some errors. Here are some tips to avoid that. When ever you found an error which is not explained here and you don't know how to fix it by your own, feel free to contact [André Greiner-Petter](https://github.com/AndreG-P).

1. **Translation of CSV file to lexicon files**: (typical exception: `MalformedInputException`)
This could happen when our program cannot find out the encoding of your CSV file. It is strongly recommended to set the encoding to UTF-8 (with or without BOM) of your CSV file.
2. **SIGSEGV**: (typical exception: `Process finished with exit code 139 (interrupted by signal 11: SIGSEGV)`) 
You forget to provide Maple enough heap space. Start java with the JVM option: `-Xss50M`
3. **Errors with Maple SO files**: (typical exception: `Process finished with exit code 134 (interrupted by signal 6: SIGABRT)` and `java.lang.UnsatisfiedLinkError: /opt/maple2019/bin.X86_64_LINUX/libjopenmaple.so: libimf.so: cannot open shared object file: No such file or directory`) 
There is a problem with the environment variables `Maple` and `LD_LIBRARY_PATH`.
4. **Cannot initiate translator or other programs**: (typical exception: `java.nio.file.NoSuchFileException: libs/ReferenceData/BasicConversions/GreekLettersAndConstants.json`)
You start the program within the wrong directory. The working directory has to be the root directory of the project (i.e., directory `LaCASt`). To change the default working directory
in IntelliJ, check the hint in step 8 (*Build and Run in IntelliJ*) under [Setup Project](#start).
5. **Mathematica License Expired**: Under unknown circumstances, it may appear that your free (and unlimited) license of
the wolfram engine for developers suddenly *expired*. This seems to be a bug in the licensing file. Under your home directory
you find the `~/.WolframEngine/Licensing/mathpass` file. This error occurs when this file contains multiple entities (multiple
lines). Backup the file first, then delete all lines with only one remaining (the line shall start with the name of your machine). 
This should solve the error. If the error remains, there might be another issue with your license. Try if you can start the
engine on the console (without LaCASt). If this works but LaCASt doesn't, contact [André Greiner-Petter](https://github.com/AndreG-P)
or open an ticket. If the console approach doesn't work either, contact Wolfram to check your license.

## Useful Command-Line Counting Methods

Calculate all results per file:
```shell script
find . -name "*symbolic*" | sort | xargs -n 1 gawk 'match($0, /.*SUCCESS_SYMB: ([0-9]+),.*TRANS: ([0-9]+),.*CASES: ([0-9]+),.*MISSING: ([0-9]+),.*/, arr) {success=arr[1]; cases=arr[3]; trans=arr[2]; transavg=arr[2]/arr[3]; succavg=arr[1]/arr[2]; miss=arr[4];}; END {print FILENAME"\t"cases"\t"trans"\t"transavg"\t"miss"\t"success"\t"succavg}'
find . -type f | sort | xargs -n 1 gawk 'match($0, /.*SUCCESS: ([0-9]+),.*FAILURE: ([0-9]+),.*TESTED: ([0-9]+),.*/, arr) {success=arr[1]; fail=arr[2]; tested=arr[3]; avg=arr[1]/arr[3]}; END {print FILENAME"\t"tested"\t"success"\t"avg"\t"fail}'
find . -name "*symbolic*" | sort | xargs -n 1 gawk 'match($0, /.*SUCCESS_SYMB: ([0-9]+),.*TRANS: ([0-9]+),.*CASES: ([0-9]+),.*MISSING: ([0-9]+),.*/, arr) {success=arr[1]; cases=arr[3]; trans=arr[2]; transavg=arr[2]*100/arr[3]; succavg=arr[1]/arr[2]; miss=arr[4];}; END {printf("%3d & (%.1f%)\n", trans, transavg)}'
```

Count total number of test cases:
```shell script
find . -name "*symbolic*" | xargs -n 1 gawk 'match($0, /.*CASES: ([0-9]+),.*/, arr) {sum = arr[1]}; END {print sum}' | paste -sd+ - | bc

```

Group and count all missing macros:
```shell script
awk -F, '{arr[$1] += $2;} END {for (a in arr) print arr[a]", "a}' *missing* | sort -n -r >> ../maple-missing.txt
```
