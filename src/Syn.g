// COMS22201: Syntax analyser

parser grammar Syn;

options {
  tokenVocab = Lex;
  output = AST;
}

@members
{
	private String cleanString(String s){
		String tmp;
		tmp = s.replaceAll("^'", "");
		s = tmp.replaceAll("'$", "");
		tmp = s.replaceAll("''", "'");
		return tmp;
	}
  public static SymbolTable table=new SymbolTable();
}

program :
    statements
  ;

statements :
    statement ( SEMICOLON^ statement )*
  ;

statement :
    WRITE^ OPENPAREN! ( string | exp ) CLOSEPAREN!
  | WRITELN^
  | IF^ exp
    THEN! statement
    ELSE! statement
  | WHILE^ exp
    DO! statement
  | READ^ OPENPAREN!  (name=IDENTIFIER ) CLOSEPAREN! { if(!table.containsKey($name.getText())) table.setType($name.getText(),"int"); else{if(!table.get($name.getText()).equals("int")) {System.out.println("Syntax Error : The type of variable "+$name.getText()+" is being changed in line "+$name.getLine());System.exit(1);} }if($name.getText().length()>8) {System.out.println("Syntax Error : Name of variable "+$name.getText()+" is longer than 8 characters"); System.exit(1);}}
  | READR^ OPENPAREN! (name=IDENTIFIER ) CLOSEPAREN! { if(!table.containsKey($name.getText())) table.setType($name.getText(),"real"); else{if(!table.get($name.getText()).equals("real")) {System.out.println("Syntax Error : The type of variable "+$name.getText()+" is being changed in line "+$name.getLine());System.exit(1);} }if($name.getText().length()>8) {System.out.println("Syntax Error : Name of variable "+$name.getText()+" is longer than 8 characters"); System.exit(1);}}
  | (s=SYMBOL!) (name=IDENTIFIER) ASSIGN^ exp { if(!table.containsKey($name.getText())) table.setType($name.getText(),$s.getText()); else {if(!table.get($name.getText()).equals($s.getText())) {System.out.println("Syntax Error : The type of variable "+$name.getText()+" is being changed in line "+$name.getLine());System.exit(1);}  }if($name.getText().length()>8) {System.out.println("Syntax Error : Name of variable "+$name.getText()+" is longer than 8 characters"); System.exit(1);}}
  | (IDENTIFIER ASSIGN)=> (name=IDENTIFIER) ASSIGN^ exp { if(!table.containsKey($name.getText())) table.setType($name.getText(),"int"); if($name.getText().length()>8) {System.out.println("Syntax Error : Name of variable "+$name.getText()+" is longer than 8 characters"); System.exit(1);} }
  | OPENPAREN! ( statements ) CLOSEPAREN!
  | SKIP^
  ;

exp:
    oring
  ;

oring:
    anding (OR^ anding)*
  ;

anding:
    noting (AND^ noting)*
  ;

noting:
    (NOT^)? rel
  ;

rel:
    add ( (EQUALS|LOWERTHAN)^ add)*
  ;

add:
    mult( ( PLUS | MINUS )^ mult) *
  ;

mult:
    factor (( TIMES | DIV )^ factor)*
  ;

factor:
    (name=IDENTIFIER) {if(!table.containsKey($name.getText())) {System.out.println("Syntax error : use of undeclared identifier "+$name.getText()+" at line "+$name.getLine()); System.exit(1);} else if($name.getText().length()>8){System.out.println("Syntax Error : Name of variable "+$name.getText()+" is longer than 8 characters"); System.exit(1);} }
  | INTNUM
  | TRUE
  | FALSE
  | OPENPAREN! exp CLOSEPAREN!
  | REALNUM
  ;

string
    scope { String tmp; }
    :
    s=STRING { $string::tmp = cleanString($s.text); }-> STRING[$string::tmp]
;
