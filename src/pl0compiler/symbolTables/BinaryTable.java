package pl0compiler.symbolTables;

import javafx.util.Pair;

import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Created by lizhen on 15/1/14.
 */
public class BinaryTable extends Table {

    /**
     * 采用TreeTable套HashSet的方法，存放<变量名，层次> -> 栈序号的键值对
     * 借用TreeTable的红黑树实现二分查找
     */
    TreeMap<String, HashSet<Pair<Integer,Integer> > > treeTable;
    public BinaryTable(){
        super();
        treeTable = new TreeMap<String, HashSet<Pair<Integer, Integer>>>();
    }

    @Override
    public int position(String name) {
        if(treeTable.get(name) == null || treeTable.get(name).size() == 0){
            return 0;
        }
        Iterator i = treeTable.get(name).iterator();
        return ((Pair<Integer,Integer>)i.next()).getValue();
    }

    @Override
    public void enterTable(Record record) {
        String name = record.name;
        Integer level = record.level;
        if(treeTable.get(name) == null){
            HashSet<Pair<Integer, Integer>> hashSet = new HashSet<Pair<Integer, Integer>>();
            hashSet.add(new Pair<Integer,Integer>(level,tx+1));
            treeTable.put(name, hashSet);
        }else{
            treeTable.get(name).add(new Pair<Integer,Integer>(level,tx+1));
        }
        tab[++tx] = record;
    }

    @Override
    public void pop() {
        Record record = at(tx);
        if(treeTable.get(record.name).contains(new Pair<Integer, Integer>(record.level,tx)))
            treeTable.get(record.name).remove(new Pair<Integer, Integer>(record.level,tx));
        if(tx <= 0){
            try{
                throw new Exception();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
        tx--;
    }

    @Override
    public void printTable(int start) {

    }
}
