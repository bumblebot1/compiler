// COMS22201: IR tree construction

import java.util.*;
import java.io.*;
import java.lang.reflect.Array;
import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;

public class Irt
{
// The code below is generated automatically from the ".tokens" file of the
// ANTLR syntax analysis, using the TokenConv program.
//
// CAMLE TOKENS BEGIN
  public static final String[] tokenNames = new String[] {
"NONE", "NONE", "NONE", "NONE", "AND", "ASSIGN", "CLOSEPAREN", "COMMENT", "DIV", "DO", "ELSE", "EQUALS", "FALSE", "IDENTIFIER", "IF", "INTNUM", "LOWERTHAN", "MINUS", "NOT", "OPENPAREN", "OR", "PLUS", "READ", "READR", "REALNUM", "SEMICOLON", "SKIP", "STRING", "SYMBOL", "THEN", "TIMES", "TRUE", "WHILE", "WRITE", "WRITELN", "WS"};
  public static final int AND=4;
  public static final int ASSIGN=5;
  public static final int CLOSEPAREN=6;
  public static final int COMMENT=7;
  public static final int DIV=8;
  public static final int DO=9;
  public static final int ELSE=10;
  public static final int EQUALS=11;
  public static final int FALSE=12;
  public static final int IDENTIFIER=13;
  public static final int IF=14;
  public static final int INTNUM=15;
  public static final int LOWERTHAN=16;
  public static final int MINUS=17;
  public static final int NOT=18;
  public static final int OPENPAREN=19;
  public static final int OR=20;
  public static final int PLUS=21;
  public static final int READ=22;
  public static final int READR=23;
  public static final int REALNUM=24;
  public static final int SEMICOLON=25;
  public static final int SKIP=26;
  public static final int STRING=27;
  public static final int SYMBOL=28;
  public static final int THEN=29;
  public static final int TIMES=30;
  public static final int TRUE=31;
  public static final int WHILE=32;
  public static final int WRITE=33;
  public static final int WRITELN=34;
  public static final int WS=35;
// CAMLE TOKENS END

  public static int freeLabel=0;
  static Map<String,String> table = Syn.table.getTable();

  public static IRTree convert(CommonTree ast)
  {
    IRTree irt = new IRTree();
    program(ast, irt);
    return irt;
  }

  public static void program(CommonTree ast, IRTree irt)
  {
    statements(ast, irt);
  }

  public static void statements(CommonTree ast, IRTree irt)
  {
    int i;
    Token t = ast.getToken();
    int tt = t.getType();
    if (tt == SEMICOLON) {
      IRTree irt1 = new IRTree();
      IRTree irt2 = new IRTree();
      CommonTree ast1 = (CommonTree)ast.getChild(0);
      CommonTree ast2 = (CommonTree)ast.getChild(1);
      statements(ast1, irt1);
      statements(ast2, irt2);
      irt.setOp("SEQ");
      irt.addSub(irt1);
      irt.addSub(irt2);
    }
    else {
      statement(ast, irt);
    }
  }

