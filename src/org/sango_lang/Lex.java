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

import java.io.LineNumberReader;
import java.io.IOException;

public class Lex {
  Cstr srcName;
  LineNumberReader reader;
  Cstr line;
  int lineno;
  boolean space;
  int start;
  int pos;
  int state;
  int radix;
  int charVal;
  Cstr cstr;
  LToken token;

  public Lex(Cstr srcName, LineNumberReader r) throws CompileException, IOException {
    this.srcName = srcName;
    this.reader = r;
    this.lineno = this.reader.getLineNumber();
    this.line = this.processCharEntity(this.reader.readLine());
    this.space = true;
  }

  public Cstr getSrcName() { return this.srcName; }

  private String getCurrentLineLoc() {
    return this.srcName.toJavaString() + ":L" + this.lineno;
  }

  public LToken readToken() throws CompileException, IOException {
    if (this.token == null) {
      this.analyze();
    }
    LToken t = this.token;
    this.token = null;
    return t;
  }

  private void analyze() throws CompileException, IOException {
    if (this.line == null) { return; }
    int c;
    do {
      if (this.pos < this.line.getLength()) {
        c = this.line.getCharAt(this.pos);
      } else if (this.pos == this.line.getLength()) {
        c = '\n';
      } else {
        this.lineno = this.reader.getLineNumber();
        this.line = this.processCharEntity(this.reader.readLine());
        if (this.line == null) { return; }
        this.pos = 0;
        c = (this.line.getLength() == 0)? '\n': this.line.getCharAt(0);
      }
    } while(!this.dispatch(c));
  }

