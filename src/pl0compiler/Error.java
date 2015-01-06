package pl0compiler;

import javafx.util.Pair;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by lizhen on 14/12/3.
 */
public class Error {

    private HashMap<javafx.util.Pair<String,Integer>, Integer> rem;
    private static final int errMaxCnt = 100;
    public Error(){
            errCnt = 0;
            rem = new HashMap<Pair<String, Integer>, Integer>();
    }
    public static int errCnt = 0;
    public static final String[] errorInfo = new String[]{
            "",
            "1.应是=而不是:=",
            "2.=后应为数字",
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
            "30.递归层数超过限制",
            "31.嵌套层数过高，应在[1,3]范围内",
            "32.repeat后没有until",
            "33.read语句中需要是变量",
            "34.这个数太大",
            "35.无法识别的标识符",
            "36.程序不完整(program incomplete)",
            "37.无法识别的字符",
            "38.程序过长(program too long)",
            "39.符号表溢出错误",
        };

    public boolean isSystemError(int errID){
        return errID == 39 || errID == 31 || errID == 30 || errID == 36;
    }

    public void outputErrMessage(int errID, int lineNumber, int cc){
        String name = "";
        if(PL0.parser.sym != null)
            name = PL0.parser.sym.name;
        if(redabundant(name,errID))return;
        if(errCnt > errMaxCnt){
            String errMessage = "****";
            errMessage += "编译错误达到上限！";
            System.out.println(errMessage);
            try {
                PL0.outputWriter.write(errMessage + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        String errMessage = "****";
        for(int i = 0 ; i < cc-1 ; i ++){
            errMessage += ' ';
        }
        errMessage += "^" + errorInfo[errID];
        try {
            System.out.println(errMessage);
            PL0.outputWriter.write(errMessage + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        errCnt ++ ;
        errMessage = "Error Message at " + " Line " + lineNumber + " : " + errorInfo[errID];
        try {
            PL0.errWriter.write(errMessage + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        insert(name,errID);
    }

    void insert(String name, int errID){
        if(isSystemError(errID))name = "";
        rem.put(new Pair<String, Integer>(name,errID), 1);
    }

    private boolean redabundant(String name, int errID){
        if(isSystemError(errID))name = "";
        Object idx = rem.get(new Pair<String,Integer>(name,errID));
        if(idx == null)return false;
        else return errID == 11 || errID == 12 || errID == 15 || isSystemError(errID);
    }
}
