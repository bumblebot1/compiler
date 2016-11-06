// COMS22201: Memory allocation for strings

import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;

public class Memory {

  static ArrayList<Byte> memory = new ArrayList<Byte>();
  static HashMap<HashMap<String,Integer>,ArrayList<Byte>> table = new HashMap<HashMap<String,Integer>,ArrayList<Byte>>();


  static public int allocateString(String text)
  {
    int addr = memory.size();
    int size = text.length();
    for (int i=0; i<size; i++) {
      memory.add(new Byte("", text.charAt(i)));
    }
    memory.add(new Byte("", 0));
    return addr;
  }

  //try adding a function that checks if variable has been allocated already
  //numbers are 32 bit wide thus 4 bytes
  //mem locations end with 0
  static public int allocateVariable(int value,String name)
  {
    ArrayList<Byte> byteValue=new ArrayList<Byte>();
    HashMap<String,Integer> nameLocation=new HashMap<String,Integer>();
    HashMap<String,Integer> variableTableEntry=checkVariableInsideTable(name);
    if(variableTableEntry.size()==1){
      int addr=variableTableEntry.get(name);
      for(int i=0;i<4;i++){
        Byte b=new Byte("", value & 255);
        memory.set(addr+i,b);
        byteValue.add(b);
        value=value>>8;
      }
      table.put(variableTableEntry,byteValue);
      return addr;
    }
    else{
      while(memory.size()%4!=0){
        allocateString("");
      }
      int addr = memory.size();
      int size = 4;
      for(int i=0;i<=3;i++){
        Byte b=new Byte("", value & 255);
        memory.add(b);
        byteValue.add(b);
        value=value>>8;
      }
      nameLocation.put(name,addr);
      table.put(nameLocation,byteValue);
      return addr;
    }

  }

  static public int allocateVariable(String name1,String name2)
  {
    ArrayList<Byte> byteValue=new ArrayList<Byte>();
    HashMap<String,Integer> var1nameLocation=new HashMap<String,Integer>();
    ArrayList<Byte> var2Byte=getVariableValueInMem(name2);
    HashMap<String,Integer> variableTableEntry=checkVariableInsideTable(name1);
    if(variableTableEntry.size()==1){
      int addr = variableTableEntry.get(name1);
      int i=0;
      for(Byte b: var2Byte){
        memory.set(addr+i,b);
        byteValue.add(b);
        i+=1;
      }
      table.put(variableTableEntry,byteValue);
      return addr;
    }
    else{
      while(memory.size()%4!=0){
        allocateString("");
      }
      int addr = memory.size();
      for(Byte b: var2Byte){
        memory.add(b);
        byteValue.add(b);
      }
      var1nameLocation.put(name1,addr);
      table.put(var1nameLocation,byteValue);
      return addr;
    }

  }

  static public ArrayList<Byte> getVariableValueInMem(String name){
    for(HashMap<String,Integer> variable:table.keySet()){
      if(variable.containsKey(name))
        return table.get(variable);
    }
    return new ArrayList<Byte>();
  }

  static public int getVariableAddressInMem(String name){
    for(HashMap<String,Integer> variable:table.keySet()){
      if(variable.containsKey(name))
        return variable.get(name);
    }
    return -1;
  }
  static private HashMap<String,Integer> checkVariableInsideTable(String name){
    for(HashMap<String,Integer> variable:table.keySet()){
      if(variable.containsKey(name))
        return variable;
    }
    return new HashMap<String,Integer>();
  }
  static public String getVaraibleNameByLocation(int value){
    for(HashMap<String,Integer> var : table.keySet()){
      if(var.containsValue(value)){
        for(String name:var.keySet()){
          if(var.get(name)==value){
            return name;
          }
        }
      }
    }
    return null;
  }


  static public void dumpData(PrintStream o)
  {
    Byte b;
    String s;
    int c;
    int size = memory.size();
    for (int i=0; i<size; i++) {
      b = memory.get(i);
      c = b.getContents();
      if (c >= 32) {
        s = String.valueOf((char)c);
      }
      else {
        s = ""; // "\\"+String.valueOf(c);
      }
      o.println("DATA "+c+" ; "+s+" "+b.getName());
    }
  }
}

class Byte {
  String varname;
  int contents;

  Byte(String n, int c)
  {
    varname = n;
    contents = c;
  }

  String getName()
  {
    return varname;
  }

  int getContents()
  {
    return contents;
  }
}
