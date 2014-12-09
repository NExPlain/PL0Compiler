package com.company;

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
    String currentBuffer;
    public int currentPosition;
    public int currentLineNumber;
    public char currentChar;
    private BufferedReader cin;

    public Scanner(String fileURL) {
        try {
            cin = new BufferedReader(new FileReader(fileURL));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            System.out.println("File not found!");
        }
        currentBuffer = "";
        currentLineNumber = 0;
        currentPosition = 0;
    }

    public char getch() {
        if (currentPosition == currentBuffer.length()) {
            try {
                do {
                    currentBuffer = cin.readLine();
                    currentLineNumber++;
                    currentBuffer.trim();
                    System.out.println(currentBuffer); // 把读入的源程序同时输出到output文件上
                } while (currentBuffer.equals(""));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("reading character error");
            }
            currentBuffer += " ";                     //  加一个空格表示到达行末尾，与下一行的开头分开
            currentPosition = 0;
        }
        return currentChar = currentBuffer.charAt(currentPosition++);
    }

    /**
     * 判断一个字符是否为数字
     * @param x
     * @return
     */
    public boolean isDigit(char x) {
        return x >= '0' && x <= '9';
    }

    /**
     * 判断一个字符是否为英文字母
     * @param x
     * @return
     */
    public boolean isAlpha(char x) {
        return x >= 'a' && x <= 'z' && x >= 'A' && x <= 'Z';
    }

    /**
     * 返回一个单词（忽略空格）
     *
     * @return
     */
    public Symbol getsym() throws Pl0Exception {
        Symbol currentSym = null;
        currentChar = getch();
        while (currentChar == ' ') {
            currentChar = getch();
        }
        if (isDigit(currentChar)) {
            currentSym = AnalysisNumber();
        } else if (isAlpha(currentChar)) {
            currentSym = AnalysisWords();
        } else {
            currentSym = AnalysisOperator();
        }
        return currentSym;
    }

    /**
     * 分析一个操作符
     *
     * @return
     */
    private Symbol AnalysisOperator() {
        Symbol sym = null;
        switch (currentChar) {
            case ':':
                getch();
                if (currentChar == '=') {
                    sym = new Symbol(Symbol.SymbolType.becomes.getIntValue());
                } else {
                    sym = new Symbol(Symbol.SymbolType.nul.getIntValue());
                }
                break;
            case '<':
                getch();
                if (currentChar == '=') {
                    sym = new Symbol(Symbol.SymbolType.leq.getIntValue());
                    getch();
                } else if (currentChar == '>') {
                    sym = new Symbol(Symbol.SymbolType.neq.getIntValue());
                    getch();
                } else {
                    sym = new Symbol(Symbol.SymbolType.lss.getIntValue());
                }
                break;
            case '>':
                getch();
                if (currentChar == '=') {
                    sym = new Symbol(Symbol.SymbolType.geq.getIntValue());
                    getch();
                } else {
                    sym = new Symbol(Symbol.SymbolType.gtr.getIntValue());
                }
                break;
            default:
                sym = new Symbol(Symbol.operatorToIdx.get(currentChar));
                getch();
        }
        return sym;
    }

    /**
     * 分析保留字或者一般字符
     *
     * @return
     */
    private Symbol AnalysisWords(){
        StringBuffer str = new StringBuffer();
        do {
            str.append(currentChar);
            getch();
        } while (isDigit(currentChar) || isAlpha(currentChar));

        String token = str.toString();
        int idx = Arrays.binarySearch(Symbol.usedWords, token);
        Symbol sym;
        if (idx >= 0) {
            sym = new Symbol(Symbol.usedWordsId[idx]);  // 保留字
        } else {
            sym = new Symbol(Symbol.SymbolType.ident.getIntValue());  // 一般标识符
            sym.content = token;
        }
        return sym;
    }

    /**
     * 分析数字
     * 以String的形式存在sym.content里
     * 不包含负数
     *
     * @return
     */
    private Symbol AnalysisNumber() throws Pl0Exception {
        Symbol sym = new Symbol(Symbol.SymbolType.number.getIntValue());
        while (isDigit(currentChar)) {
            sym.content += currentChar;
            getch();
        }
        if(sym.content.length() >= numMax){
            throw new Pl0Exception(25);
        }
        return sym;
    }

}
