package pl0compiler.errorHandler;
import pl0compiler.lexicalAnalysis.Scanner;

/**
 * PL0异常类，用于处理PL0产生的异常
 * Created by lizhen on 14/12/4.
 */
public class PL0Exception extends Exception{
    public int errType;
    public PL0Exception(int errType){
        super();
        this.errType = errType;
    }

    /**
     * 按异常类别直接处理，用于抛系统异常如符号栈溢出
     * @param errType
     * @param err
     * @param scan
     */
    public static void handle(int errType, pl0compiler.errorHandler.Error err, Scanner scan){
        if(scan.isfileEneded)return;
        err.outputErrMessage(errType, scan.lineNumber, scan.getcc(), scan.getccbuf());
    }

    /**
     * 直接处理，直接处理异常
     * @param err
     * @param scan
     */
    public void handle(Error err, Scanner scan) {
        if (scan.isfileEneded) return;
        err.outputErrMessage(errType, scan.lineNumber, scan.getcc(), scan.getccbuf());
    }
}
