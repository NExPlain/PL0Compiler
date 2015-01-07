package pl0compiler.lexicalAnalysis;

import pl0compiler.Compiler;
import pl0compiler.errorHandler.PL0Exception;
import pl0compiler.utils.Symbol;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by lizhen on 14/12/3.
 * 词法分析类
 * 调用函数：getsym，返回一个词
 */


public class Scanner {

    public final static int numMax = 14;        // maximum length of numbers
    public final static int al = 10;            // maximum length of identifiers
    private int cc;                             // 当前在行内的位置
    private int ccbuf;                          // \t的增益值
    public int lineNumber;                      // 行号
    public char ch;                             // 当前字符
    public Symbol sym;                          // 当前读到的符号
    public boolean isfileEneded;
    String Buffer;
    private BufferedReader cin;

    public Scanner(String fileURL) {
        try {
            cin = new BufferedReader(new FileReader(fileURL));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            System.out.println("File not found!");
        }
        isfileEneded = false;
        Buffer = "";
        lineNumber = 0;
        cc = ccbuf = 0;
        ccbuf = 0;
        sym = new Symbol(0);
    }

    /**
     * 获取当前扫描位置，由扫描的字符数+\t的增益值决定
     * @return
     */
    public int getcc(){
        return cc + ccbuf - 1;
    }

    /**
     * 读取一个字符，'\0'表示已读到文件末尾
     * @return 返回当前获取到的字符
     */
    public char getch() {
        if(isfileEneded == true){
            return ch = '\0';
        }
        if (cc == Buffer.length()) {
            try {
                do {
                    Buffer = cin.readLine();
                    if(Buffer == null){
                        pl0compiler.Compiler.parser.err.outputErrMessage(36, lineNumber, cc);
                        isfileEneded = true;
                        return ch = '\0';
                    }
                    lineNumber++;
                    Compiler.outputWriter.write("    " + Buffer + "\n");
                    Compiler.outputWriter.flush();                       // 把读入的源程序同时输出到output文件上
                } while (Buffer.equals(""));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("reading files error");
            }
            Buffer += " ";                                         // 加一个空格表示到达行末尾，与下一行的开头分开
            System.out.println("    "+Buffer);
            cc = ccbuf = 0;
        }
        if(cc < Buffer.length())ch = Buffer.charAt(cc++);
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
        Symbol currentSym = new Symbol(Symbol.type.nul.val());
        while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\u0010') {
            if(ch == '\t'){
                ccbuf += 3;
            }
            getch();
        }
        if(ch == '\0'){
            return currentSym;
        }
        if (isDigit(ch)) {
            currentSym = WorkNumber();
        } else if (isAlpha(ch)) {
            currentSym = WorkReservedWords();
        } else {
            currentSym = WorkOperator();
        }
        return sym = currentSym;
    }

    /**
     * 分析一个运算符
     *  := | < | <= | <> | >= | > | SymbolTable中的单目运算符
     * @return
     */
    private Symbol WorkOperator() throws PL0Exception {
        Symbol sym = null;
        switch (ch) {
            case ':':
                getch();
                if (ch == '=') {
                    sym = new Symbol(Symbol.type.becomes.val());
                    getch();
                } else {
                    sym = new Symbol(Symbol.type.nul.val());
                }
                break;
            case '<':
                getch();
                if (ch == '=') {
                    sym = new Symbol(Symbol.type.leq.val());
                    getch();
                } else if (ch == '>') {
                    sym = new Symbol(Symbol.type.neq.val());
                    getch();
                } else {
                    sym = new Symbol(Symbol.type.lss.val());
                }
                break;
            case '>':
                getch();
                if (ch == '=') {
                    sym = new Symbol(Symbol.type.geq.val());
                    getch();
                } else {
                    sym = new Symbol(Symbol.type.gtr.val());
                }
                break;
            default:
                if(Symbol.operatorToIdx.containsKey(ch)) {
                    sym = new Symbol(Symbol.operatorToIdx.get(ch));
                    getch();
                }
                else
                    throw new PL0Exception(37);
        }
        return sym;
    }

    /**
     * 分析保留字或者一般字符
     *
     * @return
     */
    private Symbol WorkReservedWords() {
        StringBuffer str = new StringBuffer();
        do {
            if(str.length() < al)
                str.append(ch);
            getch();
        } while (isDigit(ch) || isAlpha(ch));
        String token = str.toString();

        Symbol sym;
        Object idx = Symbol.reservedWords.get(token);
        if(idx != null){
            sym = new Symbol((Integer)idx);
        }else{
            sym = new Symbol(Symbol.type.ident.val());  // 一般标识符
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
    private Symbol WorkNumber() throws PL0Exception {
        Symbol sym = new Symbol(Symbol.type.number.val());
        while (isDigit(ch)) {
            sym.name += ch;
            getch();
        }
        if (sym.name.length() >= numMax) {
            sym.name = "0";
            throw new PL0Exception(25);
        }
        return sym;
    }

}
