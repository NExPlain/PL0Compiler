package pl0compiler;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class PL0 {

    public static final String pcodeFile = "./src/test/pcode/1.txt";
    public static final String tableFile = "./src/test/table/1.txt";
    public static final String runtimeFile = "./src/test/runtime/1.txt";
    public static final String errFile = "./src/test/error/1.txt";
    public static final String inputFile="./src/test/input/1.txt";
    public static BufferedWriter pcodeWriter;                   //输出虚拟机代码
    public static BufferedWriter outputWriter;                 //输出结果
    public static BufferedWriter tableWriter;                   //输出名字表
    public static BufferedWriter errWriter;                     //输出错误信息

    public static Parser praser;
    public static Scanner scan;

    public PL0(String filepath) {
        scan = new Scanner(filepath);
        praser = new Parser(scan,                               //词法分析器
                new SymbolTable(),                              //名字表
                new Interpreter());
    }

    public boolean compile() {
        try {
            pcodeWriter = new BufferedWriter(new FileWriter(pcodeFile));
            tableWriter = new BufferedWriter(new FileWriter(tableFile));
            outputWriter = new BufferedWriter(new FileWriter(runtimeFile));
            errWriter = new BufferedWriter(new FileWriter(errFile));
            praser.lex.getch();
            praser.getsym();                                      //前瞻分析需要预先读入一个符号
            praser.parse();                                        //开始语法分析过程（连同语法检查，目标代码生成）
            pcodeWriter.close();
            tableWriter.close();
            outputWriter.close();
            errWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("compile error");
        } finally {

        }
        System.out.println("compile completed");
        //编译成功是指完成编译过程并且没有错误
        return (praser.err.errCnt == 0);
    }
}