  public static void statement(CommonTree ast, IRTree irt)
  {
    CommonTree ast1, ast2, ast3;
    IRTree irt1 = new IRTree(), irt2 = new IRTree(), irt3 = new IRTree();
    Token t = ast.getToken();
    int tt = t.getType();
    if (tt == WRITE) {
      ast1 = (CommonTree)ast.getChild(0);
      String type = arg(ast1, irt1);
      if (type.equals("int")) {
        irt.setOp("WR");
        irt.addSub(irt1);
      }
      else if(type.equals("string")) {
        irt.setOp("WRS");
        irt.addSub(irt1);
      }
      else if(type.equals("real")){
        irt.setOp("WRR");
        irt.addSub(irt1);
      }
      else{
        irt.setOp("WRB");
        irt.addSub(irt1);
      }
    }
    else if (tt == WRITELN) {
      String a = String.valueOf(Memory.allocateString("\n"));
      irt.setOp("WRS");
      irt.addSub(new IRTree("MEM", new IRTree("CONST", new IRTree(a))));
    }
    else if(tt== ASSIGN){
      irt.setOp("MOVE");
      ast1 = (CommonTree)ast.getChild(0);
      ast2 = (CommonTree)ast.getChild(1);
      Token t1=ast1.getToken();//IDENTIFIER name of the variable
      Token t2=ast2.getToken();//Value of the variable
      int tt1=t1.getType();
      int tt2=t2.getType();
      if(table.get(t1.getText()).equals("real"))
        irt3.setOp("REAL");
      else
        irt3.setOp("INT");
      if(tt1==IDENTIFIER){
        if(tt2!=IDENTIFIER){
          expression(ast2,irt2);
          String location=String.valueOf(Memory.allocateVariable(0,t1.getText()));
          irt.addSub(new IRTree("MEM",new IRTree("CONST",new IRTree(location))));
          irt.addSub(irt2);
          irt.addSub(irt3);
        }
        else{
          String nameLocation=String.valueOf(Memory.allocateVariable(t1.getText(),t2.getText()));
          irt.addSub(new IRTree("MEM",new IRTree("CONST",new IRTree(nameLocation))));
          String valueLocation=String.valueOf(Memory.getVariableAddressInMem(t2.getText()));
          irt.addSub(new IRTree("MEM",new IRTree("CONST",new IRTree(valueLocation))));
          irt.addSub(irt3);
        }
      }
      else{
        error(tt);
      }
    }
    else if(tt == READ){
      ast1=(CommonTree)ast.getChild(0);
      Token t1=ast1.getToken();//IDENTIFIER name of the variable
      String location=String.valueOf(Memory.allocateVariable(0,t1.getText()));
      irt.setOp("READ");
      irt.addSub(new IRTree("CONST",new IRTree(location)));
    }
    else if(tt == READR){
      ast1=(CommonTree)ast.getChild(0);
      Token t1=ast1.getToken();//IDENTIFIER name of the variable
      String location=String.valueOf(Memory.allocateVariable(0,t1.getText()));
      irt.setOp("READR");
      irt.addSub(new IRTree("CONST",new IRTree(location)));
    }
    else if(tt == IF){
      ast1=(CommonTree)ast.getChild(0);
      ast2=(CommonTree)ast.getChild(1);
      ast3=(CommonTree)ast.getChild(2);
      statement(ast2,irt2);
      statement(ast3,irt3);
      expression(ast1,irt1);
      irt.setOp("SEQ");
      irt.addSub(new IRTree("CJUMP",irt1,new IRTree("NAME",new IRTree("n"+(freeLabel))),new IRTree("NAME",new IRTree("n"+(freeLabel+1)))));
      irt.addSub(new IRTree("SEQ",new IRTree("LABEL",new IRTree("n"+(freeLabel))),new IRTree("SEQ",irt2,new IRTree("SEQ",new IRTree("JUMP",new IRTree("NAME",new IRTree("n"+(freeLabel+2)))),new IRTree("SEQ",new IRTree("LABEL",new IRTree("n"+(freeLabel+1))),new IRTree("SEQ",irt3,new IRTree("LABEL",new IRTree("n"+(freeLabel+2)))))))));
      freeLabel=freeLabel+3;
    }
    else if(tt == SKIP){
      irt.setOp("SKIP");
    }
    else if(tt == SEMICOLON){
      statements(ast,irt);
    }
    else if(tt == WHILE){
      ast1=(CommonTree)ast.getChild(0);
      ast2=(CommonTree)ast.getChild(1);
      expression(ast1,irt1);
      statement(ast2,irt2);
      irt.setOp("SEQ");
      irt.addSub(new IRTree("LABEL",new IRTree("n"+(freeLabel))));
      irt.addSub(new IRTree("SEQ",new IRTree("CJUMP",irt1,new IRTree("NAME",new IRTree("n"+(freeLabel+1))),new IRTree("NAME",new IRTree("n"+(freeLabel+2)))),new IRTree("SEQ",new IRTree("LABEL",new IRTree("n"+(freeLabel+1))),new IRTree("SEQ",irt2,new IRTree("SEQ",new IRTree("JUMP",new IRTree("NAME",new IRTree("n"+(freeLabel)))),new IRTree("LABEL",new IRTree("n"+(freeLabel+2))))))));
      freeLabel=freeLabel+3;
    }
    else {
      error(tt);
    }
  }

  public static String arg(CommonTree ast, IRTree irt)
  {
    Token t = ast.getToken();
    int tt = t.getType();
    if (tt == STRING) {
      String tx = t.getText();
      int a = Memory.allocateString(tx);
      String st = String.valueOf(a);
      irt.setOp("MEM");
      irt.addSub(new IRTree("CONST", new IRTree(st)));
      return "string";
    }
    else if(tt == TRUE){
      expression(ast,irt);
      return "boolean";
    }
    else if(tt == FALSE){
      expression(ast,irt);
      return "boolean";
    }
    else if(tt == NOT){
      expression(ast,irt);
      return "boolean";
    }
    else if(tt == AND){
      expression(ast,irt);
      return "boolean";
    }
    else if(tt == OR){
      expression(ast,irt);
      return "boolean";
    }
    else if(tt == EQUALS){
      expression(ast,irt);
      return "boolean";
    }
    else if(tt == LOWERTHAN){
      expression(ast,irt);
      return "boolean";
    }
    else if(tt == REALNUM){
      expression(ast,irt);
      return "real";
    }
    else if(tt == IDENTIFIER && table.get(t.getText()).equals("real")){
      expression(ast,irt);
      return "real";
    }
    else {
      expression(ast, irt);
      return "int";
    }
  }

