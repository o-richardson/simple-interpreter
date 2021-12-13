// Author:         OAR180004
// Course:         CS 4337.502
// Professor:      Dr. Salazar
// Term:           Spring 2021
// Title:          Project Part 2
// File:           Lexer.java

import java.util.Arrays;
import java.util.LinkedList;
import java.util.AbstractMap.SimpleEntry;

public class Lexer
{
    public static enum Token
    {
        LT,     // <
        LTE,    // <=
        GT,     // >
        GTE,    // >=
        EQ,     // ==
        NEQ,    // !=
        CLOSE,  // )
        OPEN,   // (
        ASSIGN, // =
        SEMI,   // ;
        PLUS,   // +
        MINUS,  // -
        TIMES,  // *
        DIV,    // /
        MOD,    // %

        LB,     // [
        RB,     // ]

        DOT,    // .

        LENGTH, // length keyword

        OR,     // Reserved words, all are actually lower case in the language
        AND,
        NOT,
        PRINT,
        GET,
        IF,
        WHILE,
        FOR,
        DO,
        THEN,
        ELSE,
        END,

        STR,    // String, "([^"]-\\")*"
        INT,    // Immediate integer value, [0-9]+
        ID,     // Variable name [_a-zA-Z][_a-zA-Z0-9]*

        ENDSTREAM // end of input indicator
    }

    private static int line = 1;

    // ID's start with underscore or letter only
    private static boolean isIDStart(char c)
    {
        return c == '_' || Character.isLetter(c);
    }

    // ID's may contain numbers as long as they are not the first character
    private static boolean isIDChar(char c)
    {
        return isIDStart(c) || Character.isDigit(c);
    }

    private static SimpleEntry<SimpleEntry<Token, String>, char[]> lexID(char[] input)
    {
        String lexeme = "";
        lexeme += input[0];
        int i = 1;
        while(i < input.length && isIDChar(input[i]))
        {
            lexeme += input[i];
            i++;
        }
        // Check if our complete lexeme matches any reserved words
        Token t;
        switch(lexeme)
        {
            case "length":
                t = Token.LENGTH;
                break;
            case "or":
                t = Token.OR;
                break;
            case "and":
                t = Token.AND;
                break;
            case "not":
                t = Token.NOT;
                break;
            case "print":
                t = Token.PRINT;
                break;
            case "get":
                t = Token.GET;
                break;
            case "if":
                t = Token.IF;
                break;
            case "while":
                t = Token.WHILE;
                break;
            case "for":
                t = Token.FOR;
                break;
            case "do":
                t = Token.DO;
                break;
            case "then":
                t = Token.THEN;
                break;
            case "else":
                t = Token.ELSE;
                break;
            case "end":
                t = Token.END;
                break;
            default:
                t = Token.ID; // Lexeme is a real ID not matching any keywords
                break;
        }
        return new SimpleEntry<SimpleEntry<Token, String>, char[]>
        (
            new SimpleEntry<Token, String>(t, lexeme),
            Arrays.copyOfRange(input, i, input.length)
        );
    }

    private static SimpleEntry<SimpleEntry<Token, String>, char[]> lexINT(char[] input)
    {
        String lexeme = "";
        lexeme += input[0];
        int i = 1;
        while(i < input.length && Character.isDigit(input[i]))
        {
            lexeme += input[i];
            i++;
        }
        return new SimpleEntry<SimpleEntry<Token, String>, char[]>
        (
            new SimpleEntry<Token, String>(Token.INT, lexeme),
            Arrays.copyOfRange(input, i, input.length)
        );
    }

    // throws Exception when out of array space without hitting un-escaped quote
    private static SimpleEntry<SimpleEntry<Token, String>, char[]> lexSTR(char[] input) throws IllegalStateException
    {
        String lexeme = "";
        // We know input[0] is a quote since lexSTR is triggered by a quote
        int i = 1;
        while(i < input.length && input[i] != '\"')
        {
            if(input[i] == '\n') // unescaped newline
            {
                line++;
            }
            if(input[i] == '\\') // The escape backslash is never added to the lexeme
            {
                try
                {
                    switch(input[i + 1])
                    {
                        case '\\':
                            lexeme += '\\';
                            break;
                        case 't':
                            lexeme += '\t';
                            break;
                        case 'n':
                            lexeme += '\n';
                            break;
                        case '\"':
                            lexeme += '\"';
                            break;
                        default:
                            // any other character is not a valid escape sequence, ignore it
                            break;
                    }
                }
                catch(ArrayIndexOutOfBoundsException e)
                {
                    throw new IllegalStateException("Expected end quote for String on line " + line);
                }
                i += 2; // Just processed backslash and another character, move by 2
            }
            else // regular non-quote non-escaped character
            {
                lexeme += input[i];
                i++;
            }
        }
        try
        {
            return new SimpleEntry<SimpleEntry<Token, String>, char[]>
            (
                new SimpleEntry<Token, String>(Token.STR, lexeme),
                Arrays.copyOfRange(input, i + 1, input.length) // i + 1 to ignore the last quote
            );
        }
        catch(IllegalArgumentException e)
        {
            throw new IllegalStateException("Expected end quote for String on line " + line);
        }
    }

