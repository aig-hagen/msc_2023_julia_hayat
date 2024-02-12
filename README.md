# bcCompiler

**bcCompiler** is an application for compilation-based belief change, that supports Dalal's and Satoh's revision operators and their
belief contraction counterparts (Harper Identity). **bcCompiler** has three main operating modes. The **compilation** mode
takes as input a belief change instance and outputs a SAT, ASP or ILP encoding, that is query-equivalent to the resulting
belief base. The **inference** and **model** check modes take as input a previously generated encoding and either a formula
consisting of variables of the original belief change instance (inference check) or an interpretation over the variables of
the original belief change instance (model check). The output then indicates, whether the new belief base represented by the
encoding entails the provided formula (inference check) or holds for the provided interpretation (model check).

## System requirements

**bcCompiler** was tested on Ubuntu 22.04 LTS and requires the following installations:
* Java 17 (https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
* MaxHS solver (e.g. https://github.com/fbacchus/MaxHS/releases/tag/2021_eval)
* CaDiCal solver (e.g. https://github.com/arminbiere/cadical/releases/tag/rel-1.7.3)
* Clingo solver (e.g. https://github.com/potassco/clingo/releases/tag/v5.6.2)
* Glpsol solver (e.g. http://ftp.gnu.org/gnu/glpk/glpk-5.0.tar.gz)

## Compilation mode
### Instance file format
The file containing the belief change instance must follow the below format, which is based on the official DIMACS format:
* Lines starting with 'c' are comment lines and are ignored.
* The first non-comment line has to be of the following form:
    ```
    p cnf <number of variables> <number of clauses>
    ```
  NOTE: ```<number of clauses>``` is the sum of belief base and change formula clauses.
* This line is followed by the belief base cnf clauses in DIMACS format. Multiline clauses are not supported.
* To separate belief base clauses from change formula clauses the below separator line is used:
    ```
    n ---
    ```
* The separator line is followed by the change formula cnf clauses in DIMACS format.

For examples of valid instance files, refer to [examples](https://github.com/julsched/belief-change-solver/tree/main/examples).

### Supported encoding types
**bcCompiler** supports generation of the following types of encodings:
* SAT (Satisfiability; solver: CaDiCal)
* ASP (Answer Set Programming; solver: clingo)
* ILP (Integer Linear Programming; solver: glpsol)

Alternatively, a naive encoding type can be selected, which uses the CaDiCal solver and returns the list of models of the
new belief base.

### Implemented pre-compilation algorithms
The **bcCompiler** implements three distinct pre-compilation algorithms to determine
Dalal's minimum distance and Satoh's minimal sets. The algorithms differ solely in the
technology used:
* Partial MaxSAT (Solver: MaxHS)
* ASP (Solver: Clingo)
* ILP (Solver: Glpsol)

NOTE: when choosing the 'naive' encoding type, it is not possible to specify a
pre-compilation algorithm

### Command
To execute **bcCompiler** in compilation mode run a command of the following form:
```
java -jar bcCompiler.jar -C <OPTIONS>
```
#### Options
* ```-f/--file <instance_file_path>```: Path to belief change instance file (REQUIRED)
* ```-o/--operation [contraction|revision]```: Operation (OPTIONAL, default 'revision')
* ```-d/--distance [dalal|satoh]```: Distance metric (OPTIONAL, default 'dalal')
* ```-t/--type [asp|ilp|sat|naive]```: Encoding type (OPTIONAL, default 'sat')
* ```-a/--algorithm [asp|ilp|maxsat]```: Pre-compilation algorithm (OPTIONAL, default 'maxsat')
* ```-s/--skip-validation```: Skip validation of belief change instance


#### Example: Generate SAT encoding of Dalal's contraction, using the MaxSAT pre-compilation algorithm
```
java -jar bcCompiler.jar -C -o contraction -d dalal -t sat \
-f examples/contraction/instance-1.cnf
```

#### Example: Generate ASP encoding of Satoh's revision, using the ASP pre-compilation algorithm
```
java -jar bcCompiler.jar -C -o revision -d satoh -t asp -a asp \
-f examples/revision/instance-1.cnf
```

### Input validation
The **bcCompiler**'s input validation in the compilation mode consists of
* a check for belief base satisfiability
* a check for change formula satisfiability
* a check for whether the change formula is a tautology
* a check for whether the change formula is believed in the belief base

The compilation phase is automatically aborted if one of the following conditions holds:

* The belief base formula is unsatisfiable.
* The change formula is unsatisfiable.
* The change formula is a tautology.
* The change formula is already believed in the belief base and the desired belief change operation is revision OR the change
formula is not believed in the belief base and the desired belief change operation is contraction.

If the correctness of a belief change instance is guaranteed, **bcCompiler**'s input validation step can be skipped by passing
the flag ```--skip-validation``` (```-s```). In this case **bcCompiler** restricts its input validation phase to a short check
for file format correctness.

NOTE: The flag ```--skip-validation``` (```-s```) should be used with great care since incorrect input (e.g. unsatisfiable belief base formula / change formula) might lead to incorrect results if the validation step is skipped.

## Inference and model check modes
### Instance file format
#### Inference check
The inference formula must consist of variables of the belief change instance from which the provided
encoding has been generated and must be in DIMACS format. Multiline clauses are not supported
and a parameter line at the top is not required. Lines starting with 'c' are considered comment lines.

#### Model check
The interpretation must contain all variables of the belief change instance from which the provided
encoding has been generated. Lines starting with 'c' are considered comment lines.


For examples of valid inference/model check instances, refer to [examples](https://github.com/julsched/belief-change-solver/tree/main/examples).

### Command
To execute an inference check, run a command of the following form:
```
java -jar bcCompiler.jar -I <OPTIONS>
```

To execute a model check, run a command of the following form:
```
java -jar bcCompiler.jar -M <OPTIONS>
```

#### Options
* ```-f/--file <instance_file_path>```: Path to inference/model instance file (REQUIRED)
* ```-e/--encoding <encoding_file_path>```: Path to encoding file (REQUIRED)
* ```-s/--skip-validation```: Skip validation of encoding and instance file

### Input validation
**bcCompiler**'s input validation in the inference and model check modes consists of
* a check for encoding satisfiability
* a check for inference formula satisfiability
* a check for whether the inference formula is a tautology

The inference/model check is automatically aborted if one of the following conditions holds:

* The encoding is unsatisfiable.
* The inference formula is unsatisfiable.
* The inference formula is a tautology.

Analogously to the compilation mode, **bcCompiler**'s input validation step can be skipped by
passing the flag ```--skip-validation``` (```-s```) if the correctness of an inference/model check
instance is guaranteed. In this case **bcCompiler** restricts its input validation phase to a short
check for file format correctness.

NOTE: The flag ```--skip-validation``` (```-s```) should be used with great care since incorrect input might lead to incorrect
results if the validation step is skipped.
