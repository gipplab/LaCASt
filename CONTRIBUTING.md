# Contributing to LaTeX-Grammar

1. It is strictly prohibited to share any files from this repository without permission!
2. Do not push to the master branch of the project. Please checkout a different branch and create a pull request!

## Structure
1. [Setup Project](#start)
2. [Generate Jars](#jars)
3. [Test Coverage](#test-coverage)
4. [Update translations](#howToUpdate)
5. [Support a new CAS](#newCAS)
6. [The program structure and important main classes](#program)
7. [Troubleshooting](#troubleshooting)
8. [Update vmext-demo endpoints](#deployDkeContainer)
9. [Useful Commands when working with LaCASt](#commands)

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
  
* Run <code>git clone git@github.com:ag-gipp/LaCASt.git --recurse-submodules</code>
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

* Change the properties in `lacast.config.yaml`
```yaml
Maple:
    install.path: "/opt/maple2020"
    native.library.path: "/opt/maple2020/bin.X86_64_LINUX"
Mathematica:
    install.path: "/opt/Wolfram"
    native.library.path: "/opt/Wolfram/SystemFiles/Links/JLink/SystemFiles/Libraries/Linux-x86-64"
    license: "4311-6677-4H3V66"
```

* If you work on Windows, you may have to change the environment variables in the `pom.xml` because there is no
`LD_LIBRARY_PATH` and `MAPLE` necessary on windows. In case of Apple OS, you need something else again. See 
[here](https://de.maplesoft.com/support/help/maple/view.aspx?path=OpenMaple%2fJava%2frunning)
for more information. If you have trouble, open an issue.
```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <environmentVariables>
            <PATH>${lacast.cas.Maple.native.library.path}</PATH>
        </environmentVariables>
        <!--suppress UnresolvedMavenProperty -->
        <argLine>${argLine} -Xss50M</argLine>
    </configuration>
</plugin>
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
    * Add `MAPLE=/opt/maple2019` and `LD_LIBRARY_PATH=/opt/maple2019/bin.X86_64_LINUX` (the variables and paths might be different on your system, so update it accordingly and check [this guide](https://de.maplesoft.com/support/help/maple/view.aspx?path=OpenMaple%2fJava%2frunning)))
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

## Update translation patterns<a name="howToUpdate"></a>
Basic translation patterns can be found in `libs/ReferenceData/BasicConversions`. Here, basic functions, symbols, and 
constants are defined in JSON files.
We store the general translation patterns in `libs/ReferenceData/CSVTables`. In there, you will find two groups of files.
1. The supported macros (in `DLMFMacro.csv`).
2. The supported translations to a CAS. There are two files for each CAS. One defines the meta information (e.g., hyperlinks), 
and the other defines the translations (e.g., for Maple its `CAS_Maple.csv` and `DLMF_Maple.csv`).

There are two general things you should know before you start.
1. Every time you change something in the CSV files, you have to run `bin/lexicon-creator.jar` to update the actual lexicon files. See below how to [add new translations to the internal lexicons](#lexiconAddOn).
2. Optional parameters, such as in `\LaguerrepolyL[\alpha]{n}@{x}`, needs special treatment. In all CSV files, it requires multiple lines.
The first line contains the information for the macro without optional parameters; the second with one optional parameter, the third with two
optional parameters, and so on. The `DLMF` entry for optional parameters should be `X<NumOfOptParas>:<macro-name>X<macro>`. For example: 

| DLMF |
| :--- |
| `\macro{n}@{x}` |
| `X1:\macroX\macro[a]{n}@{x}` |
| `X2:\macroX\macro[a][b]{n}@{x}` |
| `X3:\macroX\macro[a][b][c]{n}@{x}` |

***Important:*** The number of parameters does not include the number of optional parameters.

<details><summary><strong>How to support new macros</strong></summary>
  
To support new macros, you have to extend the `DLMFMacro.csv` with all information. The columns `branch-cuts` and `constraints` are optional.
For `role` you should enter either `dlmf-macro`, `mathematical constant`, `symbol`, or `ignore`. Obviously, `ignore` means it will be ignored due to compilation.
</details>

<details><summary><strong>How to support new translations</strong></summary>

Consider you want to add a new translation pattern to Maple, you have to add it to `DLMF_Maple.csv`.
The columns for alternative translations and required packages allow multiple entries.
In this case you have to split the entries with ` || ` (including the spaces). For example, see the line 22 for
`\acot@@{$0}`. It contains two alternative translation patterns: `arctan(1/($0)) || I/2*ln(($0-I)/($0+I) )`.

If you want to add required packages, it makes sense to only require the actual function.
For example, `\Eulertotientphi@{n}` requires in Maple the package `NumberTheory` to allow the translation to `phi(n)`.
However, for performance reasons, it makes sense to only require the actual function, here `NumberTheory,phi`.
</details>

## Support a new CAS<a name="newCAS"></a>
Supporting an entire new CAS can cause a lot of trouble. Hence, it is recommended you are familiar with the project
to some degree before you start to support another CAS. In the following, I will describe the general steps that were
necessary to basically support SymPy. Basically, because you can always dig deeper and deeper which requires more
advanced updates all over the place. For example, in the following we will not go into the trouble to support SymPy
for the evaluation engine as well. The evaluation engine requires a direct interaction between LaCASt and the CAS.
In contrast, a simple forward translation does not require the CAS to run.

***Important:*** Find a unique notation for the name of your CAS that does not use special characters. Make sure
you will use this exact notation (case-sensitive) all over the place! If you change the notation in a single location,
LaCASt will not be able to set up and use everything properly. For SymPy, I decided to use `SymPy` rather than `Sympy`.
Hence, I use the capital `P` everywhere. If you want to allow the user to use another notation for the frontend, you
have to extend the code to allow aliases (currently there is no alias logic implemented).

<details><summary><strong>1. Basic Conversions</strong></summary>

The very first thing to do is to update the basic conversion definitions in `libs/ReferenceData/BasicConversions`.
In both JSON files are numerous of standard translations that are absolutely required to run a translation process.
If the new CAS does not support some translations (e.g., there is no `\pm` symbol in SymPy), just ignore this case.
Do **not** define an empty translation in this case! The translator would not through an error and simply
translate and empty string. In such a scenario, it would be very difficult to find errors in LaCASt.

Once you updated as much as possible, you are free to go. LaCASt already supports your new CAS now. However, of course,
it only supports the basic translations you defined in the JSON files. Test it by run `SemanticToCASInterpreter` with
your new CAS and translate `\frac{x^2}{y\cpi\idot\iunit}`.
</details>

<details><summary><strong>2. Add macro translations</strong></summary>

The very first step to support macro translations for the new CAS is to add your CAS to `lacast.config.yaml`. 
As you can see, you can define paths for the cas installation but this is currently optional. It might change in the future
though.

```
lacast.cas:
  - cas: "Maple"
    paths:
      - "/opt/maple2019"
      - "/opt/maple2019/bin.X86_64_LINUX"
  - cas: "Mathematica"
    paths: "/opt/Wolfram"
  - cas: "SymPy"
```

Next, you have to add your CSV files for your new CAS in `libs/ReferenceData/CSVTables`. In case of SymPy:
1. `CAS_SymPy.csv` which defines the functions of SymPy with all necessary information. Note that it has a DLMF column
which can be used to perform backward translations in the future (`SymPy -> LaTeX`).
2. `DLMF_SymPy.csv` which defines the forward translations for all functions defined in `CAS_SymPy.csv`.

For a detailed explanation how to fill out both CSV files, take a look at 
[Section 4, update translation patterns](#howToUpdate).

</details>

<details><summary><strong>3. Support new translations</strong></summary>

If you followed the previous steps correctly, you are ready to bring your new defined CSV tranlsations to the internally
used lexicon files. That's rather simple to do, just start the `CSVtoLexiconConverter` (see below in [Section 5](#program) 
if you don't know where to find it). It asks you enter the CAS you want to support. Just enter `-all` and you should see

```
Current list: [CAS_Maple.csv, CAS_Mathematica.csv, CAS_SymPy.csv]
```

If you entered your CAS in the `lacast.config.yaml` list, you shall see your CAS in the list as well. Enter `-end` to
kick off the integration process. Once it's done, you should see a summary. I just added translations for the
elementary functions to SymPy, hence my summary looks like this:

```
Time elapsed:  0,441 seconds
Number of DLMF-Macros: 675
Number of supported Maple translations: 260
Number of supported Mathematica translations: 269
Number of supported SymPy translations: 29
```
</details>

<details><summary><strong>4. Test your translations</strong></summary>

You should be able to run translations for your new CAS already by now. As long as your test expression does not include
functions you did not define yet, it should work smoothly. Of course, you should implement test cases to be sure.

The test cases are all defined in a separated files which makes it very easy to extend them for a new CAS. However,
be aware that all of these test cases cannot cover every possible problem. Especially when you need to implement
new code (maybe because of your CAS handles certain actions entirely different to the other supported CAS), you have
to test your new code extensively! This cannot be covered by the outsourced test files. But, if you come that far and
implemented your own code, you won't have trouble to implement your own test cases.

The *golden dataset* of test cases is defined in two locations:
1. `interpreter.common/src/test/resources/` contains two CSV files that are used to test your constants and greek
letters
2. `interpreter.lacast/src/test/resources/translations` contains five JSON files that define the tested translations.
Every test case contains the following information:

```json
{
    "name": "name of test case (should be unique)",
    "DLMF": "the DLMF label if that exists (e.g., 14.3.2)",
    "LaTeX": "the semantic LaTeX expression to test",
    "CAS_ID": "translation to the CAS_ID"
}
```

Every test case can contain multiple CAS. For example:

```json
{
    "name": "SEMANTIC",
    "DLMF": "",
    "LaTeX": "\\Sum{k}{1}{n}@{\\frac{1}{y^{k}}}",
    "Maple": "sum((1)/((y)^(k)), k = 1..n)",
    "Mathematica": "Sum[Divide[1, (y)^(k)], {k, 1, n}]"
}
```

As you can see, in this case, there is no translation defined for SymPy, even though we support SymPy translations.
This does not harm the test suites, since this case will simply be ignored when the SymPy translator is tested.
Once you add a SymPy translation, it will be covered automatically.

***Important:*** A CAS in these files will only be tested if it is defined in the early mentioned `lacast.config.yaml` 
under `lacast.cas`. If you enter test cases for a CAS that is not defined in this config file, the tests will not be 
triggered!
</details>

## The program structure and important main classes<a name="program"></a>
There are a couple of main classes in the project.
* `interpreter.common -> ...interpreter.examples.MLP.java`: This is an example of Abdou's math language processors. It runs a hard-coded example of the analyzing process.
* `interpreter.lacast -> ...interpreter.cas.SemanticToCASInterpreter.java`: That's the main class to translate formulae to a computer algebra system.
* `interpreter.common -> ...interpreter.mlp.CSVtoLexiconConverter.java`: This class translates given CSV files to lexicon files. You only have to add the translation CSV files and not the DLMFMacro.csv itself to create a correct lexicon.
* `interpreter.maple  -> ...interpreter.maple.MapleToSemanticInterpreter.java`: This class translates Maple expressions back to semantic LaTeX.

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
lines). See Wolfram's [mathpass support page](https://support.wolfram.com/112) for more information. 
Backup the file first, then delete all lines with only one remaining (the line shall start with the name of your machine). 
This should solve the error. If the error remains, there might be another issue with your license. Try if you can start the
engine on the console (without LaCASt). If this works but LaCASt doesn't, contact [André Greiner-Petter](https://github.com/AndreG-P)
or open an ticket. If the console approach doesn't work either, contact Wolfram to check your license.
6. **Unable to Activate Mathematica License**: If you had a free license for Wolfram's Engine for Developers and change your machine you need to activate the license again. This sometimes doesn't work for some reasons. A workaround is to do it manually. First, try to get your activation keys from [here](https://www.wolframcloud.com/users/user-current/activationkeys). If this doesn't show your keys, you may not have any activation keys yet. Simply follow the instructions when searching for free Wolfram engine. Once you have an activation key and it doesn't work the normal way, try to activate one of them with `WolframKernel -activate 1234-5678-ABCDEF`. If this doesn't work either, try to follow this [stackexchange discussion](https://mathematica.stackexchange.com/questions/198822/the-wolfram-kernel-must-be-activated-for-wolframscript-to-use-it) to see if it helps. Finally, if it still won't let you activate your free license, you must contact the Wolfram Technical Support. In my case, I had already two activation keys and that seems to be the max number (see stackexchange discussion above). Hence, I was unable to activate the license on another machine and needed to contact the support either via `support@wolfram.com` or the [support website](https://www.wolfram.com/support/contact/). Once they reset the activation keys for me, it worked again.

## Update vmext-demo endpoints on DKE01<a name="deployDkeContainer"></a>

This is what you need to update the vmext-demo endpoints that provide access to LaCASt. Note, that this requires
admin access to DKE01. Obviously this is more of a reminder for Andre/Moritz instead of a guide for any developer.

#### Update DLMF Macros ES Database

This is rather easy because we can simply re-index the entire DB (it does not take much time).

1. Make sure you stop your local ES instance
2. Connect to DKE with Port forwarding
```
LocalForward 9200 192.168.112.3:9200
```
3. Now you can use the exact DKE instance of ES to update the macros. To test this, use `interpreter.generic/http/dlmf-macro.http`
commands and see if they work for `localhost:9200`.
4. Now simply run the main `gov.nist.drmf.interpreter.generic.elasticsearch.DLMFElasticSearchClient` will delete the existing
index of macros and reindex it with the new code.

#### Updating vmext-demo Jar only

1. Build `mathpipeline.jar` locally
2. SCP it to DKE01
3. Login to DKE
4. Push the new jar into the running container
```console
agp@dke01:~$ docker cp new-mathpipeline.jar docker_vmext-demo_1:/mathpipeline.jar
```console
5. Save the new container state in a new commit
```
agp@dke01:~$ docker commit docker_vmext-demo_1 vmext-demo:lacast
```
6. Restart the container
```console
agp@dke01:~$ cp ../git/srv-dke01/docker
agp@dke01:/home/git/srv-dke01/docker$ docker-compose restart vmext-demo
```

#### Deploy Actual Container

Usually, you do not want to do this. If you just have a new `mathpipeline.jar`, please use the previous guide updating 
the entrypoint of the existing container. If there is no such container on DKE yet, you indeed need to create a new one
and bring the image to DKE. Only in this case, follow the next steps

1. Save and test your build locally
```console
agp@lab:~$ docker cp target/mathpipeline.jar vmext-demo:/mathpipeline.jar
agp@lab:~$ docker-compose restart vmext-demo
```
2. If its working as expected, save and store the image and move it to DKE01
```console
agp@lab:~$ docker commit vmext-demo vmext-demo:lacast
agp@lab:~$ docker save vmext-demo:lacast > vmext-demo-lacast.tar
agp@lab:~$ scp vmext-demo-lacast.tar andreg-p@dke01:~/vmext-demo-lacast.tar
```
3. Save the new image as a release on LaCASt (you need to split the release into multiple zips because the maximum file size is 2GB for releases).
4. Deploy the new image on DKE01
```
agp@lab:~$ ssh dke01
andreg-p@dke01:~$ docker load -i vmext-demo-lacast.tar
andreg-p@dke01:~$ cd ../git/srv-dke01/docker
andreg-p@dke01:~$ docker-compose up -d --remove-orphans --force-recreate vmext-demo >> ../../srv-dke01_last.log
```
5. Wait a few seconds and check https://vmext-demo.formulasearchengine.com/swagger-ui.html

## Useful Command-Line Tweaks<a name="commands"></a>

Calculate all results per file:
```shell script
find . -name "*symbolic*" | sort | xargs -n 1 gawk 'match($0, /.*SUCCESS_SYMB: ([0-9]+),.*TRANS: ([0-9]+),.*CASES: ([0-9]+),.*MISSING: ([0-9]+),.*/, arr) {success=arr[1]; cases=arr[3]; trans=arr[2]; transavg=arr[2]/arr[3]; succavg=arr[1]/arr[2]; miss=arr[4];}; END {print FILENAME"\t"cases"\t"trans"\t"transavg"\t"miss"\t"success"\t"succavg}'
find . -type f | sort | xargs -n 1 gawk 'match($0, /.*SUCCESS: ([0-9]+),.*FAILURE: ([0-9]+),.*TESTED: ([0-9]+),.*/, arr) {success=arr[1]; fail=arr[2]; tested=arr[3]; avg=arr[1]/arr[3]}; END {print FILENAME"\t"tested"\t"success"\t"avg"\t"fail}'
find . -name "*symbolic*" | sort | xargs -n 1 gawk 'match($0, /.*SUCCESS_SYMB: ([0-9]+),.*TRANS: ([0-9]+),.*CASES: ([0-9]+),.*MISSING: ([0-9]+),.*/, arr) {success=arr[1]; cases=arr[3]; trans=arr[2]; transavg=arr[2]*100/arr[3]; succavg=arr[1]/arr[2]; miss=arr[4];}; END {printf("%3d & (%.1f%)\n", trans, transavg)}'

find . -name "*symbolic*" | sort | xargs -n 1 gawk 'match($0, /.*STARTED_TEST_CASES: ([0-9]+),.*MISSING: ([0-9]+),.*SUCCESS_TRANS: ([0-9]+),.*SUCCESS_SYMB: ([0-9]+),.*SUCCESS_UNDER_EXTRA_CONDITION: ([0-9]+),.*/, arr) {success=arr[4]+arr[5]; cases=arr[1]; trans=arr[3]; miss=arr[2]; transavg=trans/cases; succavg=success/trans;}; END {print FILENAME"\t"cases"\t"trans"\t"transavg"\t"miss"\t"success"\t"succavg}'
```

Count total number of test cases:
```shell script
find . -name "*symbolic*" | xargs -n 1 gawk 'match($0, /.*STARTED_TEST_CASES: ([0-9]+),.*/, arr) {sum = arr[1]}; END {print sum}' | paste -sd+ - | bc
```

Group and count all missing macros:
```shell script
awk -F, '{arr[$1] += $2;} END {for (a in arr) print arr[a]", "a}' *missing* | sort -n -r >> ../maple-missing.txt
```

Hanging on Maple? Kill it:
```shell script
ps aux | grep MapleRmiServer | grep -v grep | awk '{print $2}' | xargs kill
```