    private static SimpleEntry<SimpleEntry<Token, String>, char[]> nextToken(char[] input) throws IllegalStateException
    {
        int i = 0; // Ignore whitespace but count lines for error reporting
        while(i < input.length && Character.isWhitespace(input[i]))
        {
            if(input[i] == '\n')
            {
                line++;
            }
            i++;
        }
        if(i >= input.length)
        {
            return new SimpleEntry<SimpleEntry<Token, String>, char[]>
            (
                new SimpleEntry<Token, String>(Token.ENDSTREAM, "End of Input"),
                null
            );
        }
        else if(Character.isDigit(input[i])) // INT
        {
            return lexINT(Arrays.copyOfRange(input, i, input.length));
        }
        else if(isIDStart(input[i])) // ID
        {
            return lexID(Arrays.copyOfRange(input, i, input.length));
        }
        else if(input[i] == '\"') // STR
        {
            return lexSTR(Arrays.copyOfRange(input, i, input.length));
        }
        // If not a keyword, ID, INT, or STR
        Token t;
        String lexeme = "";
        switch(input[i])
        {
            case '<':
                if(i + 1 < input.length && input[i + 1] == '=')
                {
                    t = Token.LTE;
                    lexeme = "<=";
                    i += 2;
                }
                else
                {
                    t = Token.LT;
                    lexeme = "<";
                    i++;
                }
                break;
            case '>':
                if(i + 1 < input.length && input[i + 1] == '=')
                {
                    t = Token.GTE;
                    lexeme = ">=";
                    i += 2;
                }
                else
                {
                    t = Token.GT;
                    lexeme = ">";
                    i++;
                }
                break;
            case '=':
                if(i + 1 < input.length && input[i + 1] == '=')
                {
                    t = Token.EQ;
                    lexeme = "==";
                    i += 2;
                }
                else
                {
                    t = Token.ASSIGN;
                    lexeme = "=";
                    i++;
                }
                break;
            case '!':
                if(i + 1 < input.length && input[i + 1] == '=')
                {
                    t = Token.NEQ;
                    lexeme = "!=";
                    i += 2;
                }
                else
                {
                    throw new IllegalStateException("Expected = after ! on line " + line);
                }
                break;
            case ')':
                t = Token.CLOSE;
                lexeme = ")";
                i++;
                break;
            case '(':
                t = Token.OPEN;
                lexeme = "(";
                i++;
                break;
            case ';':
                t = Token.SEMI;
                lexeme = ";";
                i++;
                break;
            case '+':
                t = Token.PLUS;
                lexeme = "+";
                i++;
                break;
            case '-':
                t = Token.MINUS;
                lexeme = "-";
                i++;
                break;
            case '*':
                t = Token.TIMES;
                lexeme = "*";
                i++;
                break;
            case '/':
                t = Token.DIV;
                lexeme = "/";
                i++;
                break;
            case '%':
                t = Token.MOD;
                lexeme = "%";
                i++;
                break;
            case '[':
                t = Token.LB;
                lexeme = "[";
                i++;
                break;
            case ']':
                t = Token.RB;
                lexeme = "]";
                i++;
                break;
            case '.':
                t = Token.DOT;
                lexeme = ".";
                i++;
                break;
            default:
                throw new IllegalStateException("Unrecognized symbol " + input[i] + " on line " + line);
        }
        return new SimpleEntry<SimpleEntry<Token, String>, char[]>
        (
            new SimpleEntry<Token, String>(t, lexeme),
            Arrays.copyOfRange(input, i, input.length)
        );
    }

//    private static void printToken(SimpleEntry<SimpleEntry<Token, String>, char[]> t)
//    {
//        Token tok = t.getKey().getKey();
//        String lexeme;
//        if(tok == Token.STR || tok == Token.INT || tok == Token.ID)
//        {
//            lexeme = t.getKey().getValue();
//            System.out.println(tok.name() + "(" + lexeme + ")");
//        }
//        else
//        {
//            System.out.println(tok.name());
//        }
//    }

    private Lexer()
    {
        // Do not instantiate
    }

    public static LinkedList<SimpleEntry<Token, String>> lex(char[] input)
    {
        LinkedList<SimpleEntry<Token, String>> list = new LinkedList<SimpleEntry<Token, String>>();
        try
        {
            SimpleEntry<SimpleEntry<Token, String>, char[]> temp = nextToken(input);
            while(temp.getKey().getKey() != Token.ENDSTREAM)
            {
                list.addLast(temp.getKey());
                temp = nextToken(temp.getValue());
            }
            list.addLast(temp.getKey());
        }
        catch(IllegalStateException e)
        {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        return list;
    }
}
