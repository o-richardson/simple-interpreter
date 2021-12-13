//Author:         OAR180004
//Course:         CS 4337.502
//Professor:      Dr. Salazar
//Term:           Spring 2021
//Title:          Project Part 2
//File:           Parser.java

/**
 * Sorry this is so long and complicated, the for loop was bad enough but adding array support in all
 * the ways I wanted ended up pushing it to 1000 lines. Multi-dimensional arrays would be a significant
 * increase to complexity so I have decided to not support them at this time, you will get a syntax
 * error if the parser finds an opening bracket immediately following a closing bracket ( ] [ ).
 */


import java.io.File;
import java.io.FileNotFoundException;

import java.util.HashMap;
import java.util.Scanner;
import java.util.Iterator;
import java.util.LinkedList;

import java.util.AbstractMap.SimpleEntry;

public class Parser
{
    // Whether we are executing the code we are parsing or not at any given moment
    // It will suppress get and print as well as updates to variables when we are
    // parsing statements that should not be executed, like when an if condition
    // is not met
    public static boolean exec;

    // Map variable ID to value, we do not allow accessing uninitialized variables. If we assign to
    // an existing variable again, its old value will be lost. There can't be an array and a variable
    // with the same ID since print and get would not know how to handle that case, so if you try and
    // make a variable or array with duplicate ID you will receive a syntax error.
    public static HashMap<String, Integer> variables;

    // Map array ID to array of values. NOTE: There can't be a variable and array with the same name!
    // Arrays may be printed or used in a get statement without specifying a subscript, and they may
    // also have their length accessed by ID.length, but they require subscripts when declaring
    // as well as when using in expressions besides when using length. When declared their values
    // default to 0. All accesses are bounds checked, and we cannot create an array with size < 1.
    public static HashMap<String, Integer[]> arrays;

    // List of Lexer.Tokens obtained from the Lexer
    public static LinkedList<SimpleEntry<Lexer.Token, String>> input;

    // Scanner on System.in for taking user input
    public static Scanner in;

    static
    {
        exec = true;
        variables = new HashMap<String, Integer>();
        arrays = new HashMap<String, Integer[]>();
        input = new LinkedList<SimpleEntry<Lexer.Token, String>>();
        in = new Scanner(System.in);
    }

    public static int parseValue()
    {
        switch(input.peekFirst().getKey())
        {
            case ID:
                SimpleEntry<Lexer.Token, String> id = input.removeFirst();
                Integer val;
                if(input.peekFirst().getKey() != Lexer.Token.LB) // Not an indexed array access
                {
                    if(input.peekFirst().getKey() != Lexer.Token.DOT) // Not performing .length on an array, return variable value
                    {
                        val = variables.get(id.getValue());
                        if(!exec)
                        {
                            val = 0;
                        }
                        if(val == null)
                        {
                            in.close();
                            throw new IllegalStateException("Need to initialize " + id.getValue() + " non-array variable before using.");
                        }
                    }
                    else // .length operation, return length of array with name given by id
                    {
                        Integer array[] = arrays.get(id.getValue());
                        if(array == null)
                        {
                            in.close();
                            throw new IllegalStateException("Can only use . operator on instantiated array, " + id.getValue() + " was not instantiated.");
                        }
                        input.removeFirst(); // Discard DOT Lexer.Token
                        if(input.peekFirst().getKey() != Lexer.Token.LENGTH)
                        {
                            in.close();
                            throw new IllegalStateException("Dot operator must be followed by length keyword, found " + input.peekFirst().getValue());
                        }
                        input.removeFirst(); // Discard LENGTH Lexer.Token
                        val = array.length;
                    }
                }
                else // indexed array access
                {
                    Integer array[] = arrays.get(id.getValue());
                    if(exec && array == null)
                    {
                        in.close();
                        throw new IllegalStateException("Need to declare " + id.getValue() + " array before using.");
                    }
                    input.removeFirst(); // Discard LB left bracket Lexer.Token
                    int index = parseExpr(); // parse index
                    if(!exec)
                    {
                        index = 0;
                    }
                    if(input.peekFirst().getKey() != Lexer.Token.RB)
                    {
                        in.close();
                        throw new IllegalStateException("Missing closing ] on array access for " + id.getValue());
                    }
                    input.removeFirst(); // Discard RB right bracket Lexer.Token
                    
                    if(input.peekFirst().getKey() == Lexer.Token.LB)
                    {
                        in.close();
                        throw new IllegalStateException("Multidimensional arrays are not supported, Array: " + id);
                    }
                    
                    if(exec && (index < 0 || index >= array.length))
                    {
                        in.close();
                        throw new IllegalStateException("Array Index out of Bounds! Array: " + id.getValue() + " Index: " + index);
                    }
                    if(exec)
                    {
                        val = array[index];
                    }
                    else
                    {
                        val = 0;
                    }
                    if(val == null)
                    {
                        in.close();
                        throw new IllegalStateException("Cannot access uninitialized array element! Array: " + id.getValue() + " Index: " + index);
                    }
                }
                return val;
            case INT:
                SimpleEntry<Lexer.Token, String> num = input.removeFirst();
                try
                {
                    return Integer.parseInt(num.getValue());
                }
                catch(NumberFormatException e)
                {
                    in.close();
                    throw new IllegalStateException("Malformed integer constant " + num.getValue());
                }
            case MINUS:
                input.removeFirst();
                return 0 - parseValue();
            case NOT:
                input.removeFirst();
                return parseValue() == 0 ? 1 : 0;
            case OPEN:
                input.removeFirst();    // remove (
                int exp = parseExpr();
                input.removeFirst();    // remove )
                return exp;
            default:
                in.close();
                throw new IllegalStateException("Defaulted parsing value " + input.peekFirst().getValue());
        }
    }
    
