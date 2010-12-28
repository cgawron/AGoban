
//----------------------------------------------------
// The following code was generated by CUP v0.11a beta 20060608
// Mon Dec 27 18:55:06 CET 2010
//----------------------------------------------------

package de.cgawron.go.sgf;

import java_cup.runtime.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** CUP v0.11a beta 20060608 generated parser.
  * @version Mon Dec 27 18:55:06 CET 2010
  */
public class Parser extends java_cup.runtime.lr_parser {

  /** Default constructor. */
  public Parser() {super();}

  /** Constructor which sets the default scanner. */
  public Parser(java_cup.runtime.Scanner s) {super(s);}

  /** Constructor which sets the default scanner. */
  public Parser(java_cup.runtime.Scanner s, java_cup.runtime.SymbolFactory sf) {super(s,sf);}

  /** Production table. */
  protected static final short _production_table[][] = 
    unpackFromStrings(new String[] {
    "\000\015\000\002\002\003\000\002\002\004\000\002\002" +
    "\004\000\002\003\006\000\002\004\004\000\002\004\002" +
    "\000\002\005\004\000\002\005\003\000\002\006\004\000" +
    "\002\007\005\000\002\007\004\000\002\010\004\000\002" +
    "\010\003" });

  /** Access to production table. */
  public short[][] production_table() {return _production_table;}

  /** Parse-action table. */
  protected static final short[][] _action_table = 
    unpackFromStrings(new String[] {
    "\000\024\000\004\004\004\001\002\000\004\006\013\001" +
    "\002\000\006\002\010\004\004\001\002\000\006\002\001" +
    "\004\001\001\002\000\006\002\uffff\004\uffff\001\002\000" +
    "\004\002\000\001\002\000\010\004\ufffc\005\ufffc\006\013" +
    "\001\002\000\010\004\ufffa\005\ufffa\006\ufffa\001\002\000" +
    "\004\007\015\001\002\000\012\004\ufff9\005\ufff9\006\ufff9" +
    "\007\021\001\002\000\004\010\017\001\002\000\014\004" +
    "\ufff7\005\ufff7\006\ufff7\007\ufff7\010\020\001\002\000\014" +
    "\004\ufff5\005\ufff5\006\ufff5\007\ufff5\010\ufff5\001\002\000" +
    "\014\004\ufff6\005\ufff6\006\ufff6\007\ufff6\010\ufff6\001\002" +
    "\000\004\010\017\001\002\000\014\004\ufff8\005\ufff8\006" +
    "\ufff8\007\ufff8\010\020\001\002\000\010\004\ufffb\005\ufffb" +
    "\006\ufffb\001\002\000\006\004\004\005\025\001\002\000" +
    "\010\002\ufffe\004\ufffe\005\ufffe\001\002\000\006\004\ufffd" +
    "\005\ufffd\001\002" });

  /** Access to parse-action table. */
  public short[][] action_table() {return _action_table;}

  /** <code>reduce_goto</code> table. */
  protected static final short[][] _reduce_table = 
    unpackFromStrings(new String[] {
    "\000\024\000\006\002\004\003\005\001\001\000\006\005" +
    "\010\006\011\001\001\000\004\003\006\001\001\000\002" +
    "\001\001\000\002\001\001\000\002\001\001\000\006\004" +
    "\023\006\022\001\001\000\002\001\001\000\004\007\013" +
    "\001\001\000\002\001\001\000\004\010\015\001\001\000" +
    "\002\001\001\000\002\001\001\000\002\001\001\000\004" +
    "\010\021\001\001\000\002\001\001\000\002\001\001\000" +
    "\004\003\025\001\001\000\002\001\001\000\002\001\001" +
    "" });

  /** Access to <code>reduce_goto</code> table. */
  public short[][] reduce_table() {return _reduce_table;}

  /** Instance of action encapsulation class. */
  protected CUP$Parser$actions action_obj;

  /** Action encapsulation object initializer. */
  protected void init_actions()
    {
      action_obj = new CUP$Parser$actions(this);
    }

  /** Invoke a user supplied parse action. */
  public java_cup.runtime.Symbol do_action(
    int                        act_num,
    java_cup.runtime.lr_parser parser,
    java.util.Stack            stack,
    int                        top)
    throws java.lang.Exception
  {
    /* call code in generated class */
    return action_obj.CUP$Parser$do_action(act_num, parser, stack, top);
  }

