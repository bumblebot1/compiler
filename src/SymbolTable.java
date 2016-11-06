import java.util.*;
public class SymbolTable{

  Map<String,String> table;
  public SymbolTable(){
    table=new HashMap<String,String>();
  }

  public void setType(String name,String type){
    table.put(name,type);
  }

  public Map<String,String> getTable(){
    return table;
  }
  public String get(String name){
    return table.get(name);
  }
  public boolean containsKey(String name){
    return table.containsKey(name);
  }
}