    public static int parseFactor()
    {
        switch(input.peekFirst().getKey())
        {
            case ID:
            case INT:
            case MINUS:
            case NOT:
            case OPEN:
                int value = parseValue();
                switch(input.peekFirst().getKey())
                {
                    case GT:
                        input.removeFirst();
                        return value > parseValue() ? 1 : 0;
                    case GTE:
                        input.removeFirst();
                        return value >= parseValue() ? 1 : 0;
                    case LT:
                        input.removeFirst();
                        return value < parseValue() ? 1 : 0;
                    case LTE:
                        input.removeFirst();
                        return value <= parseValue() ? 1 : 0;
                    case EQ:
                        input.removeFirst();
                        return value == parseValue() ? 1 : 0;
                    case NEQ:
                        input.removeFirst();
                        return value != parseValue() ? 1 : 0;
                    default:
                        return value;
                }
            default:
                in.close();
                throw new IllegalStateException("Defaulted parsing factor " + input.peekFirst().getValue());
        }
    }
    
    public static int parseTerm()
    {
        switch(input.peekFirst().getKey())
        {
            case ID:
            case INT:
            case MINUS:
            case NOT:
            case OPEN:
                int factor = parseFactor();
                if(input.peekFirst().getKey() == Lexer.Token.TIMES)
                {
                    input.removeFirst();
                    return factor * parseTerm();
                }
                else if(input.peekFirst().getKey() == Lexer.Token.DIV)
                {
                    input.removeFirst();
                    int term = parseTerm();
                    if(!exec && term == 0)
                    {
                        return 0;
                    }
                    return factor / term;
                }
                else if(input.peekFirst().getKey() == Lexer.Token.MOD)
                {
                    input.removeFirst();
                    int term = parseTerm();
                    if(!exec && term == 0)
                    {
                        return 0;
                    }
                    return factor % term;
                }
                return factor;
            default:
                in.close();
                throw new IllegalStateException("Defaulted parsing term " + input.peekFirst().getValue());
        }
    }
    
