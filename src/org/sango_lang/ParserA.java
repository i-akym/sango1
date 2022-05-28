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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;
import java.util.Map;

class ParserA extends Parser {
  static final int SPACE_DO_NOT_CARE = 0;
  static final int SPACE_NEEDED = 1;

  TokenReader tokenReader;

  ParserA(Compiler theCompiler, Compiler.CompileEntry ce) throws CompileException, IOException {
    super(theCompiler, ce);
    LineNumberReader r = new LineNumberReader(new FileReader(ce.srcFile));
    r.setLineNumber(1);
    this.tokenReader = new TokenReader(this.modName, new Lex(modName, r));
  }

  void parse1() throws CompileException, IOException {
    this.mod = PModule.accept(this.theCompiler, this.tokenReader, this.modName);
    // this.mod.theCompiler = this.theCompiler;
  }

  static Token acceptToken(TokenReader reader, int tag, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    Token t = reader.getToken();
    if (t.tagEquals(tag)) {
      if (!separationSpace(t, spc)) {
        emsg = new StringBuffer();
        emsg.append("Space missing before \"");
        emsg.append(t);
        emsg.append("\" at ");
        emsg.append(t.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      reader.tokenConsumed();
      return t;
    } else {
      return null;
    }
  }

  static Token acceptSpecifiedWord(TokenReader reader, String word, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    Token t = reader.getToken();
    if (t.tagEquals(LToken.WORD) && t.value.token.equals(word)) {
      if (!separationSpace(t, spc)) {
        emsg = new StringBuffer();
        emsg.append("Space missing before \"");
        emsg.append(t);
        emsg.append("\" at ");
        emsg.append(t.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      reader.tokenConsumed();
      return t;
    } else {
      return null;
    }
  }

  static Token acceptNormalWord(TokenReader reader, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    Token t = reader.getToken();
    if (t.tagEquals(LToken.WORD) && isNormalWord(t.value.token)) {
      if (!separationSpace(t, spc)) {
        emsg = new StringBuffer();
        emsg.append("Space missing before \"");
        emsg.append(t.value.token);
        emsg.append("\" at ");
        emsg.append(t.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      reader.tokenConsumed();
      return t;
    } else {
      return null;
    }
  }

  static Token acceptSpecialWord(TokenReader reader, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    Token t = reader.getToken();
    if (t.tagEquals(LToken.WORD) && !isNormalWord(t.value.token)) {
      if (!separationSpace(t, spc)) {
        emsg = new StringBuffer();
        emsg.append("Space missing before \"");
        emsg.append(t);
        emsg.append("\" at ");
        emsg.append(t.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      reader.tokenConsumed();
      return t;
    } else {
      return null;
    }
  }

  static Token acceptCstr(TokenReader reader, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    Token t = reader.getToken();
    if (t.tagEquals(LToken.CSTR)) {
      if (!separationSpace(t, spc)) {
        emsg = new StringBuffer();
        emsg.append("Space missing before char string at ");
        emsg.append(t.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      reader.tokenConsumed();
      return t;
    } else {
      return null;
    }
  }

  private static boolean isNormalWord(String s) {  // assume s is a word
    return Lex.isNormalWordStart(s.charAt(0));
  }

  private static boolean separationSpace(Token token, int necessity) {
    return (necessity == SPACE_DO_NOT_CARE || token.value == null)? true: token.value.space;
  }

  class TokenReader {
    Cstr src;
    Lex lex;
    LToken token;
    LToken nextToken;

    TokenReader(Cstr src, Lex lex) {
      this.src = src;
      this.lex = lex;
    }

    Token getToken() throws CompileException, IOException {
      if (this.token == null) {
        this.token = this.lex.readToken();
      }
      return toMyToken(this.src, this.token);
    }

    Token getNextToken() throws CompileException, IOException {
      if (this.token == null) {
        this.token = this.lex.readToken();
      }
      if (this.nextToken == null) {
        this.nextToken = this.lex.readToken();
      }
      return toMyToken(this.src, this.nextToken);
    }

    void tokenConsumed() {
      this.token = this.nextToken;
      this.nextToken = null;
    }

    SrcInfo getCurrentSrcInfo() throws CompileException, IOException {
      return this.getToken().getSrcInfo();
    }
  }

  private static Token toMyToken(Cstr src, LToken t) {
    return new Token(src, t);
  }
  
  static class Token {
    Cstr src;
    LToken value;

    Token(Cstr src, LToken t) {
      this.src = src;
      this.value = t;
    }

    public String toString() {
      String val;
      if (this.value == null) {
        val = "end-of-file";
      } else if (this.value.tag == LToken.CSTR) {
        val =  "\"" + this.value.token + "\"";
      } else {
        val = "'" + this.value.token + "'";
      }
      return this.src + ":" + val;
    }

    SrcInfo getSrcInfo() {
      return new SrcInfo(
        this.src,
        (this.value == null)? "EOF": lineNumToSrcLoc(this.value.lineNum));
      // return this.src + ":" + ((this.value == null)? "EOF": lineNumToSrcLoc(this.value.lineNum));
    }

    boolean tagEquals(int tag) {
      return (this.value == null)? false: tag == this.value.tag;
    }

    boolean wordEquals(String s) {
      return (this.value == null)? false: this.value.token.equals(s);
    }

    boolean isEOF() {
      return this.value == null;
    }
  }

  private static String lineNumToSrcLoc(int lineNum) {
    return "L" + lineNum;
  }
}
