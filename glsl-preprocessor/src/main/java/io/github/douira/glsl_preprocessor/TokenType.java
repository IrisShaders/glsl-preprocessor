/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.douira.glsl_preprocessor;

import static io.github.douira.glsl_preprocessor.Token.*;

import java.util.*;

import edu.umd.cs.findbugs.annotations.*;

/**
 * @author shevek
 */
record TokenType(String name, String text) {

	private static final List<TokenType> TYPES = new ArrayList<>();

	private static void addTokenType(int type, @NonNull String name, @CheckForNull String text) {
		while (TYPES.size() <= type)
			TYPES.add(null);
		TYPES.set(type, new TokenType(name, text));
	}

	private static void addTokenType(int type, @NonNull String name) {
		addTokenType(type, name, null);
	}

	@CheckForNull
	public static TokenType getTokenType(int type) {
		try {
			return TYPES.get(type);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	@NonNull
	public static String getTokenName(int type) {
		if (type < 0)
			return "Invalid" + type;
		TokenType tokenType = getTokenType(type);
		if (tokenType == null)
			return "Unknown" + type;
		return tokenType.name();
	}

	@CheckForNull
	public static String getTokenText(int type) {
		TokenType tokenType = getTokenType(type);
		if (tokenType == null)
			return null;
		return tokenType.text();
	}

	static {
		for (int i = 0; i < 255; i++) {
			String text = String.valueOf((char) i);
			addTokenType(i, text, text);
		}
		addTokenType(AND_EQ, "AND_EQ", "&=");
		addTokenType(ARROW, "ARROW", "->");
		addTokenType(CHARACTER, "CHARACTER");
		addTokenType(CCOMMENT, "CCOMMENT");
		addTokenType(CPPCOMMENT, "CPPCOMMENT");
		addTokenType(DEC, "DEC", "--");
		addTokenType(DIV_EQ, "DIV_EQ", "/=");
		addTokenType(ELLIPSIS, "ELLIPSIS", "...");
		addTokenType(EOF, "EOF");
		addTokenType(EQ, "EQ", "==");
		addTokenType(GE, "GE", ">=");
		addTokenType(HASH, "HASH", "#");
		addTokenType(HEADER, "HEADER");
		addTokenType(IDENTIFIER, "IDENTIFIER");
		addTokenType(INC, "INC", "++");
		addTokenType(NUMBER, "NUMBER");
		addTokenType(LAND, "LAND", "&&");
		addTokenType(LAND_EQ, "LAND_EQ", "&&=");
		addTokenType(LE, "LE", "<=");
		addTokenType(LITERAL, "LITERAL");
		addTokenType(LOR, "LOR", "||");
		addTokenType(LOR_EQ, "LOR_EQ", "||=");
		addTokenType(LSH, "LSH", "<<");
		addTokenType(LSH_EQ, "LSH_EQ", "<<=");
		addTokenType(MOD_EQ, "MOD_EQ", "%=");
		addTokenType(MULT_EQ, "MULT_EQ", "*=");
		addTokenType(NE, "NE", "!=");
		addTokenType(NL, "NL");
		addTokenType(OR_EQ, "OR_EQ", "|=");
		addTokenType(PASTE, "PASTE", "##");
		addTokenType(PLUS_EQ, "PLUS_EQ", "+=");
		addTokenType(RANGE, "RANGE", "..");
		addTokenType(RSH, "RSH", ">>");
		addTokenType(RSH_EQ, "RSH_EQ", ">>=");
		addTokenType(SQSTRING, "SQSTRING");
		addTokenType(STRING, "STRING");
		addTokenType(SUB_EQ, "SUB_EQ", "-=");
		addTokenType(WHITESPACE, "WHITESPACE");
		addTokenType(XOR_EQ, "XOR_EQ", "^=");
		addTokenType(M_ARG, "M_ARG");
		addTokenType(M_PASTE, "M_PASTE");
		addTokenType(M_STRING, "M_STRING");
		addTokenType(P_LINE, "P_LINE");
		addTokenType(INVALID, "INVALID");
	}

	TokenType(@NonNull String name, @CheckForNull String text) {
		this.name = name;
		this.text = text;
	}

	@Override
	@NonNull
	public String name() {
		return name;
	}

	@Override
	@CheckForNull
	public String text() {
		return text;
	}
}
