package com.company;

import sun.util.resources.th.CalendarData_th;

/**
 * Created by lizhen on 14/12/3.
 */
public class Error {
    public static int errCnt = 0;
    public static final String[] errorInfo = new String[]{
            "",
            "1.应是=而不是:=",
            "2.=后应为数",
            "3.标识符后应为=",
            "4.const,var,procedure 后应为标识符",
            "5.漏掉逗号或分号",
            "6.过程说明后的符号不正确",
            "7.应为语句",
            "8.程序体内语句后的符号不正确",
            "9.应为句号",
            "10.语句之间漏分号",
            "11.标识符未说明",
            "12.不可向常量或过程名赋值",
            "13.应为赋值运算符:=",
            "14.call后应为标识符",
            "15.不可调用常量或变量",
            "16.应为then",
            "17.应为分号或end",
            "18.应为do",
            "19.语句后的符号不正确",
            "20.应为关系运算符",
            "21.表达式内不可有过程标识符",
            "22.漏右括号",
            "23.因子后不可为此符号",
            "24.表达式不能以此符号开始",
            "25.这个数太大",
            "26.应该为左括号",
            "27.标识符定义不正确：不能以数字开头",  // 以下开始为自定义的错误类型
            "28.标识符定义不正确：包含不合法符号",
            "29.重复声明变量",
            "30.递归层数超过限制"
        };
    public static void outputErrMessage(int errID, int lineNumber){
        System.out.println("***" + "Error Message at " + lineNumber + " Line : " + errorInfo[errID] + "***");
        errCnt ++ ;
    }
}
