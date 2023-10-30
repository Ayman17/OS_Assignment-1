import java.util.HashMap;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.io.File;
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
            // commandMap.put("mkdir", v -> mkdir(parser.args));
            // commandMap.put("rmdir", v -> rmdir(parser.args));
            // commandMap.put("touch", v -> rmdir(parser.args));
            // commandMap.put("cp", v -> cp(parser.args));
            // commandMap.put("rm", v -> rm(parser.args));
            // commandMap.put("cat", v -> rm(parser.args));
            // commandMap.put("wc", v -> rm(parser.args));
            // commandMap.put("history", v -> rm(parser.args));
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

            String pathString = "";
            
            for (int i = 0; i < parser.args.length; i++) {
                pathString += parser.args[i] + " ";
            }
            
            pathString = pathString.substring(0, pathString.length() - 1);
            
            try {
                Path newPath = Path.of(pathString);
                if (!newPath.isAbsolute()) {
                    newPath = Path.of(this.path.toString() + "/" + pathString); 
                }
                this.path = newPath.toRealPath();

            } catch (Exception e) {
                System.out.println("Invalid path");
            }
        }

        private void  ls() {
            if (parser.args.length > 0) {
                System.out.println("You have to provide at least one argument");
                return;
            }

            File currentFolder =new File(this.path.toUri());

            for (File file : currentFolder.listFiles()) {
                System.out.print(file.getName() + "\t");
            }
            System.out.println();

        } 
        
        //This method will choose the suitable command method to be called
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