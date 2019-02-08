package shell;

import java.io.File;
import java.io.IOException;


public class RunBash {
    
    public void excuteCommand(String filePath) throws IOException{
        File file = new File(filePath);
        if(!file.isFile()){
            throw new IllegalArgumentException("The file " + filePath + " does not exist");
        }
        Runtime.getRuntime().exec(new String[] {"/bin/sh", filePath}, null);
    }


    public static void main(String args[]) throws IOException{
        RunBash bash = new RunBash();
        //bash.readBashScript("sh /root/Desktop/testScript.sh");
        bash.excuteCommand("/Users/laricdbuddy/Desktop/jamscript/plot1.sh");
    }
}