    public static int parseNExpr()
    {
        switch(input.peekFirst().getKey())
        {
            case ID:
            case INT:
            case MINUS:
            case NOT:
            case OPEN:
                int term = parseTerm();
                if(input.peekFirst().getKey() == Lexer.Token.PLUS)
                {
                    input.removeFirst();
                    return term + parseNExpr();
                }
                else if(input.peekFirst().getKey() == Lexer.Token.MINUS)
                {
                    input.removeFirst();
                    return term - parseNExpr();
                }
                return term;
            default:
                in.close();
                throw new IllegalStateException("Defaulted parsing n_expr " + input.peekFirst().getValue());
        }
    }

    public static int parseExpr()
    {
        switch(input.peekFirst().getKey())
        {
            case ID:
            case INT:
            case MINUS:
            case NOT:
            case OPEN:
                int leftExpr = parseNExpr();
                if(input.peekFirst().getKey() == Lexer.Token.AND)
                {
                    input.removeFirst();
                    int rightExpr = parseNExpr();
                    return (leftExpr != 0 && rightExpr != 0) ? 1 : 0;
                }
                else if(input.peekFirst().getKey() == Lexer.Token.OR)
                {
                    input.removeFirst();
                    int rightExpr = parseNExpr();
                    return (leftExpr != 0 || rightExpr != 0) ? 1 : 0;
                }
                return leftExpr;
            default:
                in.close();
                throw new IllegalStateException("Defaulted parsing expr " + input.peekFirst().getValue());
        }
    }
    
