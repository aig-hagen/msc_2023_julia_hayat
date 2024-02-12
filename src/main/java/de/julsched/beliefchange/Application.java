package de.julsched.beliefchange;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

import de.julsched.beliefchange.asp.AspInferenceCheck;
import de.julsched.beliefchange.asp.AspModelCheck;
import de.julsched.beliefchange.asp.CompilerAsp;
import de.julsched.beliefchange.exceptions.WrongInputException;
import de.julsched.beliefchange.ilp.CompilerIlp;
import de.julsched.beliefchange.ilp.IlpInferenceCheck;
import de.julsched.beliefchange.ilp.IlpModelCheck;
import de.julsched.beliefchange.instance.InferenceCheckInstance;
import de.julsched.beliefchange.instance.ModelCheckInstance;
import de.julsched.beliefchange.naive.NaiveInferenceCheck;
import de.julsched.beliefchange.naive.NaiveModelCheck;
import de.julsched.beliefchange.naive.NaiveModelDetermination;
import de.julsched.beliefchange.sat.CompilerSat;
import de.julsched.beliefchange.sat.SatInferenceCheck;
import de.julsched.beliefchange.sat.SatModelCheck;
import de.julsched.beliefchange.values.EncodingType;
import de.julsched.beliefchange.values.Algorithm;
import de.julsched.beliefchange.values.Distance;
import de.julsched.beliefchange.values.Operation;

public class Application {

    private static Algorithm preCompilationAlgorithm;
    private static boolean compilation;
    private static BeliefChangeCompiler compiler;
    private static Distance distance;
    private static File encodingFile;
    private static EncodingType encodingType;
    private static boolean inferenceCheck;
    private static File instanceFile;
    public static String instanceName;
    private static boolean modelCheck;
    private static Operation operation;
    private static boolean validateInstance = true;

    public static final String dirResults = "results";
    public static String dirInterimResults;
    public static String resultFilePath;

    public static long applicationStartTime = 0;
    public static long applicationEndTime = 0;
    public static long compilationStartTime = 0;
    public static long compilationEndTime = 0;
    public static long optimumFinderStartTime = 0;
    public static long optimumFinderEndTime = 0;
    public static long solverCallsStartTime = 0;
    public static long solverCallsEndTime = 0;
    public static long inferenceCheckStartTime = 0;
    public static long inferenceCheckEndTime = 0;
    public static long modelCheckStartTime = 0;
    public static long modelCheckEndTime = 0;

    public static long timeCompilation = 0;
    public static long timeInference = 0;
    public static long timeModel = 0;
    public static long timeOptimumFinder = 0;
    public static long timeSolverCalls = 0;
    public static long timeTotal = 0;

