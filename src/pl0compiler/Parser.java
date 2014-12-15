package pl0compiler;

import java.lang.*;
import java.util.BitSet;

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
     * 计算每个变量在运行栈中相对本过程基地址的偏移量 （注意是相对本过程的偏移量）
     * 放在symboltable的address域
     * 生成目标代码放在code中的a域
     */
    private int dx = 0;

    public Parser(Scanner lex, SymbolTable table, Interpreter interp){
        this.lex = lex;
        this.table = table;
        this.interp = interp;
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
        try {
            sym = lex.getsym();
        } catch (PL0Exception e) {
            sym = new Symbol(Symbol.SymbolType.nul.getIntValue());
            err.outputErrMessage(e.errType, lex.lineNumber);
        }
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
         * 序在需要检测时指定当前需要的符号集合和补救用的集合（如之前未完成部分的后跟符号），以及检测不通过时的错误号。
         */
        System.out.println("test start");
        if (!s1.get(sym.symtype)) {
            err.outputErrMessage(errcode,lex.lineNumber);
            //当检测不通过时，不停地获取符号，直到它属于需要的集合
            s1.or(s2);                                                     //把s2集合补充进s1集合
            while (!s1.get(sym.symtype)) {
                if(sym.symtype == Symbol.SymbolType.nul.getIntValue()){
                    //err.outputErrMessage(errcode, lex.lineNumber);
                    System.out.println("test end");
                    return;
                }
                nextsym();
            }
        }
        System.out.println("test end");
    }

    /**
     * <程序> ::= <分程序>. 启动语法分析过程
     */
    public void parse(){
        BitSet fsys = new BitSet(Symbol.symbolNumber);
        fsys.or(declBegSyms);                                         // 可以是声明开头
        fsys.or(stateBegSyms);                                        // 可以是语句开头
        fsys.set(Symbol.SymbolType.period.getIntValue());

        block(0, fsys);                                               // 解析<分程序>

        if(sym.symtype != Symbol.SymbolType.period.getIntValue()){
            err.outputErrMessage(9, lex.lineNumber);
        }
    }

    /**
     * 分程序分析
     * <分程序>：=[<常数说明部分>][<变量说明部分>]{<过程说明部分>}<语句>
     *
     * FIRST(S):const, var, procedure, ident, if, call, begin, while, read, write
     * FOLLOW(s):. ;
     * @param lev 当前分程序所在层
     * @param fsys 当前模块的FOLLOW集合
     */
    public void block(int lev, BitSet fsys) {

        BitSet nxtlev = new BitSet(Symbol.symbolNumber);

        int dx0 = dx,               //记录本层之前的数据量,以便返回时恢复
                tx0 = table.tablePtr,   //记录本层名字的初始位置0
                cx0;

        //置初始值为3的原因是：
        //每一层最开始的位置有三个空间用于存放静态链SL、动态链DL和返回地址RA
        dx = 3;

        //当前pcode代码的地址，传给当前符号表的addr项（第几条pcode代码）
        table.getItemAt(table.tablePtr).addr = interp.arrayPtr;                                 //在符号表的当前位置tablePtr记录下这个jmp指令在代码段中的位置(即arrayPtr)
        interp.gen(Pcode.JMP, 0, 0);                                                            //JMP 0 0 TODO 不确定是否应是0 0

        if (lev > SymbolTable.levMax) //必须先判断嵌套层层数
        {
            err.outputErrMessage(31,lex.lineNumber);                                          // error 31: 嵌套层数过大
        }
        do {
            //分析<说明部分>
            //<常量说明部分> ::= const<常量定义>{,<常量定义>};
            if (sym.symtype == Symbol.SymbolType.constsym.getIntValue()) {                 //例如const a=0,b=0,... ...,z=0;
                nextsym();
                constdeclaration(lev);                                                     //分析<常量定义>
                nextsym();
                System.out.println("fucking shit");
                while (sym.symtype == Symbol.SymbolType.comma.getIntValue()) {
                    nextsym();
                    constdeclaration(lev);
                    System.out.println("fucking shit");
                    nextsym();
                }
                if (sym.symtype == Symbol.SymbolType.semicolon.getIntValue())                   //如果是分号，表示常量申明结束
                {
                    nextsym();
                } else {
                    err.outputErrMessage(5,lex.lineNumber);                                     //漏了逗号或者分号
                }
                System.out.println("fucking shit");
            }
            //分析<变量说明>
            //var<标识符>{,<标识符>};
            if (sym.symtype == Symbol.SymbolType.varsym.getIntValue()) {                       //读入的数为var
                nextsym();
                vardeclaration(lev);                                                        //识别<标识符>
                while (sym.symtype == Symbol.SymbolType.comma.getIntValue()) {              //识别{,<标识符>}
                    nextsym();
                    vardeclaration(lev);
                }
                if (sym.symtype == Symbol.SymbolType.semicolon.getIntValue()) //如果是分号，表示变量申明结束
                {
                    nextsym();
                } else {
                    err.outputErrMessage(5,lex.lineNumber);                                       // error 5: 漏了逗号或者分号
                }
            }

            /**
             * <过程说明部分> ::=  procedure<标识符>; <分程序> ;
             * FOLLOW(semicolon)={NULL<过程首部>}，
             * 需要进行test procedure a1; procedure 允许嵌套，故用while
             */
            while (sym.symtype == Symbol.SymbolType.procsym.getIntValue()) {                 //如果是procedure
                nextsym();
                if (sym.symtype == Symbol.SymbolType.ident.getIntValue()) {                      //填写符号表
                    try {
                        table.enter(sym, SymbolTable.ItemKind.procedure, lev, dx);                                             //当前作用域的大小
                    }catch (PL0Exception e){
                        err.outputErrMessage(e.errType, lex.lineNumber);
                    }
                    nextsym();
                } else {
                    err.outputErrMessage(4, lex.lineNumber);                                     // error 4: procedure后应为标识符
                }
                if (sym.symtype == Symbol.SymbolType.semicolon.getIntValue())               //分号，表示<过程首部>结束
                {
                    nextsym();
                } else {
                    err.outputErrMessage(5, lex.lineNumber);                                     // error 5: 漏了逗号或者分号
                }
                nxtlev = (BitSet) fsys.clone();                      //当前模块(block)的FOLLOW集合
                //FOLLOW(block)={ ; }
                nxtlev.set(Symbol.SymbolType.semicolon.getIntValue());
                block(lev + 1, nxtlev);                                  //嵌套层次+1，分析分程序

                if (sym.symtype == Symbol.SymbolType.semicolon.getIntValue()) {                          //<过程说明部分> 识别成功

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
        } while (declBegSyms.get(sym.symtype));                     //直到没有声明符号

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
        if(sym.symtype == Symbol.SymbolType.ident.getIntValue()){    // const后面接ident
            String constName = sym.name;
            nextsym();
            if(sym.symtype == Symbol.SymbolType.eql.getIntValue() || sym.symtype == Symbol.SymbolType.becomes.getIntValue()){     // 等于或者赋值符号
                if(sym.symtype == Symbol.SymbolType.becomes.getIntValue()){
                    err.outputErrMessage(1, lex.lineNumber);  // error 1: 应是=而不是:=
                }
                nextsym();                                    // 自动纠正，将:=纠错为=
                if(sym.symtype == Symbol.SymbolType.number.getIntValue()){
                    sym.content = sym.name;
                    sym.name = constName;
                    try {
                        table.enter(sym, SymbolTable.ItemKind.constant, lev, dx);       //将该常量输入符号表中
                    } catch (PL0Exception e) {
                        err.outputErrMessage(e.errType, lex.lineNumber);
                        e.printStackTrace();
                    }
                }else{
                    err.outputErrMessage(2, lex.lineNumber);            // error 2: 常量=后应为数字
                }
            } else{
                err.outputErrMessage(3, lex.lineNumber);                // error 3: 常量说明符后应为=
            }
        }else{
            err.outputErrMessage(4, lex.lineNumber);                    // error 4: const后应接标识符
        }
    }

    /**
     * 识别<标识符>
     * <变量说明部分>::= var <标识符>{ , <标识符> } ;
     * @param lev
     */
    void vardeclaration(int lev){
        if(sym.symtype == Symbol.SymbolType.ident.getIntValue()){
            /**
             * 填写名字表并改变堆桢栈计数器
             * 符号表中记录下标识符的名字，它所在的层和它所在层的偏移地址
             */
            try {
                table.enter(sym, SymbolTable.ItemKind.variable, lev, dx);
            } catch (PL0Exception e) {
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
        if(sym.symtype == Symbol.SymbolType.ident.getIntValue()){
            praseAssignStatement(fsys, lev);
        }else if(sym.symtype == Symbol.SymbolType.readsym.getIntValue()){
            praseReadStatement(fsys, lev);
        }else if(sym.symtype == Symbol.SymbolType.writesym.getIntValue()){
            praseWriteStatement(fsys, lev);
        }else if(sym.symtype == Symbol.SymbolType.callsym.getIntValue()){
            praseCallStatement(fsys, lev);
        }else if(sym.symtype == Symbol.SymbolType.ifsym.getIntValue()){
            praseIfStatement(fsys, lev);
        }else if(sym.symtype == Symbol.SymbolType.beginsym.getIntValue()){
            praseBeginStatement(fsys,lev);
        }else if(sym.symtype == Symbol.SymbolType.whilesym.getIntValue()){
            praseWhileStatement(fsys, lev);
        }else if(sym.symtype == Symbol.SymbolType.repeatsym.getIntValue()){
            praseRepeatStatement(fsys, lev);
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

        while(stateBegSyms.get(sym.symtype) || sym.symtype == Symbol.SymbolType.semicolon.getIntValue()){
            if(sym.symtype == Symbol.SymbolType.semicolon.getIntValue()){
                nextsym();
            } else {
                err.outputErrMessage(10, lex.lineNumber);           // error 10: 语句之间漏分号
            }

            statement(nxtlev, lev);
        }
        if(sym.symtype == Symbol.SymbolType.untilsym.getIntValue()){     // 到了until
            nextsym();
            condition(fsys, lev);
            interp.gen(Pcode.JPC, 0, cx1);                              // TODO cx1是什么东西 以及JPC的条件跳转含义
        }else{
            err.outputErrMessage(32, lex.lineNumber);
        }
    }

    /**
     * 分析<while循环语句>
     * <while循环语句> ::= while<条件>do<语句>
     * 首先用cx1变量记下当前代码段分配位置， 作为循环的开始位置。 然后处理while语句中的条件表达式生成相应代码把结果放在数据栈顶，
     * 再用cx2变量记下当前位置， 生成条件转移指令， 转移位置未知，填0。 通过递归调用语句分析过程分析do语句后的语句或语句块并生成相应代码。
     * 最后生成一条无条件跳转指令jmp，跳转到cx1所指位置， 并把cx2所指的条件跳转指令JPC的跳转位置,改成当前代码段分配位置
     *
     * @param fsys FOLLOW符号集
     * @param lev 当前层次
     */
    private void praseWhileStatement(BitSet fsys, int lev) {
        int cx1 = interp.arrayPtr;                                //保存判断条件操作的位置
        nextsym();
        BitSet nxtlev = (BitSet) fsys.clone();
        //FOLLOW(条件)={ do }

        nxtlev.set(Symbol.SymbolType.dosym.getIntValue());         //后跟符号为do
        condition(nxtlev, lev);                                    //分析<条件>

        int cx2 = interp.arrayPtr;                                 // 保存循环体的结束下一个位置
        interp.gen(Pcode.JPC, 0, 0);                               // 条件跳转 TODO 为啥是 0 0
        if (sym.symtype == Symbol.SymbolType.dosym.getIntValue()) {
            nextsym();
        } else {
            err.outputErrMessage(18, lex.lineNumber);               // error 18: 应为do
        }
        statement(fsys, lev);                                      //分析<语句>
        interp.gen(Pcode.JMP, 0, cx1);                             // TODO 不理解 回头重新判断条件
        interp.pcodeArray[cx2].a = interp.arrayPtr;                //反填跳出循环的地址，与<条件语句>类似
    }

    /**
     * 分析<复合语句>
     * <复合语句> ::= begin<语句>{;<语句>}end 通过循环遍历begin/end语句块中的每一个语句，
     * 通过递归调用语句分析过程分析并生成相应代码。
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void praseBeginStatement(BitSet fsys, int lev) {
        nextsym();
        BitSet nxtlev = (BitSet) fsys.clone();
        //FOLLOW(statement)={ ; end }
        nxtlev.set(Symbol.SymbolType.semicolon.getIntValue());
        nxtlev.set(Symbol.SymbolType.endsym.getIntValue());
        statement(nxtlev, lev);

        //循环分析{;<语句>},直到下一个符号不是语句开始符号或者收到end
        while (stateBegSyms.get(sym.symtype) || sym.symtype == Symbol.SymbolType.semicolon.getIntValue()) {
            if (sym.symtype == Symbol.SymbolType.semicolon.getIntValue()) {
                nextsym();
            } else {
                err.outputErrMessage(10, lex.lineNumber);                                    // error 10: 语句之间漏分号
            }
            statement(nxtlev, lev);
        }
        if (sym.symtype == Symbol.SymbolType.endsym.getIntValue())                           //若为end，则statement解析正确
        {
            nextsym();
        } else {
            err.outputErrMessage(17, lex.lineNumber);                                                  //应为分号或者end
        }
    }


    /**
     * 分析<条件语句>
     * <条件语句> ::= if <条件> then <语句>
     * 按if语句的语法，首先调用逻辑表达式处理过程， 处理if语句的条件，把相应的真假值放到数据栈顶。
     * 接下去记录下代码段分配位置（即下面生成的jpc指令的位置）， 然后生成条件转移jpc指令（遇0或遇假转移）， 转移地址未知暂时填0。
     * 然后调用语句处理过程处理then语句后面的语句或语句块。 then后的语句处理完后， 当前代码段分配指针的位置就应该是上面的jpc指令的转移位置。
     * 通过前面记录下的jpc指令的位置， 把它的跳转位置改成当前的代码段指针位置。
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void praseIfStatement(BitSet fsys, int lev) {
        nextsym();
        BitSet nxtlev = (BitSet) fsys.clone();

        //FOLLOW(condition)={ then do }
        //注释：<当型循环语句> ::= while<条件>do<语句>
        nxtlev.set(Symbol.SymbolType.thensym.getIntValue());
        nxtlev.set(Symbol.SymbolType.dosym.getIntValue());
        condition(nxtlev, lev);                                                    //分析<条件>
        if (sym.symtype == Symbol.SymbolType.thensym.getIntValue()) {
            nextsym();
        } else {
            err.outputErrMessage(16, lex.lineNumber);                   //error 16: 应为then
        }
        int cx1 = interp.arrayPtr;                                                         //保存当前指令地址
        interp.gen(Pcode.JPC, 0, 0);                                            //生成条件跳转指令，跳转地址位置，暂时写0
        statement(fsys, lev);                                                     //处理then后的statement
        interp.pcodeArray[cx1].a = interp.arrayPtr;                                         //经statement处理后，cx为then后语句执行
        //完的位置，它正是前面未定的跳转地址

        if (sym.symtype == Symbol.SymbolType.elsesym.getIntValue()) {
            interp.pcodeArray[cx1].a++;
            nextsym();
            int tmpPtr = interp.arrayPtr;
            interp.gen(Pcode.JMP, 0, 0);
            statement(fsys, lev);
            interp.pcodeArray[tmpPtr].a = interp.arrayPtr;
        }

    }

    /**
     * 分析<标识符>
     * <过程调用语句> ::= call<标识符>
     * 从符号表中找到call语句右部的标识符， 获得其所在层次和偏移地址。 然后生成相应的cal指令。 至于调用子过程所需的保护现场等工作
     * 是由类PCODE解释程序在解释执行cal指令时自动完成的
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void praseCallStatement(BitSet fsys, int lev) {
        nextsym();
        if (sym.symtype == Symbol.SymbolType.ident.getIntValue()) {              //检查符号表中该标识符是否已声明
            int index = table.position(sym.name);
            if (index != 0) {                                                    //若table中无此名字，返回0
                SymbolTable.Item item = table.getItemAt(index);                  //获得名字表某一项的内容
                if (item.kind == SymbolTable.ItemKind.procedure)                 //检查该标识符的类型是否为procedure
                {
                    interp.gen(Pcode.CAL, lev - item.level, item.addr);
                } else {
                    err.outputErrMessage(15, lex.lineNumber);                                        //error 15: 不可调用常量或变量
                }
            } else {
                err.outputErrMessage(11, lex.lineNumber);                                             //error 11: 过程调用未找到
            }
            nextsym();
        } else {
            err.outputErrMessage(14, lex.lineNumber);                                                //error 14: call后应为标识符
        }
    }

    /**
     * 分析'(' <表达式> { , <表达式> } ')'
     * <写语句> ::= write '(' <表达式> { , <表达式> } ')' 在语法正确的前提下，生成指令： 通过循环调用表达式处理过程
     * 分析write语句括号中的每一个表达式， 生成相应指令 保证把表达式的值算出并放到数据栈顶 并生成14号操作的opr指令， 输出表达式的值。
     * 最后生成15号操作的opr指令，输出一个换行
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void praseWriteStatement(BitSet fsys, int lev) {
        nextsym();
        if (sym.symtype == Symbol.SymbolType.lparen.getIntValue()) {
            do {
                nextsym();
                BitSet nxtlev = (BitSet) fsys.clone();
                //FOLLOW={ , ')' }
                nxtlev.set(Symbol.SymbolType.rparen.getIntValue());
                nxtlev.set(Symbol.SymbolType.comma.getIntValue());
                expression(nxtlev, lev);
                interp.gen(Pcode.OPR, 0, 14);                                     //OPR 0 14:输出栈顶的值
            } while (sym.symtype == Symbol.SymbolType.comma.getIntValue());

            if (sym.symtype == Symbol.SymbolType.rparen.getIntValue()) //解析成功
            {
                nextsym();
            } else {
                err.outputErrMessage(22, lex.lineNumber);                                                        // error 22: 漏右括号
            }
        } else {
            err.outputErrMessage(26, lex.lineNumber);                                                  // error 26: 应为左括号
        }
        interp.gen(Pcode.OPR, 0, 15);                                             //OPR 0 15:输出换行
    }

    /**
     * 分析'(' <标识符> { , <标识符> } ')'
     * <读语句> ::= read '(' <标识符> { , <标识符> } ')' 确定read语句语法合理的前提下（否则报错）， 生成相应的指令：
     * 第一条是16号操作的opr指令， 实现从标准输入设备上读一个整数值，放在数据栈顶。 第二条是sto指令，
     * 把栈顶的值存入read语句括号中的变量所在的单元
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void praseReadStatement(BitSet fsys, int lev) {
        nextsym();
        if (sym.symtype == Symbol.SymbolType.lparen.getIntValue()) {                                            //左括号
            int index = 0;
            do {
                nextsym();
                if (sym.symtype == Symbol.SymbolType.ident.getIntValue()) //标识符
                {
                    index = table.position(sym.name);
                }
                if (index == 0) {
                    err.outputErrMessage(11, lex.lineNumber);                              //error 11: 标识符未声明
                } else {
                    SymbolTable.Item item = table.getItemAt(index);
                    if (item.kind != SymbolTable.ItemKind.variable) {                      //判断符号表中的该符号类型是否为变量
                        err.outputErrMessage(33, lex.lineNumber);                                        //read()中的标识符不是变量
                    } else {
                        interp.gen(Pcode.OPR, 0, 16);                            //OPR 0 16:读入一个数据
                        interp.gen(Pcode.STO, lev - item.level, item.addr);   //STO L A;存储变量
                    }
                }
                nextsym();
            } while (sym.symtype == Symbol.SymbolType.comma.getIntValue());
        } else {
            err.outputErrMessage(26, lex.lineNumber);                                              // error 26: 应为左括号
        }
        if (sym.symtype == Symbol.SymbolType.rparen.getIntValue()) //匹配成功！
        {
            nextsym();
        } else {
            err.outputErrMessage(22, lex.lineNumber);                                              // 应为右括号
            while (!fsys.get(sym.symtype)) //sym.symtype!=NULL ??? TODO 这tm是啥
            {
                nextsym();
            }
        }
    }

    /**
     * 分析:=<表达式>
     * <赋值语句> ::= <标识符>:=<表达式>
     * 首先获取赋值号左边的标识符， 从符号表中找到它的信息， 并确认这个标识符确为变量名。 然后通过调用表达式处理过程 算得赋值号右部的表达式的值
     * 并生成相应的指令 保证这个值放在运行期的数据栈顶。 最后通过前面查到的左部变量的位置信息， 生成相应的sto指令， 把栈顶值存入指定的变量的空间，
     * 实现了赋值操作。
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void praseAssignStatement(BitSet fsys, int lev) {
        //从符号表中找到该标识符的信息
        int index = table.position(sym.name);
        if (index > 0) {
            SymbolTable.Item item = table.getItemAt(index);
            if (item.kind == SymbolTable.ItemKind.variable) {                            //标识符
                nextsym();
                if (sym.symtype == Symbol.SymbolType.becomes.getIntValue()) {
                    nextsym();
                } else {
                    err.outputErrMessage(13, lex.lineNumber);                                    //error 13: 应为赋值运算符:=
                }
                BitSet nxtlev = (BitSet) fsys.clone();
                expression(nxtlev, lev);                                                         //解析表达式
                //expression将执行一系列指令，
                //但最终结果将会保存在栈顶，
                //执行sto命令完成赋值
                interp.gen(Pcode.STO, lev - item.level, item.addr);
            } else {
                err.outputErrMessage(12, lex.lineNumber);                                        // error 12: 不可向常量或过程名赋值
            }
        } else {
            err.outputErrMessage(11, lex.lineNumber);                                             // error 11: 标识符未说明
        }
    }

    /**
     * 分析<表达式>
     * <表达式> ::= [+|-]<项>{<加法运算符><项>} 根据PL/0语法可知，
     * 表达式应该是由正负号或无符号开头、由若干个项以加减号连接而成。 而项是由若干个因子以乘除号连接而成， 因子则可能是一个标识符或一个数字，
     * 或是一个以括号括起来的子表达式。 根据这样的结构，构造出相应的过程， 递归调用就完成了表达式的处理。
     * 把项和因子独立开处理解决了加减号与乘除号的优先级问题。 在这几个过程的反复调用中，始终传递fsys变量的值，
     * 保证可以在出错的情况下跳过出错的符号，使分析过程得以进行下去
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void expression(BitSet fsys, int lev) {
        if (sym.symtype == Symbol.SymbolType.plus.getIntValue() || sym.symtype == Symbol.SymbolType.minus.getIntValue()) {                                 //分析[+|-]<项>
            int addOperatorType = sym.symtype;
            nextsym();
            BitSet nxtlev = (BitSet) fsys.clone();
            nxtlev.set(Symbol.SymbolType.plus.getIntValue());
            nxtlev.set(Symbol.SymbolType.minus.getIntValue());
            term(nxtlev, lev);
            if (addOperatorType == Symbol.SymbolType.minus.getIntValue()) //OPR 0 1:：NEG取反
            {
                interp.gen(Pcode.OPR, 0, 1);
            }
            // 如果不是负号就是正号，不需生成相应的指令
        } else {
            BitSet nxtlev = (BitSet) fsys.clone();
            nxtlev.set(Symbol.SymbolType.plus.getIntValue());
            nxtlev.set(Symbol.SymbolType.minus.getIntValue());
            term(nxtlev, lev);
        }

        //分析{<加法运算符><项>}
        while (sym.symtype == Symbol.SymbolType.plus.getIntValue() || sym.symtype == Symbol.SymbolType.minus.getIntValue()) {
            int addOperatorType = sym.symtype;
            nextsym();
            BitSet nxtlev = (BitSet) fsys.clone();
            //FOLLOW(term)={ +,- }
            nxtlev.set(Symbol.SymbolType.plus.getIntValue());
            nxtlev.set(Symbol.SymbolType.minus.getIntValue());
            term(nxtlev, lev);
            interp.gen(Pcode.OPR, 0, addOperatorType);                                    //opr 0 2:执行加法,opr 0 3:执行减法
        }
    }

    /**
     * 分析<项>
     * <项> ::= <因子>{<乘法运算符><因子>}
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void term(BitSet fsys, int lev) {
        //分析<因子>
        BitSet nxtlev = (BitSet) fsys.clone();
        //FOLLOW(factor)={ * /}
        //一个因子后应当遇到乘号或除号
        nxtlev.set(Symbol.SymbolType.times.getIntValue());
        nxtlev.set(Symbol.SymbolType.slash.getIntValue());

        factor(nxtlev, lev);                                                                               //先分析<因子>

        //分析{<乘法运算符><因子>}
        while (sym.symtype == Symbol.SymbolType.times.getIntValue() || sym.symtype == Symbol.SymbolType.slash.getIntValue()) {
            int mulOperatorType = sym.symtype;                                                          //4表示乘法 ,5表示除法
            nextsym();
            factor(nxtlev, lev);
            interp.gen(Pcode.OPR, 0, mulOperatorType);                                        //乘法:OPR 0 4 ,除法:OPR 0 5
        }
    }

    /**
     * 分析<因子>
     * <因子>=<标识符>|<无符号整数>|'('<表达式>')' 开始因子处理前，先检查当前token是否在facbegsys集合中。
     * 如果不是合法的token，抛24号错误，并通过fsys集恢复使语法处理可以继续进行
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void factor(BitSet fsys, int lev) {
        test(facBegSyms, fsys, 24);//                       TODO    !!!!!!!有问题                                    //检测因子的开始符号

        if (facBegSyms.get(sym.symtype)) {
            if (sym.symtype == Symbol.SymbolType.ident.getIntValue()) {                            //因子为常量或变量或者过程名
                int index = table.position(sym.name);
                if (index > 0) {                                               //大于0:找到，等于0:未找到
                    SymbolTable.Item item = table.getItemAt(index);
                                                                                //如果这个标识符对应的是常量，值为val，生成lit指令，把val放到栈顶
                    if(item.kind == SymbolTable.ItemKind.constant){             //名字为常量
                        interp.gen(Pcode.LIT, 0, item.value);                   //生成lit指令，把这个数值字面常量放到栈顶
                    }else if(item.kind == SymbolTable.ItemKind.variable){       //名字为变量
                        interp.gen(Pcode.LOD, lev - item.level, item.addr);     //把位于距离当前层level的层的偏移地址为adrr的变量放到栈顶
                    }else if(item.kind == SymbolTable.ItemKind.procedure){
                        err.outputErrMessage(21, lex.lineNumber);               //表达式内不可有过程标识符
                    }
                } else {
                    err.outputErrMessage(11, lex.lineNumber);                  //标识符未声明
                }
                nextsym();
            } else if (sym.symtype == Symbol.SymbolType.number.getIntValue()) {               //因子为数
                int num = Integer.parseInt(sym.name);
                if (num > SymbolTable.addrMax) {                                   //数越界
                    err.outputErrMessage(34, lex.lineNumber);
                    num = 0;
                }
                interp.gen(Pcode.LIT, 0, num);                     //生成lit指令，把这个数值字面常量放到栈顶
                nextsym();
            } else if (sym.symtype == Symbol.SymbolType.lparen.getIntValue()) {                 //因子为表达式：'('<表达式>')'
                nextsym();
                BitSet nxtlev = (BitSet) fsys.clone();
                //FOLLOW(expression)={ ) }
                nxtlev.set(Symbol.SymbolType.rparen.getIntValue());
                expression(nxtlev, lev);
                if (sym.symtype == Symbol.SymbolType.rparen.getIntValue()) //匹配成功
                {
                    nextsym();
                } else {
                    err.outputErrMessage(22, lex.lineNumber);                                   //漏右括号
                }
            } else //做补救措施
            {
                test(fsys, facBegSyms, 23);                         //一个因子处理完毕，遇到的token应在fsys集合中
            }																			 //如果不是，抛23号错，并找到下一个因子的开始，使语法分析可以继续运行下去
        }
    }

    /**
     * 分析<条件>
     * <表达式><关系运算符><表达式>|odd<表达式>
     * 首先判断是否为一元逻辑表达式：判奇偶。 如果是，则通过调用表达式处理过程分析计算表达式的值， 然后生成判奇指令。
     * 如果不是，则肯定是二元逻辑运算符， 通过调用表达式处理过程依次分析运算符左右两部分的值， 放在栈顶的两个空间中，然后依不同的逻辑运算符，
     * 生成相应的逻辑判断指令，放入代码段。
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void condition(BitSet fsys, int lev) {
        if (sym.symtype == Symbol.SymbolType.oddsym.getIntValue()) {                        //分析ODD<表达式>
            nextsym();
            expression(fsys, lev);
            interp.gen(Pcode.OPR, 0, 6);                        //OPR 0 6:判断栈顶元素是否为奇数
        } else {                                                           //分析<表达式><关系运算符><表达式>
            BitSet nxtlev = (BitSet) fsys.clone();
            //FOLLOW(expression)={  =  !=  <  <=  >  >= }
            nxtlev.set(Symbol.SymbolType.eql.getIntValue());
            nxtlev.set(Symbol.SymbolType.neq.getIntValue());
            nxtlev.set(Symbol.SymbolType.lss.getIntValue());
            nxtlev.set(Symbol.SymbolType.leq.getIntValue());
            nxtlev.set(Symbol.SymbolType.gtr.getIntValue());
            nxtlev.set(Symbol.SymbolType.geq.getIntValue());
            expression(nxtlev, lev);
            if (sym.symtype == Symbol.SymbolType.eql.getIntValue() || sym.symtype == Symbol.SymbolType.neq.getIntValue()
                    || sym.symtype == Symbol.SymbolType.lss.getIntValue() || sym.symtype == Symbol.SymbolType.leq.getIntValue()
                    || sym.symtype == Symbol.SymbolType.gtr.getIntValue() || sym.symtype == Symbol.SymbolType.geq.getIntValue()) {
                int relationOperatorType = sym.symtype;                                                  //预先保存symtype的值
                nextsym();
                expression(fsys, lev);
                interp.gen(Pcode.OPR, 0, relationOperatorType);                                //symtype=eql... leq与7... 13相对应
            } else {
                err.outputErrMessage(20, lex.lineNumber);                                                                              //应为关系运算符
            }
        }
    }

    void debug(String msg) {
        System.out.println("*** DEDUG : " + msg + "  ***");
    }
}