  Cstr processCharEntity(String line) throws CompileException {
    StringBuffer emsg;
    Cstr L;
    if (line == null) { return null; } 
    Cstr s = new Cstr(line);
    boolean e = false;
    for (int i = 0; !e && i < s.getLength(); i++) {  // scan '`'
      e = s.getCharAt(i) == '`';
    }
    if (!e) { return s; }  // if not included, no process needed
    L = new Cstr();
    int ent = -2;  // -2:out of seq, -1:"`", 0:"`u", 1:"`uH", 2:"`uHH", ...
    int uc = 0;  // dummy init
    int[] ucs = null;  // unicode buffer, dummy init
    for (int i = 0; i < s.getLength(); i++) {
      int c = s.getCharAt(i);
      if (ent < -1) {
        if (c == '`') {  // enter ent seq
          ent = -1;
        } else {  // raw
          L.append(c);
        }
      } else if (ent == -1) {
        if (c == 'u') {  // unicode
          ent = 0;
          uc = 0;
          ucs = new int[8];
        } else if (c == '`') {  // cancel entering seq but char is '`' itself
          L.append(c);
          ent = -2;
        } else {
          emsg = new StringBuffer();
          emsg.append("Invalid entity sequence indicator '");
          emsg.appendCodePoint(c);
          emsg.append("' at ");
          emsg.append(this.getCurrentLineLoc());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      } else if (c == ';') {  // end seq
        if (ent == 0) {
          emsg = new StringBuffer();
          emsg.append("No hexadecimal digit at ");
          emsg.append(this.getCurrentLineLoc());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        L.append(uc);
        ent = -2;         
      } else if (!isHexDigit(c)) {
        emsg = new StringBuffer();
        emsg.append("Invalid hexadecimal digit after `");
        for (int j = 0; j < ent; j++) {
          emsg.appendCodePoint(ucs[j]);
        }
        emsg.append(" at ");
        emsg.append(this.getCurrentLineLoc());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      } else if (ent >= 8) {
        emsg = new StringBuffer();
        emsg.append("Too long hexadecimal digits at ");
        emsg.append(this.getCurrentLineLoc());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      } else {
        ucs[ent++] = c;
        uc = uc << 4;
        uc += hexDigitCharToInt(c);
        if (uc > 0x10ffff) {
          emsg = new StringBuffer();
          emsg.append("Too large Unicode value at ");
          emsg.append(this.getCurrentLineLoc());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      }
    }
    if (ent > -2) {
      emsg = new StringBuffer();
      emsg.append("Incomplete charactor entity at ");
      emsg.append(this.getCurrentLineLoc());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return L;
  }

  private void consumed() {
    this.pos++;
  }

  private void setStart() {
    this.start = this.pos;
  }

  private void transit(int s) {
    this.state = s;
  }

  private void setWordToken() {
    this.token = LToken.createWord(this.lineno, this.space, this.line.substring(this.start, this.pos).toJavaString());
    this.space = false;
  }

  private void setByteToken() throws CompileException {
    StringBuffer emsg;
    String b = this.line.substring(this.start, this.pos).toJavaString();
    int i = Integer.parseInt(b.substring(0, b.length() - 1), this.radix);  // remove postfix '~'
    if (i < 0 || i > 255) {
      emsg = new StringBuffer();
      emsg.append("Byte value out of range at ");
      emsg.append(this.getCurrentLineLoc());
      emsg.append(". - ");
      emsg.append(i);
      throw new CompileException(emsg.toString());
    }
    this.token = LToken.createByte(this.lineno, this.space, b, i);
    this.space = false;
    this.radix = 0;
  }

  private void setIntToken() {
    String i = this.line.substring(this.start, this.pos).toJavaString();
    this.token = LToken.createInt(this.lineno, this.space, i,
      (this.radix == 10)? Integer.parseInt(i, this.radix): Integer.parseUnsignedInt(i, this.radix));
    this.space = false;
    this.radix = 0;
  }

  private void setRealToken() {
    String r = this.line.substring(this.start, this.pos).toJavaString();
    this.token = LToken.createReal(this.lineno, this.space, r, Double.parseDouble(r));
    this.space = false;
  }

  private void setChar(int c) {
    this.charVal = (int)c;
  }

  private void setCharToken() {
    this.token = LToken.createChar(this.lineno, this.space, this.line.substring(this.start, this.pos).toJavaString(), this.charVal);
    this.space = false;
  }

  private void setCstrToken() {
    this.token = LToken.createCstr(this.lineno, this.space, this.line.substring(this.start, this.pos).toJavaString(), this.cstr);
    this.space = false;
    this.cstr = null;
  }

  private void setSymToken(int tag) {
    this.token = LToken.createSym(this.lineno, this.space, this.line.substring(this.start, this.pos).toJavaString(), tag);
    this.space = false;
  }

  private static boolean isSpace(int c) {
    return Character.isWhitespace(c);
  }

  static boolean isNormalWordStart(int c) {
    return
      'A' <= c && c <= 'Z'
      || 'a' <= c && c <= 'z'
      || c == '_';
  }

  static boolean isWordStart(int c) {
    return
      isNormalWordStart(c)
      || c == '@'
      || c == '$';
  }

  static boolean isWordPart(int c) {
    return
      isWordStart(c)
      || c == '?'
      || c == '\''
      || isDecimalDigit(c);
  }

  private static boolean isDecimalDigit(int c) {
    return '0' <= c && c <= '9';
  }

  private static boolean isHexDigit(int c) {
    return
      '0' <= c && c <= '9'
      || 'A' <= c && c <='F'
      || 'a' <= c && c <='f';
  }

  private static boolean isOctalDigit(int c) {
    return '0' <= c && c <= '7';
  }

  private static int hexDigitCharToInt(int c) {
    int h;
    if ('0' <= c && c <= '9') {
      h = c - '0';
    } else if ('A' <= c && c <='F') {
      h = c - 'A' + 10;
    } else if ('a' <= c && c <='f') {
      h = c - 'a' + 10;
    } else {
      throw new IllegalArgumentException("Not hexadecimal digit.");
    }
    return h;
  }

  private boolean dispatch(int c) throws CompileException {
    boolean fin = false;
    StringBuffer errmsg = null;
    switch (this.state) {
    case 0: fin = dispatch0(c); break;
    case 50: fin = dispatch50(c); break;
    case 51: fin = dispatch51(c); break;
    case 52: fin = dispatch52(c); break;
    case 61: fin = dispatch61(c); break;
    case 62: fin = dispatch62(c); break;
    case 100: fin = dispatch100(c); break;
    case 200: fin = dispatch200(c); break;
    case 250: fin = dispatch250(c); break;
    case 260: fin = dispatch260(c); break;
    case 270: fin = dispatch270(c); break;
    case 300: fin = dispatch300(c); break;
    case 301: fin = dispatch301(c); break;
    case 310: fin = dispatch310(c); break;
    case 400: fin = dispatch400(c); break;
    case 410: fin = dispatch410(c); break;
    case 420: fin = dispatch420(c); break;
    case 500: fin = dispatch500(c); break;
    case 510: fin = dispatch510(c); break;
    case 520: fin = dispatch520(c); break;
    case 530: fin = dispatch530(c); break;
    case 540: fin = dispatch540(c); break;
    case 541: fin = dispatch541(c); break;
    case 550: fin = dispatch550(c); break;
    case 560: fin = dispatch560(c); break;
    case 570: fin = dispatch570(c); break;
    case 580: fin = dispatch580(c); break;
    case 590: fin = dispatch590(c); break;
    case 600: fin = dispatch600(c); break;
    case 610: fin = dispatch610(c); break;
    case 620: fin = dispatch620(c); break;
    case 900: fin = dispatch900(c); break;
    default:
      errmsg = new StringBuffer();
      errmsg.append("Internal error (Lex) at ");
      errmsg.append(this.getCurrentLineLoc());
      errmsg.append(" : invalid state ");
      errmsg.append(this.state);
      throw new CompileException(errmsg.toString());
    }
    return fin;
  }

  private boolean dispatch0(int c) throws CompileException {
    StringBuffer errmsg = null;
    if (isSpace(c)) {
      this.consumed(); this.space = true; this.transit(0); return false;
    } else if (isWordStart(c)) {
      this.setStart(); this.consumed(); this.transit(100); return false;
    } else if (isDecimalDigit(c)) {
      this.radix = 10;
      this.setStart(); this.consumed(); this.transit(200); return false;
    } else {
      switch (c) {
      case '\n': this.consumed(); this.space = true; this.transit(0); return false;
      case '%': this.setStart(); this.consumed(); this.transit(50); return false;
      case '\'': this.setStart(); this.consumed(); this.transit(300); return false;
      case ';': this.setStart(); this.consumed(); this.transit(500); return false;
      case '>': this.setStart(); this.consumed(); this.transit(510); return false;
      case ':': this.setStart(); this.consumed(); this.transit(520); return false;
      case '-': this.setStart(); this.consumed(); this.transit(530); return false;
      case '*': this.setStart(); this.consumed(); this.transit(540); return false;
      case '|': this.setStart(); this.consumed(); this.transit(550); return false;
      case '&': this.setStart(); this.consumed(); this.transit(560); return false;
      case '(': this.setStart(); this.consumed(); this.transit(570); return false;
      case '^': this.setStart(); this.consumed(); this.transit(580); return false;
      case '[': this.setStart(); this.consumed(); this.transit(590); return false;
      case '=': this.setStart(); this.consumed(); this.transit(600); return false;
      case '+': this.setStart(); this.consumed(); this.transit(610); return false;
      case '\\': this.setStart(); this.consumed(); this.transit(620); return false;
      case '#': this.setStart(); this.consumed(); this.space = true; this.transit(900); return false;
      case '"': this.setStart(); this.consumed(); this.cstr = new Cstr(); this.transit(400); return false;
      case '<': this.setStart(); this.consumed(); this.setSymToken(LToken.LT); this.transit(0); return true;
      case '{': this.setStart(); this.consumed(); this.setSymToken(LToken.LBRACE); this.transit(0); return true;
      case '}': this.setStart(); this.consumed(); this.setSymToken(LToken.RBRACE); this.transit(0); return true;
      case ')': this.setStart(); this.consumed(); this.setSymToken(LToken.RPAR); this.transit(0); return true;
      case ']': this.setStart(); this.consumed(); this.setSymToken(LToken.RBRACKET); this.transit(0); return true;
      case ',': this.setStart(); this.consumed(); this.setSymToken(LToken.COMMA); this.transit(0); return true;
      case '.': this.setStart(); this.consumed(); this.setSymToken(LToken.DOT); this.transit(0); return true;
      case '!': this.setStart(); this.consumed(); this.setSymToken(LToken.EXCLA); this.transit(0); return true;
      case '~': this.setStart(); this.consumed(); this.setSymToken(LToken.TILD); this.transit(0); return true;
      case '/': this.setStart(); this.consumed(); this.setSymToken(LToken.SLASH); this.transit(0); return true;
      default:
        errmsg = new StringBuffer();
        errmsg.append("Invalid expression at ");
        errmsg.append(this.getCurrentLineLoc());
        errmsg.append(". - ");
        errmsg.appendCodePoint(c);
        throw new CompileException(errmsg.toString());
      }
    }
  }

  private boolean dispatch50(int c) throws CompileException {
    StringBuffer errmsg = null;
    switch (c) {
    case 'x': this.consumed(); this.setStart(); this.radix = 16; this.transit(51); return false;
    case 'o': this.consumed(); this.setStart(); this.radix = 8; this.transit(52); return false;
    case '\n':
      errmsg = new StringBuffer();
      errmsg.append("No letter after % at ");
      errmsg.append(this.getCurrentLineLoc());
      errmsg.append(".");
      throw new CompileException(errmsg.toString());
    default:
      errmsg = new StringBuffer();
      errmsg.append("Invalid letter after % at ");
      errmsg.append(this.getCurrentLineLoc());
      errmsg.append(". - ");
      errmsg.appendCodePoint(c);
      throw new CompileException(errmsg.toString());
    }
  }

  private boolean dispatch51(int c) throws CompileException {
    StringBuffer errmsg = null;
    if (isHexDigit(c)) {
      this.consumed(); this.transit(61); return false;
    } else if (c == '\n') {
      errmsg = new StringBuffer();
      errmsg.append("No hex digit at ");
      errmsg.append(this.getCurrentLineLoc());
      errmsg.append(".");
      throw new CompileException(errmsg.toString());
    } else {
      errmsg = new StringBuffer();
      errmsg.append("Invalid hex digit at ");
      errmsg.append(this.getCurrentLineLoc());
      errmsg.append(".");
      throw new CompileException(errmsg.toString());
    }
  }

  private boolean dispatch52(int c) throws CompileException {
    StringBuffer errmsg = null;
    if (isOctalDigit(c)) {
      this.consumed(); this.transit(62); return false;
    } else if (c == '\n') {
      errmsg = new StringBuffer();
      errmsg.append("No octal digit at ");
      errmsg.append(this.getCurrentLineLoc());
      errmsg.append(".");
      throw new CompileException(errmsg.toString());
    } else {
      errmsg = new StringBuffer();
      errmsg.append("Invalid octal digit at ");
      errmsg.append(this.getCurrentLineLoc());
      errmsg.append(".");
      throw new CompileException(errmsg.toString());
    }
  }

  private boolean dispatch61(int c) throws CompileException {
    if (isHexDigit(c)) {
      this.consumed(); this.transit(61); return false;
    } else if (c == '~') {
      this.consumed(); this.setByteToken(); this.transit(0); return true;
    } else {
      this.setIntToken(); this.transit(0); return true;
    }
  }

  private boolean dispatch62(int c) throws CompileException {
    if (isOctalDigit(c)) {
      this.consumed(); this.transit(62); return false;
    } else if (c == '~') {
      this.consumed(); this.setByteToken(); this.transit(0); return true;
    } else {
      this.setIntToken(); this.transit(0); return true;
    }
  }

  private boolean dispatch100(int c) {
    if (isWordPart(c)) {
      this.consumed(); this.transit(100); return false;
    } else {
      this.setWordToken(); this.transit(0); return true;
    }
  }

  private boolean dispatch200(int c) throws CompileException {
    if (isDecimalDigit(c)) {
      this.consumed(); this.transit(200); return false;
    } else if (c == '.') {
      this.consumed(); this.transit(250); return false;
    } else if (c == '~') {
      this.consumed(); this.setByteToken(); this.transit(0); return true;
    } else {
      this.setIntToken(); this.transit(0); return true;
    }
  }

  private boolean dispatch250(int c) throws CompileException {
    if (c == 'e' || c == 'E') {
      this.consumed(); this.transit(260); return false;
    } else if (isDecimalDigit(c)) {
      this.consumed(); this.transit(250); return false;
    } else {
      this.setRealToken(); this.transit(0); return true;
    }
  }

  private boolean dispatch260(int c) throws CompileException {
    StringBuffer errmsg = null;
    switch (c) {
    case '+':
    case '-':
      this.consumed(); this.transit(270); return false;
    default:
      if (isDecimalDigit(c)) {
        this.consumed(); this.transit(270); return false;
      } else {
       errmsg = new StringBuffer();
       errmsg.append("Incomplete real expression at ");
       errmsg.append(this.getCurrentLineLoc());
       errmsg.append(".");
       throw new CompileException(errmsg.toString());
      }
    }
  }

  private boolean dispatch270(int c) throws CompileException {
    if (isDecimalDigit(c)) {
      this.consumed(); this.transit(270); return false;
    } else {
      this.setRealToken(); this.transit(0); return true;
    }
  }

  private boolean dispatch300(int c) throws CompileException {
    StringBuffer errmsg = null;
    switch (c) {
    case '\n':
    case '\r':
    case '\t':
      errmsg = new StringBuffer();
      errmsg.append("Incomplete character expression at ");
      errmsg.append(this.getCurrentLineLoc());
      errmsg.append(".");
      throw new CompileException(errmsg.toString());
    case '\'':
      errmsg = new StringBuffer();
      errmsg.append("Empty character expression at ");
      errmsg.append(this.getCurrentLineLoc());
      errmsg.append(".");
      throw new CompileException(errmsg.toString());
    case '\\': this.consumed(); this.transit(310); return false;
    default: this.consumed(); this.setChar(c); this.transit(301); return false;
    }
  }

  private boolean dispatch301(int c) throws CompileException {
    StringBuffer errmsg = null;
    switch (c) {
    case '\'': this.consumed(); this.setCharToken(); this.transit(0); return true;
    default:
      errmsg = new StringBuffer();
      errmsg.append("Quotation mark missing at ");
      errmsg.append(this.getCurrentLineLoc());
      errmsg.append(".");
      throw new CompileException(errmsg.toString());
    }
  }

  private boolean dispatch310(int c) throws CompileException {
    StringBuffer errmsg = null;
    switch (c) {
    case '\'': this.consumed(); this.setChar('\''); this.transit(301); return false;
    // case '\"': this.consumed(); this.setChar('\"'); this.transit(301); return false;
    case '\\': this.consumed(); this.setChar('\\'); this.transit(301); return false;
    case 'n': this.consumed(); this.setChar('\n'); this.transit(301); return false;
    case 'r': this.consumed(); this.setChar('\r'); this.transit(301); return false;
    case 't': this.consumed(); this.setChar('\t'); this.transit(301); return false;
    default:
      errmsg = new StringBuffer();
      errmsg.append("Invalid character expression at ");
      errmsg.append(this.getCurrentLineLoc());
      errmsg.append(".");
      throw new CompileException(errmsg.toString());
    }
  }

  private boolean dispatch400(int c) throws CompileException {
    StringBuffer errmsg = null;
    switch (c) {
    case '"': this.consumed(); this.setCstrToken(); this.transit(0); return true;
    case '\n':
    case '\r':
    case '\t':
      errmsg = new StringBuffer();
      errmsg.append("String incomplete at ");
      errmsg.append(this.getCurrentLineLoc());
      errmsg.append(".");
      throw new CompileException(errmsg.toString());
    case '\\': this.consumed(); this.transit(410); return false;
    default: this.cstr.append(c); this.consumed(); this.transit(400); return false;
    }
  }

  private boolean dispatch410(int c) throws CompileException {
    StringBuffer errmsg = null;
    switch (c) {
    // case '\'': this.consumed(); this.cstr.append('\''); this.transit(400); return false;
    case '\"': this.consumed(); this.cstr.append('\"'); this.transit(400); return false;
    case '\\': this.consumed(); this.cstr.append('\\'); this.transit(400); return false;
    case 'n': this.consumed(); this.cstr.append('\n'); this.transit(400); return false;
    case 'r': this.consumed(); this.cstr.append('\r'); this.transit(400); return false;
    case 't': this.consumed(); this.cstr.append('\t'); this.transit(400); return false;
    default:
      if (isSpace(c)) {
        this.consumed(); this.transit(420); return false;
      } else {
        errmsg = new StringBuffer();
        errmsg.append("Invalid character expression at ");
        errmsg.append(this.getCurrentLineLoc());
        errmsg.append(". - ");
        errmsg.appendCodePoint(c);
        throw new CompileException(errmsg.toString());
      }
    }
  }

  private boolean dispatch420(int c) throws CompileException {
    StringBuffer errmsg = null;
    switch (c) {
    case '"': this.consumed(); this.transit(400); return false;
    default:
      if (isSpace(c)) {
        this.consumed(); this.transit(420); return false;
      } else {
        errmsg = new StringBuffer();
        errmsg.append("Non-space character expression at ");
        errmsg.append(this.getCurrentLineLoc());
        errmsg.append(". - ");
        errmsg.appendCodePoint(c);
        throw new CompileException(errmsg.toString());
      }
    }
  }

  private boolean dispatch500(int c) {
    switch (c) {
    case ';': this.consumed(); this.setSymToken(LToken.SEM_SEM); this.transit(0); return true;
    default: this.setSymToken(LToken.SEM); this.transit(0); return true;
    }
  }

  private boolean dispatch510(int c) {
    switch (c) {
    case '>': this.consumed(); this.setSymToken(LToken.GT_GT); this.transit(0); return true;
    default: this.setSymToken(LToken.GT); this.transit(0); return true;
    }
  }

  private boolean dispatch520(int c) {
    switch (c) {
    case ':': this.consumed(); this.setSymToken(LToken.COL_COL); this.transit(0); return true;
    case '=': this.consumed(); this.setSymToken(LToken.COL_EQ); this.transit(0); return true;
    default: this.setSymToken(LToken.COL); this.transit(0); return true;
    }
  }

  private boolean dispatch530(int c) {
    switch (c) {
    case '>': this.consumed(); this.setSymToken(LToken.HYPH_GT); this.transit(0); return true;
    default:
      if (isDecimalDigit(c)) {
        this.radix = 10;
        this.consumed(); this.transit(200); return false;
      } else {
        this.setSymToken(LToken.MINUS); this.transit(0); return true;
      }
    }
  }

  private boolean dispatch540(int c) {
    switch (c) {
    case '*': this.consumed(); this.transit(541); return false;
    default: this.setSymToken(LToken.AST); this.transit(0); return true;
    }
  }

  private boolean dispatch541(int c) {
    switch (c) {
    case '*': this.consumed(); this.setSymToken(LToken.AST_AST_AST); this.transit(0); return true;
    default: this.setSymToken(LToken.AST_AST); this.transit(0); return true;
    }
  }

  private boolean dispatch550(int c) {
    switch (c) {
    case '|': this.consumed(); this.setSymToken(LToken.VBAR_VBAR); this.transit(0); return true;
    case ')': this.consumed(); this.setSymToken(LToken.VBAR_RPAR); this.transit(0); return true;
    case ']': this.consumed(); this.setSymToken(LToken.VBAR_RBRACKET); this.transit(0); return true;
    default: this.setSymToken(LToken.VBAR); this.transit(0); return true;
    }
  }

  private boolean dispatch560(int c) {
    switch (c) {
    case '&': this.consumed(); this.setSymToken(LToken.AMP_AMP); this.transit(0); return true;
    default: this.setSymToken(LToken.AMP); this.transit(0); return true;
    }
  }

  private boolean dispatch570(int c) {
    switch (c) {
    case '|': this.consumed(); this.setSymToken(LToken.LPAR_VBAR); this.transit(0); return true;
    default: this.setSymToken(LToken.LPAR); this.transit(0); return true;
    }
  }

  private boolean dispatch580(int c) {
    switch (c) {
    case '^': this.consumed(); this.setSymToken(LToken.CARET_CARET); this.transit(0); return true;
    default: this.setSymToken(LToken.CARET); this.transit(0); return true;
    }
  }

  private boolean dispatch590(int c) {
    switch (c) {
    case '|': this.consumed(); this.setSymToken(LToken.LBRACKET_VBAR); this.transit(0); return true;
    case '/': this.consumed(); this.setSymToken(LToken.LBRACKET_SLASH); this.transit(0); return true;
    default: this.setSymToken(LToken.LBRACKET); this.transit(0); return true;
    }
  }

  private boolean dispatch600(int c) {
    switch (c) {
    case '=': this.consumed(); this.setSymToken(LToken.EQ_EQ); this.transit(0); return true;
    case '>': this.consumed(); this.setSymToken(LToken.EQ_GT); this.transit(0); return true;
    default: this.setSymToken(LToken.EQ); this.transit(0); return true;
    }
  }

  private boolean dispatch610(int c) {
    switch (c) {
    case '+': this.consumed(); this.setSymToken(LToken.PLUS_PLUS); this.transit(0); return true;
    default:
      if (isDecimalDigit(c)) {
        this.radix = 10;
        this.consumed(); this.transit(200); return false;
      } else {
        this.setSymToken(LToken.PLUS); this.transit(0); return true;
      }
    }
  }

  private boolean dispatch620(int c) {
    switch (c) {
    case '\\': this.consumed(); this.setSymToken(LToken.BKSLASH_BKSLASH); this.transit(0); return true;
    default: this.setSymToken(LToken.BKSLASH); this.transit(0); return true;
    }
  }

  private boolean dispatch900(int c) {
    switch (c) {
    case '\n': this.consumed(); this.transit(0); return false;
    default: this.consumed(); this.transit(900); return false;
    }
  }

  void test() throws CompileException, IOException {
    for (;;) {
      LToken t = this.readToken();
      if (t == null) { return; }
      System.out.println(t);
    }
  }
}
