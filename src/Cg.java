// COMS22201: Code generation

import java.util.*;
import java.io.*;
import java.lang.reflect.Array;
import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;

public class Cg
{

  static int nextFree=2;
  static String falseLoc = String.valueOf(Memory.allocateString("false"));
  static String trueLoc = String.valueOf(Memory.allocateString("true"));
  static int boolLabel=0;
  static List<String> assembly = new ArrayList<String>();
  static int maxRegUsed=2;
  static int[] colours;
  static List<String> registerAllocated = new ArrayList<String>();
  static boolean hasReals = false;
  static int[][] interferenceGraph;
  static Map<Integer,HashSet<Integer>> graphList;
  static List<Integer> nodes;


  public static void program(IRTree irt, PrintStream o)
  {
    emit(o, "XOR R0,R0,R0");   // Initialize R0 to 0
    emit(o, "ADDI R1,R0,1");
    statement(irt, o);
    colours = new int[maxRegUsed+1];
    registerAllocation(assembly);
    for(String instruction:registerAllocated){
      emit(o,instruction);
    }
    emit(o, "HALT");           // Program must end with HALT
    Memory.dumpData(o);        // Dump DATA lines: initial memory contents

  }

  private static void statement(IRTree irt, PrintStream o)
  {
    hasReals = false;
    nextFree=3;
    if (irt.getOp().equals("SEQ")) {
      statement(irt.getSub(0), o);
      statement(irt.getSub(1), o);
    }
    else if (irt.getOp().equals("WRS") && irt.getSub(0).getOp().equals("MEM") && irt.getSub(0).getSub(0).getOp().equals("CONST")) {
      String a = irt.getSub(0).getSub(0).getSub(0).getOp();
      assembly.add( "WRS "+a);
    }
    else if (irt.getOp().equals("WR")) {
      hasReals = false;
      checkExpression(irt.getSub(0));
      if(hasReals){
        String e = realExpression(irt.getSub(0), o);;
        assembly.add( "WRR "+e);
      }
      else{
        String e = expression(irt.getSub(0), o);
        assembly.add( "WR "+e);
      }

    }
    else if (irt.getOp().equals("WRR")){
      String e = realExpression(irt.getSub(0),o);
      assembly.add( "WRR "+e);
    }
    else if (irt.getOp().equals("WRB")) {
      hasReals = false;
      checkExpression(irt.getSub(0));
      if(hasReals){
        String result=realExpression(irt.getSub(0), o);
        assembly.add("BNEZR "+result+",l"+(boolLabel));
        assembly.add("WRS "+falseLoc);
        assembly.add("JMP "+"l"+(boolLabel+1));
        assembly.add("l"+(boolLabel)+" : ");
        assembly.add("WRS "+trueLoc);
        assembly.add("l"+(boolLabel+1)+" : ");
        boolLabel=boolLabel+2;
      }
      else{
        String result=expression(irt.getSub(0), o);
        assembly.add("BNEZ "+result+",l"+(boolLabel));
        assembly.add("WRS "+falseLoc);
        assembly.add("JMP "+"l"+(boolLabel+1));
        assembly.add("l"+(boolLabel)+" : ");
        assembly.add("WRS "+trueLoc);
        assembly.add("l"+(boolLabel+1)+" : ");
        boolLabel=boolLabel+2;
      }


    }
    else if (irt.getOp().equals("MOVE")&&irt.getSub(0).getOp().equals("MEM") &&irt.getSub(0).getSub(0).getOp().equals("CONST")){
      if(irt.getSub(1).getOp().equals("MEM") && irt.getSub(1).getSub(0).getOp().equals("CONST")){
        String val0=irt.getSub(0).getSub(0).getSub(0).getOp();
        String val1=irt.getSub(1).getSub(0).getSub(0).getOp();
        String name = Memory.getVaraibleNameByLocation(Integer.parseInt(val1));
        assembly.add( "LOAD R2,R0,"+val1);
        if(irt.getSub(2).getOp().equals("INT")){
          if(Syn.table.get(name).equals("real")){
            assembly.add("RTOI R2,R2");
          }
        }
        else{
          if(Syn.table.get(name).equals("int")){
            assembly.add("ITOR R2,R2");
          }
        }
        assembly.add( "STORE R2,R0,"+val0);
      }
      else{
        String val0 = irt.getSub(0).getSub(0).getSub(0).getOp();
        hasReals = false;
        checkExpression(irt.getSub(1));
        String value;
        if(irt.getSub(2).getOp().equals("INT")){
          if(hasReals){
            value = realExpression(irt.getSub(1),o);
            assembly.add("RTOI "+value+","+value);
          }
          else{
            value = expression(irt.getSub(1),o);
          }
        }
        else{
          if(hasReals){
            value = realExpression(irt.getSub(1),o);

          }
          else{
            value = expression(irt.getSub(1),o);
            assembly.add("ITOR "+value+","+value);
          }
        }

        assembly.add( "STORE "+value+",R0,"+val0);

      }
    }
    else if(irt.getOp().equals("READ") ){
        String value=irt.getSub(0).getSub(0).getOp();
        assembly.add( "RD R2");
        assembly.add( "STORE R2,R0,"+value);
    }
    else if(irt.getOp().equals("READR") ){
        String value=irt.getSub(0).getSub(0).getOp();
        assembly.add( "RDR R2");
        assembly.add( "STORE R2,R0,"+value);
    }
    else if(irt.getOp().equals("CJUMP")){

        if(irt.getSub(0).getSub(0).getOp().equals("TRUE")){
          String result="R1";
          assembly.add( "BEQZ "+result+","+irt.getSub(2).getSub(0).getOp());
        }
        else if(irt.getSub(0).getSub(0).getOp().equals("FALSE")){
          String result="R0";
          assembly.add( "BEQZ "+result+","+irt.getSub(2).getSub(0).getOp());
        }
        else if(irt.getSub(0).getSub(0).getOp().equals("NOT")){
          hasReals = false;
          checkExpression(irt.getSub(0));
          if(hasReals){
              String result = realExpression(irt.getSub(0),o);
              assembly.add( "BEQZR "+result+","+irt.getSub(2).getSub(0).getOp());
          }
          else{
            String result=expression(irt.getSub(0),o);
            assembly.add( "BEQZ "+result+","+irt.getSub(2).getSub(0).getOp());
          }

        }
        else if(irt.getSub(0).getSub(0).getOp().equals("AND")){
          hasReals = false;
          checkExpression(irt.getSub(0));
          if(hasReals){
            String result=realExpression(irt.getSub(0),o);
            assembly.add( "BEQZR "+result+","+irt.getSub(2).getSub(0).getOp());
          }
          else{
            String result=expression(irt.getSub(0),o);
            assembly.add( "BEQZ "+result+","+irt.getSub(2).getSub(0).getOp());
          }

        }
        else if(irt.getSub(0).getOp().equals("BINOP")){
          hasReals=false;
          checkExpression(irt.getSub(0));
          if(hasReals){
            String result=realExpression(irt.getSub(0),o);
            assembly.add( "BEQZR "+result+","+irt.getSub(2).getSub(0).getOp());
          }
          else{
            String result=expression(irt.getSub(0),o);
            assembly.add( "BEQZ "+result+","+irt.getSub(2).getSub(0).getOp());
          }
        }
        else{
          hasReals = false;
          checkExpression(irt.getSub(0));
          if(hasReals){
            String result=realExpression(irt.getSub(0),o);
            assembly.add( "BEQZR "+result+","+irt.getSub(2).getSub(0).getOp());
          }
          else{
            String result=expression(irt.getSub(0),o);
            assembly.add( "BEQZ "+result+","+irt.getSub(2).getSub(0).getOp());
          }
        }

    }
    else if(irt.getOp().equals("LABEL")){
        assembly.add(irt.getSub(0).getOp()+" :");
    }
    else if(irt.getOp().equals("JUMP")){
        assembly.add("JMP "+irt.getSub(0).getSub(0).getOp());
    }
    else if(irt.getOp().equals("SKIP")){
        assembly.add("NOP");
    }
    else {

      error(irt.getOp());
    }
  }

