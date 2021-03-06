package pl0compiler.syntaxAnalysis;

import pl0compiler.Compiler;
import pl0compiler.errorHandler.PL0Exception;
import pl0compiler.symbolTables.BinaryTable;
import pl0compiler.symbolTables.HashTable;
import pl0compiler.symbolTables.StackTable;
import pl0compiler.symbolTables.Table;
import pl0compiler.lexicalAnalysis.Scanner;
import pl0compiler.utils.Pcode;
import pl0compiler.pcode.PcodeVM;
import pl0compiler.utils.Symbol;

import java.lang.*;
import java.util.BitSet;

/**
 * Created by lizhen on 14/12/4.
 * 语法分析器，在语法分析过程中处理语义和目标代码生成
 */

public class Parser {
    public Symbol sym;          //当前读到的符号
    public Scanner scan;        //词法分析器
    public Table table;         //符号表
    public PcodeVM pcodeVM;     //pcode虚拟机
    public pl0compiler.errorHandler.Error err;           //对应错误处理机

    /**
     * 声明、语句和因子的FIRST集合
     */
    private BitSet declbegSyms, statebegSyms, facbegSyms;

    /**
     * 桢栈大小（相对本过程的偏移量）
     */
    public int dx = 0;

    public Parser(String inputfilePath){
        this.scan = new Scanner(inputfilePath);
        this.table = new StackTable();
        if(Compiler.MODEID == Compiler.HASHTABLEMODE){
            this.table = new HashTable();
        }else if(Compiler.MODEID == Compiler.BINARYTABLEMODE){
            this.table = new BinaryTable();
        }
        this.pcodeVM = new PcodeVM();
        this.err = new pl0compiler.errorHandler.Error();

        /**
         * 设置声明开始符号集
         * FIRST(declaration)={const var procedure null };
         */
        declbegSyms = new BitSet(Symbol.symbolNumber);
        declbegSyms.set(Symbol.type.constsym.val());
        declbegSyms.set(Symbol.type.varsym.val());
        declbegSyms.set(Symbol.type.procsym.val());

        /**
         * 设置语句开始符号集
         * FIRST(statement)={begin call if while repeat null };
         */
        statebegSyms = new BitSet(Symbol.symbolNumber);
        statebegSyms.set(Symbol.type.beginsym.val());
        statebegSyms.set(Symbol.type.callsym.val());
        statebegSyms.set(Symbol.type.whilesym.val());
        statebegSyms.set(Symbol.type.ifsym.val());
        statebegSyms.set(Symbol.type.repeatsym.val());

        /**
         * 设置因子开始符号集
         * FIRST(factor)={ident,number,( };
         */
        facbegSyms = new BitSet(Symbol.symbolNumber);
        facbegSyms.set(Symbol.type.ident.val());
        facbegSyms.set(Symbol.type.number.val());
        facbegSyms.set(Symbol.type.lparen.val());
    }

    /**
     * 获取下一个语法符号
     */
    public void getsym(){
        try {
            sym = scan.getsym();
        } catch (PL0Exception e) {
            sym = new Symbol(Symbol.type.nul.val());
            e.handle(err, scan);
        }
    }

    public void getsymToEOF(){
        try {
            sym = scan.getsym();
        } catch (PL0Exception e) {
            sym = new Symbol(Symbol.type.nul.val());
            err.outputErrMessage(e.errType, scan.lineNumber, scan.getcc(), scan.getccbuf());
        }
    }

    /**
     * 在自程序出口处，检测下一个取来的符号是否为该语法成分的合法后继符号
     * 若不是则不断跳过，直到后继符号 | 补救符号集合为止
     * @param s1 合法符号集合
     * @param s2 停止符号集合
     * @param n 错误编码
     */
    void test(BitSet s1, BitSet s2, int n) {
        if (!s1.get(sym.symtype)) {
            PL0Exception.handle(n, err, scan);
            s1.or(s2);
            while (!s1.get(sym.symtype) && sym.symtype != Symbol.type.nul.val()) {
                getsym();
            }
        }
    }