  public static void expression(CommonTree ast, IRTree irt)
  {
    CommonTree ast1;
    CommonTree ast2;
    IRTree irt1 = new IRTree();
    IRTree irt2 = new IRTree();
    IRTree irt3 = new IRTree();
    Token t = ast.getToken();
    int tt = t.getType();
    if (tt == INTNUM) {
      constant(ast, irt1);
      irt.setOp("CONST");
      irt.addSub(irt1);
    }
    else if (tt == REALNUM) {
      constant(ast, irt1);
      irt.setOp("REALCONST");
      irt.addSub(irt1);
    }
    else if(tt == IDENTIFIER){
      if(table.get(t.getText()).equals("real")){
        irt1.setOp("REAL");
      }
      else{
        irt1.setOp("INT");
      }
      irt.setOp("MEM");
      irt.addSub(new IRTree("CONST",new IRTree(String.valueOf(Memory.getVariableAddressInMem(t.getText())))));
      irt.addSub(irt1);
    }
    else if(tt==PLUS){
      ast1=(CommonTree) ast.getChild(0);
      ast2=(CommonTree) ast.getChild(1);
      expression(ast1,irt2);
      expression(ast2,irt3);
      irt.setOp("BINOP");
      irt1.setOp("ADD");
      irt.addSub(irt1);
      irt.addSub(irt2);
      irt.addSub(irt3);
    }
    else if(tt==MINUS){
      ast1=(CommonTree) ast.getChild(0);
      ast2=(CommonTree) ast.getChild(1);
      expression(ast1,irt2);
      expression(ast2,irt3);
      irt.setOp("BINOP");
      irt1.setOp("SUB");
      irt.addSub(irt1);
      irt.addSub(irt2);
      irt.addSub(irt3);
    }
    else if(tt==TIMES){
      ast1=(CommonTree) ast.getChild(0);
      ast2=(CommonTree) ast.getChild(1);
      expression(ast1,irt2);
      expression(ast2,irt3);
      irt.setOp("BINOP");
      irt1.setOp("MUL");
      irt.addSub(irt1);
      irt.addSub(irt2);
      irt.addSub(irt3);
    }
    else if(tt==DIV){
      ast1=(CommonTree) ast.getChild(0);
      ast2=(CommonTree) ast.getChild(1);
      expression(ast1,irt2);
      expression(ast2,irt3);
      irt.setOp("BINOP");
      irt1.setOp("DIV");
      irt.addSub(irt1);
      irt.addSub(irt2);
      irt.addSub(irt3);
    }
    else if(tt==EQUALS){
      ast1=(CommonTree) ast.getChild(0);
      ast2=(CommonTree) ast.getChild(1);
      expression(ast1,irt2);
      expression(ast2,irt3);
      irt.setOp("RELOP");
      irt1.setOp("EQ");
      irt.addSub(irt1);
      irt.addSub(irt2);
      irt.addSub(irt3);
    }
    else if(tt==LOWERTHAN){
      ast1=(CommonTree) ast.getChild(0);
      ast2=(CommonTree) ast.getChild(1);
      expression(ast1,irt2);
      expression(ast2,irt3);
      irt.setOp("RELOP");
      irt1.setOp("LWT");
      irt.addSub(irt1);
      irt.addSub(irt2);
      irt.addSub(irt3);
    }
    else if(tt==NOT){
      ast1=(CommonTree) ast.getChild(0);
      irt.setOp("BOOLOP");
      irt2.setOp("NOT");
      expression(ast1,irt1);
      irt2.addSub(irt1);
      irt.addSub(irt2);
    }
    else if(tt==AND){
      ast1=(CommonTree) ast.getChild(0);
      ast2=(CommonTree) ast.getChild(1);
      irt.setOp("BOOLOP");
      irt3.setOp("AND");
      expression(ast1,irt1);
      expression(ast2,irt2);
      irt3.addSub(irt1);
      irt3.addSub(irt2);
      irt.addSub(irt3);
    }
    else if(tt==OR){
      ast1=(CommonTree) ast.getChild(0);
      ast2=(CommonTree) ast.getChild(1);
      irt.setOp("BOOLOP");
      irt3.setOp("OR");
      expression(ast1,irt1);
      expression(ast2,irt2);
      irt3.addSub(irt1);
      irt3.addSub(irt2);
      irt.addSub(irt3);
    }
    else if(tt==TRUE){
      irt.setOp("BOOLOP");
      irt1.setOp("TRUE");
      irt.addSub(irt1);
    }
    else if(tt==FALSE){
      irt.setOp("BOOLOP");
      irt1.setOp("FALSE");
      irt.addSub(irt1);
    }
    else{
        error(tt);
    }
  }



  public static void constant(CommonTree ast, IRTree irt)
  {
    Token t = ast.getToken();
    int tt = t.getType();
    if (tt == INTNUM) {
      String tx = t.getText();
      irt.setOp(tx);
    }
    else if(tt == REALNUM){
      String tx = t.getText();
      irt.setOp(tx);
    }
    else {

      error(tt);
    }
  }

  private static void error(int tt)
  {
    System.out.println("IRT error: "+tokenNames[tt]);
    System.exit(1);
  }
}