  private static void checkExpression(IRTree irt){
    if(!hasReals){
      if(irt.getOp().equals("REALCONST") || irt.getOp().equals("REAL")){
        hasReals = true;
      }
      else{
        for(int a=0;a<irt.getSubSize();a++){
          checkExpression(irt.getSub(a));
        }
      }
    }
  }


  private static String realExpression(IRTree irt, PrintStream o){
    String result = "";

    if (irt.getOp().equals("CONST")) {
      String t = irt.getSub(0).getOp();
      result = "R"+nextFree;
      if(nextFree>maxRegUsed){
        maxRegUsed=nextFree;
      }
      nextFree=nextFree+1;
      assembly.add( "ADDI "+result+",R0,"+t);
      assembly.add( "ITOR "+result+","+result);
    }
    else if(irt.getOp().equals("REALCONST")){
      String t = irt.getSub(0).getOp();
      result = "R"+nextFree;
      if(nextFree>maxRegUsed){
        maxRegUsed=nextFree;
      }
      nextFree=nextFree+1;
      assembly.add( "MOVIR "+result+","+t);
    }
    else if(irt.getOp().equals("MEM")){
      String t=irt.getSub(0).getSub(0).getOp();
      result = "R"+nextFree;
      if(nextFree>maxRegUsed){
        maxRegUsed=nextFree;
      }
      nextFree=nextFree+1;
      if(irt.getSub(1).getOp().equals("REAL")){
        assembly.add( "LOAD "+result+",R0,"+t);
      }
      else{
        assembly.add( "LOAD "+result+",R0,"+t);
        assembly.add( "ITOR "+result+","+result);
      }


    }
    else if(irt.getOp().equals("BINOP")){
        IRTree t=irt.getSub(0);

        IRTree t1=irt.getSub(1);
        IRTree t2=irt.getSub(2);
        if(t.getOp().equals("ADD")){
            result="R"+nextFree;
            if(nextFree>maxRegUsed){
              maxRegUsed=nextFree;
            }
            nextFree=nextFree+1;
            String r1=realExpression(t1,o);
            String r2=realExpression(t2,o);
            assembly.add("ADDR "+result+","+r1+","+r2);
        }
        else if(t.getOp().equals("SUB")){
            result="R"+nextFree;
            if(nextFree>maxRegUsed){
              maxRegUsed=nextFree;
            }
            nextFree=nextFree+1;
            String r1=realExpression(t1,o);
            String r2=realExpression(t2,o);
            assembly.add("SUBR "+result+","+r1+","+r2);
        }
        else if(t.getOp().equals("MUL")){
            result="R"+nextFree;
            if(nextFree>maxRegUsed){
              maxRegUsed=nextFree;
            }
            nextFree=nextFree+1;
            String r1=realExpression(t1,o);
            String r2=realExpression(t2,o);
            assembly.add("MULR "+result+","+r1+","+r2);
        }
        else if(t.getOp().equals("DIV")){
            result="R"+nextFree;
            if(nextFree>maxRegUsed){
              maxRegUsed=nextFree;
            }
            nextFree=nextFree+1;
            String r1=realExpression(t1,o);
            String r2=realExpression(t2,o);
            assembly.add("DIVR "+result+","+r1+","+r2);
        }
    }
    else if(irt.getOp().equals("RELOP")){
      IRTree t=irt.getSub(0);

      IRTree t1=irt.getSub(1);
      IRTree t2=irt.getSub(2);
      if(t.getOp().equals("EQ")){
          result="R"+nextFree;
          if(nextFree>maxRegUsed){
            maxRegUsed=nextFree;
          }
          nextFree=nextFree+1;
          String r1=realExpression(t1,o);
          String r2=realExpression(t2,o);

          assembly.add("SUBR "+result+","+r1+","+r2);
          assembly.add("BEQZR "+result+",l"+boolLabel);
          assembly.add("MOVIR "+result+",0.0");
          assembly.add("JMP l"+(boolLabel+1));
          assembly.add("l"+(boolLabel)+" : ");
          assembly.add("MOVIR "+result+",1.0");
          assembly.add("l"+(boolLabel+1)+" : ");
          boolLabel+=2;
      }
      else if(t.getOp().equals("LWT")){
          result="R"+nextFree;
          if(nextFree>maxRegUsed){
            maxRegUsed=nextFree;
          }
          nextFree=nextFree+1;
          String r1=realExpression(t1,o);
          String r2=realExpression(t2,o);

          assembly.add("SUBR "+result+","+r2+","+r1);
          assembly.add("BGEZR "+result+",l"+boolLabel);
          assembly.add("MOVIR "+result+",0.0");
          assembly.add("JMP l"+(boolLabel+1));
          assembly.add("l"+(boolLabel)+" : ");
          assembly.add("MOVIR "+result+",1.0");
          assembly.add("l"+(boolLabel+1)+" : ");
          boolLabel+=2;
      }
    }
    else if(irt.getOp().equals("BOOLOP")){
      if(irt.getSub(0).getOp().equals("FALSE")){
        result="R"+nextFree;
        if(nextFree>maxRegUsed){
          maxRegUsed=nextFree;
        }
        assembly.add("MOVIR "+result+",0.0");
        nextFree+=1;
      }
      else if(irt.getSub(0).getOp().equals("TRUE")){
        result="R"+nextFree;
        if(nextFree>maxRegUsed){
          maxRegUsed=nextFree;
        }
        assembly.add("MOVIR "+result+",1.0");
        nextFree+=1;
      }
      else if(irt.getSub(0).getOp().equals("NOT")){
        result=realExpression(irt.getSub(0).getSub(0),o);
        if(nextFree>maxRegUsed){
          maxRegUsed=nextFree;
        }
        assembly.add("BEQZR "+result+",l"+boolLabel);
        assembly.add("MOVIR "+result+",0.0");
        assembly.add("JMP "+"l"+(boolLabel+1));
        assembly.add("l"+boolLabel+" : ");
        assembly.add("MOVIR "+result+","+"1.0");
        assembly.add("l"+(boolLabel+1)+" : ");
        boolLabel+=2;
        nextFree+=1;
      }
      else if(irt.getSub(0).getOp().equals("AND")){
        String result1=realExpression(irt.getSub(0).getSub(0),o);
        String result2=realExpression(irt.getSub(0).getSub(1),o);
        result="R"+nextFree;
        if(nextFree>maxRegUsed){
          maxRegUsed=nextFree;
        }
        assembly.add("BEQZR "+result1+",l"+(boolLabel));
        assembly.add("BEQZR "+result2+",l"+(boolLabel));
        assembly.add("MOVIR "+result+",1.0");
        assembly.add("JMP l"+(boolLabel+1));
        assembly.add("l"+boolLabel+" : ");
        assembly.add("MOVIR "+result+",0.0");
        assembly.add("l"+(boolLabel+1)+" : ");
        boolLabel+=2;
        nextFree+=1;
      }
      else if(irt.getSub(0).getOp().equals("OR")){
        String result1=realExpression(irt.getSub(0).getSub(0),o);
        String result2=realExpression(irt.getSub(0).getSub(1),o);
        result="R"+nextFree;
        if(nextFree>maxRegUsed){
          maxRegUsed=nextFree;
        }
        assembly.add("BNEZR "+result1+",l"+(boolLabel));
        assembly.add("BNEZR "+result2+",l"+(boolLabel));
        assembly.add("MOVIR "+result+",0.0");
        assembly.add("JMP l"+(boolLabel+1));
        assembly.add("l"+boolLabel+" : ");
        assembly.add("MOVIR "+result+",1.0");
        assembly.add("l"+(boolLabel+1)+" : ");
        boolLabel+=2;
        nextFree+=1;
      }
    }
    else {
        error(irt.getOp());
    }
    return result;
  }



