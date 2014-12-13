package pl0compiler;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class PL0 {

    public static final String pcodeFile = "\\\\psf\\Home\\Documents\\Project\\PL0Compiler\\PL0Compiler\\src\\test\\pcode\\1.txt";
    public static final String tableFile = "\\\\psf\\Home\\Documents\\Project\\PL0Compiler\\PL0Compiler\\src\\test\\table\\1.txt";
    public static final String runtimeFile = "\\\\psf\\Home\\Documents\\Project\\PL0Compiler\\PL0Compiler\\src\\test\\runtime\\1.txt";
    public static final String errFile = "\\\\psf\\Home\\Documents\\Project\\PL0Compiler\\PL0Compiler\\src\\test\\error\\1.txt";
    public static final String inputFile="\\\\psf\\Home\\Documents\\Project\\PL0Compiler\\PL0Compiler\\src\\test\\input\\1.txt";
    public static BufferedWriter pcodeWriter;                 //输出虚拟机代码
    public static BufferedWriter runtimeWriter;               //输出结果
    public static BufferedWriter tableWriter;                //输出名字表
    public static BufferedWriter errWriter;                   //输出错误信息

    public static Parser praser;

    public PL0(String filepath) {
        Scanner scan = new Scanner(filepath);
        praser = new Parser(scan,//词法分析器
                new SymbolTable(),//名字表
                new Interpreter());
    }

    public static boolean compile() {
        try {
            pcodeWriter = new BufferedWriter(new FileWriter(pcodeFile));
            tableWriter = new BufferedWriter(new FileWriter(tableFile));
            runtimeWriter = new BufferedWriter(new FileWriter(runtimeFile));
            errWriter = new BufferedWriter(new FileWriter(errFile));
            praser.nextsym();                                    //前瞻分析需要预先读入一个符号
            praser.parse();                                        //开始语法分析过程（连同语法检查，目标代码生成）
            pcodeWriter.close();
            tableWriter.close();
            runtimeWriter.close();
            errWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("***compile error***");
        } finally {

        }
        //编译成功是指完成编译过程并且没有错误
        return (praser.err.errCnt == 0);
    }
    public static void main(String args[]){
        compile();
    }
}
