import java.util.HashMap;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args){
        Terminal terminal = new Main().new Terminal();
        while(true){
            if (!terminal.chooseCommandAction()) {
                break;
            }
        }
    }

    class Terminal {

        private Parser parser;
        public Map<String, Consumer<Void>> commandMap = new HashMap<>();
        private Path path;
    
        public Terminal(){
            initCommandMap();
            this.path = Path.of("").toAbsolutePath();
            this.parser = new Parser();
        }

        private void initCommandMap() {
            commandMap.put("echo", v -> echo());
            commandMap.put("pwd", v -> pwd());
            commandMap.put("cd", v -> cd());
            commandMap.put("ls", v -> ls());
            commandMap.put("mkdir", v -> mkdir());
            commandMap.put("rmdir", v -> rmdir());
            commandMap.put("touch", v -> touch());
            // commandMap.put("cp", v -> cp(parser.args));
            // commandMap.put("rm", v -> rm(parser.args));
            // commandMap.put("cat", v -> cat(parser.args));
            // commandMap.put("wc", v -> wc(parser.args));
            // commandMap.put("history", v -> history(parser.args));
        }

        private String getPathStringFromArgs(String[] args) {
            String pathString = "";
            
            for (int i = 0; i < parser.args.length; i++) {
                pathString += parser.args[i] + " ";
            }
            
            pathString = pathString.substring(0, pathString.length() - 1);

            return pathString;
        }

        private Path getNewPath(Path oldPath, Path newPath) {
                try {
                if (!newPath.isAbsolute()) {
                    newPath = Path.of(this.path.toString() + "/" + newPath.toString()); 
                }
                return newPath.toRealPath();

            } catch (Exception e) {
                return null;
            }
        }
        //Implement each command in a method, for example:
        public void echo(){
            for (int i = 0; i < parser.args.length; i++) {
                System.out.print(parser.args[i] + " ");
            }
            System.out.println();
        }
        
        public void pwd(){
            String s = this.path.toString();
            System.out.println(s);
        }
        
        public void cd(){
            if (parser.args.length < 1) {
                System.out.println("You have to provide at least one argument");
                return;
            }
           
            String pathString = getPathStringFromArgs(parser.args);
            
            Path newPath = Path.of(pathString);
            newPath = getNewPath(path, newPath);

            if (newPath == null) {
                System.out.println("Invalid path");
            } else {
                this.path = newPath;
            } 
        }

        private void ls() {
            if (parser.args.length > 1) {
                System.out.println("Wrong number of arguments");
                return;
            }

            boolean inReverse = false;

            if (parser.args.length == 1) {
                if (parser.args[0].equals("-r")) {
                    inReverse = true;
                } else {
                    System.out.println("Invalid argument");
                    return;
                }
            }

            File currentFolder = new File(this.path.toUri());
            File[] files = currentFolder.listFiles();

            for (int i = 0; i <  files.length; i++) {
                int currentFileIndex = (inReverse) ? (files.length - 1) - i : i;
                System.out.print(files[currentFileIndex].getName() + "\t");
            }

            System.out.println();
        } 

        private void mkdir() {
            if (parser.args.length < 1) {
                System.out.println("You have to provide at least one argument");
                return;
            }

            String pathString = getPathStringFromArgs(parser.args);

            Path newPath = Path.of(pathString);

            //TODO: this code is work and create new directory
            // try {
            //     Files.createDirectory(newPath);
            // } catch (Exception e) {
            //     System.out.println("An unexpected error occurred");
            //     return; 
            // }

            //TODO: this code isn't work with Ayman no errors but don't create new directory
            String newDirectoryName = newPath.getFileName().toString();

            if (getNewPath(path, newPath) != null) {
                System.out.println("File already exists: " + pathString);
                return; 
            }

            if (newPath.getParent() == null) {
                newPath = Path.of("");
                newPath = getNewPath(path, newPath);
            } else {
                newPath = getNewPath(newPath, newPath.getParent());
            }

            if (newPath == null) {
                System.out.println("Cannot create directory: " + pathString +  " No such file or directory");
            } else {
                File newDirectory = new File(newPath.toString() + "\\" + newDirectoryName);
                boolean created = newDirectory.mkdir();
                if (!created) {
                    System.out.println("An unexpected error occurred");
                }
            }
        }

        private void rmdir() {
            if (parser.args.length < 1) {
                System.out.println("You have to provide at least one argument");
                return;
            }

            String pathString = getPathStringFromArgs(parser.args);

            Path newPath = Path.of(pathString);

            try {
                Files.delete(newPath);
            } catch (Exception e) {
                System.out.println("An unexpected error occurred");
                return; 
            }
        }
        
        private void touch() {
            if (parser.args.length < 1) {
                System.out.println("You have to provide at least one argument");
                return;
            }

            String pathString = getPathStringFromArgs(parser.args);

            Path newPath = Path.of(pathString);
            newPath = this.path.resolve(newPath);

            try {
                Files.createFile(newPath);
            } catch (Exception e) {
                System.out.println("An unexpected error occurred");
                return; 
            }
        }
        // This method will choose the suitable command method to be called
        public boolean chooseCommandAction(){
            System.out.println();
            System.out.print(path.toString() + "> ");
            String input = System.console().readLine();
            this.parser.parse(input);
            if (this.parser.commandName.equals("exit")) {
                return false;
            }
            if (this.commandMap.containsKey(parser.commandName)){
                Consumer<Void> action = this.commandMap.get(parser.commandName);
                action.accept(null);
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