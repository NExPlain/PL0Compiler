package pl0compiler;

import pl0compiler.syntaxAnalysis.Parser;

import java.io.*;

/**
 * 编译器类，启动函数的基本操作
 * 输入：input文件夹中所有文件
 * 输出：在output文件夹中输出错误信息，在pcode文件夹中输出对应pcode
 * Created by lizhen on 14/12/3.
 */
public class Compiler {

    public static final String pcodeFilePrefix = "./src/samples/pcode";
    public static final String outputFilePrefix = "./src/samples/output";
    public static final String inputFilePrefix ="./src/samples/input";
    public static BufferedWriter pcodeWriter;                   //输出虚拟机代码
    public static BufferedWriter outputWriter;                 //输出结果

    public static Parser parser;

    public Compiler() {
    }

    /**
     * 清空以filepath为路径的文件夹
     * @param filepath
     */
    void clean(String filepath)
    {
        File cleanfile = new File(filepath);
        String[] cleanfilelist = cleanfile.list();
        for(int i = 0 ; i < cleanfilelist.length ; i ++){
            String fp = cleanfilelist[i];
            String path = filepath + "/" + fp;
            File tf = new File(path);
            tf.delete();
        }
    }

    /**
     * 开始编译
     * @return
     */
    public boolean compile() {
        clean(outputFilePrefix);
        clean(pcodeFilePrefix);
        File file = new File(inputFilePrefix);
        String[] filelist = file.list();
        for(int i = 0 ;i < filelist.length ; i ++) {
            try {
                String add = "/" + filelist[i];
                parser = new Parser(inputFilePrefix + add);
                pcodeWriter = new BufferedWriter(new FileWriter(pcodeFilePrefix + add));
                outputWriter = new BufferedWriter(new FileWriter(outputFilePrefix + add));
                parser.scan.getch();
                parser.getsym();
                parser.start();

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("compile error");
            }
            System.out.println("compile completed");
            try {
                outputWriter.write("****************************\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (parser.err.errCnt == 0) {
                try {
                    outputWriter.write("Accepted\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                try {
                    outputWriter.write("COMPILE ERROR: " + parser.err.errCnt + " ERROR" + (parser.err.errCnt == 1 ? "" : "s") + '\n');
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                pcodeWriter.close();
                outputWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(parser == null)return false;                                                     // 编译遇到问题
        return (parser.err.errCnt == 0);                                                    // 根据编译错误数量判断编译是否有问题
    }
}