    // <for> -> "for" "(" <assign> ";" <expr> ";" <assign> ")" "do" <stmt_list> "end"
    public static void parseFor()
    {
        input.removeFirst(); // Discard FOR Lexer.Token
        if(input.peekFirst().getKey() != Lexer.Token.OPEN)
        {
            in.close();
            throw new IllegalStateException("Missing \"(\" in for loop header at " + input.peekFirst().getValue());
        }
        input.removeFirst(); // Discard "(" OPEN Lexer.Token
        
        parseAssign(); // Always perform first assign once, may exec or not based on flag setting

        if(input.peekFirst().getKey() != Lexer.Token.SEMI)
        {
            in.close();
            throw new IllegalStateException("Missing \";\" in for loop header at " + input.peekFirst().getValue());
        }
        input.removeFirst(); // Discard ";" SEMI Lexer.Token
        
        // Copy condition to list in case we need to check it repeatedly
        LinkedList<SimpleEntry<Lexer.Token, String>> conditionExpr = new LinkedList<SimpleEntry<Lexer.Token, String>>();
        Iterator<SimpleEntry<Lexer.Token, String>> it = input.iterator();
        SimpleEntry<Lexer.Token, String> temp;
        while(it.hasNext())
        {
            temp = it.next();
            if(temp.getKey() == Lexer.Token.ENDSTREAM)
            {
                in.close();
                throw new IllegalStateException("Missing semicolon in for loop header at " + input.peekFirst().getValue());
            }
            if(temp.getKey() == Lexer.Token.SEMI)
            {
                break;
            }
            conditionExpr.addLast(temp);
        }
        if(conditionExpr.isEmpty())
        {
            in.close();
            throw new IllegalStateException("Empty for condition is not allowed!");
        }

        LinkedList<SimpleEntry<Lexer.Token, String>> updateAssignment = new LinkedList<SimpleEntry<Lexer.Token, String>>();
        int parenLevel = 1; // Parenthesis depth, number of closing parentheses we want to see
        while(it.hasNext())
        {
            temp = it.next();
            if(temp.getKey() == Lexer.Token.ENDSTREAM)
            {
                in.close();
                throw new IllegalStateException("Missing \")\" in for loop header at " + input.peekFirst().getValue());
            }
            if(temp.getKey() == Lexer.Token.OPEN)
            {
                parenLevel++;
            }
            if(temp.getKey() == Lexer.Token.CLOSE)
            {
                parenLevel--;
                if(parenLevel == 0)
                {
                    break;
                }
            }
            updateAssignment.addLast(temp);
        }
        if(updateAssignment.isEmpty())
        {
            in.close();
            throw new IllegalStateException("Empty for loop counter update is not allowed!");
        }
        temp = it.next(); // skip DO
        
        LinkedList<SimpleEntry<Lexer.Token, String>> stmtList = new LinkedList<SimpleEntry<Lexer.Token, String>>();
        int level = 1; // number of whiles, ifs, and fors that need to end to stop getting stmtList
        while(it.hasNext())
        {
            temp = it.next();
            if(temp.getKey() == Lexer.Token.ENDSTREAM)
            {
                in.close();
                throw new IllegalStateException("Expected end after while body");
            }
            if(temp.getKey() == Lexer.Token.WHILE || temp.getKey() == Lexer.Token.FOR || temp.getKey() == Lexer.Token.IF)
            {
                level++;
            }
            if(temp.getKey() == Lexer.Token.END)
            {
                level--;
                if(level == 0)
                {
                    break;
                }
            }
            stmtList.addLast(temp);
        }
        
        int cond = parseExpr();
        
        if(input.peekFirst().getKey() != Lexer.Token.SEMI)
        {
            in.close();
            throw new IllegalStateException("Expected semicolon in for loop header at " + input.peekFirst().getValue());
        }
        input.removeFirst(); // Discard SEMI Lexer.Token
        
        // parse assignment that updates loop counter but suppress execution
        if(exec)
        {
            exec = false;
            parseAssign();
            exec = true;
        }
        else
        {
            parseAssign();
        }
        
        if(input.peekFirst().getKey() != Lexer.Token.CLOSE)
        {
            in.close();
            throw new IllegalStateException("Expected \")\" in for loop header at " + input.peekFirst().getValue());
        }
        input.removeFirst(); // Discard CLOSE ")" Lexer.Token

        if(input.peekFirst().getKey() != Lexer.Token.DO)
        {
            in.close();
            throw new IllegalStateException("Expected \"do\" after for loop header at " + input.peekFirst().getValue());
        }
        input.removeFirst(); // Discard DO Lexer.Token
        
        // Parse body once and suppress execution
        if(exec && input.peekFirst().getKey() != Lexer.Token.END)
        {
            exec = false;
            parseStmtList();
            exec = true;
        }
        else if(input.peekFirst().getKey() != Lexer.Token.END)
        {
            parseStmtList();
        }
        
        if(input.peekFirst().getKey() != Lexer.Token.END)
        {
            in.close();
            throw new IllegalStateException("Expected \"end\" after for loop body at " + input.peekFirst().getValue());
        }
        input.removeFirst(); // Discard END Lexer.Token
        
        if(exec)
        {
            LinkedList<SimpleEntry<Lexer.Token, String>> backup = new LinkedList<SimpleEntry<Lexer.Token, String>>(input);
            while(cond != 0)
            {
                if(!stmtList.isEmpty())
                {
                    input = new LinkedList<SimpleEntry<Lexer.Token, String>>(stmtList);
                    parseStmtList();
                }
                input = new LinkedList<SimpleEntry<Lexer.Token, String>>(updateAssignment);
                input.addLast(new SimpleEntry<Lexer.Token, String>(Lexer.Token.ENDSTREAM, "End of Input"));
                parseAssign();
                input = new LinkedList<SimpleEntry<Lexer.Token, String>>(conditionExpr);
                input.addLast(new SimpleEntry<Lexer.Token, String>(Lexer.Token.ENDSTREAM, "End of Input"));
                cond = parseExpr();
            }
            input = new LinkedList<SimpleEntry<Lexer.Token, String>>(backup);
        }
    }
    