  /** Indicates start state. */
  public int start_state() {return 0;}
  /** Indicates start production. */
  public int start_production() {return 1;}

  /** <code>EOF</code> Symbol index. */
  public int EOF_sym() {return 0;}

  /** <code>error</code> Symbol index. */
  public int error_sym() {return 1;}



    private static Logger logger = Logger.getLogger(Parser.class.getName());

    public void report_error(String message, Symbol info)
    {
	java_cup.runtime.Scanner scanner = getScanner();

	if (scanner instanceof InputPosition) 
	   throw new ParseError(message, (InputPosition) scanner, info);
	else 
	   throw new ParseError(message, info);
    }

    public void syntax_error(Symbol cur_token)
    {
	logger.warning("Syntax error at " + cur_token);
	report_error("Syntax error", cur_token);
    }

    public void debug_message(String mess)
    {
        if (logger.isLoggable(Level.FINE))
  	  logger.fine(mess);
    }

    public Symbol scan() throws java.lang.Exception 
    {
        Symbol sym = getScanner().next_token();
        sym =  (sym!=null) ? sym : new Symbol(EOF_sym());
	return sym;
    }


}

/** Cup generated class to encapsulate user supplied action code.*/
class CUP$Parser$actions {
  private final Parser parser;

  /** Constructor */
  CUP$Parser$actions(Parser parser) {
    this.parser = parser;
  }