  private static String expression(IRTree irt, PrintStream o)
  {
    String result = "";

    if (irt.getOp().equals("CONST")) {
      String t = irt.getSub(0).getOp();
      result = "R"+nextFree;
      if(nextFree>maxRegUsed){
        maxRegUsed=nextFree;
      }
      nextFree=nextFree+1;
      assembly.add( "ADDI "+result+",R0,"+t);
    }
    else if(irt.getOp().equals("MEM")){
      String t=irt.getSub(0).getSub(0).getOp();
      result = "R"+nextFree;
      if(nextFree>maxRegUsed){
        maxRegUsed=nextFree;
      }
      nextFree=nextFree+1;
      if(irt.getSub(1).getOp().equals("REAL")){
        assembly.add( "LOAD "+result+",R0,"+t);
        assembly.add( "RTOI "+result+","+result);
      }
      else{
        assembly.add( "LOAD "+result+",R0,"+t);
      }

    }
    else if(irt.getOp().equals("BINOP")){
        IRTree t=irt.getSub(0);

        IRTree t1=irt.getSub(1);
        IRTree t2=irt.getSub(2);
        if(t.getOp().equals("ADD")){
            result="R"+nextFree;
            if(nextFree>maxRegUsed){
              maxRegUsed=nextFree;
            }
            nextFree=nextFree+1;
            String r1=expression(t1,o);
            String r2=expression(t2,o);
            assembly.add("ADD "+result+","+r1+","+r2);
        }
        else if(t.getOp().equals("SUB")){
            result="R"+nextFree;
            if(nextFree>maxRegUsed){
              maxRegUsed=nextFree;
            }
            nextFree=nextFree+1;
            String r1=expression(t1,o);
            String r2=expression(t2,o);
            assembly.add("SUB "+result+","+r1+","+r2);
        }
        else if(t.getOp().equals("MUL")){
            result="R"+nextFree;
            if(nextFree>maxRegUsed){
              maxRegUsed=nextFree;
            }
            nextFree=nextFree+1;
            String r1=expression(t1,o);
            String r2=expression(t2,o);
            assembly.add("MUL "+result+","+r1+","+r2);
        }
        else if(t.getOp().equals("DIV")){
            result="R"+nextFree;
            if(nextFree>maxRegUsed){
              maxRegUsed=nextFree;
            }
            nextFree=nextFree+1;
            String r1=expression(t1,o);
            String r2=expression(t2,o);
            assembly.add("DIV "+result+","+r1+","+r2);
        }
    }
    else if(irt.getOp().equals("RELOP")){
      IRTree t=irt.getSub(0);

      IRTree t1=irt.getSub(1);
      IRTree t2=irt.getSub(2);
      if(t.getOp().equals("EQ")){
          result="R"+nextFree;
          if(nextFree>maxRegUsed){
            maxRegUsed=nextFree;
          }
          nextFree=nextFree+1;
          String r1=expression(t1,o);
          String r2=expression(t2,o);

          assembly.add("SUB "+result+","+r1+","+r2);
          assembly.add("BEQZ "+result+",l"+boolLabel);
          assembly.add("ADDI "+result+",R0,0");
          assembly.add("JMP l"+(boolLabel+1));
          assembly.add("l"+(boolLabel)+" : ");
          assembly.add("ADDI "+result+",R0,1");
          assembly.add("l"+(boolLabel+1)+" : ");
          boolLabel+=2;
      }
      else if(t.getOp().equals("LWT")){
          result="R"+nextFree;
          if(nextFree>maxRegUsed){
            maxRegUsed=nextFree;
          }
          nextFree=nextFree+1;
          String r1=expression(t1,o);
          String r2=expression(t2,o);

          assembly.add("SUB "+result+","+r2+","+r1);
          assembly.add("BGEZ "+result+",l"+boolLabel);
          assembly.add("ADDI "+result+",R0,0");
          assembly.add("JMP l"+(boolLabel+1));
          assembly.add("l"+(boolLabel)+" : ");
          assembly.add("ADDI "+result+",R0,1");
          assembly.add("l"+(boolLabel+1)+" : ");
          boolLabel+=2;
      }
    }
    else if(irt.getOp().equals("BOOLOP")){
      if(irt.getSub(0).getOp().equals("FALSE")){
        result="R"+nextFree;
        if(nextFree>maxRegUsed){
          maxRegUsed=nextFree;
        }
        assembly.add("ADDI "+result+",R0,0");
        nextFree+=1;
      }
      else if(irt.getSub(0).getOp().equals("TRUE")){
        result="R"+nextFree;
        if(nextFree>maxRegUsed){
          maxRegUsed=nextFree;
        }
        assembly.add("ADDI "+result+",R0,1");
        nextFree+=1;
      }
      else if(irt.getSub(0).getOp().equals("NOT")){
        result=expression(irt.getSub(0).getSub(0),o);
        if(nextFree>maxRegUsed){
          maxRegUsed=nextFree;
        }
        assembly.add("BEQZ "+result+",l"+boolLabel);
        assembly.add("ADDI "+result+",R0,0");
        assembly.add("JMP "+"l"+(boolLabel+1));
        assembly.add("l"+boolLabel+" : ");
        assembly.add("ADDI "+result+",R0,1");
        assembly.add("l"+(boolLabel+1)+" : ");
        boolLabel+=2;
        nextFree+=1;
      }
      else if(irt.getSub(0).getOp().equals("AND")){
        String result1=expression(irt.getSub(0).getSub(0),o);
        String result2=expression(irt.getSub(0).getSub(1),o);
        result="R"+nextFree;
        if(nextFree>maxRegUsed){
          maxRegUsed=nextFree;
        }
        assembly.add("BEQZ "+result1+",l"+(boolLabel));
        assembly.add("BEQZ "+result2+",l"+(boolLabel));
        assembly.add("ADDI "+result+",R0,1");
        assembly.add("JMP l"+(boolLabel+1));
        assembly.add("l"+boolLabel+" : ");
        assembly.add("ADDI "+result+",R0,0");
        assembly.add("l"+(boolLabel+1)+" : ");
        boolLabel+=2;
        nextFree+=1;
      }
      else if(irt.getSub(0).getOp().equals("OR")){
        String result1=expression(irt.getSub(0).getSub(0),o);
        String result2=expression(irt.getSub(0).getSub(1),o);
        result="R"+nextFree;
        if(nextFree>maxRegUsed){
          maxRegUsed=nextFree;
        }
        assembly.add("BNEZ "+result1+",l"+(boolLabel));
        assembly.add("BNEZ "+result2+",l"+(boolLabel));
        assembly.add("ADDI "+result+",R0,0");
        assembly.add("JMP l"+(boolLabel+1));
        assembly.add("l"+boolLabel+" : ");
        assembly.add("ADDI "+result+",R0,1");
        assembly.add("l"+(boolLabel+1)+" : ");
        boolLabel+=2;
        nextFree+=1;
      }
    }
    else {
        error(irt.getOp());
    }
    return result;
  }