    public static void parseWhile()
    {
        input.removeFirst(); // Discard WHILE Lexer.Token
        
        // Copy Condition to list in case we need to check it repeatedly
        LinkedList<SimpleEntry<Lexer.Token, String>> conditionExpr = new LinkedList<SimpleEntry<Lexer.Token, String>>();
        Iterator<SimpleEntry<Lexer.Token, String>> it = input.iterator();
        SimpleEntry<Lexer.Token, String> temp;
        while(it.hasNext())
        {
            temp = it.next();
            if(temp.getKey() == Lexer.Token.ENDSTREAM)
            {
                in.close();
                throw new IllegalStateException("Missing do after while condition at" + input.peekFirst().getValue());
            }
            if(temp.getKey() == Lexer.Token.DO)
            {
                break;
            }
            conditionExpr.addLast(temp);
        }
        if(conditionExpr.isEmpty())
        {
            in.close();
            throw new IllegalStateException("Empty while condition is not allowed!");
        }

        LinkedList<SimpleEntry<Lexer.Token, String>> stmtList = new LinkedList<SimpleEntry<Lexer.Token, String>>();
        
        // If we reach here we have a DO Lexer.Token in temp, we can ignore it
        int level = 1; // number of whiles, ifs, and fors that need to end to stop getting stmtList
        while(it.hasNext())
        {
            temp = it.next();
            if(temp.getKey() == Lexer.Token.ENDSTREAM)
            {
                in.close();
                throw new IllegalStateException("Expected end after while body");
            }
            if(temp.getKey() == Lexer.Token.WHILE || temp.getKey() == Lexer.Token.FOR || temp.getKey() == Lexer.Token.IF)
            {
                level++;
            }
            if(temp.getKey() == Lexer.Token.END)
            {
                level--;
                if(level == 0)
                {
                    break;
                }
            }
            stmtList.addLast(temp);
        }

        // clean up input List since we have saved condition and body
        // also parse the body at least once without executing to check validity
        int cond = parseExpr();
        if(input.peekFirst().getKey() != Lexer.Token.DO)
        {
            in.close();
            throw new IllegalStateException("Expected \"do\" at " + input.peekFirst().getValue());
        }
        input.removeFirst(); // Discard DO Lexer.Token

        if(exec && input.peekFirst().getKey() != Lexer.Token.END)
        {
            exec = false;
            parseStmtList();
            exec = true;
        }
        else if(input.peekFirst().getKey() != Lexer.Token.END)
        {
            parseStmtList();
        }

        if(input.peekFirst().getKey() != Lexer.Token.END)
        {
            in.close();
            throw new IllegalStateException("Expected \"end\" at " + input.peekFirst().getValue());
        }
        input.removeFirst(); // Discard END Lexer.Token
        
        // Loop however many times we need and then restore the original input List
        if(exec)
        {
            LinkedList<SimpleEntry<Lexer.Token, String>> backup = new LinkedList<SimpleEntry<Lexer.Token, String>>(input);
            while(cond != 0)
            {
                if(!stmtList.isEmpty())
                {
                    input = new LinkedList<SimpleEntry<Lexer.Token, String>>(stmtList);
                    parseStmtList();
                }
                input = new LinkedList<SimpleEntry<Lexer.Token, String>>(conditionExpr);
                input.addLast(new SimpleEntry<Lexer.Token, String>(Lexer.Token.ENDSTREAM, "End of Input"));
                cond = parseExpr();
            }
            input = new LinkedList<SimpleEntry<Lexer.Token, String>>(backup);
        }
    }

