package edu.cmu.ml.rtw.generic.parse;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import java_cup.runtime.*;

public class CtxScanner implements Scanner {
	private static final char[] SPECIAL_CHARACTERS = { ',', '=', ';', 'o', '[', ']', '{', '}', '(', ')', '$', '-', '>' };
	private static final int[] SPECIAL_CHARACTER_SYMBOLS = { CtxSymbol.COMMA, CtxSymbol.EQUALS, CtxSymbol.SEMI,
		CtxSymbol.COMP, CtxSymbol.LSQUARE_BRACKET, CtxSymbol.RSQUARE_BRACKET, CtxSymbol.LCURLY_BRACE, 
		CtxSymbol.RCURLY_BRACE, CtxSymbol.LPAREN, CtxSymbol.RPAREN, CtxSymbol.DOLLAR, 
		CtxSymbol.RIGHT_ARROW, CtxSymbol.RIGHT_ARROW};
	private Reader reader;
	private SymbolFactory symbolFactory;
	private int nextChar;
	
	public CtxScanner(String str) {
		this(new StringReader(str));
	}
	
	public CtxScanner(Reader reader) {
		this.reader = reader;
		this.symbolFactory = new ComplexSymbolFactory();
	}

	public Symbol next_token() throws IOException {
		do {
			if (this.nextChar == 0)
				advance();
			if (this.nextChar == -1)
				break;
			
			char nextChar = (char)this.nextChar;
			if (Character.isWhitespace(nextChar)) {
				advance();
				continue;
			} else if (nextChar == ',') {
				advance();
				return this.symbolFactory.newSymbol("COMMA", CtxSymbol.COMMA);
			} else if (nextChar == '=') {
				advance();
				return this.symbolFactory.newSymbol("EQUALS", CtxSymbol.EQUALS);
			} else if (nextChar == ';') {
				advance();
				return this.symbolFactory.newSymbol("SEMI", CtxSymbol.SEMI);
			} else if (nextChar == 'o') {
				advance();
				return this.symbolFactory.newSymbol("COMP", CtxSymbol.COMP);
			} else if (nextChar == '[') {
				advance();
				return this.symbolFactory.newSymbol("LSQUARE_BRACKET", CtxSymbol.LSQUARE_BRACKET);
			} else if (nextChar == ']') {
				advance();
				return this.symbolFactory.newSymbol("RSQUARE_BRACKET", CtxSymbol.RSQUARE_BRACKET);
			} else if (nextChar == '{') {
				advance();
				return this.symbolFactory.newSymbol("LCURLY_BRACE", CtxSymbol.LCURLY_BRACE);
			} else if (nextChar == '}') {
				advance();
				return this.symbolFactory.newSymbol("RCURLY_BRACE", CtxSymbol.RCURLY_BRACE);
			} else if (nextChar == '(') {
				advance();
				return this.symbolFactory.newSymbol("LPAREN", CtxSymbol.LPAREN);
			} else if (nextChar == ')') {
				advance();
				return this.symbolFactory.newSymbol("RPAREN", CtxSymbol.RPAREN);
			} else if (nextChar == '$') {
				advance();
				return this.symbolFactory.newSymbol("DOLLAR", CtxSymbol.DOLLAR);
			} else if (nextChar == '-') {
				if (advance() != '>')
					return this.symbolFactory.newSymbol("error", CtxSymbol.error);
				advance();
				return this.symbolFactory.newSymbol("RIGHT_ARROW", CtxSymbol.RIGHT_ARROW);
			} else {
				return nextString();
			}
		} while (true);
		
		return this.symbolFactory.newSymbol("EOF", CtxSymbol.EOF);
    }
	
	private Symbol nextString() throws IOException {
		StringBuilder str = new StringBuilder();
		boolean inQuotes = false;
		boolean escapeCharacter = false;
		
		do {
			if (this.nextChar == -1)
				return this.symbolFactory.newSymbol("error", CtxSymbol.error);
			char nextChar = (char)this.nextChar;
			if (inQuotes) {
				if (escapeCharacter) {
					str.append(nextChar);
					escapeCharacter = false;
				} else if (nextChar == '\\')
					escapeCharacter = true;
				else if  (nextChar == '"') {
					inQuotes = false;
					advance();
					break;
				} else {
					str.append(nextChar);
				}
			} else {
				if (str.length() == 0 && nextChar == '"')
					inQuotes = true;
				else if (Character.isWhitespace(nextChar)
						  || (isSpecialCharacter(nextChar)) && nextChar != 'o') // FIXME: This is a hack
					break;
				else
					str.append(nextChar);
			}
			advance();
		} while (true);
		
		return this.symbolFactory.newSymbol("STRING", CtxSymbol.STRING, str.toString());
	}
	
    private int advance() throws IOException  {
    	this.nextChar = this.reader.read();
    	return this.nextChar;
    }
    
    public static int getStringSymbol(String str) {
		for (int i = 0; i < SPECIAL_CHARACTERS.length; i++) {
			if (str.equals("->"))
				return CtxSymbol.RIGHT_ARROW;
			else if (String.valueOf(SPECIAL_CHARACTERS[i]).equals(str))
				return SPECIAL_CHARACTER_SYMBOLS[i];
		}
		return -1;
	}
	
	private static boolean isSpecialCharacter(char c) {
		for (int i = 0; i < SPECIAL_CHARACTERS.length; i++) {
			if (SPECIAL_CHARACTERS[i] == c)
				return true;
		}
		return false;
	}
	
	public static boolean isQuotedString(String str) {
		return str.startsWith("\"") && str.endsWith("\"");
	}
	
	public static String unescapeQuotedString(String str) {
		return str.substring(1, str.length() - 1).replaceAll("\\\\", "");
	}
}
