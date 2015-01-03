package pl0compiler;

/**
 * Created by lizhen on 14/12/4.
 */
public class PL0Exception extends Exception{
    public int errType;
    public PL0Exception(int errType){
        super();
        this.errType = errType;
    }
    public static void handle(int errType, Error err, Scanner lex){
        err.outputErrMessage(errType, lex.lineNumber, lex.cc);
    }
    void handle(Error err, Scanner lex){
        err.outputErrMessage(errType, lex.lineNumber, lex.cc);
    }
}
