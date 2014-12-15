package pl0compiler;

import jdk.internal.org.objectweb.asm.tree.LineNumberNode;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by lizhen on 14/12/3.
 * 词法分析类
 * 调用函数：getsym，返回一个词
 */


public class Scanner {

    public final static int numMax = 14;        //数字的最大位数
    public int positionInLine;
    public int lineNumber;
    public char ch;
    private  boolean fileEneded;
    String Buffer;
    private BufferedReader cin;

    public Scanner(String fileURL) {
        try {
            cin = new BufferedReader(new FileReader(fileURL));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            System.out.println("File not found!");
        }
        fileEneded = false;
        Buffer = "";
        lineNumber = 0;
        positionInLine = 0;
    }

    /**
     * 读取一个字符，'\0'表示已读到文件末尾
     * @return
     */
    public char getch() {
        if(fileEneded == true){
            return ch = '\0';
        }
        if (positionInLine == Buffer.length()) {
            try {
                do {
                    Buffer = cin.readLine();
                    if(Buffer == null){
                        fileEneded = true;
                        return ch = '\0';
                    }
                    lineNumber++;
                    Buffer.trim();
                    PL0.runtimeWriter.write(lineNumber + " " + Buffer + "\n");
                    PL0.runtimeWriter.flush();  // 把读入的源程序同时输出到output文件上
                } while (Buffer.equals(""));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("reading character error");
            }
            Buffer += " ";                     //  加一个空格表示到达行末尾，与下一行的开头分开
            positionInLine = 0;
        }
        if(positionInLine < Buffer.length())ch = Buffer.charAt(positionInLine++);
        else ch = ' ';
        return ch;
    }

    /**
     * 判断一个字符是否为数字
     *
     * @param x
     * @return
     */
    public boolean isDigit(char x) {
        return x >= '0' && x <= '9';
    }

    /**
     * 判断一个字符是否为英文字母
     *
     * @param x
     * @return
     */
    public boolean isAlpha(char x) {
        return (x >= 'a' && x <= 'z') || (x >= 'A' && x <= 'Z');
    }

    /**
     * 返回下一个单词（忽略空格）
     *
     * 如果碰到文件末尾则返回无法识别的符号
     * @return
     */
    public Symbol getsym() throws PL0Exception {
        Symbol currentSym = new Symbol(Symbol.SymbolType.nul.getIntValue());
        if(ch == '\0'){
            return currentSym;
        }
        while (ch == ' ') {
            getch();
        }
        if(ch == '\0'){
            return currentSym;
        }
        if (isDigit(ch)) {
            currentSym = AnalysisNumber();
        } else if (isAlpha(ch)) {
            currentSym = AnalysisWords();
        } else {
            currentSym = AnalysisOperator();
        }
        if(currentSym.symtype == Symbol.SymbolType.nul.getIntValue()){
            System.out.println("fuck?");
        }
        return currentSym;
    }

    /**
     * 分析一个运算符
     *  := | < | <= | <> | >= | > | SymbolTable中的单目运算符
     * @return
     */
    private Symbol AnalysisOperator() {
        Symbol sym = null;
        switch (ch) {
            case ':':
                getch();
                if (ch == '=') {
                    sym = new Symbol(Symbol.SymbolType.becomes.getIntValue());
                } else {
                    sym = new Symbol(Symbol.SymbolType.nul.getIntValue());
                }
                break;
            case '<':
                getch();
                if (ch == '=') {
                    sym = new Symbol(Symbol.SymbolType.leq.getIntValue());
                    getch();
                } else if (ch == '>') {
                    sym = new Symbol(Symbol.SymbolType.neq.getIntValue());
                    getch();
                } else {
                    sym = new Symbol(Symbol.SymbolType.lss.getIntValue());
                }
                break;
            case '>':
                getch();
                if (ch == '=') {
                    sym = new Symbol(Symbol.SymbolType.geq.getIntValue());
                    getch();
                } else {
                    sym = new Symbol(Symbol.SymbolType.gtr.getIntValue());
                }
                break;
            default:
                sym = new Symbol(Symbol.operatorToIdx.get(ch));
                getch();
        }
        return sym;
    }

    /**
     * 分析保留字或者一般字符
     *
     * @return
     */
    private Symbol AnalysisWords() {
        StringBuffer str = new StringBuffer();
        do {
            str.append(ch);
            getch();
        } while (isDigit(ch) || isAlpha(ch));

        String token = str.toString();
        int idx = Arrays.binarySearch(Symbol.usedWords, token);
        Symbol sym;
        if (idx >= 0) {
            sym = new Symbol(Symbol.usedWordsId[idx]);  // 保留字
        } else {
            sym = new Symbol(Symbol.SymbolType.ident.getIntValue());  // 一般标识符
            sym.name = token;
        }
        return sym;
    }

    /**
     * 分析数字
     * 以String的形式存在sym.content里
     * 不包含负数，只处理整数
     *
     * @return
     */
    private Symbol AnalysisNumber() throws PL0Exception {
        Symbol sym = new Symbol(Symbol.SymbolType.number.getIntValue());
        while (isDigit(ch)) {
            sym.name += ch;
            getch();
        }
        if (sym.name.length() >= numMax) {
            throw new PL0Exception(25);
        }
        return sym;
    }

}