    /**
     * <程序> ::= <分程序>. 启动语法分析过程
     */
    public void start(){
        BitSet fsys = new BitSet(Symbol.symbolNumber);
        fsys.or(declbegSyms);
        fsys.or(statebegSyms);
        fsys.set(Symbol.type.period.val());

        block(0, fsys);

        if(sym.symtype != Symbol.type.period.val()){
            PL0Exception.handle(9, err, scan);
        }

        if(sym.symtype == Symbol.type.nul.val())
            getsymToEOF();
        else                                        // 如果文件还没有读完，则以错误36进行判断
            getsym();

        while(!scan.isfileEneded){
            try{
                throw new PL0Exception(41);
            }catch (PL0Exception e){
                e.handle(err,scan);
            }
            scan.readToEOF();                   // 读取到EOF
        }
    }

    /**
     * 分析<常量说明部分>
     * @param lev
     */
    private void parseConstDecProc(int lev) {
        if (sym.symtype == Symbol.type.constsym.val()) {                                   //分析<说明部分>
            getsym();
            do {
                constdeclaration(lev);                                                     //分析<常量定义>
                while (sym.symtype == Symbol.type.comma.val()) {
                    getsym();
                    constdeclaration(lev);
                }
                if (sym.symtype == Symbol.type.semicolon.val()) {
                    getsym();
                } else {
                    PL0Exception.handle(5, err, scan);                                     //漏了逗号或者分号
                }
            } while (sym.symtype == Symbol.type.ident.val());
        }
    }

    /**
     * 分析<变量说明部分>
     * @param lev
     */

    private void parseVarDecProc(int lev) {
        do {
            getsym();
            vardeclaration(lev);
            while (sym.symtype == Symbol.type.comma.val()) {                             //识别{,<标识符>}
                getsym();
                vardeclaration(lev);
            }
            if (sym.symtype == Symbol.type.semicolon.val())
            {
                getsym();
            } else {
                PL0Exception.handle(5, err, scan);                                       // error 5: 漏了逗号或者分号
            }
        }while(sym.symtype == Symbol.type.ident.val());
    }


