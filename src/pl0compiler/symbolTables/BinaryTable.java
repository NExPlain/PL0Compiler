package pl0compiler.symbolTables;

import javafx.util.Pair;

import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

/**
 *
 * 采用TreeTable套HashSet的方法，存放<变量名，层次> -> 栈序号的键值对
 * 借用TreeTable的红黑树实现二分查找
 * Created by lizhen on 15/1/14.
 */
public class BinaryTable extends Table {

    TreeMap<String, HashSet<Pair<Integer,Integer> > > treeTable;            // 有序符号表，利用TreeMap的红黑树实现
    public BinaryTable(){
        super();
        treeTable = new TreeMap<String, HashSet<Pair<Integer, Integer>>>();
    }

    /**
     * 查找名为name的符号最近出现的位置，在TreeMap里面二分查找，复杂度log(n)
     * @param name
     * @return
     */
    @Override
    public int position(String name) {
        if(treeTable.get(name) == null || treeTable.get(name).size() == 0){
            return 0;
        }
        Iterator i = treeTable.get(name).iterator();
        return ((Pair<Integer,Integer>)i.next()).getValue();
    }

    /**
     * 将符号插入符号表，除了正常的插入栈式符号表外，增加了插入TreeMap的过程
     * @param record
     */
    @Override
    public void enterTable(Record record) {
        String name = record.name;
        Integer level = record.level;
        if(treeTable.get(name) == null){
            HashSet<Pair<Integer, Integer>> hashSet = new HashSet<Pair<Integer, Integer>>();
            hashSet.add(new Pair<Integer,Integer>(-level,tx+1));
            treeTable.put(name, hashSet);
        }else{
            treeTable.get(name).add(new Pair<Integer,Integer>(-level,tx+1));
        }
        tab[++tx] = record;
    }

    /**
     * 从栈式符号表以及TreeMap中删除
     */
    @Override
    public void pop() {
        Record record = at(tx);
        if(treeTable.get(record.name).contains(new Pair<Integer, Integer>(-record.level,tx)))
            treeTable.get(record.name).remove(new Pair<Integer, Integer>(-record.level,tx));
        if(tx <= 0){
            try{
                throw new Exception();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
        tx--;
    }
}
