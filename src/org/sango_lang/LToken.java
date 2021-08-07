/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2018 Isao Akiyama                                         *
 *                                                                         *
 * Permission is hereby granted, free of charge, to any person obtaining   *
 * a copy of this software and associated documentation files (the         *
 * "Software"), to deal in the Software without restriction, including     *
 * without limitation the rights to use, copy, modify, merge, publish,     *
 * distribute, sublicense, and/or sell copies of the Software, and to      *
 * permit persons to whom the Software is furnished to do so, subject to   *
 * the following conditions:                                               *
 *                                                                         *
 * The above copyright notice and this permission notice shall be          *
 * included in all copies or substantial portions of the Software.         *
 *                                                                         *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,         *
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF      *
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  *
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY    *
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,    *
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE       *
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                  *
 ***************************************************************************/
package org.sango_lang;

public class LToken {
  public static final int WORD = 1;
  public static final int BYTE = 2;
  public static final int INT = 3;
  public static final int REAL = 4;
  public static final int CHAR = 5;
  public static final int CSTR = 6;
  public static final int DOT = 11;
  public static final int COMMA = 13;
  public static final int SEM = 15;
  public static final int SEM_SEM = 16;
  public static final int COL = 17;
  public static final int COL_COL = 18;
  public static final int LPAR = 21;
  public static final int RPAR = 22;
  public static final int LBRACKET = 23;
  public static final int RBRACKET = 24;
  public static final int LBRACE = 25;
  public static final int RBRACE = 26;
  public static final int LPAR_VBAR = 31;
  public static final int VBAR_RPAR = 32;
  public static final int LBRACKET_VBAR = 33;
  public static final int VBAR_RBRACKET = 34;
  public static final int LBRACKET_SLASH = 35;
  public static final int SLASH_RBRACKET = 36;
  public static final int EQ = 41;
  public static final int EQ_EQ = 42;
  public static final int EQ_GT = 43;
  public static final int LT = 44;
  public static final int GT = 45;
  public static final int GT_GT = 46;
  public static final int PLUS = 51;
  public static final int PLUS_PLUS = 52;
  public static final int AST = 53;
  public static final int AST_AST = 54;
  public static final int AST_AST_AST = 55;
  public static final int SLASH = 56;
  public static final int BKSLASH = 57;
  public static final int BKSLASH_BKSLASH = 58;
  public static final int VBAR = 61;
  public static final int VBAR_VBAR = 62;
  public static final int AMP = 63;
  public static final int AMP_AMP = 64;
  public static final int CARET = 65;
  public static final int CARET_CARET = 66;
  public static final int TILD = 67;
  public static final int COL_EQ = 71;
  public static final int HYPH_GT = 72;
  public static final int EXCLA = 75;
  public static final int OTHER = 99;

  public int tag;
  public int lineNum;
  public boolean space;
  public String token;
  public int intValue;
  public double realValue;
  public int charValue;
  public Cstr cstrValue;

  LToken(int t, int ln, boolean sp, String s) {
    this.tag = t;
    this.lineNum = ln;
    this.space = sp;
    this.token = s;
  }

  static LToken createWord(int ln, boolean sp, String s) {
    return new LToken(WORD, ln, sp, s);
  }

  static LToken createByte(int ln, boolean sp, String s, int val) {
    LToken t = new LToken(BYTE, ln, sp, s);
    t.intValue = val;
    return t;
  }

  static LToken createInt(int ln, boolean sp, String s, int val) {
    LToken t = new LToken(INT, ln, sp, s);
    t.intValue = val;
    return t;
  }

  static LToken createReal(int ln, boolean sp, String s, double val) {
    LToken t = new LToken(REAL, ln, sp, s);
    t.realValue = val;
    return t;
  }

  static LToken createChar(int ln, boolean sp, String s, int val) {
    LToken t = new LToken(CHAR, ln, sp, s);
    t.charValue = val;
    return t;
  }

  static LToken createCstr(int ln, boolean sp, String s, Cstr cs) {
    LToken t = new LToken(CSTR, ln, sp, s);
    t.cstrValue = cs;
    return t;
  }

  static LToken createSym(int ln, boolean sp, String s, int t) {
    return new LToken(t, ln, sp, s);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("tag:");
    buf.append(this.tag);
    buf.append(" lineno:");
    buf.append(this.lineNum);
    buf.append(" space:");
    buf.append(this.space);
    buf.append(" token:");
    buf.append(this.token);
    buf.append(" int:");
    buf.append(this.intValue);
    buf.append(" real:");
    buf.append(this.realValue);
    buf.append(" char:");
    buf.append(this.charValue);
    buf.append(" cstr:");
    buf.append(this.cstrValue);
    return buf.toString();
  }
}
