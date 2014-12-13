package com.company;

import java.util.BitSet;
import java.util.concurrent.locks.Condition;

/**
 * Created by lizhen on 14/12/4.
 * 语法分析器，在语法分析过程中处理语义和目标代码生成
 */
public class Parser {
    /*说明和语句分别处理*/
    public Symbol sym;     //当前的符号
    public Scanner lex;    //词法分析器
    public SymbolTable table;  //符号表
    public Interpreter interp;  //虚拟机指令
    public Error err;

    /**
     * 声明的First符号集合
     */
    private BitSet declBegSyms;
    /**
     * 语句的First符号集合
     */
    private BitSet stateBegSyms;
    /**
     * 因子开始的First符号集合
     */
    private BitSet facBegSyms;

    /**
     * 当前作用域的堆栈帧大小，或者说是数据大小
     * 计算每个变量在运行栈中相对本过程基地址的偏移量
     * 放在symboltable的address域
     * 生成目标代码放在code中的a域
     */
    private int dx = 0;

    public Parser(Scanner lex, SymbolTable table){
        this.lex = lex;
        this.table = table;
        this.err = new Error();

        /**
         * 设置声明开始符号集
         * <分程序> ::= [<常量说明部分>][<变量说明部分>]{<过程说明部分>}<语句>
         * <常量说明部分> ::= const<常量定义>{,<常量定义>};
         * <变量说明部分>::= var<标识符>{,<标识符>};
         * <过程说明部分> ::= <过程首部>procedure<标识符>; <分程序>;
         * FIRST(declaration)={const var procedure null };
         */
        declBegSyms = new BitSet(Symbol.symbolNumber);
        declBegSyms.set(Symbol.SymbolType.constsym.getIntValue());
        declBegSyms.set(Symbol.SymbolType.varsym.getIntValue());
        declBegSyms.set(Symbol.SymbolType.procsym.getIntValue());

        /**
         * 设置语句开始符号集
         * <语句> ::=<赋值语句>|<条件语句>|<当型循环语句>|<过程调用语句>|<读语句>|<写语句>|<复合语句>|<重复语句>|<空>
         * <赋值语句> ::= <标识符>:=<表达式>
         * <条件语句> ::= if<条件>then<语句>[else<语句>]
         * <当型循环语句> ::= while<条件>do<语句>
         * <重复语句> ::= repeat<语句>{;<语句>}until<条件>
         * <过程调用语句> ::= call<标识符>
         * <复合语句> ::= begin<语句>{;<语句>}end
         * FIRST(statement)={begin call if while repeat null };
         */
        stateBegSyms = new BitSet(Symbol.symbolNumber);
        stateBegSyms.set(Symbol.SymbolType.beginsym.getIntValue());
        stateBegSyms.set(Symbol.SymbolType.callsym.getIntValue());
        stateBegSyms.set(Symbol.SymbolType.whilesym.getIntValue());
        stateBegSyms.set(Symbol.SymbolType.ifsym.getIntValue());
        stateBegSyms.set(Symbol.SymbolType.repeatsym.getIntValue());

        /**
         * 设置因子开始符号集
         * <因子> ::= <标识符>|<无符号整数>|'('<表达式>')'
         * FIRST(factor)={ident,number,( };
         */
        facBegSyms = new BitSet(Symbol.symbolNumber);
        facBegSyms.set(Symbol.SymbolType.ident.getIntValue());
        facBegSyms.set(Symbol.SymbolType.number.getIntValue());
        facBegSyms.set(Symbol.SymbolType.lparen.getIntValue());
    }

    /**
     * 获取下一个语法符号
     */
    public void nextsym(){
        do {
            try {
                sym = lex.getsym();
            } catch (Pl0Exception e) {
                sym = null;
                err.outputErrMessage(e.errType, lex.lineNumber);
            }
        }while(sym == null);
    }

