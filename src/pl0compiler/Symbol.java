package pl0compiler;

import java.util.BitSet;
import java.util.HashMap;

/**
 * Created by lizhen on 14/12/3.
 * 符号类，用枚举类型存内部的符号表
 */
public class Symbol {

    public static final int symbolNumber = 35;

    public static final HashMap<String, Integer> reservedWords;      // 存放字符名 -> 标号的键值对
    public static final HashMap<Character, Integer> operatorToIdx;   // 存放运算符的符号 --> 对应type id的map

    static {
        operatorToIdx = new HashMap<Character, Integer>();
        operatorToIdx.put('+', type.plus.val());
        operatorToIdx.put('-', type.minus.val());
        operatorToIdx.put('*', type.times.val());
        operatorToIdx.put('/', type.slash.val());
        operatorToIdx.put('(', type.lparen.val());
        operatorToIdx.put(')', type.rparen.val());
        operatorToIdx.put('=', type.eql.val());
        operatorToIdx.put(',', type.comma.val());
        operatorToIdx.put('.', type.period.val());
        operatorToIdx.put(';', type.semicolon.val());

        reservedWords = new HashMap<String, Integer>();
        reservedWords.put("begin",type.beginsym.val());
        reservedWords.put("call",type.callsym.val());
        reservedWords.put("const",type.constsym.val());
        reservedWords.put("do",type.dosym.val());
        reservedWords.put("else",type.elsesym.val());
        reservedWords.put("end",type.endsym.val());
        reservedWords.put("if",type.ifsym.val());
        reservedWords.put("odd",type.oddsym.val());
        reservedWords.put("procedure",type.procsym.val());
        reservedWords.put("read",type.readsym.val());
        reservedWords.put("repeat",type.repeatsym.val());
        reservedWords.put("then",type.thensym.val());
        reservedWords.put("until",type.untilsym.val());
        reservedWords.put("var",type.varsym.val());
        reservedWords.put("while",type.whilesym.val());
        reservedWords.put("write",type.writesym.val());
    }

    public String name;
    public int symtype;
    public String content;

    public Symbol(int symtype) {
        this.symtype = symtype;
        name = "";
        content = "";
    }

    public static String[] typeName = new String[]{
            "nul(0)",   // 0 NULL
            "ident(1)",  // 1 标识符
            "number(2)", // 2 数字
            "plus(3)",   // 3 加号
            "minus(4)",  // 4 减号
            "times(5)",  // 5 乘
            "slash(6)",  // 6 除
            "oddsym(7)", // 7 odd
            "eql(8)",    // 8 等于
            "neq(9)",    // 9 不等于
            "lss(10)",    // 10 小于
            "leq(11)",    // 11 小于等于
            "gtr(12)",    // 12 大于
            "geq(13)",    // 13 大于等于
            "lparen(14)", // 14 左括号
            "rparen(15)", // 15 右括号
            "comma(16)",  // 16 逗号,
            "semicolon(17)", // 17 分号;
            "period(18)",    // 18 句号.
            "becomes(19)",   // 19 赋值符号 :=
            "beginsym(20)",  // 20 开始符号begin
            "endsym(21)",    // 21 结束符号end
            "ifsym(22)",     // 22 if
            "elsesym(23)",   // 23 else
            "thensym(24)",   // 24 then
            "whilesym(25)",  // 25 while
            "dosym(26)",     // 26 do
            "callsym(27)",   // 27 call
            "constsym(28)",  // 28 const
            "varsym(29)",    // 29 var
            "procsym(30)",   // 30 procedure
            "readsym(31)",   // 31 读操作
            "writesym(32)",   // 32 写操作
            "repeatsym(33)",  // 33 repeat
            "untilsym(34)"    // 34 until
    };
    public static enum type {
        nul(0),   // 0 NULL
        ident(1),  // 1 标识符
        number(2), // 2 数字
        plus(3),   // 3 加号
        minus(4),  // 4 减号
        times(5),  // 5 乘
        slash(6),  // 6 除
        oddsym(7), // 7 odd
        eql(8),    // 8 等于
        neq(9),    // 9 不等于
        lss(10),    // 10 小于
        leq(11),    // 11 小于等于
        gtr(12),    // 12 大于
        geq(13),    // 13 大于等于
        lparen(14), // 14 左括号
        rparen(15), // 15 右括号
        comma(16),  // 16 逗号,
        semicolon(17), // 17 分号;
        period(18),    // 18 句号.
        becomes(19),   // 19 赋值符号 :=
        beginsym(20),  // 20 开始符号begin
        endsym(21),    // 21 结束符号end
        ifsym(22),     // 22 if
        elsesym(23),   // 23 else
        thensym(24),   // 24 then
        whilesym(25),  // 25 while
        dosym(26),     // 26 do
        callsym(27),   // 27 call
        constsym(28),  // 28 const
        varsym(29),    // 29 var
        procsym(30),   // 30 procedure
        readsym(31),   // 31 读操作
        writesym(32),   // 32 写操作
        repeatsym(33),  // 33 repeat
        untilsym(34);    // 34 until

        private int enumValue;

        private type(int _enumValue) {
            this.enumValue = _enumValue;
        }

        public int val() {
            return enumValue;
        }
    }
}