    /**
     * 分析<过程说明部分>
     * @param fsys
     * @param lev
     */
    private void parseProDecProc(BitSet fsys,int lev){
        BitSet nxtset = new BitSet();
        getsym();
        if (sym.symtype == Symbol.type.ident.val()) {
            try {
                table.enter(sym, Table.type.procedure, lev, this);
            }catch (PL0Exception e){
                e.handle(err, scan);
            }
            getsym();
        } else {
            PL0Exception.handle(4, err, scan);                                     // error 4: procedure后应为标识符
        }
        if (sym.symtype == Symbol.type.semicolon.val())
        {
            getsym();
        } else {
            PL0Exception.handle(5, err, scan);                                     // error 5: 漏了逗号或者分号
        }
        nxtset = (BitSet) fsys.clone();

        //FOLLOW(block)={ ; }
        nxtset.set(Symbol.type.semicolon.val());
        block(lev + 1, nxtset);                                                     // 递归调用，分析下一层分程序

        if (sym.symtype == Symbol.type.semicolon.val()) {

            getsym();

            nxtset = (BitSet) statebegSyms.clone();
            nxtset.set(Symbol.type.ident.val());
            nxtset.set(Symbol.type.procsym.val());
            test(nxtset, fsys, 6);
        } else {
            PL0Exception.handle(5, err, scan);                                      //error 5: 漏了逗号或者分号
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

        BitSet nxtset = new BitSet(Symbol.symbolNumber);

        int dx0 = dx,               //data allocation index
                tx0 = table.tx,     //initial table index
                cx0;            //initial code index

        dx = 3;                     // 存放静态链SL、动态链DL和返回地址RA

        Table.Record record = table.at(table.tx);
        record.reDirectAddr(pcodeVM.cx);
        table.modify(record, table.tx);

        try {
            pcodeVM.gen(Pcode.JMP, 0, 0);
        } catch (PL0Exception e) {
            e.handle(err, scan);
        }

        if (lev > Table.levMax)
        {
            PL0Exception.handle(31, err, scan);                                                //error 31: 嵌套层数过大
        }
        do {
            if (sym.symtype == Symbol.type.constsym.val()) {                                   //分析<说明部分>
                parseConstDecProc(lev);
            }
            if (sym.symtype == Symbol.type.varsym.val()) {                                      //分析<变量说明>
                parseVarDecProc(lev);
            }
            while (sym.symtype == Symbol.type.procsym.val()) {                                   //分析<过程说明>
                parseProDecProc(fsys,lev);
            }
            nxtset = (BitSet) statebegSyms.clone();
            nxtset.set(Symbol.type.ident.val());
            test(nxtset, declbegSyms, 7);                                                    //7:应为语句
            //FIRST(declaration)={const var procedure null };
        } while (declbegSyms.get(sym.symtype));                                             //直到没有声明符号结束

        record = table.at(tx0);
        if(pcodeVM.code[record.adr] == null){
            pcodeVM.code[record.adr] = new Pcode(0,0,0);
        }
        pcodeVM.code[record.adr].a = pcodeVM.cx;                         //过程入口地址填写在code中的jmp 的a参数里
        record.adr = pcodeVM.cx;                                        //当前过程代码地址
        record.size = dx;                                               //dx: 一个procedure中的变量数目+3 ，声明部分中每增加一条声明都会给dx+1
        table.modify(record, tx0);

        cx0 = pcodeVM.cx;                                                                 // 记录地址值，用于重定向这个jmp的a

        try {
            pcodeVM.gen(Pcode.INT, 0, dx);
        } catch (PL0Exception e) {
            e.handle(err, scan);
        }

        nxtset = (BitSet) fsys.clone();                                                    //每个FOLLOW集合都包含上层FOLLOW集合，以便补救
        nxtset.set(Symbol.type.semicolon.val());                                           //语句后跟符号为分号或者end
        nxtset.set(Symbol.type.endsym.val());
        statement(nxtset, lev);

        try {
            pcodeVM.gen(Pcode.OPR, 0, 0);                                                   //每个过程出口都要使用的释放数据段指令
        } catch (PL0Exception e) {
            e.handle(err, scan);
        }

        nxtset = new BitSet(Symbol.symbolNumber);                                           //分程序没有补救集合
        test(fsys, nxtset, 8);                                                              //检测后跟符号正确性


        dx = dx0;                                                                           //恢复堆栈帧计数器
        while(table.tx > tx0){                                                              //回复名字表位置
            table.pop();
        }
    }

    /**
     * 分析<常量定义>
     * <常量定义> ::= <标识符>=<无符号整数>
     *
     * @param lev 当前所在层次
     */
    void constdeclaration(int lev){
        if(sym.symtype == Symbol.type.ident.val()){    // const后面接ident
            String constName = sym.name;

            getsym();
            if(sym.symtype == Symbol.type.eql.val() || sym.symtype == Symbol.type.becomes.val()){     // 等于或者赋值符号
                if(sym.symtype == Symbol.type.becomes.val()){
                    PL0Exception.handle(1, err, scan);  // error 1: 应是=而不是:=
                }
                getsym();                                    // 自动纠正，将:=纠错为=
                if(sym.symtype == Symbol.type.number.val()){
                    sym.content = sym.name;
                    sym.name = constName;
                    try {
                        table.enter(sym, Table.type.constant, lev, this);       //将该常量输入符号表中
                        getsym();
                    } catch (PL0Exception e) {
                        e.handle(err, scan);
                        getsym();
                    }
                }else{
                    PL0Exception.handle(2 , err, scan);            // error 2: 常量=后应为数字
                }
            } else{
                PL0Exception.handle(3, err, scan);               // error 3: 常量说明符后应为=
            }
        }else{
            PL0Exception.handle(4, err, scan);                    // error 4: const后应接标识符
        }
    }

    /**
     * <语句>::= <赋值语句>|<条件语句>|<当型循环语句>|<过程调用语句>|<读语句>|<写语句>|<复合语句>|<重复语句>|<空>
     *
     * FIRST(statement) = {ident, read, write, call, if, while}
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    void statement(BitSet fsys, int lev){
        if(sym.symtype == Symbol.type.ident.val()){                        // <赋值语句>
            parseAssignStatement(fsys, lev);
        }else if(sym.symtype == Symbol.type.readsym.val()){                // <read>
            parseReadStatement(fsys, lev);
        }else if(sym.symtype == Symbol.type.writesym.val()){               // <write>
            parseWriteStatement(fsys, lev);
        }else if(sym.symtype == Symbol.type.callsym.val()){                // <过程调用语句>
            parseCallStatement(fsys, lev);
        }else if(sym.symtype == Symbol.type.ifsym.val()){                  // <条件语句>
            parseIfStatement(fsys, lev);
        }else if(sym.symtype == Symbol.type.beginsym.val()){               // <begin statement>
            parseBeginStatement(fsys, lev);
        }else if(sym.symtype == Symbol.type.whilesym.val()){               // <while>
            parseWhileStatement(fsys, lev);
        }else if(sym.symtype == Symbol.type.repeatsym.val()){              // <repeat>
            parseRepeatStatement(fsys, lev);
        }
        BitSet nxlev = new BitSet(Symbol.symbolNumber);     // 没有停止结合
        test(fsys, nxlev, 19);                              // error 19 : 语句后的符号不正确
    }

    /**
     * 识别<标识符>
     * <变量说明部分>::= var <标识符>{ , <标识符> } ;
     *
     * @param lev
     */
    void vardeclaration(int lev){
        if(sym.symtype == Symbol.type.ident.val()){
            try {
                table.enter(sym, Table.type.variable, lev, this);
            } catch (PL0Exception e) {
                e.handle(err, scan);
            }
            getsym();
        }else{
            PL0Exception.handle(4, err, scan);            // error 4: const, var, procedure 后应为标识符
        }
    }


    /**
     * 分析:=<表达式>
     * <赋值语句> ::= <标识符>:=<表达式>
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void parseAssignStatement(BitSet fsys, int lev) {
        int i = table.position(sym.name);
        if(i == 0){
            PL0Exception.handle(11, err, scan);
        }else if(table.at(i).type != Table.type.variable){
            PL0Exception.handle(12, err, scan);
            i = 0;
        }
        getsym();
        if(sym.symtype == Symbol.type.becomes.val()){
            getsym();
        }else{
            PL0Exception.handle(13, err, scan);
        }
        expression(fsys, lev);
        if(i != 0){
            try {
                Table.Record record = table.at(i);
                pcodeVM.gen(Pcode.STO, lev - record.level, record.adr);
            } catch (PL0Exception e) {
                e.handle(err, scan);
            }
        }
    }


    /**
     * <过程调用语句> ::= call<标识符>
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void parseCallStatement(BitSet fsys, int lev) {
        getsym();
        if (sym.symtype == Symbol.type.ident.val()) {
            int index = table.position(sym.name);
            if (index != 0) {
                Table.Record item = table.at(index);
                if (item.type == Table.type.procedure)                 //检查该标识符的类型是否为procedure
                {
                    try {
                        pcodeVM.gen(Pcode.CAL, lev - item.level, item.adr);
                    } catch (PL0Exception e) {
                        e.handle(err, scan);
                    }
                } else {
                    PL0Exception.handle(15, err, scan);                                        //error 15: 不可调用常量或变量
                }
            } else {
                PL0Exception.handle(11, err, scan);                                             //error 11: 过程调用未找到
            }
            getsym();
        } else {
            PL0Exception.handle(14, err, scan);                                               //error 14: call后应为标识符
        }
    }


    /**
     * 分析<条件语句>
     * <条件语句> ::= if <条件> then <语句>
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void parseIfStatement(BitSet fsys, int lev) {
        getsym();
        BitSet nxtlev = (BitSet) fsys.clone();
        nxtlev.set(Symbol.type.thensym.val());
        nxtlev.set(Symbol.type.dosym.val());

        condition(nxtlev, lev);
        if (sym.symtype == Symbol.type.thensym.val()) {
            getsym();
        } else {
            PL0Exception.handle(16, err, scan);
        }
        int cx1 = pcodeVM.cx;
        try {
            pcodeVM.gen(Pcode.JPC, 0, 0);
        } catch (PL0Exception e) {
            e.handle(err, scan);
        }
        statement(fsys, lev);
        pcodeVM.code[cx1].a = pcodeVM.cx;

        if (sym.symtype == Symbol.type.elsesym.val()) {
            pcodeVM.code[cx1].a++;
            getsym();
            cx1 = pcodeVM.cx;
            try {
                pcodeVM.gen(Pcode.JMP, 0, 0);
            } catch (PL0Exception e) {
                e.handle(err, scan);
            }
            statement(fsys, lev);
            pcodeVM.code[cx1].a = pcodeVM.cx;
        }
    }

    /**
     * <重复语句>::= repeat<语句>{;<语句>}until<条件>
     */
    private void parseRepeatStatement(BitSet fsys, int lev){
        int cx1 = pcodeVM.cx;
        getsym();
        BitSet nxtlev = (BitSet) fsys.clone();
        nxtlev.set(Symbol.type.semicolon.val());
        nxtlev.set(Symbol.type.untilsym.val());
        statement(fsys, lev);

        while(statebegSyms.get(sym.symtype) || sym.symtype == Symbol.type.semicolon.val()){
            if(sym.symtype == Symbol.type.semicolon.val()){
                getsym();
            } else {
                PL0Exception.handle(10, err, scan);
            }
            statement(nxtlev, lev);
        }
        if(sym.symtype == Symbol.type.untilsym.val()){
            getsym();
            condition(fsys, lev);
            try {
                pcodeVM.gen(Pcode.JPC, 0, cx1);
            } catch (PL0Exception e) {
                e.handle(err, scan);
            }
        }else{
            PL0Exception.handle(32, err, scan);
        }
    }

    /**
     * 分析<while循环语句>
     * <while循环语句> ::= while<条件>do<语句>
     *
     * @param fsys FOLLOW符号集
     * @param lev 当前层次
     */
    private void parseWhileStatement(BitSet fsys, int lev) {
        int cx1 = pcodeVM.cx;                                           //保存判断条件操作的位置
        getsym();
        BitSet nxtset = (BitSet) fsys.clone();

        nxtset.set(Symbol.type.dosym.val());                           //后跟符号为do
        condition(nxtset, lev);                                        //分析<条件>

        int cx2 = pcodeVM.cx;
        try {
            pcodeVM.gen(Pcode.JPC, 0, 0);                               //条件跳转
        } catch (PL0Exception e) {
            e.handle(err, scan);
        }
        if (sym.symtype == Symbol.type.dosym.val()) {
            getsym();
        } else {
            PL0Exception.handle(18, err, scan);                         //error 18: 应为do
        }
        statement(fsys, lev);                                          //分析<语句>
        try {
            pcodeVM.gen(Pcode.JMP, 0, cx1);
        } catch (PL0Exception e) {
            e.handle(err, scan);
        }
        pcodeVM.code[cx2].a = pcodeVM.cx;                                //反填跳出循环的地址，与<条件语句>类似
    }

    /**
     * 分析<复合语句>
     * <复合语句> ::= begin<语句>{;<语句>}end
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void parseBeginStatement(BitSet fsys, int lev) {
        getsym();
        BitSet nxtlev = (BitSet) fsys.clone();

        nxtlev.set(Symbol.type.semicolon.val());
        nxtlev.set(Symbol.type.endsym.val());
        statement(nxtlev, lev);

        while (sym.symtype == Symbol.type.semicolon.val() || statebegSyms.get(sym.symtype)) {
            if (sym.symtype == Symbol.type.semicolon.val()) {
                getsym();
            } else {
                PL0Exception.handle(10, err, scan);                                    // error 10: 语句之间漏分号
            }
            statement(nxtlev, lev);
        }
        if (sym.symtype == Symbol.type.endsym.val())
        {
            getsym();
        } else {
            PL0Exception.handle(17, err, scan);                                                  //应为分号或者end
        }
    }


    /**
     * <读语句> ::= read '(' <标识符> { , <标识符> } ')'
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void parseReadStatement(BitSet fsys, int lev) {
        getsym();
        if (sym.symtype == Symbol.type.lparen.val()) {                                            //左括号
            int index = 0;
            do {
                getsym();
                if (sym.symtype == Symbol.type.ident.val()) {
                    index = table.position(sym.name);
                    if (index == 0) {
                        PL0Exception.handle(11, err, scan);                              //error 11: 标识符未声明
                    } else {
                        Table.Record record = table.at(index);
                        if (record.type != Table.type.variable) {
                            PL0Exception.handle(33, err, scan);                          //read()中的标识符不是变量
                        } else {
                            try {
                                pcodeVM.gen(Pcode.RED, lev - record.level, record.adr);
                            } catch (PL0Exception e) {
                                e.handle(err, scan);
                            }
                        }
                    }
                }else{
                    PL0Exception.handle(4, err, scan);
                }
                getsym();
            } while (sym.symtype == Symbol.type.comma.val());
        } else {
            PL0Exception.handle(26, err, scan);                                             // error 26: 应为左括号
        }
        if(sym.symtype != Symbol.type.rparen.val()){
            PL0Exception.handle(22, err, scan);                                              // error 22: 应为右括号
        }else {
            getsym();
        }
    }

    /**
     * <写语句> ::= write '(' <表达式> { , <表达式> } ')'
     * 这里写语句以表达式处理 -> 标识符无法通过部分数据
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void parseWriteStatement(BitSet fsys, int lev) {
        getsym();
        BitSet nxtset = (BitSet) fsys.clone();

        nxtset.set(Symbol.type.rparen.val());
        nxtset.set(Symbol.type.comma.val());

        int index = 0;
        if (sym.symtype == Symbol.type.lparen.val()) {
            do {
                getsym();
                expression(nxtset, lev);
                try{
                    pcodeVM.gen(Pcode.WRT, 0, 0);
                }catch (PL0Exception e){
                    e.handle(err, scan);
                }
            } while (sym.symtype == Symbol.type.comma.val());
            if(sym.symtype != Symbol.type.rparen.val()){
                PL0Exception.handle(22, err, scan);                                              // error 22: 应为右括号
            }else {
                getsym();
            }
        } else {
            PL0Exception.handle(26, err, scan);                                                  // error 26: 应为左括号
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
        BitSet nxtset = (BitSet) fsys.clone();

        nxtset.set(Symbol.type.times.val());
        nxtset.set(Symbol.type.slash.val());

        factor(nxtset, lev);

        while (sym.symtype == Symbol.type.times.val() || sym.symtype == Symbol.type.slash.val()) {
            Symbol mulop = sym;
            getsym();
            factor(nxtset, lev);
            if(mulop.symtype == Symbol.type.times.val()){
                try {
                    pcodeVM.gen(Pcode.OPR, 0, 4);
                } catch (PL0Exception e) {
                    e.handle(err, scan);
                }
            }else{
                try {
                    pcodeVM.gen(Pcode.OPR, 0, 5);
                } catch (PL0Exception e) {
                    e.handle(err, scan);
                }
            }
        }
    }

    /**
     * 分析<因子>
     * <因子>=<标识符>|<无符号整数>|'('<表达式>')'
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void factor(BitSet fsys, int lev) {
        test(facbegSyms, fsys, 24);                                                 // ERROR 24: 表达式不能以此符号开始
        if (facbegSyms.get(sym.symtype)) {
            if (sym.symtype == Symbol.type.ident.val()) {
                int index = table.position(sym.name);
                if (index > 0) {
                    Table.Record record = table.at(index);
                    if(record.type == Table.type.constant){
                        try {
                            pcodeVM.gen(Pcode.LIT, 0, record.value);                     //生成lit指令，把这个数值字面常量放到栈顶
                        } catch (PL0Exception e) {
                            e.handle(err, scan);
                        }
                    }else if(record.type == Table.type.variable){
                        try {
                            pcodeVM.gen(Pcode.LOD, lev - record.level, record.adr);     //取变量放在栈顶
                        } catch (PL0Exception e) {
                            e.handle(err, scan);
                        }
                    }else if(record.type == Table.type.procedure){
                        PL0Exception.handle(21, err, scan);               //表达式内不可有过程标识符
                    }
                } else {
                    PL0Exception.handle(11, err, scan);
                }
                getsym();
            } else if (sym.symtype == Symbol.type.number.val()) {               //因子为数
                int num = Integer.parseInt(sym.name);
                if (num > Table.addrMax) {
                    PL0Exception.handle(34, err, scan);
                    num = 0;
                }
                try {
                    pcodeVM.gen(Pcode.LIT, 0, num);
                } catch (PL0Exception e) {
                    e.handle(err, scan);
                }
                getsym();
            } else if (sym.symtype == Symbol.type.lparen.val()) {                 //因子为表达式：'('<表达式>')'
                getsym();
                BitSet nxtsys = (BitSet) fsys.clone();
                nxtsys.set(Symbol.type.rparen.val());
                expression(nxtsys, lev);
                if (sym.symtype == Symbol.type.rparen.val()) {
                    getsym();
                } else {
                    PL0Exception.handle(22, err, scan);                                   //漏右括号
                }
            }
            BitSet nxtSet = new BitSet(1);
            nxtSet.set(Symbol.type.lparen.val());
            test(fsys, nxtSet, 23);
        }
    }


    /**
     * 分析<表达式>
     * <表达式> ::= [+|-]<项>{<加法运算符><项>}
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void expression(BitSet fsys, int lev) {
        BitSet nxtset = (BitSet) fsys.clone();
        nxtset.set(Symbol.type.plus.val());
        nxtset.set(Symbol.type.minus.val());
        if (sym.symtype == Symbol.type.plus.val() || sym.symtype == Symbol.type.minus.val()) {                                 //分析[+|-]<项>
            Symbol addop = sym;
            getsym();
            term(nxtset, lev);
            if (addop.symtype == Symbol.type.minus.val()) //OPR 0 1:：NEG取反
            {
                try {
                    pcodeVM.gen(Pcode.OPR, 0, 1);
                } catch (PL0Exception e) {
                    e.handle(err, scan);
                }
            }
        } else {
            term(nxtset, lev);
        }

        while (sym.symtype == Symbol.type.plus.val() || sym.symtype == Symbol.type.minus.val()) {
            Symbol addop = sym;
            getsym();
            term(nxtset, lev);
            if(addop.symtype == Symbol.type.plus.val())
                try {
                    pcodeVM.gen(Pcode.OPR, 0, 2);
                } catch (PL0Exception e) {
                    e.handle(err, scan);
                }
            else
                try {
                    pcodeVM.gen(Pcode.OPR, 0, 3);
                } catch (PL0Exception e) {
                    e.handle(err, scan);
                }
        }
    }

    /**
     * 分析<条件>
     * <表达式><关系运算符><表达式>|odd<表达式>
     *
     * @param fsys FOLLOW集合
     * @param lev 当前层次
     */
    private void condition(BitSet fsys, int lev) {
        if (sym.symtype == Symbol.type.oddsym.val()) {
            getsym();
            expression(fsys, lev);
            try {
                pcodeVM.gen(Pcode.OPR, 0, 6);                                //OPR 0 6:判断栈顶元素的odd属性
            } catch (PL0Exception e) {
                e.handle(err, scan);
            }
        } else {
            BitSet nxtset = new BitSet();
            nxtset.set(Symbol.type.eql.val());
            nxtset.set(Symbol.type.neq.val());
            nxtset.set(Symbol.type.lss.val());
            nxtset.set(Symbol.type.leq.val());
            nxtset.set(Symbol.type.gtr.val());
            nxtset.set(Symbol.type.geq.val());
            BitSet nxtset2 = (BitSet) fsys.clone();
            nxtset2.or(nxtset);
            expression(nxtset2, lev);
            if (nxtset.get(sym.symtype)) {
                Symbol relop = sym;
                getsym();
                expression(fsys, lev);
                int reloptype = 0;
                if(relop.symtype == Symbol.type.eql.val()){
                    reloptype = 8;
                }else if(relop.symtype == Symbol.type.neq.val()){
                    reloptype = 9;
                }else if(relop.symtype == Symbol.type.lss.val()){
                    reloptype = 10;
                }else if(relop.symtype == Symbol.type.geq.val()){
                    reloptype = 11;
                }else if(relop.symtype == Symbol.type.gtr.val()){
                    reloptype = 12;
                }else if(relop.symtype == Symbol.type.leq.val()){
                    reloptype = 13;
                }else{
                    outputDebugMessage("not supported type");
                }
                try {
                    pcodeVM.gen(Pcode.OPR, 0, reloptype);
                } catch (PL0Exception e) {
                    e.handle(err, scan);
                }
            } else {
                PL0Exception.handle(20, err, scan);                                 //应为关系运算符
            }
        }
    }

    void outputDebugMessage(String output){
        System.out.println("**** " + output + "***");
    }
}