    /**
     * 测试当前符号是否合法 本过程有三个参数，s1、s2为两个符号集合，n为出错代码。
     * 本过程的功能是：测试当前符号（即sym变量中的值）是否在s1集合中， 如果不在，就通过调用出错报告过程输出出错代码n，
     * 并放弃当前符号，通过词法分析过程获取一下单词， 直到这个单词出现在s1或s2集合中为止。 这个过程在实际使用中很灵活，主要有两个用法：
     * 在进入某个语法单位时，调用本过程， 检查当前符号是否属于该语法单位的开始符号集合。 若不属于，则滤去开始符号和后继符号集合外的所有符号。
     * 在语法单位分析结束时，调用本过程， 检查当前符号是否属于调用该语法单位时应有的后继符号集合。 若不属于，则滤去后继符号和开始符号集合外的所有符号。
     * 通过这样的机制，可以在源程序出现错误时， 及时跳过出错的部分，保证语法分析可以继续下去。
     *
     * @param s1 需要的符号
     * @param s2 不需要的符号，添加一个补救集合
     * @param errcode 错误号
     */
    void test(BitSet s1, BitSet s2, int errcode) {
        /**
         * 在某一部分（如一条语句，一个表达式）将要结束时,我们希望下一个符号属于某集合
         * （该部分的FOLLOW集合），test负责这项检测，并且负责当检测不通过时的补救措施，程
         * 序在需要检测时指定当前需要的符号集合和补救用的集合（如之前未完成部分的后跟符 号），以及检测不通过时的错误号。
         */
        if (!s1.get(sym.typeid)) {
            err.outputErrMessage(errcode,lex.lineNumber);
            //当检测不通过时，不停地获取符号，直到它属于需要的集合
            s1.or(s2);                                                     //把s2集合补充进s1集合
            while (!s1.get(sym.typeid)) {
                nextsym();
            }
        }
    }

    /**
     * <程序> ::= <分程序>. 启动语法分析过程
     */
    public void parse(){
        BitSet nxtLev = new BitSet(Symbol.symbolNumber);
        nxtLev.or(declBegSyms);
        nxtLev.or(stateBegSyms);
        nxtLev.set(Symbol.SymbolType.period.getIntValue());

        block(0, nxtLev);           // 解析<分程序>

        if(sym.typeid != Symbol.SymbolType.period.getIntValue()){
            err.outputErrMessage(9, lex.lineNumber);
        }
    }

