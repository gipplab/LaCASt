<a href="https://go.java/index.html"><img align="right" src="https://forthebadge.com/images/badges/made-with-java.svg" alt="Made With Java" height="20"></a><a href="https://www.latex-project.org/"><img align="right" src="https://img.shields.io/badge/Made%20with-LaTeX-1f425f.svg" alt="Made With LaTeX" height="20"></a> 
[![Build Status](https://travis-ci.com/ag-gipp/LaCASt.svg?token=3obgod4qv4wGCx8sihym&branch=master)](https://travis-ci.com/ag-gipp/LaCASt) [![Tests](https://github.com/ag-gipp/latex-grammar/workflows/translator-build-tests/badge.svg)](https://github.com/ag-gipp/latex-grammar/actions) [![Maintainability](https://api.codeclimate.com/v1/badges/3960df830b098ef0afa9/maintainability)](https://codeclimate.com/repos/5df6328a606a9501a1001189/maintainability) [![Test Coverage](https://api.codeclimate.com/v1/badges/3960df830b098ef0afa9/test_coverage)](https://codeclimate.com/repos/5df6328a606a9501a1001189/test_coverage)

# LaCASt - A LaTeX Translator for Computer Algebra Systems

1. This is a private repository with non-public sources. It is strictly **_prohibited to share_** any files without permission.
2. If you wish to contribute, read the [contribution guidelines](CONTRIBUTING.md) first.

## Publications
<details>
  <summary>A. Greiner-Petter, H. S. Cohl, A. Youssef, M. Schubotz, A. Trost, R. Dey, A. Aizawa, B. Gipp (2020) "Comparative Verification & Validation of Digital Mathematical Libraries and Computer Algebra Systems". In Submission</summary>
  
```
Currently in submission, no bibtex available yet.
```
</details>

<details>
  <summary>A. Greiner-Petter, M. Schubotz, H. S. Cohl, B. Gipp (2019) "Semantic preserving bijective mappings for expressions involving special functions between computer algebra systems and document preparation systems". In: Aslib Journal of Information Management. 71(3): 415-439</summary>
  
```bibtex
@Article{Greiner-Petter19,
  author    = {Andr{\'{e}} Greiner{-}Petter and
               Moritz Schubotz and
               Howard S. Cohl and
               Bela Gipp},
  title     = {Semantic preserving bijective mappings for expressions involving special
               functions between computer algebra systems and document preparation
               systems},
  journal   = {Aslib Journal of Information Management},
  volume    = {71},
  number    = {3},
  pages     = {415--439},
  year      = {2019},
  url       = {https://doi.org/10.1108/AJIM-08-2018-0185},
  doi       = {10.1108/AJIM-08-2018-0185}
}
```
</details>

<details>
  <summary>H. S. Cohl, A. Greiner-Petter, M. Schubotz (2018) "Automated Symbolic and Numerical Testing of DLMF Formulae Using Computer Algebra Systems". In: CICM: 39-52</summary>
  
```bibtex
@InProceedings{Cohl18,
  author    = {Howard S. Cohl and
               Andr{\'{e}} Greiner{-}Petter and
               Moritz Schubotz},
  title     = {Automated Symbolic and Numerical Testing of {DLMF} Formulae Using
               Computer Algebra Systems},
  booktitle = {Intelligent Computer Mathematics - 11th International Conference,
               {CICM} 2018, Hagenberg, Austria, August 13-17, 2018, Proceedings},
  series    = {Lecture Notes in Computer Science},
  volume    = {11006},
  pages     = {39--52},
  publisher = {Springer},
  year      = {2018},
  url       = {https://doi.org/10.1007/978-3-319-96812-4\_4},
  doi       = {10.1007/978-3-319-96812-4\_4}
}
```
</details>

<details>
  <summary>H. S. Cohl, M. Schubotz, A. Youssef, A. Greiner-Petter, J. Gerhard, B. V. Saunders, M. A. McClain, J. Bang, K. Chen (2017) <it>"Semantic Preserving Bijective Mappings of Mathematical Formulae Between Document Preparation Systems and Computer Algebra Systems"</it>. In: CICM: 115-131</summary>
  
```bibtex
@InProceedings{Cohl17,
  author    = {Howard S. Cohl and
               Moritz Schubotz and
               Abdou Youssef and
               Andr{\'{e}} Greiner{-}Petter and
               J{\"{u}}rgen Gerhard and
               Bonita V. Saunders and
               Marjorie A. McClain and
               Joon Bang and
               Kevin Chen},
  title     = {Semantic Preserving Bijective Mappings of Mathematical Formulae Between
               Document Preparation Systems and Computer Algebra Systems},
  booktitle = {Intelligent Computer Mathematics - 10th International Conference,
               {CICM} 2017, Edinburgh, UK, July 17-21, 2017, Proceedings},
  series    = {Lecture Notes in Computer Science},
  volume    = {10383},
  pages     = {115--131},
  publisher = {Springer},
  year      = {2017},
  url       = {https://doi.org/10.1007/978-3-319-62075-6\_9},
  doi       = {10.1007/978-3-319-62075-6\_9}
}
```
</details>

# How to use our program<a name="howTo"></a>
The bin directory contains a couple of executable jars. Any of these programs require the `lacast.config.yaml`.
This config file must specify the paths to the `libs` and `config` folder, see [lacast.config.yaml](lacast.config.yaml).
LaCASt tries to load the config automatically following these approaches:
1. There a system environment that specifies the file location: `export LACAST_CONFIG="path/to/lacast.config.yaml"`.
2. The config file is in the current directory
3. Loads the default config from the resources in the jar.

<details><summary><code>latex-to-cas-converter.jar</code>: The forward translator (LaTeX -> CAS)</summary>

---
The executable jar for the translator can be found in the `bin` subdirectory. A standalone version can be found in the `bin/*.zip` file. Unzip the archive where you want and run the jar from the root folder of the respository

```shell script
java -jar bin/latex-to-cas-converter.jar
```

Without additional information, the jar runs as an interactive program. You can start the program to directly trigger
the translation process or set further flags (every flag is optional):
* `-CAS=<NameOfCAS>`: Sets the computer algebra system you want to translate to, e.g., `-CAS=Maple` for Maple;
* `-Expression="<exp>"`: Sets the expression you want to translate. Double qutation marks are mandatory;
* `--clean` or `-c`: Only returns the translated expression without any other information. (since v1.0.1)
* `--debug` or `-d`: Returns extra information for debugging, such as computation time and list of elements. (`--clean` overrides this setting).
* `--extra` or `-x`: Shows further information about translation of functions, e.g., branch cuts, DLMF-links and more. (`--clean` flag overrides this setting)

---
</details>

<details><summary><code>lexicon-creator.jar</code>: Maintain the translation dictionary</summary>

---
Is used to maintain the internal translation dictionaries. Once the translation pattern is defined in the CSV files it must be trasformed to the dictionaries. The typical workflow is:

```shell script
andre@agp:~$ java -jar bin/lexicon-creator.jar 
Welcome, this converter translates given CSV files to lexicon files.
You didn't specified CSV files (do not add DLMFMacro.csv).
Add a new CSV file and hit enter or enter '-end' to stop the adding process.
all
Current list: [CAS_Maple.csv, CAS_Mathematica.csv]
-end
```

---
</details>


<details><summary><code>maple-translator.jar</code>: The backward translator for Maple (Maple -> Semantic LaTeX)</summary>

---
This jar requires an installed Maple license on the machine! To start the translator, you have to set the environment variables to properly run Maple. In my case, Maple is installed in `/opt/maple2019` and I'm on a Linux machine. In addition, you have to provide more heap size, otherwise Maple crashes. Here is an example:

```shell script
andre@agp:~$ export MAPLE="/opt/maple2019"
andre@agp:~$ export LD_LIBRARY_PATH="/opt/maple2019/bin.X86_64_LINUX"
andre@agp:~$ java -Xss50M -jar bin/maple-translator.jar 
```

To get the Maple paths, you can start maple and enter the following commands:

```
kernelopts( bindir );   <- returns <Maple-BinDir>
kernelopts( mapledir ); <- returns <Maple-Directory>
```

---
</details>

<details><summary><code>symbolic-tester.jar</code>: Symbolic verification program</summary>

---
This is only for advanced users! First, setup the properties:

1) `config/symbolic_tests.properties`
Critical and required settings are:

```properties
# the path to the dataset
dlmf_dataset=/home/andreg-p/Howard/together.txt

# the lines that should be tested in the provided dataset
subset_tests=7209,7483

# the output path
output=/home/andreg-p/Howard/Results/AutoMaple/22-JA-symbolic.txt

# the output path for missing macros
missing_macro_output=/home/andreg-p/Howard/Results/AutoMaple/22-JA-missing.txt
```

2) `symbolic-tester.jar` program arguments:
    * `-maple` to run the tests with Maple
    * `-mathematica` to run the tests with Mathematica
    * `-Xmx8g` increase the java memory, that's not required but useful
    * `-Xss50M` increase the heap size if you use Maple