  private static void emit(PrintStream o, String s)
  {
    o.println(s);
  }

  private static void error(String op)
  {
    System.out.println("CG error: "+op);
    System.exit(1);
  }

  private static void registerAllocation(List<String> assembly){

    buildGraph(assembly);

    boolean finished = false;
    int x=3;
    while(!finished){
      resetColouring();
      colour_graph(x,interferenceGraph,graphList,nodes);
      finished = checkColouringFinished();
      x++;
    }

    for(String s:assembly){
      int i=2;
      String[] parts = s.split("\\s+");
      if(parts.length>1){
        String[] registers=parts[1].split(",");
        for(int j=0;j <= registers.length-1;j++){
          if(registers[j].contains("R")){
            int value = Integer.parseInt(registers[j].replace("R",""));
            registers[j]="R"+colours[value];
          }
        }
        String replaced=parts[0]+" ";
        for(int j=0;j <= registers.length-1;j++){
          if(j==registers.length-1){
            replaced=replaced+registers[j];
          }
          else{
            replaced=replaced+registers[j]+",";
          }
        }
        s=replaced;
      }
      registerAllocated.add(s);
    }
  }

  private static void buildGraph(List<String> assembly){
    interferenceGraph=new int[maxRegUsed+1][maxRegUsed+1];
    graphList=new HashMap<Integer,HashSet<Integer>>();
    nodes = new ArrayList<Integer>();


    for(int i = 0; i <= maxRegUsed; i++){
      if(i>1){
        nodes.add(i);
      }
      HashSet<Integer> neighbours=new HashSet<Integer>();
      graphList.put(i,neighbours);
    }


    String instruction;
    //LIVENESS ANALYSIS BEGINS

    List<HashMap<Integer,HashSet<Integer>>> analysisResults = livenessAnalysis(assembly);

    Map<Integer,HashSet<Integer>> in = analysisResults.get(0);
    Map<Integer,HashSet<Integer>> out = analysisResults.get(1);

    //LIVENESS ANALYSIS ENDS

    //Generate interference graph

    for(int i=0;i<=assembly.size()-1;i++){
      //need to check only the arithmetic ones i.e adds sub div mul addi
      instruction = assembly.get(i);
      if(instruction.contains("ADDI")){
        String[] parts = instruction.split("\\s+");
        String registers = parts[1].replaceAll("R","");
        String[] indexes = registers.split(",");
        int current = Integer.parseInt(indexes[0]);
        Set<Integer> edges = graphList.get(current);
        for(int v : out.get(i)){
          if(v!=current){
            interferenceGraph[v][current]=1;
            interferenceGraph[current][v]=1;
            edges.add(v);
            HashSet<Integer> other=graphList.get(v);
            other.add(current);
          }
        }
      }
      else{
        //nonmove instruction
        if(instruction.contains("ADD")){
          String[] parts = instruction.split("\\s+");
          String registers = parts[1].replaceAll("R","");
          String[] indexes = registers.split(",");
          int current = Integer.parseInt(indexes[0]);
          Set<Integer> edges = graphList.get(current);
          if(instruction.contains("R0")){
            for(int  v : out.get(i)){
              if(v!=current){
                interferenceGraph[v][current]=1;
                interferenceGraph[current][v]=1;
                edges.add(v);
                HashSet<Integer> other=graphList.get(v);
                other.add(current);
              }
            }
          }
          else{
            int index1 = Integer.parseInt(indexes[1]);
            int index2 = Integer.parseInt(indexes[2]);
            if(index1!=index2){
              interferenceGraph[index1][index2]=1;
              interferenceGraph[index2][index1]=1;
              Set<Integer> first = graphList.get(index1);
              Set<Integer> second = graphList.get(index2);
              first.add(index2);
              second.add(index1);
            }
            for(int v : out.get(i)){
              if(v!=current){
                interferenceGraph[v][current]=1;
                interferenceGraph[current][v]=1;
                edges.add(v);
                HashSet<Integer> other=graphList.get(v);
                other.add(current);
              }
            }
          }
        }
        else if(instruction.contains("SUB")||instruction.contains("DIV")||instruction.contains("MUL")|| instruction.contains("SUBR")||instruction.contains("DIVR")||instruction.contains("MULR")){
          String[] parts = instruction.split("\\s+");
          String registers = parts[1].replaceAll("R","");
          String[] indexes = registers.split(",");
          int current = Integer.parseInt(indexes[0]);
          Set<Integer> edges = graphList.get(current);
          int index1 = Integer.parseInt(indexes[1]);
          int index2 = Integer.parseInt(indexes[2]);
          if(index1!=index2){
            interferenceGraph[index1][index2]=1;
            interferenceGraph[index2][index1]=1;
            Set<Integer> first = graphList.get(index1);
            Set<Integer> second = graphList.get(index2);
            first.add(index2);
            second.add(index1);
          }
          for(int v : out.get(i)){
            if(v!=current){
              interferenceGraph[v][current]=1;
              interferenceGraph[current][v]=1;
              edges.add(v);
              HashSet<Integer> other=graphList.get(v);
              other.add(current);
            }
          }
        }
        else{
          if(instruction.contains("ITOR")||instruction.contains("RTOI")){
            //actual move instructions
            String[] parts = instruction.split("\\s+");
            String registers = parts[1].replaceAll("R","");
            String[] indexes = registers.split(",");
            int current = Integer.parseInt(indexes[0]);
            int second = Integer.parseInt(indexes[1]);

            Set<Integer> edges = graphList.get(current);
            for(int v : out.get(i)){
              if(v!=current && v!=second){
                interferenceGraph[v][current]=1;
                interferenceGraph[current][v]=1;
                edges.add(v);
                HashSet<Integer> other=graphList.get(v);
                other.add(current);
              }
            }
          }
          else{
            if(instruction.contains("MOVIR")){
              String[] parts = instruction.split("\\s+");
              String registers = parts[1].replaceAll("R","");
              String[] indexes = registers.split(",");
              int current = Integer.parseInt(indexes[0]);
              Set<Integer> edges = graphList.get(current);
              for(int v : out.get(i)){
                if(v!=current){
                  interferenceGraph[v][current]=1;
                  interferenceGraph[current][v]=1;
                  edges.add(v);
                  HashSet<Integer> other=graphList.get(v);
                  other.add(current);
                }
              }
            }
            else{
              if(instruction.contains("LOAD")){
                String[] parts = instruction.split("\\s+");
                String registers = parts[1].replaceAll("R","");
                String[] indexes = registers.split(",");
                int current = Integer.parseInt(indexes[0]);
                Set<Integer> edges = graphList.get(current);
                for(int v : out.get(i)){
                  if(v!=current){
                    interferenceGraph[v][current]=1;
                    interferenceGraph[current][v]=1;
                    edges.add(v);
                    HashSet<Integer> other=graphList.get(v);
                    other.add(current);
                  }
                }
              }
              else{
                /*dont care about the other instructions
                since they do not repesent moves*/
              }
            }
          }
        }
      }
    }

  }