    /**
     * 分程序分析
     * <分程序>：=[<常数说明部分>][<变量说明部分>]{<过程说明部分>}<语句>
     *
     * @param lev 当前分程序所在层
     * @param fsys 当前模块的FOLLOW集合
     */
    public void block(int lev, BitSet fsys) {
        BitSet nxtlev = new BitSet(Symbol.symbolNumber);

        int dx0 = dx,               //记录本层之前的数据量,以便返回时恢复
                tx0 = table.tablePtr,   //记录本层名字的初始位置
                cx0;

        //置初始值为3的原因是：
        //每一层最开始的位置有三个空间用于存放静态链SL、动态链DL和返回地址RA
        dx = 3;

        //当前pcode代码的地址，传给当前符号表的addr项
        table.getItemAt(table.tablePtr).addr = interp.arrayPtr;                      //在符号表的当前位置记录下这个jmp指令在代码段中的位置
        interp.gen(Pcode.JMP, 0, 0);                             //JMP 0 0

        if (lev > SymbolTable.levMax) //必须先判断嵌套层层数
        {
            err.outputErrMessage(31,lex.lineNumber);                                          //嵌套层数过大
        }
        //分析<说明部分>
        do {
            //<常量说明部分> ::= const<常量定义>{,<常量定义>};
            if (sym.typeid == Symbol.SymbolType.constsym.getIntValue()) {                 //例如const a=0,b=0,... ...,z=0;
                nextsym();
                constdeclaration(lev);                            //分析<常量定义>
                while (sym.typeid == Symbol.SymbolType.comma.getIntValue()) {
                    nextsym();
                    constdeclaration(lev);
                }

                if (sym.typeid == Symbol.SymbolType.semicolon.getIntValue()) //如果是分号，表示常量申明结束
                {
                    nextsym();
                } else {
                    err.outputErrMessage(5,lex.lineNumber);                                     //漏了逗号或者分号
                }
            }

            //分析<变量说明>
            //var<标识符>{,<标识符>};
            if (sym.typeid == Symbol.SymbolType.varsym.getIntValue()) {                       //读入的数为var
                nextsym();
                vardeclaration(lev);                                  //识别<标识符>
                while (sym.typeid == Symbol.SymbolType.comma.getIntValue()) {              //识别{,<标识符>}
                    nextsym();
                    vardeclaration(lev);
                }
                if (sym.typeid == Symbol.SymbolType.semicolon.getIntValue()) //如果是分号，表示变量申明结束
                {
                    nextsym();
                } else {
                    err.outputErrMessage(5,lex.lineNumber);                                       //  漏了逗号或者分号
                }
            }

            /**
             * <过程说明部分> ::=  procedure<标识符>; <分程序> ;
             * FOLLOW(semicolon)={NULL<过程首部>}，
             * 需要进行test procedure a1; procedure 允许嵌套，故用while
             */
            while (sym.typeid == Symbol.SymbolType.procsym.getIntValue()) {                 //如果是procedure
                nextsym();
                if (sym.typeid == Symbol.SymbolType.ident.getIntValue()) {                      //填写符号表
                    try {
                        table.enter(sym, SymbolTable.ItemKind.procedure, lev, dx);                                             //当前作用域的大小
                    }catch (Pl0Exception e){
                        err.outputErrMessage(e.errType, lex.lineNumber);
                    }
                    nextsym();
                } else {
                    err.outputErrMessage(4, lex.lineNumber);                                     //procedure后应为标识符
                }
                if (sym.typeid == Symbol.SymbolType.semicolon.getIntValue())               //分号，表示<过程首部>结束
                {
                    nextsym();
                } else {
                    err.outputErrMessage(5, lex.lineNumber);                                     //漏了逗号或者分号
                }
                nxtlev = (BitSet) fsys.clone();                      //当前模块(block)的FOLLOW集合
                //FOLLOW(block)={ ; }
                nxtlev.set(Symbol.SymbolType.semicolon.getIntValue());
                block(lev + 1, nxtlev);                                  //嵌套层次+1，分析分程序

                if (sym.typeid == Symbol.SymbolType.semicolon.getIntValue()) {                          //<过程说明部分> 识别成功

                    nextsym();
                    //FIRST(statement)={begin call if while repeat null };
                    nxtlev = (BitSet) stateBegSyms.clone();                     //语句的FIRST集合
                    //FOLLOW(嵌套分程序)={ ident , procedure }   TODO 为啥有ident?
                    nxtlev.set(Symbol.SymbolType.ident.getIntValue());
                    nxtlev.set(Symbol.SymbolType.procsym.getIntValue());
                    test(nxtlev, fsys, 6);                             // 测试symtype属于FIRST(statement),
                    //6:过程说明后的符号不正确
                } else {
                    err.outputErrMessage(5, lex.lineNumber);                                    //     漏了逗号或者分号
                }
            }

            /**
             * FIRST(statement)={begin call if while repeat null };
             * FIRST(declaration)={const var procedure null };
             * 一个分程序的说明部分识别结束后，下面可能是语句statement或者嵌套的procedure（first（block）={各种声明}）
             */
            nxtlev = (BitSet) stateBegSyms.clone();
            //FIRST(statement)={ ident }
            nxtlev.set(Symbol.SymbolType.ident.getIntValue());
            test(nxtlev, declBegSyms, 7);                           //7:应为语句
            //FIRST(declaration)={const var procedure null };
        } while (declBegSyms.get(sym.typeid));                     //直到没有声明符号

        //开始生成当前过程代码
        /**
         * 分程序声明部分完成后，即将进入语句的处理， 这时的代码分配指针cx的值正好指向语句的开始位置，
         * 这个位置正是前面的jmp指令需要跳转到的位置
         */
        SymbolTable.Item item = table.getItemAt(tx0);
        interp.pcodeArray[item.addr].a = interp.arrayPtr;//过程入口地址填写在pcodeArray中的jmp 的第二个参数
        item.addr = interp.arrayPtr;       //当前过程代码地址
        item.size = dx;//dx:一个procedure中的变量数目+3 ，声明部分中每增加一条声明都会给dx+1
        //声明部分已经结束，dx就是当前过程的堆栈帧大小
        /**
         * 于是通过前面记录下来的地址值，把这个jmp指令的跳转位置改成当前cx的位置。
         * 并在符号表中记录下当前的代码段分配地址和局部数据段要分配的大小（dx的值）。 生成一条int指令，分配dx个空间，
         * 作为这个分程序段的第一条指令。 下面就调用语句处理过程statement分析语句。
         */
        cx0 = interp.arrayPtr;
        //生成分配内存代码，
        interp.gen(Pcode.INT, 0, dx);

        //打印<说明部分>代码
        table.debugTable(tx0);

        //分析<语句>
        nxtlev = (BitSet) fsys.clone();                                             //每个FOLLOW集合都包含上层FOLLOW集合，以便补救
        nxtlev.set(Symbol.SymbolType.semicolon.getIntValue());                                           //语句后跟符号为分号或者end
        nxtlev.set(Symbol.SymbolType.endsym.getIntValue());
        statement(nxtlev, lev);

        /**
         * 分析完成后，生成操作数为0的opr指令， 用于从分程序返回（对于0层的主程序来说，就是程序运行完成，退出）。
         */
        interp.gen(Pcode.OPR, 0, 0);                                               //每个过程出口都要使用的释放数据段指令

        nxtlev = new BitSet(Symbol.symbolNumber);                                  //分程序没有补救集合
        test(fsys, nxtlev, 8);                                                          //检测后跟符号正确性

        interp.listcode(cx0);

        dx = dx0;                                                                           //恢复堆栈帧计数器
        table.tablePtr = tx0;                                                                     //回复名字表位置
    }