    public static void parseIf()
    {
        input.removeFirst(); // Discard IF Lexer.Token
        int cond = parseExpr();
        if(input.peekFirst().getKey() != Lexer.Token.THEN)
        {
            in.close();
            throw new IllegalStateException("Expected \"then\" at " + input.peekFirst().getValue());
        }
        input.removeFirst(); // Discard THEN Lexer.Token
        if(cond != 0) // If condition is true
        {
            if(input.peekFirst().getKey() != Lexer.Token.ELSE)
            {
                parseStmtList(); // Will exec based on existing flag setting
            }
            if(input.peekFirst().getKey() != Lexer.Token.ELSE)
            {
                in.close();
                throw new IllegalStateException("Expected \"else\" at " + input.peekFirst().getValue());
            }
            input.removeFirst(); // Discard ELSE Lexer.Token
            // If we are supposed to be executing, set exec to false to avoid executing the wrong code
            if(exec && input.peekFirst().getKey() != Lexer.Token.END)
            {
                exec = false;
                parseStmtList();
                exec = true;
            }
            else if(input.peekFirst().getKey() != Lexer.Token.END)
            {
                parseStmtList();
            }
            if(input.peekFirst().getKey() != Lexer.Token.END)
            {
                in.close();
                throw new IllegalStateException("Expected \"end\" at " + input.peekFirst().getValue());
            }
            input.removeFirst(); // Discard END Lexer.Token
        }
        else // If condition is false
        {
            if(exec && input.peekFirst().getKey() != Lexer.Token.ELSE)
            {
                exec = false;
                parseStmtList();
                exec = true;
            }
            else if(input.peekFirst().getKey() != Lexer.Token.ELSE)
            {
                parseStmtList();
            }
            if(input.peekFirst().getKey() != Lexer.Token.ELSE)
            {
                in.close();
                throw new IllegalStateException("Expected \"else\" at " + input.peekFirst().getValue());
            }
            input.removeFirst(); // Discard ELSE Lexer.Token
            if(input.peekFirst().getKey() != Lexer.Token.END)
            {
                parseStmtList(); // Will exec based on existing flag setting
            }
            if(input.peekFirst().getKey() != Lexer.Token.END)
            {
                in.close();
                throw new IllegalStateException("Expected \"end\" at " + input.peekFirst().getValue());
            }
            input.removeFirst(); // Discard END Lexer.Token
        }
    }
    
    public static void parseAssign()
    {
        String id = input.peekFirst().getValue();
        input.removeFirst(); // Remove ID that was just stored
        if(input.peekFirst().getKey() != Lexer.Token.LB)
        {
            if(input.peekFirst().getKey() != Lexer.Token.ASSIGN)
            {
                in.close();
                throw new IllegalStateException("Expected \"=\" after " + id);
            }
            input.removeFirst(); // Discard ASSIGN Lexer.Token
            int num = parseExpr();
            if(exec)
            {
                if(arrays.containsKey(id))
                {
                    in.close();
                    throw new IllegalStateException("Tried to create variable with same name as array " + id);
                }
                variables.put(id, num);
            }
        }
        else
        {
            Integer array[] = arrays.get(id);
            
            input.removeFirst(); // Discard LB left bracket Lexer.Token [
            
            int index = parseExpr();
            
            
            if(input.peekFirst().getKey() != Lexer.Token.RB)
            {
                in.close();
                throw new IllegalStateException("Missing closing ] on array access for " + id);
            }
            input.removeFirst(); // Discard RB right bracket Lexer.Token ]
            
            if(input.peekFirst().getKey() == Lexer.Token.LB)
            {
                in.close();
                throw new IllegalStateException("Multidimensional arrays are not supported, Array: " + id);
            }
            
            if(input.peekFirst().getKey() == Lexer.Token.SEMI) // Array Declaration, can be first assign in for loop but not second
            {
                if(variables.containsKey(id))
                {
                    in.close();
                    throw new IllegalStateException("Tried to create array with same name as variable " + id);
                }
                int size = index;
                if(!exec)
                {
                    size = 1;
                }
                if(size < 1)
                {
                    in.close();
                    throw new IllegalStateException("Cannot create 0 or negative size array! Array: " + id + " Size: " + size);
                }
                if(exec)
                {
                    Integer newArray[] = new Integer[size];
                    for(int i = 0 ; i < newArray.length ; i++)
                    {
                        newArray[i] = 0;
                    }
                    arrays.put(id, newArray);
                }
            }
            else
            {
                if(exec && array == null)
                {
                    in.close();
                    throw new IllegalStateException("Need to declare " + id + " array before using.");
                }
                if(!exec)
                {
                    index = 0;
                }
                if(exec && (index < 0 || index >= array.length))
                {
                    in.close();
                    throw new IllegalStateException("Array Index out of Bounds! Array: " + id + " Index: " + index);
                }
                if(input.peekFirst().getKey() != Lexer.Token.ASSIGN)
                {
                    in.close();
                    throw new IllegalStateException("Expected \"=\" after " + id);
                }
                input.removeFirst(); // Discard ASSIGN Lexer.Token
                int num = parseExpr();
                if(exec)
                {
                    array[index] = num;
                }
            }
        }
    }
    