3) Since you may want to run automatically evaluations on subsets, you can use the `scripts/symbolic-evaluator.sh`. Of course you need to update the paths in the script. With `config/together-lines.txt` you can control what subsets the script shall evaluate, e.g.,

```
04-EF: 1465,1994
05-GA: 1994,2179
```

To test the lines `1465-1994` and `1994-2179` and store the results in `04-EF-symbolic.txgt` and `05-GA-symbolic.txt` file.

---
</details>

<details><summary><code>numeric-tester.jar</code>: Numeric verification program</summary>

---
This is only for advanced users! First, setup the properties:

1) `config/numerical_tests.properties`
Critical and required settings are:

```properties
# the path to the dataset
dlmf_dataset=/home/andreg-p/Howard/together.txt

# either you define a subset of lines to test or you define the results file of symbolic evaluation, which is recommended
# subset_tests=7209,7483
symbolic_results_data=/home/andreg-p/Howard/Results/AutoMath/11-ST-symbolic.txt

# the output path
output=/home/andreg-p/Howard/Results/MathNumeric/11-ST-numeric.txt
```

2) `numeric-tester.jar` program arguments:
    * `-maple` to run the tests with Maple
    * `-mathematica` to run the tests with Mathematica
    * `-Xmx8g` increase the java memory, that's not required but useful
    * `-Xss50M` increase the heap size if you use Maple