    public static void main(String[] args) {
        Application.applicationStartTime = System.currentTimeMillis();
        try {
            analyzeArgs(args);
            if (compilation) {
                if (encodingType == EncodingType.NAIVE) {
                    runNaiveImplementation();
                } else {
                    runCompiler();
                }
            } else if (inferenceCheck) {
                runInferenceCheck();
            } else if (modelCheck) {
                runModelCheck();
            }
            Application.applicationEndTime = System.currentTimeMillis();
            printExecutionTimes();
        } catch (Exception e) {
            System.out.println("[ERROR] The program terminated with an error:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void analyzeArgs(String[] args) throws IOException {
        for (int a = 0; a < args.length; a++) {
            switch (args[a]) {
                case "-s": case "--skip-validation":
                    if (!validateInstance) {
                        throw new WrongInputException("Duplicate specification of '-s/--skip-validation'");
                    }
                    validateInstance = false;
                    break;
                case "-t": case "--type":
                    if (encodingType != null) {
                        throw new WrongInputException("Duplicate specification of '-t/--type'");
                    }
                    if (args.length >= a) {
                        encodingType = EncodingType.getType(args[a + 1]);
                        if (encodingType == null) {
                            throw new WrongInputException(
                                new StringBuilder("Invalid input '")
                                .append(args[a + 1])
                                .append("'. Please specify one of the following encoding types: [")
                                .append(EncodingType.getValues())
                                .append("]").toString()
                            );
                        }
                        a++;
                    }
                    break;
                case "-C": case "--compilation":
                    if (compilation) {
                        throw new WrongInputException("Duplicate specification of '-C/--compilation'");
                    }
                    compilation = true;
                    break;
                case "-a": case "--algorithm":
                    if (preCompilationAlgorithm != null) {
                        throw new WrongInputException("Duplicate specification of '-a/--algorithm'");
                    }
                    if (args.length >= a) {
                        preCompilationAlgorithm = Algorithm.getAlgorithm(args[a + 1]);
                        if (preCompilationAlgorithm == null) {
                            throw new WrongInputException(
                                new StringBuilder("Invalid input '")
                                .append(args[a + 1])
                                .append("'. Please specify one of the following algorithms: [")
                                .append(Algorithm.getValues())
                                .append("]").toString()
                            );
                        }
                        a++;
                    }
                    break;
                case "-d": case "--distance":
                    if (distance != null) {
                        throw new WrongInputException("Duplicate specification of '-d/--distance'");
                    }
                    if (args.length >= a) {
                        distance = Distance.getDistance(args[a + 1]);
                        if (distance == null) {
                            throw new WrongInputException(
                                new StringBuilder("Invalid input '")
                                .append(args[a + 1])
                                .append("'. Please specify one of the following distances: [")
                                .append(Distance.getValues())
                                .append("]").toString()
                            );
                        }
                        a++;
                    }
                    break;
                case "-e": case "--encoding":
                    if (encodingFile != null) {
                        throw new WrongInputException("Duplicate specification of '-e/--encoding'");
                    }
                    if (args.length >= a) {
                        encodingFile = new File(args[a + 1]);
                        a++;
                    }
                    if (encodingFile == null || encodingFile.getName().startsWith("-")) {
                        throw new WrongInputException("Please provide a path to an encoding file");
                    }
                    break;
                case "-f": case "--file":
                    if (instanceFile != null) {
                        throw new WrongInputException("Duplicate specification of '-f/--file'");
                    }
                    if (args.length >= a) {
                        instanceFile = new File(args[a + 1]);
                        a++;
                    }
                    if (instanceFile == null || instanceFile.getName().startsWith("-")) {
                        throw new WrongInputException("Please provide a path to an instance file");
                    }
                    break;
                case "-I": case "--inference-check":
                    if (inferenceCheck) {
                        throw new WrongInputException("Duplicate specification of '-I/--inference-check'");
                    }
                    inferenceCheck = true;
                    break;
                case "-M": case "--model-check":
                    if (modelCheck) {
                        throw new WrongInputException("Duplicate specification of '-M/--model-check'");
                    }
                    modelCheck = true;
                    break;
                case "-o": case "--operation":
                    if (operation != null) {
                        throw new WrongInputException("Duplicate specification of '-o/--operation'");
                    }
                    if (args.length >= a) {
                        operation = Operation.getOperation(args[a + 1]);
                        if (operation == null) {
                            throw new WrongInputException(
                                new StringBuilder("Invalid input '")
                                .append(args[a + 1])
                                .append("'. Please specify one of the following operations: [")
                                .append(Operation.getValues())
                                .append("]").toString()
                            );
                        }
                        a++;
                    }
                    break;
                default:
                    throw new WrongInputException(
                        new StringBuilder("Invalid input '")
                        .append(args[a])
                        .append("'").toString()
                    );
            }
        }
        if (instanceFile == null) {
            throw new WrongInputException("Please specify a path to an instance file using [-f/--file <file_path>]");
        }
        int modes = 0;
        if (compilation) {
            modes++;
        }
        if (inferenceCheck) {
            modes++;
        }
        if (modelCheck) {
            modes++;
        }
        if (modes == 0) {
            throw new WrongInputException("Please choose the desired operating mode: [-C/--compilation | -I/--inference-check | -M/--model-check]");
        }
        if (modes > 1) {
            throw new WrongInputException("Duplicate operating modes. Please choose only one of [-C/--compilation | -I/--inference-check | -M/--model-check]");
        }

        instanceName = instanceFile.getName().split("\\.")[0];

        if (inferenceCheck || modelCheck) {
            if (encodingFile == null) {
                throw new WrongInputException("Please specify a path to an encoding file using [-e/--encoding <file_path>]");
            }
            if (encodingType != null) {
                throw new WrongInputException("Encoding provided. Flag '--type " + encodingType + "' not allowed");
            }
            if (distance != null) {
                throw new WrongInputException("Encoding provided. Flag '--distance " + distance + "' not allowed");
            }
            if (operation != null) {
                throw new WrongInputException("Encoding provided. Flag '--operation " + operation + "' not allowed");
            }
            if (preCompilationAlgorithm != null) {
                throw new WrongInputException("Encoding provided. Flag '--algorithm " + preCompilationAlgorithm + "' not allowed");
            }
            // Naive model check does not require an interim results directory
            if (FilenameUtils.getExtension(encodingFile.getName()).equals("") && modelCheck) {
                return;
            }
            dirInterimResults = "interim_results/" + instanceName;
            Files.createDirectories(Paths.get(dirInterimResults));
            return;
        }
        if (encodingType == EncodingType.NAIVE && preCompilationAlgorithm != null) {
            throw new WrongInputException("Naive execution selected. Flag '--algorithm " + preCompilationAlgorithm + "' not allowed");
        }

        System.out.println("-".repeat(100));
        if (operation == null) {
            operation = Operation.getDefault();
            System.out.println("[INFO] No operation specified. Using default '" + operation + "'");
        } else {
            System.out.println("[INFO] Specified operation: " + operation);
        }
        if (distance == null) {
            distance = Distance.getDefault();
            System.out.println("[INFO] No distance specified. Using default '" + distance + "'");
        } else {
            System.out.println("[INFO] Specified distance: " + distance);
        }
        if (encodingType == null) {
            encodingType = EncodingType.getDefault();
            System.out.println("[INFO] No encoding type specified. Using default '" + encodingType + "'");
        } else {
            System.out.println("[INFO] Specified encoding type: " + encodingType);
        }
        if (preCompilationAlgorithm == null) {
            if (encodingType != EncodingType.NAIVE) {
                preCompilationAlgorithm = Algorithm.getDefault();
                System.out.println("[INFO] No pre-compilation algorithm specified. Using default '" + preCompilationAlgorithm + "'");
            }
        } else {
            System.out.println("[INFO] Specified pre-compilation algorithm: " + preCompilationAlgorithm);
        }

        dirInterimResults = dirResults + "/interim_results/" + instanceName;
        Files.createDirectories(Paths.get(dirInterimResults));
        if (encodingType == EncodingType.NAIVE) {
            resultFilePath = dirResults + "/" + instanceName + "_" + operation + "_" + distance + "_models" + encodingType.getFileExtension();
        } else {
            resultFilePath = dirResults + "/" + instanceName + "_" + operation + "_" + distance + "_encoding" + encodingType.getFileExtension();
        }
    }

    private static void runCompiler() throws IOException {
        compiler = createBeliefChangeCompiler();
        compiler.createEncoding();
    }

    private static void runNaiveImplementation() throws IOException, InterruptedException {
        NaiveModelDetermination naiveImplementation = new NaiveModelDetermination(instanceFile, validateInstance, operation, distance);
        naiveImplementation.execute();
    }

    private static void runInferenceCheck() throws IOException, InterruptedException {
        InferenceCheckInstance instance = new InferenceCheckInstance(instanceFile, validateInstance);
        String fileExtension = FilenameUtils.getExtension(encodingFile.getName());
        switch (fileExtension) {
            case "lp":
                new AspInferenceCheck(encodingFile, validateInstance, instance).execute();
                break;
            case "mod":
                new IlpInferenceCheck(encodingFile, validateInstance, instance).execute();
                break;
            case "cnf":
                new SatInferenceCheck(encodingFile, validateInstance, instance).execute();
                break;
            case "":
                new NaiveInferenceCheck(encodingFile, instance).execute();
                break;
            default:
                throw new WrongInputException("Unsupported file extension: '." + fileExtension + "'");
        }
    }

    private static void runModelCheck() throws IOException, InterruptedException {
        String fileExtension = FilenameUtils.getExtension(encodingFile.getName());
        ModelCheckInstance instance = new ModelCheckInstance(instanceFile);
        switch (fileExtension) {
            case "lp":
                new AspModelCheck(encodingFile, validateInstance, instance).execute();
                break;
            case "mod":
                new IlpModelCheck(encodingFile, validateInstance, instance).execute();
                break;
            case "cnf":
                new SatModelCheck(encodingFile, validateInstance, instance).execute();
                break;
            case "":
                new NaiveModelCheck(encodingFile, instance).execute();
                break;
            default:
                throw new WrongInputException("Unsupported file extension: '." + fileExtension + "'");
        }
    }

    private static BeliefChangeCompiler createBeliefChangeCompiler() {
        switch (encodingType) {
            case ASP:
                return new CompilerAsp(instanceFile, validateInstance, operation, distance, preCompilationAlgorithm);
            case ILP:
                return new CompilerIlp(instanceFile, validateInstance, operation, distance, preCompilationAlgorithm);
            case SAT:
                return new CompilerSat(instanceFile, validateInstance, operation, distance, preCompilationAlgorithm);
            default:
                throw new WrongInputException("Unsupported encoding type: '" + encodingType + "'");
        }
    }

    private static void printExecutionTimes() {
        System.out.println("-".repeat(100));
        System.out.println("Elapsed wall clock time:");
        if (compilation) {
            if (encodingType == EncodingType.NAIVE) {
                printExecutionTimesNaiveImplementation();
            } else {
                printExecutionTimesCompiler();
            }
        } else if (inferenceCheck) {
            printExecutionTimesInferenceCheck();
        } else if (modelCheck) {
            printExecutionTimesModelCheck();
        }
    }

    private static void printExecutionTimesCompiler() {
        Application.timeCompilation = Application.compilationEndTime - Application.compilationStartTime;
        Application.timeOptimumFinder = Application.optimumFinderEndTime - Application.optimumFinderStartTime;
        Application.timeSolverCalls = Application.solverCallsEndTime - Application.solverCallsStartTime;
        Application.timeTotal = Application.applicationEndTime - Application.applicationStartTime;
        System.out.println("Compilation overall\t\t" + Application.timeCompilation + "ms");
        System.out.println("Optimum determination overall\t" + Application.timeOptimumFinder + "ms");
        System.out.println("Solver call(s)\t\t\t" + Application.timeSolverCalls + "ms");
        System.out.println("-".repeat(40));
        System.out.println("Total\t\t\t\t" + Application.timeTotal + "ms");
    }

    private static void printExecutionTimesNaiveImplementation() {
        Application.timeTotal = Application.applicationEndTime - Application.applicationStartTime;
        System.out.println("Total\t\t\t" + Application.timeTotal + "ms");
    }

    private static void printExecutionTimesInferenceCheck() {
        Application.timeTotal = Application.applicationEndTime - Application.applicationStartTime;
        if (!FilenameUtils.getExtension(encodingFile.getName()).equals("")) {
            // No naive inference check
            Application.timeInference = Application.inferenceCheckEndTime - Application.inferenceCheckStartTime;
            Application.timeSolverCalls = Application.solverCallsEndTime - Application.solverCallsStartTime;
            System.out.println("Inference check overall\t\t" + Application.timeInference + "ms");
            System.out.println("Solver call(s)\t\t\t" + Application.timeSolverCalls + "ms");
            System.out.println("-".repeat(40));
        }
        System.out.println("Total\t\t\t\t" + Application.timeTotal + "ms");
    }

    private static void printExecutionTimesModelCheck() {
        Application.timeTotal = Application.applicationEndTime - Application.applicationStartTime;
        if (!FilenameUtils.getExtension(encodingFile.getName()).equals("")) {
            // No naive model check
            Application.timeModel = Application.modelCheckEndTime - Application.modelCheckStartTime;
            Application.timeSolverCalls = Application.solverCallsEndTime - Application.solverCallsStartTime;
            System.out.println("Model check overall\t\t" + Application.timeModel + "ms");
            System.out.println("Solver call(s)\t\t\t" + Application.timeSolverCalls + "ms");
            System.out.println("-".repeat(40));
        }
        System.out.println("Total\t\t\t\t" + Application.timeTotal + "ms");
    }
}
