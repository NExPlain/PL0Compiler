package pl0compiler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class PL0 {

    public static final String pcodeFile = "./src/test/pcode/3.txt";
    public static final String tableFile = "./src/test/table/3.txt";
    public static final String runtimeFile = "./src/test/runtime/3.txt";
    public static final String errFile = "./src/test/error/3.txt";
    public static final String inputFile="./src/test/input/3.txt";
    public static BufferedWriter pcodeWriter;                   //输出虚拟机代码
    public static BufferedWriter outputWriter;                 //输出结果
    public static BufferedWriter tableWriter;                   //输出名字表
    public static BufferedWriter errWriter;                     //输出错误信息

    public static Parser parser;
    public static Scanner scan;

    public PL0(String filepath) {
        scan = new Scanner(filepath);
        parser = new Parser(scan,                               //词法分析器
                new SymbolTable(),                              //名字表
                new PcodeVM());
    }

    public boolean compile() {
        try {
            pcodeWriter = new BufferedWriter(new FileWriter(pcodeFile));
            tableWriter = new BufferedWriter(new FileWriter(tableFile));
            outputWriter = new BufferedWriter(new FileWriter(runtimeFile));
            errWriter = new BufferedWriter(new FileWriter(errFile));
            parser.scan.getch();
            parser.getsym();                                      //前瞻分析需要预先读入一个符号
            parser.start();                                        //开始语法分析过程（连同语法检查，目标代码生成）
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("compile error");
        }
        System.out.println("compile completed");
        if(parser.err.errCnt == 0){
            try {
                errWriter.write("Accepted\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try{
            pcodeWriter.close();
            tableWriter.close();
            outputWriter.close();
            errWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (parser.err.errCnt == 0);
    }
}