    public static void parseInput()
    {
        input.removeFirst(); // Discard "get" keyword
        if(input.peekFirst().getKey() != Lexer.Token.ID)
        {
            in.close();
            throw new IllegalStateException("Syntax error: get " + input.peekFirst().getValue());
        }
        String id = input.removeFirst().getValue(); // Save ID
        if(input.peekFirst().getKey() != Lexer.Token.LB)
        {
            if(!arrays.containsKey(id))
            {
                // I will allow "get" to override the existing value of a variable
                String line = "0";
                if(exec)
                {
                    line = in.nextLine().trim();
                }
                int num = 0;
                try
                {
                    num = Integer.parseInt(line);
                }
                catch(NumberFormatException e)
                {
                    in.close();
                    throw new IllegalStateException("Malformed user input " + line + ", expected integer");
                }
                if(exec)
                {
                    variables.put(id, num);
                }
            }
            else // we have an existing array with that id, take input one int at a time
            {
                if(exec)
                {
                    String line = "0";
                    int num = 0;
                    Integer array[] = arrays.get(id);
                    for(int i = 0 ; i < array.length ; i++)
                    {
                        line = in.nextLine().trim();
                        try
                        {
                            num = Integer.parseInt(line);
                        }
                        catch(NumberFormatException e)
                        {
                            in.close();
                            throw new IllegalStateException("Malformed user input " + line + ", expected integer");
                        }
                        array[i] = num;
                    }
                }
            }
        }
        else
        {
            Integer array[] = arrays.get(id);
            if(array == null)
            {
                in.close();
                throw new IllegalStateException("Need to declare " + id + " array before using.");
            }
            input.removeFirst(); // Discard LB left bracket Lexer.Token [
            int index = parseExpr();
            if(input.peekFirst().getKey() != Lexer.Token.RB)
            {
                in.close();
                throw new IllegalStateException("Missing closing ] on array access for " + id);
            }
            input.removeFirst(); // Discard RB right bracket Lexer.Token ]
            
            if(input.peekFirst().getKey() == Lexer.Token.LB)
            {
                in.close();
                throw new IllegalStateException("Multidimensional arrays are not supported, Array: " + id);
            }
            
            if(index < 0 || index >= array.length)
            {
                in.close();
                throw new IllegalStateException("Array Index out of Bounds! Array: " + id + " Index: " + index);
            }

            String line = "0";
            if(exec)
            {
                line = in.nextLine().trim();
            }
            int num = 0;
            try
            {
                num = Integer.parseInt(line);
            }
            catch(NumberFormatException e)
            {
                in.close();
                throw new IllegalStateException("Malformed user input " + line + ", expected integer");
            }
            if(exec)
            {
                array[index] = num; // cannot call get with an unindexed array name, it will create a variable with that name instead
            }
        }
    }
    
    // <print> -> "print" STRING | "print" <expr>
    public static void parsePrint()
    {
        input.removeFirst(); // Discard "print" keyword
        switch(input.peekFirst().getKey())
        {
            case STR:
                SimpleEntry<Lexer.Token, String> t = input.peekFirst();
                String string = t.getValue();
                if(exec)
                {
                    System.out.print(string);
                }
                input.removeFirst(); // Remove STR that was just used
                break;
            case ID:
                if(arrays.containsKey(input.peekFirst().getValue()))
                {
                    Iterator<SimpleEntry<Lexer.Token, String>> it = input.iterator();
                    SimpleEntry<Lexer.Token, String> tok = it.next(); // get ID
                    tok = it.next(); // Lexer.Token after the ID, see if we have semicolon or not
                    if(tok.getKey() == Lexer.Token.SEMI)
                    {
                        // Here we are printing an entire array
                        String id = input.removeFirst().getValue();
                        Integer array[] = arrays.get(id);
                        if(exec)
                        {
                            System.out.print("[");
                            for(int i = 0 ; i < array.length - 1 ; i++)
                            {
                                System.out.print(array[i] + ", ");
                            }
                            System.out.print(array[array.length - 1] + "]");
                        }
                        break;
                    }
                }
            case INT:
            case MINUS:
            case NOT:
            case OPEN:
                int num = parseExpr();
                if(exec)
                {
                    System.out.print(num);
                }
                break;
            default:
                in.close();
                throw new IllegalStateException("Cannot print Lexer.Token " + input.peekFirst().getValue());
        }
    }
    