  private static void colour_graph(int k, int[][] interferenceGraph,Map<Integer,HashSet<Integer>> graphList,List<Integer> nodes){

    int v = 0;
    int colour;
    if(nodes.size()!=0){
      boolean graphHasNodeWithDegLessThanK = true;
      boolean found = false;
      for(int n : nodes){
        int deg = graphList.get(n).size();
        if(deg < k){
          v=n;
          found=true;
          break;
        }
      }
      if(!found){
        graphHasNodeWithDegLessThanK = false;
      }

      if(!graphHasNodeWithDegLessThanK){
        v=nodes.get(0);
      }
      nodes.remove((Object)v);
      colour_graph(k,interferenceGraph,graphList,nodes);
      nodes.add(v);
      if(graphList.get(v).size() < k){
        colour=2;
        List<Integer> set = new ArrayList<Integer>(graphList.get(v));
        for(int i = 0 ; i <= set.size()-1; i++){
          int neighbour = set.get(i);
          if(colours[neighbour] == colour){
            colour++;
            i = 0;
          }
        }
        colours[v] = colour;
      }
      else{
        colour=2;
        List<Integer> set = new ArrayList<Integer>(graphList.get(v));
        for(int i = 0 ; i <= set.size()-1; i++){
          int neighbour = set.get(i);
          if(colours[neighbour] == colour){
            colour++;
            i = 0;
          }
        }
        if(colour < k){
          colours[v] = colour;
        }
        else{
          colours[v] = -1; //actual spill
        }
      }

    }

  }

