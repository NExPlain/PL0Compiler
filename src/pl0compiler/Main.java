package pl0compiler;

/**
 * Created by lizhen on 14/12/14.
 */
public class Main {
    public static Compiler compiler = new Compiler();
    public static void main(String args[]){
        compiler.MODEID = compiler.STACKTABLEMODE;
        if(args.length == 1 && args[0].equals("1")){
            compiler.MODEID = compiler.STACKTABLEMODE;
        }else if(args.length == 1 && args[0].equals("2")){
            compiler.MODEID = compiler.BINARYTABLEMODE;
        }else if(args.length == 1 && args[0].equals("3")){
            compiler.MODEID = compiler.HASHTABLEMODE;
        }else if(args.length == 1 && args[0].equals("0")){
            compiler.compile();
            compiler.MODEID = compiler.BINARYTABLEMODE;
            compiler.compile();
            compiler.MODEID = compiler.HASHTABLEMODE;
            compiler.compile();
            return;
        }
        compiler.compile();
    }
}