3) Since you may want to run automatically evaluations on subsets, you can use the `scripts/numeric-evaluator.sh`. Of course you need to update the paths in the script. With `config/together-lines.txt` you can control what subsets the script shall evaluate, e.g.,

```
04-EF: 1465,1994
05-GA: 1994,2179
```

This will automatically load the symbolic result files `04-EF-symbolic.txg` and `05-GA-symbolic.txt` and start the evaluation.

---
</details>

### Update Translation Patterns
The translation patterns are defined in `libs/ReferenceData/CSVTables`. If you wish to add translation patterns you need to
compile the changes before the translator can use them. To update the translations, use the `lexicon-creator.jar` (see the explanations above).

### Update Pre-Processing Replacement Rules
The pre-processing replacement rules are defined in `config/replacements.yml` and `config/dlmf-replacements.yml`. Each config
contains further explanations how to add replacement rules. The replacement rules are applied without further compilation.
Just change the files to add, modify, or remove rules.

## Contributors<a name="contributers"></a>

| Role | Name | Contact |
| :---: | :---: | :---: |
| **Main Developer** | [André Greiner-Petter](https://github.com/AndreG-P) | [andre.greiner-petter@t-online.de](mailto:andre.greiner-petter@t-online.de) |
| **Supervisor** | [Dr. Howard Cohl](https://github.com/HowardCohl) | [howard.cohl@nist.gov](mailto:howard.cohl@nist.gov) |
| **Advisor** | [Dr. Moritz Schubotz](https://github.com/physikerwelt) | [schubotz@uni-wuppertal.de](mailto:schubotz@uni-wuppertal.de) |
| **Advisor** | [Prof. Abdou Youssef](https://github.com/abdouyoussef) | [abdou.youssef@nist.gov](mailto:abdou.youssef@nist.gov) |
| **Student Developers** | [Avi Trost](https://github.com/avitrost) & [Rajen Dey](https://github.com/Nejiv) & [Claude](https://github.com/ClaudeZou) & [Jagan](https://github.com/notjagan) | |