  private static void resetColouring(){
    colours[0]=0;
    colours[1]=1;
    for(int i=2;i<=maxRegUsed;i++){
      colours[i]=0;
    }
  }

  private static boolean checkColouringFinished(){
    for(int i=0 ; i <= maxRegUsed ; i++){
      if(colours[i]==-1)
        return false;
    }
    return true;
  }

  private static boolean condition(Map<Integer,HashSet<Integer>> in,Map<Integer,HashSet<Integer>> out, Map<Integer,HashSet<Integer>> inprime, Map<Integer,HashSet<Integer>> outprime){
    for(Integer i: in.keySet()){
      if(!inprime.get(i).containsAll(in.get(i)) || !outprime.get(i).containsAll(out.get(i)))
        return false;
    }
    return true;
  }


  private static List<HashMap<Integer,HashSet<Integer>>> livenessAnalysis(List<String>assembly){
     String instruction;
     List<HashMap<Integer,HashSet<Integer>>> toReturn=new ArrayList<HashMap<Integer,HashSet<Integer>>>();

     HashMap<Integer,HashSet<Integer>> in = new HashMap<Integer,HashSet<Integer>>();
     HashMap<Integer,HashSet<Integer>> out = new HashMap<Integer,HashSet<Integer>>();
     HashMap<Integer,HashSet<Integer>> inprime = new HashMap<Integer,HashSet<Integer>>();
     HashMap<Integer,HashSet<Integer>> outprime = new HashMap<Integer,HashSet<Integer>>();

     for(int i = 0; i <= assembly.size()-1; i++){
       in.put(i,new HashSet<Integer>());
       inprime.put(i,new HashSet<Integer>());
       out.put(i,new HashSet<Integer>());
       outprime.put(i,new HashSet<Integer>());
     }

     Map<String,Integer> labelLocations = createLabelLocations(assembly);
     Map<Integer,HashSet<Integer>> succ = createSuccessors(assembly,labelLocations);

     do{
       for(int i = assembly.size()-1; i >= 0; i--){
         inprime.put(i,in.get(i));
         outprime.put(i,out.get(i));

         HashSet<Integer> union=new HashSet<Integer>();

         for(Integer s : succ.get(i)){
           union.addAll(in.get(s));
         }

         out.put(i,union);

         HashSet<Integer> use = new HashSet<Integer>();
         HashSet<Integer> def = new HashSet<Integer>();

         instruction=assembly.get(i);
         String[] parts = instruction.split("\\s+");
         HashSet<Integer> difference=new HashSet<Integer>();


         if(!instruction.contains(":") && !instruction.contains("WRS") && !instruction.contains("NOP") && !instruction.contains("JMP")){

           String registers = parts[1].replaceAll("R","");
           if(!instruction.contains("BGEZ") && !instruction.contains("BEQZ") && !instruction.contains("BNEZ")  && !instruction.contains("WR") && !instruction.contains("BGEZR") && !instruction.contains("BEQZR") && !instruction.contains("BNEZR")  && !instruction.contains("WRR") ){
             //these instructions may define registers and may or may not use some aswell
             if(!instruction.contains("RD") && !instruction.contains("RDR")){
               if((instruction.contains("ADD") || instruction.contains("SUB") || instruction.contains("MUL") || instruction.contains("DIV")||instruction.contains("ADDR") || instruction.contains("SUBR") || instruction.contains("MULR") || instruction.contains("DIVR")) && !instruction.contains("ADDI")){

                 String[] indexes = registers.split(",");
                 def.add(Integer.parseInt(indexes[0]));

                 use.add(Integer.parseInt(indexes[1]));
                 use.add(Integer.parseInt(indexes[2]));

                 difference=new HashSet<Integer>(out.get(i));
                 difference.removeAll(def);

                 use.addAll(difference);
                 in.put(i,use);

               }
               else{
                 if(instruction.contains("LOAD") || instruction.contains("ADDI") || instruction.contains("ITOR") || instruction.contains("RTOI")){
                   String[] indexes = registers.split(",");

                   use.add(Integer.parseInt(indexes[1]));
                   def.add(Integer.parseInt(indexes[0]));

                   difference = new HashSet<Integer>(out.get(i));
                   difference.removeAll(def);

                   use.addAll(difference);

                   in.put(i,use);
                 }
                 else{
                   if(!instruction.contains("MOVIR")){
                     //only STORE instruction is left
                     String[] indexes = registers.split(",");
                     use.add(Integer.parseInt(indexes[0]));
                     use.add(Integer.parseInt(indexes[1]));

                     difference = new HashSet<Integer>(out.get(i));

                     use.addAll(difference);
                     in.put(i,use);
                   }
                   else{
                     String[] indexes = registers.split(",");
                     def.add(Integer.parseInt(indexes[0]));

                     difference = new HashSet<Integer>(out.get(i));

                     difference.removeAll(def);
                     in.put(i,difference);
                   }
                 }
               }

             }
             else{
               //read only defines one register and never uses any
               def.add(Integer.parseInt(registers));
               difference=new HashSet<Integer>(out.get(i));
               difference.removeAll(def);
               in.put(i,difference);
             }
           }
           else{
             //branch instructions and write ints only use registers and do not define them
             int regValue=Integer.parseInt(registers.split(",")[0]);
             use.add(regValue);
             use.addAll(out.get(i));
             in.put(i,use);
           }

         }
         else{
           //labels, skips and write strings do not use registers at al
           in.put(i,out.get(i));
         }


       }
     }while(!condition(in,out,inprime,outprime));

     toReturn.add(in);
     toReturn.add(out);
     return toReturn;
  }




