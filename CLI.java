import java.util.HashMap;
import java.util.Arrays;
import java.util.Vector;
import java.util.Map;
import java.util.function.Function;
import java.util.Comparator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
// import java.nio.file.StandardOpenOption;
import java.io.FileWriter;
import java.io.IOException;

public class CLI {

    public static void main(String[] args){
        Terminal terminal = new CLI().new Terminal();
        while(true){
            if (!terminal.chooseCommandAction()) {
                break;
            }
        }
    }

    class Terminal {

        private Parser parser;
        public Map<String, Function<Void, String>> commandMap = new HashMap<>();
        private Path path;
        private Vector<String> history;
    
        public Terminal(){
            initCommandMap();
            this.path = Path.of("").toAbsolutePath();
            this.parser = new Parser();
            this.history = new Vector<>();
        }

        private void initCommandMap() {
            commandMap.put("echo", v -> echo());
            commandMap.put("pwd", v -> pwd());
            commandMap.put("cd", v -> cd());
            commandMap.put("ls", v -> ls());
            commandMap.put("mkdir", v -> mkdir());
            commandMap.put("rmdir", v -> rmdir());
            commandMap.put("touch", v -> touch());
            commandMap.put("cp", v -> cp());
            commandMap.put("rm", v -> rm());
            commandMap.put("cat", v -> cat());
            commandMap.put("wc", v -> wc());
            commandMap.put("history", v -> history());
        }
        //TODO: args with " " should include " 
        private String getPathStringFromArgs(String[] args) {
            String pathString = "";
            
            for (int i = 0; i < getArgsLength(); i++) {
                pathString += parser.args[i] + " ";
            }
            
            pathString = pathString.substring(0, pathString.length() - 1);

            return pathString;
        }

        private Path getNewPath(Path newPath) {
                try {
                if (!newPath.isAbsolute()) {
                    newPath = Path.of(this.path.toString() + "/" + newPath.toString()); 
                }
                return newPath.toRealPath();

            } catch (Exception e) {
                return null;
            }
        }
        
        private boolean checkValideCreation(Path newPath, String pathString) {
            if (getArgsLength() < 1) {
                System.out.println("You have to provide at least one argument");
                return false;
            }
            
            // check existing file or directory 
            if (getNewPath(newPath) != null) {
                System.out.println("File already exists: " + pathString);
                return false; 
            }

            // check if the parent directory is root 
            if (newPath.getParent() == null) {
                newPath = Path.of("");
                newPath = getNewPath(newPath);
            } else {
                newPath = getNewPath(newPath.getParent());
            }

            // check if the parent directory exists
            if (newPath == null) {
                System.out.println("Cannot create directory: " + pathString +  " No such file or directory");
                return false;
            }
            
            return true;
        }
        