  /** Method with the actual generated action code. */
  public final java_cup.runtime.Symbol CUP$Parser$do_action(
    int                        CUP$Parser$act_num,
    java_cup.runtime.lr_parser CUP$Parser$parser,
    java.util.Stack            CUP$Parser$stack,
    int                        CUP$Parser$top)
    throws java.lang.Exception
    {
      /* Symbol object for return from actions */
      java_cup.runtime.Symbol CUP$Parser$result;

      /* select the action based on the action number */
      switch (CUP$Parser$act_num)
        {
          /*. . . . . . . . . . . . . . . . . . . .*/
          case 12: // ValueList ::= Value 
            {
              Object RESULT =null;
		int vleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).left;
		int vright = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).right;
		Object v = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.peek()).value;
		 RESULT = AbstractValue.createValueList((Value) v); 
              CUP$Parser$result = parser.getSymbolFactory().newSymbol("ValueList",6, ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), RESULT);
            }
          return CUP$Parser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 11: // ValueList ::= ValueList Value 
            {
              Object RESULT =null;
		int vlleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).left;
		int vlright = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).right;
		Object vl = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.elementAt(CUP$Parser$top-1)).value;
		int vleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).left;
		int vright = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).right;
		Object v = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.peek()).value;
		 ((Value.ValueList)vl).add((Value) v); 
	            RESULT = vl; 
              CUP$Parser$result = parser.getSymbolFactory().newSymbol("ValueList",6, ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)), ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), RESULT);
            }
          return CUP$Parser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 10: // PropertyList ::= Property ValueList 
            {
              Object RESULT =null;
		int pleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).left;
		int pright = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).right;
		Object p = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.elementAt(CUP$Parser$top-1)).value;
		int vlleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).left;
		int vlright = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).right;
		Object vl = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.peek()).value;
		 ((Property) p).setValue((Value) vl); 
                    RESULT = new PropertyList((Property) p); 
              CUP$Parser$result = parser.getSymbolFactory().newSymbol("PropertyList",5, ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)), ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), RESULT);
            }
          return CUP$Parser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 9: // PropertyList ::= PropertyList Property ValueList 
            {
              Object RESULT =null;
		int plleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-2)).left;
		int plright = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-2)).right;
		Object pl = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.elementAt(CUP$Parser$top-2)).value;
		int pleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).left;
		int pright = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).right;
		Object p = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.elementAt(CUP$Parser$top-1)).value;
		int vlleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).left;
		int vlright = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).right;
		Object vl = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.peek()).value;
		 ((Property) p).setValue((Value) vl); 
                    ((PropertyList) pl).add((Property) p);
	            RESULT = pl; 
              CUP$Parser$result = parser.getSymbolFactory().newSymbol("PropertyList",5, ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-2)), ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), RESULT);
            }
          return CUP$Parser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 8: // Node ::= Semi PropertyList 
            {
              Object RESULT =null;
		int plleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).left;
		int plright = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).right;
		Object pl = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.peek()).value;
		 RESULT = new Node((PropertyList) pl); 
              CUP$Parser$result = parser.getSymbolFactory().newSymbol("Node",4, ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)), ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), RESULT);
            }
          return CUP$Parser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 7: // Sequence ::= Node 
            {
              Object RESULT =null;
		int nleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).left;
		int nright = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).right;
		Object n = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.peek()).value;
		
	            RESULT = new Sequence((Node) n);
                 
              CUP$Parser$result = parser.getSymbolFactory().newSymbol("Sequence",3, ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), RESULT);
            }
          return CUP$Parser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 6: // Sequence ::= Sequence Node 
            {
              Object RESULT =null;
		int sleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).left;
		int sright = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).right;
		Object s = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.elementAt(CUP$Parser$top-1)).value;
		int nleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).left;
		int nright = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).right;
		Object n = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.peek()).value;
		 
                    ((Sequence) s).append((Node) n);
	            RESULT = s;
                 
              CUP$Parser$result = parser.getSymbolFactory().newSymbol("Sequence",3, ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)), ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), RESULT);
            }
          return CUP$Parser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 5: // GameTreeList ::= 
            {
              Object RESULT =null;
		
	            RESULT = new LinkedList();
                 
              CUP$Parser$result = parser.getSymbolFactory().newSymbol("GameTreeList",2, ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), RESULT);
            }
          return CUP$Parser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 4: // GameTreeList ::= GameTreeList GameTree 
            {
              Object RESULT =null;
		int lleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).left;
		int lright = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).right;
		Object l = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.elementAt(CUP$Parser$top-1)).value;
		int gtleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).left;
		int gtright = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).right;
		Object gt = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.peek()).value;
		
	            ((List) l).add((Sequence) gt);
                    RESULT = l; 
                 
              CUP$Parser$result = parser.getSymbolFactory().newSymbol("GameTreeList",2, ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)), ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), RESULT);
            }
          return CUP$Parser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 3: // GameTree ::= Open Sequence GameTreeList Close 
            {
              Object RESULT =null;
		int sleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-2)).left;
		int sright = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-2)).right;
		Object s = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.elementAt(CUP$Parser$top-2)).value;
		int vleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).left;
		int vright = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).right;
		Object v = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.elementAt(CUP$Parser$top-1)).value;
		
                    ((Sequence) s).addAll((List) v);
                    RESULT = s;
                 
              CUP$Parser$result = parser.getSymbolFactory().newSymbol("GameTree",1, ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-3)), ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), RESULT);
            }
          return CUP$Parser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 2: // Collection ::= Collection GameTree 
            {
              Object RESULT =null;
		int cleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).left;
		int cright = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).right;
		Object c = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.elementAt(CUP$Parser$top-1)).value;
		int gtleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).left;
		int gtright = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).right;
		Object gt = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.peek()).value;
		
	            RESULT = c;
	            GameTree gameTree = new GameTree((Node) gt);
	            ((Collection) c).add(gameTree);
                 
              CUP$Parser$result = parser.getSymbolFactory().newSymbol("Collection",0, ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)), ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), RESULT);
            }
          return CUP$Parser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 1: // $START ::= Collection EOF 
            {
              Object RESULT =null;
		int start_valleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).left;
		int start_valright = ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)).right;
		Object start_val = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.elementAt(CUP$Parser$top-1)).value;
		RESULT = start_val;
              CUP$Parser$result = parser.getSymbolFactory().newSymbol("$START",0, ((java_cup.runtime.Symbol)CUP$Parser$stack.elementAt(CUP$Parser$top-1)), ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), RESULT);
            }
          /* ACCEPT */
          CUP$Parser$parser.done_parsing();
          return CUP$Parser$result;

          /*. . . . . . . . . . . . . . . . . . . .*/
          case 0: // Collection ::= GameTree 
            {
              Object RESULT =null;
		int gtleft = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).left;
		int gtright = ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()).right;
		Object gt = (Object)((java_cup.runtime.Symbol) CUP$Parser$stack.peek()).value;
		
	            Collection c = new LinkedList();
	            c.add(new GameTree((Node) gt));
	            RESULT = c;
                 
              CUP$Parser$result = parser.getSymbolFactory().newSymbol("Collection",0, ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), ((java_cup.runtime.Symbol)CUP$Parser$stack.peek()), RESULT);
            }
          return CUP$Parser$result;

          /* . . . . . .*/
          default:
            throw new Exception(
               "Invalid action number found in internal parse table");

        }
    }
}