    public static void parseStmt()
    {
        switch(input.peekFirst().getKey())
        {
            case PRINT:
                parsePrint();
                break;
            case GET:
                parseInput();
                break;
            case ID: // assignment statement starts with an identifier
                parseAssign();
                break;
            case IF:
                parseIf();
                break;
            case WHILE:
                parseWhile();
                break;
            case FOR:
                parseFor();
                break;
            default: // no other Lexer.Token is a valid beginning to a statement
                in.close();
                throw new IllegalStateException("Invalid statement at " + input.peekFirst().getValue());
        }
    }
    
    // This method is used for the nested <stmt_list>s that appear inside other <stmt>s
    public static void parseStmtList()
    {
        switch(input.peekFirst().getKey())
        {
            case PRINT:
            case GET:
            case ID: // assignment statement starts with an identifier
            case IF:
            case WHILE:
            case FOR:
                parseStmt();
                if(input.peekFirst().getKey() != Lexer.Token.SEMI)
                {
                    in.close();
                    throw new IllegalStateException("Missing semicolon before " + input.peekFirst().getValue());
                }
                input.removeFirst();
                if(!input.isEmpty() && input.peekFirst().getKey() != Lexer.Token.ELSE && input.peekFirst().getKey() != Lexer.Token.END)
                {
                    parseStmtList();
                }
                break;
            default: // no other Lexer.Token is a valid beginning to a statement
                in.close();
                throw new IllegalStateException("Invalid statement at " + input.peekFirst().getValue());
        }
    }
    
    // Our <prog>'s global <stmt_list> ends only with ENDSTREAM Lexer.Token, but nested <stmt_list>s inside other
    // <stmt>s will end with "else" or "end". We will call parseProg repeatedly until we hit ENDSTREAM
    // and simply parse each <stmt>.
    public static void parseProg()
    {
        // parse one statement and then call again if we have more
        switch(input.peekFirst().getKey())
        {
            case PRINT:
            case GET:
            case ID: // assignment statement starts with an identifier
            case IF:
            case WHILE:
            case FOR:
                parseStmt();
                if(input.peekFirst().getKey() != Lexer.Token.SEMI)
                {
                    in.close();
                    throw new IllegalStateException("Missing semicolon before " + input.peekFirst().getValue());
                }
                input.removeFirst();
                if(input.peekFirst().getKey() != Lexer.Token.ENDSTREAM)
                {
                    parseProg();
                }
                break;
            default: // no other Lexer.Token is a valid beginning to a statement
                in.close();
                throw new IllegalStateException("Invalid statement at " + input.peekFirst().getValue());
        }
    }

    // Pass the path to a text file containing a program as the only argument please.
    public static void main(String[] args)
    {
        if(args.length == 1)
        {
            try
            {
                Scanner f = new Scanner(new File(args[0].trim()));
                StringBuilder sb = new StringBuilder();
                while(f.hasNextLine())
                {
                    sb.append(f.nextLine());
                }
                String str = sb.toString();
                try
                {
                    input = Lexer.lex(str.toCharArray());
                    parseProg();
                }
                catch(Exception e)
                {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
            catch(FileNotFoundException e)
            {
                System.out.println("Parser: Need to pass a valid filepath as the only argument. File not found!");
                System.exit(1);
            }
            finally
            {
                in.close();
            }
        }
        else if(args.length < 1)
        {
            System.out.println("Parser: Need to pass a valid filepath as the only argument. No argument found!");
            in.close();
            System.exit(1);
        }
        else
        {
            System.out.println("Parser: Need to pass a valid filepath as the only argument. Too many arguments found!");
            in.close();
            System.exit(1);
        }
    }
}