        private void copyDir(Path source, Path target) throws IOException {

            if (!Files.exists(target)) {
                Files.createDirectories(target);
            }
            
            try {
                DirectoryStream<Path> stream = Files.newDirectoryStream(source);
                for (Path entry : stream) {
                    Path targetEntry = target.resolve(entry.getFileName());
                    if (Files.isDirectory(entry)) {
                        Files.createDirectories(entry);
                        copyDir(entry, targetEntry);
                    }
                    else {
                        Files.copy(entry, targetEntry, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (IOException e) {
                System.err.println("Faild to read the file: " + e.getMessage());
            }
        }
        
        private int getArgsLength() {
            return this.parser.args.length;
        }

        private class FileComparator implements Comparator<File>{
            @Override
            public int compare(File file1, File file2) {
                return file1.getName().compareTo(file2.getName());
            }
        }
        
        //Implement each command in a method, for example:
        private String echo(){
            String output = "";

            for (int i = 0; i < getArgsLength(); i++) {
                output += parser.args[i] + " ";
            }
            output += "\n";
            return output;
        }
        
        private String pwd(){
            String output = this.path.toString() + "\n";
            return output;
        }
        
        private String cd(){
            String output = "";

            if (getArgsLength() > 1) {
                output = "You have to provide no or at least one argument";
                return output;
            }
            
            if (getArgsLength() == 0) {
                this.path = Path.of(System.getProperty("user.home"));
                return output;
            }

            String pathString = getPathStringFromArgs(parser.args);
            try {

                Path newPath = Path.of(pathString);
                newPath = getNewPath(newPath);
                
                if (newPath == null) {
                    return "Invalid path";
                } else {
                    this.path = newPath;
                } 
                
            } catch (Exception e) {
                return "Invalid path";
            }
            return output;
        }

        private String ls() {
            String output = "";

            if (getArgsLength() > 1) {
                return "Wrong number of arguments";
            }

            boolean inReverse = false;

            if (getArgsLength() == 1) {
                if (parser.args[0].equals("-r")) {
                    inReverse = true;
                } else {
                    return "Invalid argument";
                }
            }

            File currentFolder = new File(this.path.toUri());
            File[] files = currentFolder.listFiles();

            Arrays.sort(files, new FileComparator());

            for (int i = 0; i <  files.length; i++) {
                int currentFileIndex = (inReverse) ? (files.length - 1) - i : i;
                output += (files[currentFileIndex].getName() + "\t");
            }
            
            output += "\n";
            return output;
        } 

        private String mkdir() {
            String output = "";

            String pathString = "";
            Path newPath = Path.of("");
            for (int i = 0; i < getArgsLength(); i++) {
                pathString = parser.args[i];

                newPath = Path.of(pathString);

                if (!checkValideCreation(newPath, pathString)) {
                    return output; 
                }

                try {
                    newPath = this.path.resolve(newPath);
                    Files.createDirectory(newPath);
                } catch (Exception e) {
                    return "An unexpected error occurred";
                } 
            }

            return output;
        }

        private String rmdir() {
            String output = "";
            if (getArgsLength() < 1) {
                return "You have to provide at least one argument";
            }

            if (this.parser.args[0].equals("*")) {
                try {
                    Files.walk(this.path, 1)
                    .filter(Files::isDirectory)
                    .forEach(directory -> {
                            try {
                                if (directory != this.path) {
                                    Files.delete(directory);
                                }
                            } catch (Exception e) {
                                String error = "delete faild: (" + directory + ") is not empty";
                                throw new ArithmeticException(error);
                            }
                        });
                } catch (Exception e) {
                    return e.getMessage();
                }
                return output;
            }

            String pathString = getPathStringFromArgs(parser.args);

            Path newPath = Path.of(pathString);

            if (getNewPath(newPath) != null) {
                try {
                    if (!Files.isDirectory(newPath)) {
                        output = pathString + " is not a directory";
                        return output;
                    }
                    Files.delete(newPath);
                } catch (Exception e) {
                    output = "delete faild: (" + pathString + ") is not empty";
                    return output;
                }
            } else {
                output = "delete faild: (" + pathString +  ") no such file or directory";
                return output;
            }

            return output; 
        }
        
        private String touch() {
            String output = "";

            String pathString = getPathStringFromArgs(parser.args);

            Path newPath = Path.of(pathString);

            if (!checkValideCreation(newPath, pathString)) {
                return "";
            }

            try {
                newPath = this.path.resolve(newPath);
                Files.createFile(newPath);
            } catch (Exception e) {
                return "An unexpected error occurred";
            }
            return output; 
        }
        
        private String cp() {
            String output = "";
            if (getArgsLength() != 2 && getArgsLength() != 3) {
                return "You have to provide exactly two arguments: cp (source file) (target file)";
            }
            
            String pathString = "";
            Path newPath = Path.of("");

            
            
            for (int i = 0; i < getArgsLength(); i++) {
                pathString = parser.args[i];
                newPath = Path.of(pathString); 
                
                if (this.parser.args[0] == "-r" && (!Files.isDirectory(newPath) || getNewPath(newPath) == null)) {
                    output = "Faild to copy: (" + pathString + ") is not directory";
                    return output;
                }
                if (this.getArgsLength() == 2 && (Files.isDirectory(newPath) || getNewPath(newPath) == null)) {
                    output = "Faild to copy: (" + pathString + ") is not file";
                    return output;
                }
            }
            int inputIndex = (this.parser.args[0].equals("-r")) ? 1 : 0;

            Path source = Path.of(this.parser.args[inputIndex]);
            Path target = Path.of(this.parser.args[inputIndex + 1]);
            source = this.path.resolve(source);
            target = this.path.resolve(target);
            
            try {
                if (this.parser.args[0].equals("-r")) {
                    copyDir(source, target);
                    return "";
                }
                String fileContent = new String(Files.readAllBytes(source));

                Files.write(target, fileContent.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                output = "Faild to read the file: " + e.getMessage();
                return output;
            }

            return output;
        }

        private String rm() {
            String output = "";
            if (getArgsLength() != 1) {
                return "You have to provide exactly one argument: rm (file)";
            }

            String pathString = getPathStringFromArgs(parser.args);

            Path newPath = Path.of(pathString);

            if (getNewPath(newPath) != null) {
                try {
                    if (Files.isDirectory(newPath)) {
                        output = pathString + " is not a file";
                        return output;
                    }
                    Files.delete(this.path.resolve(newPath));
                } catch (Exception e) {
                    return "An unexpected error occurred";
                }
            } else {
                output = "delete faild: (" + pathString +  ") no such file or directory";
                return output;
            }
            return output;
        }
        
        private String cat() {
            String output = "";
            if (getArgsLength() != 1 && getArgsLength() != 2) {
                return "Invalid number of arguments: (cat file_name) or (cat file1 file2)";
            }
            
            String pathString = "";
            Path newPath = Path.of("");

            for (int i = 0; i < getArgsLength(); i++) {
                pathString = parser.args[i];
                newPath = Path.of(pathString); 
                
                if (Files.isDirectory(newPath) || getNewPath(newPath) == null) {
                    output = "Faild to copy: (" + pathString + ") is not file";
                    return output;
                }
            }

            try {
                String fileContent = "";
                for (int i = 0; i < getArgsLength(); i++) {
                    pathString = parser.args[i];
                    newPath = Path.of(pathString);
                    fileContent += new String(Files.readAllBytes(this.path.resolve(newPath)));
                    fileContent += "\n";
                } 

                return fileContent;
            } catch (IOException e) {
                output = "Faild to read the file: " + e.getMessage();
                return output;
            }
        }
        
        private String wc() {
            String output = "";

            if (getArgsLength() != 1) {
                return "You have to provide exactly one argument: wc (file)";
            }

            String pathString = getPathStringFromArgs(parser.args);

            Path newPath = Path.of(pathString);

            if (getNewPath(newPath) != null) {
                if (Files.isDirectory(newPath)) {
                    output = pathString + " is not a file";
                    return output;
                }
                int numLines = 0, numWords = 0, numCharacters = 0;
                try {
                    FileReader fileReader = new FileReader(this.path.resolve(newPath).toString());
                    BufferedReader bufferedReader = new BufferedReader(fileReader);
                    String line = "";
                    
                    while ((line = bufferedReader.readLine()) != null) {
                        numLines++;
                        numWords += line.split(" ").length;
                        numCharacters += line.length();
                    }
                    bufferedReader.close();
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                output = numLines + " " + numWords + " " + numCharacters + " " + pathString;
                return output;
            } else {
                output = "words count faild: (" + pathString +  ") is not a file";
                return output;
            }
        }
        
        private String history() {
            String output = "";
            if (getArgsLength() != 0) {
                return "history command takes no arguments";
            }

            if (this.history.size() == 0) {
                return "No commands in history";
            }

            for (int i = 0; i < this.history.size(); i++) {
                output += i+1 + " " + this.history.elementAt(i) + "\n"; 
            }
            return output;
        }
        // This method will choose the suitable command method to be called

        private String[] getOutputFile() {
            if (this.parser.args.length >= 2 ) {
                if (this.parser.args[this.parser.args.length - 2].equals(">")) {
                    String[] result = {this.parser.args[this.parser.args.length - 1], "w"};
                    this.parser.args = Arrays.copyOf(this.parser.args, this.parser.args.length - 2);

                    return result;
                }
                
                if (this.parser.args[this.parser.args.length - 2].equals(">>")) {
                    String[] result = {this.parser.args[this.parser.args.length - 1], "a"};
                    this.parser.args = Arrays.copyOf(this.parser.args, this.parser.args.length - 2);

                    return result;
                }
            }

            return null;
       }

        private void writeOutput(String commandOutput, String[] outputInfo) {
            String outputFilePath = (outputInfo != null) ? outputInfo[0] : null;
            String outputType = (outputInfo != null) ? outputInfo[1] : null;

            if (outputFilePath== null) {
                System.out.println(commandOutput);
            }
            
            else {
                Path newPath = this.path.resolve(outputFilePath);

                if (newPath == null) {
                    System.err.println("Could not write / append the output to the file");
                    return;
                }
                
                try {
                    boolean append = (outputType == "w") ? false : true;
                    FileWriter f =  new FileWriter(newPath.toFile(), append);
                    if (append) {
                        f.append(commandOutput);
                    } else {
                        f.write(commandOutput);
                    }
                    f.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }

        public boolean chooseCommandAction(){
            System.out.println();
            System.out.print(path.toString() + "> ");
            String input = System.console().readLine();
            this.parser.parse(input);
            if (this.parser.commandName.equals("exit")) {
                return false;
            }
            if (this.commandMap.containsKey(parser.commandName)) {
                String[] outputInfo= getOutputFile(); 

                String commandOutput = this.commandMap.get(parser.commandName).apply(null);
                
                writeOutput(commandOutput, outputInfo);
                this.history.add(this.parser.commandName);
            }
            else {
                System.out.println("This command is not available");
            }            
            return true;
        }
    }
    
    public class Parser {
        
        String commandName;
        String[] args;
        //This method will divide the input into commandName and args
        //where "input" is the string command entered by the user
        public boolean parse(String input){
            if(input.trim().isEmpty()) {
                return false;
            }
            String[] splittedInput = input.split(" ");
            commandName = splittedInput[0];
            if(splittedInput.length == 1) {
                args = new String[0];
            }
            else {
                args = Arrays.copyOfRange(splittedInput, 1, splittedInput.length);;
            }
            return true;
        }
        public String getCommandName(){
            return this.commandName;
        }
        public String[] getArgs(){
            return this.args;
        }
        
        
    }  
    
}