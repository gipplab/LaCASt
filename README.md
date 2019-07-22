# From LaTeX to CAS

# Rules
1. We are working with [Abdou's](https://github.com/abdouyoussef) PoM-Tagger, which is not public yet. It is strictly **_prohibited to upload or share_**  this JAR with other people (even with other contributers). More details or permissions can be granted by [Howard Cohl](https://github.com/HowardCohl) and [Abdou Youssef](https://github.com/abdouyoussef).
2. If you wish to contribute, you have to read the [contribution guidelines](CONTRIBUTING.md).

## Structure
1. [How to use our program](#howTo)
2. [Setup Round Trip Tests and Backward Translations](#roundtrip)
3. [Troubleshooting](#troubleshooting)
4. [The Team](#contributers)
5. [How to contribute](https://github.com/TU-Berlin/latex-grammar/edit/master/CONTRIBUTING.md)

## How to use our program<a name="howTo"></a>
You should find the `latex-grammar-<version_number>.zip` in the main directory of this repository.
Download this zip file and unzip it where ever you want.
There are two jars in this zip file.

* `latex-converter.jar`: This jar translates given formulae in semantic LaTeX into a given computer algebra system. It has the following optional flags to control the output
    * `-CAS=<NameOfCAS>`: Sets the computer algebra system you want to use. For instance `-CAS=Maple` uses Maple or `-CAS=Mathematica` uses Mathematica. If you don't set this flag, the program will ask you which CAS you want to use.
    * `-Expression="<exp>"`: Sets the expression you want to translate. Make sure you don't forget the quotation marks. If you don't specify an expression, the program will ask you about it.
    * `--clean` or `-c`: Only returns the translated expression without any other information. (since version 1.0.1)
    * `--debug` or `-d`: Returns extra information for debugging, such as computation time and list of elements. The `--clean` flag would override this effect.
    * `--extra` or `-x`: Shows further information about translation of functions. Like branch cuts, DLMF-links and so on. The `--clean` flag would override this effect.

* `lexicon-creator.jar`: This jar takes the CSV files in `libs/ReferenceData/CSVTables` and translate them to a lexicon file (the math language processors based on this lexicon files). You only have to add the CSV files for a CAS and not the `DLMFMacro.csv` file. This jar is only useful when you have any updates. For a detailed explanation how to add new translations or even support another computer algebra system take a look to the _[Update or add a new CAS to the translation process](https://github.com/TU-Berlin/latex-grammar/edit/master/CONTRIBUTING.md#howToUpdate)_ section in the contributing.md.

## Round Trip Test Setup<a name="roundtrip"></a>

The round trip tests are written with JUnit 5. Please read the [contribution guidelines](CONTRIBUTING.md) before you proceed. Otherwise it might be difficult to follow the next steps.

For round trip tests you have to specify environment variables in order to allow the engine to call the OpenMaple API native methods. 
In [libs/maple_config.properties](libs/maple_config.properties) you have to specify the path to the binary files of your local Maple instance.
It might be necessary to set the following environment variables in addition to the previous settings
```
export LD_LIBRARY_PATH = "$LD_LIBRARY_PATH:<Maple-BinDir>
export MAPLE="<Maple-Directory>"
```
Where `<Maple-Directory>` points to the directory where you installed your Maple version, e.g., `/opt/maple2016`, and `<Maple-BinDir>` points to the binary folder of your installed Maple version, e.g., `/opt/maple2016/bin.X86_64_LINUX`.
You can ask Maple where those directories are by entering the following commands in Maple
```
kernelopts( bindir );   <- returns <Maple-BinDir>
kernelopts( mapledir ); <- returns <Maple-Directory>
```

## Troubleshooting<a name="troubleshooting"></a>
When you want to contribute or just run our program it could happen to get some errors. Here are some tips to avoid that. When every you found an error which is not explained here and you don't know how to fix it by your own, feel free to contact [André Greiner-Petter](https://github.com/AndreG-P) (or some of the [other contributers](#contributers)).

1. You cannot translate your CSV file to our lexicon files. (typical exception like: MalformedInputException)
This could happen when our program cannot find out the encoding of your CSV file. It is strongly recommended to set the encoding to UTF-8 (with or without BOM) of your CSV file.

## Current contributers and their roles<a name="contributers"></a>

[Howard Cohl](https://github.com/HowardCohl): Supervisor

[Abdou Youssef](https://github.com/abdouyoussef) & [Moritz Shubotz](https://github.com/physikerwelt): Advisor

[André Greiner-Petter](https://github.com/AndreG-P): Main Developer

[Avi Trost](https://github.com/avitrost) & [Rajen Dey](https://github.com/Nejiv): Student Developers

## How to contribute?
Take a look to the [contributing guidelines](https://github.com/TU-Berlin/latex-grammar/edit/master/CONTRIBUTING.md)

## Other contributors
[Claude](https://github.com/ClaudeZou) & [Jagan](https://github.com/notjagan): Student Developers