  private static HashMap<String,Integer> createLabelLocations(List<String> assembly){
    HashMap<String,Integer> labelLocations=new HashMap<String,Integer>();
    String instruction;

    for(int i = 0; i <= assembly.size()-1; i++){
      instruction=assembly.get(i);
      if(instruction.contains(":")){
        String[] name = instruction.split("\\s+");
        labelLocations.put(name[0],i);
      }
    }
    return labelLocations;
  }

  private static HashMap<Integer,HashSet<Integer>> createSuccessors(List<String> assembly,Map<String,Integer> labelLocations){
    HashMap<Integer,HashSet<Integer>> succ = new HashMap<Integer,HashSet<Integer>>();
    String instruction;
    for(int i = 0; i <= assembly.size()-1; i++){
      instruction = assembly.get(i);
      HashSet<Integer> current = new HashSet<Integer>();
      if(!instruction.contains("BGEZ") && !instruction.contains("BEQZ") && !instruction.contains("BNEZ") && !instruction.contains("JMP")){
        if(i<assembly.size()-1)
          current.add(i+1);
        succ.put(i,current);
      }
      else{
        if(!instruction.contains("JMP")){
          String[] name = instruction.split("\\s+");
          String[] arguments =name[1].split(",");
          current.add(labelLocations.get(arguments[1]));
          succ.put(i,current);
        }
        else{
          String[] name = instruction.split("\\s+");
          if(i<assembly.size()-1)
            current.add(i+1);
          current.add(labelLocations.get(name[1]));
          succ.put(i,current);
        }
      }
    }
    return succ;
  }

}
