import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class FileDump {

    public static void main(String[] args){
        Aout.loadAout(args[0]);
        StringBuffer c = new StringBuffer();
        int i;
        for(i=0;i<Aout.aout.length;i++) {
            if(i%16 == 0) System.out.printf("%08x ",i);
            
            System.out.printf("%02x ",Aout.aout[i]);
            if(Aout.aout[i] < 33 || Aout.aout[i] > 126){
                c.append(".");
            }else{
                c.append((char)Aout.aout[i]);
            }

            if(i%16 == 15){
            	printChar(c);
            }
        }
        if((i%16) < 15){
        	for(;(i%16)<15;i++){
        		System.out.print("   ");
        	}
    		System.out.print("   ");
            printChar(c);
        }
    }
    
    public static void printChar(StringBuffer c){
    	System.out.print("|");
    	System.out.print(c);
    	System.out.print("|");
    	System.out.printf("\n");
    	c.delete(0, c.length());
    }
}

class Aout {
    static byte[] aout;

    static void loadAout(String fileName) {
        File file = new File(fileName);
        Path pathName = file.toPath();
        try {
            aout = java.nio.file.Files.readAllBytes(pathName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}