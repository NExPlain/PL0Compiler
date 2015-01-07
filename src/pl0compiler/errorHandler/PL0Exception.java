package pl0compiler.errorHandler;

import pl0compiler.errorHandler.Error;
import pl0compiler.lexicalAnalysis.Scanner;

/**
 * Created by lizhen on 14/12/4.
 */
public class PL0Exception extends Exception{
    public int errType;
    public PL0Exception(int errType){
        super();
        this.errType = errType;
    }
    public static void handle(int errType, pl0compiler.errorHandler.Error err, Scanner scan){
        if(scan.isfileEneded)return;
        err.outputErrMessage(errType, scan.lineNumber, scan.getcc());
    }
    public void handle(Error err, Scanner scan) {
        if (scan.isfileEneded) return;
        err.outputErrMessage(errType, scan.lineNumber, scan.getcc());
    }
}
