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
4. [Troubleshooting](#troubleshooting)

## What is our goal?<a name="goal"></a>
We want to create a software that translates generic LaTeX to a given computer algebra system (called CAS). To get a rough overview, take a look at our [usecase for the Jacobi polynomial](https://github.com/TU-Berlin/latex-grammar/blob/master/LaTeX2CAS.pdf).

## What should I know before I start?<a name="start"></a>
### Math Language Processor (MLP)
Abdou has uploaded a MLP.zip file. You can find it in [`libs/MLP/`](https://github.com/TU-Berlin/latex-grammar/tree/master/libs/MLP). The most important file is the MLP.jar in the same directory. Make sure your idea has included this jar. Abdou has also written a [_"Software usage guide"_](https://github.com/TU-Berlin/latex-grammar/tree/master/libs/MLP) and created a documentation for the MLP.jar [starts with the index.html](https://github.com/TU-Berlin/latex-grammar/tree/master/libs/MLP/javadoc). If you have any questions about the MLP, ask [Abdou Youssef](https://github.com/abdouyoussef) or [André Greiner-Petter](https://github.com/AndreG-P).

### Orginzing tasks via issues
For precise tasks, take a look to the [issues](https://github.com/TU-Berlin/latex-grammar/issues) and [projects](https://github.com/TU-Berlin/latex-grammar/projects). When ever you working on our project, please create issues and organize them in the projects overview. That makes it much easier for everybody to see what is going on.

## The program structure and important main classes<a name="program"></a>
TODO

## Troubleshooting<a name="troubleshooting"></a>
When you want to contribute or just run our program it could happen to get some errors. Here are some tips to avoid that. When every you found an error which is not explained here and you don't know how to fix it by your own, feel free to contact [André Greiner-Petter](https://github.com/AndreG-P) (or some of the [other contributers](#contributers)).

1. You cannot translate your CSV file to our lexicon files. (typical exception like: MalformedInputException)
This could happen when our program cannot find out the encoding of your CSV file. It is strongly recommended to set the encoding to UTF-8 (with or without BOM) of your CSV file.