    /**
     * 分析<常量定义>
     * <常量定义> ::= <标识符>=<无符号整数>
     *
     * @param lev 当前所在层次
     */
    void constdeclaration(int lev){
        if(sym.typeid == Symbol.SymbolType.ident.getIntValue()){    // const后面接ident
            String id = sym.content;
            nextsym();
            if(sym.typeid == Symbol.SymbolType.eql.getIntValue() || sym.typeid == Symbol.SymbolType.becomes.getIntValue()){     // 等于或者赋值符号
                if(sym.typeid == Symbol.SymbolType.becomes.getIntValue()){
                    err.outputErrMessage(1, lex.lineNumber);  // error 1: 应是=而不是:=
                }
                nextsym();                                    // 自动纠正，将:=纠错为=
                if(sym.typeid == Symbol.SymbolType.number.getIntValue()){
                    sym.content = id;
                    try {
                        table.enter(sym, SymbolTable.ItemKind.constant, lev, dx);       //将该常量输入符号表中
                    } catch (Pl0Exception e) {
                        err.outputErrMessage(e.errType, lex.lineNumber);
                        e.printStackTrace();
                    }
                }else{
                    err.outputErrMessage(2, lex.lineNumber);            // 常量=后应为数字
                }
            } else{
                err.outputErrMessage(3, lex.lineNumber);                // 常量说明符后应为=
            }
        }else{
            err.outputErrMessage(4, lex.lineNumber);                    // const后应接标识符
        }
    }

    /**
     * 识别<标识符>
     * <变量说明部分>::= var <标识符>{ , <标识符> } ;
     * @param lev
     */
    void vardeclaration(int lev){
        if(sym.typeid == Symbol.SymbolType.ident.getIntValue()){
            /**
             * 填写名字表并改变堆桢栈计数器
             * 符号表中记录下标识符的名字，它所在的层和它所在层的偏移地址
             */
            try {
                table.enter(sym, SymbolTable.ItemKind.variable, lev, dx);
            } catch (Pl0Exception e) {
                err.outputErrMessage(e.errType, lex.lineNumber);
                e.printStackTrace();
            }
            /**
             * 变量定义过程中，会用dx变量记录下局部数据段分配的空间个数
             */
            dx ++;                              // TODO 为啥是+1?说到底dx是做啥的。。
            nextsym();
        }else{
            err.outputErrMessage(4, lex.lineNumber);            // error 4: const, var, procedure 后应为标识符
        }
    }

    /**
     * 分析<语句>
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    void statement(BitSet fsys, int lev){
        //FIRST(statement) = {ident, read, write, call, if, while}
        if(sym.typeid == Symbol.SymbolType.ident.getIntValue()){
            //TODO praseAssignStatement
        }else if(sym.typeid == Symbol.SymbolType.readsym.getIntValue()){
            // TODO praseReadStatement
        }else if(sym.typeid == Symbol.SymbolType.writesym.getIntValue()){
            // TODO praseWriteStatement
        }else if(sym.typeid == Symbol.SymbolType.callsym.getIntValue()){
            // TODO praseCallStatement
        }else if(sym.typeid == Symbol.SymbolType.ifsym.getIntValue()){
            // TODO praseIfStatement
        }else if(sym.typeid == Symbol.SymbolType.beginsym.getIntValue()){
            // TODO praseBeginStatement
        }else if(sym.typeid == Symbol.SymbolType.whilesym.getIntValue()){
            // TODO praseWhileStatement
        }else if(sym.typeid == Symbol.SymbolType.repeatsym.getIntValue()){
            // TODO praseRepeatStatement
        }else{
            BitSet nxlev = new BitSet(Symbol.symbolNumber);
            test(fsys, nxlev, 19);                              // error 19 : 语句后的符号不争取
        }
    }

    /**
     * <重复语句>::= repeat<语句>{;<语句>}until<条件>
     */
    private void praseRepeatStatement(BitSet fsys, int lev){
        int cx1 = interp.arrayPtr;
        nextsym();
        BitSet nxtlev = (BitSet) fsys.clone();
        nxtlev.set(Symbol.SymbolType.semicolon.getIntValue());
        nxtlev.set(Symbol.SymbolType.untilsym.getIntValue());       // 分号或者until终结
        statement(fsys, lev);

        while(stateBegSyms.get(sym.typeid) || sym.typeid == Symbol.SymbolType.semicolon.getIntValue()){
            if(sym.typeid == Symbol.SymbolType.semicolon.getIntValue()){
                nextsym();
            } else {
                err.outputErrMessage(10, lex.lineNumber);           // error 10: 语句之间漏分号
            }

            statement(nxtlev, lev);
        }
        if(sym.typeid == Symbol.SymbolType.untilsym.getIntValue()){     // 到了until
            nextsym();
            condition(fsys, lev);
            interp.gen(Pcode.JPC, 0, cx1);                              // TODO 这尼玛是什么东西
        }else{
            err.outputErrMessage(32, lex.lineNumber);
        }
    }
}
