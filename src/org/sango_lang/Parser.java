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

import java.io.IOException;
import java.util.List;
import java.util.Map;

abstract class Parser {

  Compiler theCompiler;
  Cstr modName;
  PModule mod;

  Parser(Compiler theCompiler, Compiler.CompileEntry ce) throws CompileException, IOException {
    this.theCompiler = theCompiler;
    this.modName = ce.modName;
  }

  abstract void parse1() throws CompileException, IOException;

  void parse2() throws CompileException {
    this.mod.resolveId();
    this.mod.checkAccInDefs();
  }

  void parse3() throws CompileException {
    this.mod.checkCyclicAlias();
  }

  void parse4() throws CompileException {
    this.mod.normalizeTypes();
    this.mod.checkCyclicExtension();
    this.mod.checkConcreteness();
  }

  void parse5() throws CompileException {
    this.mod.makeSureTypeConsistency();
  }

  PDefDict getDefDict() {
    return this.mod;
  }

  static class SrcInfo {
    Cstr src;
    String loc;

    private SrcInfo() {}

    SrcInfo(Cstr src, String loc) {
      this.src = src;
      this.loc = loc;
    }

    public String toString() {
      return this.src.toJavaString() + ":" + this.loc;
    }

    public boolean equals(SrcInfo si) {
      return si != null && this.src.equals(si.src) && this.loc.equals(si.loc);
    }

    public int hashCode() {
      return this.src.hashCode() ^ this.loc.hashCode();
    }

    SrcInfo appendPostfix(String postfix) {
      SrcInfo si = new SrcInfo();
      si.src = this.src;
      si.loc = this.loc + postfix;
      return si;
    }
  }

  static boolean isNormalId(String s) {
    if (s.length() == 0) {
      return false;
    }
    if (!Lex.isNormalWordStart(s.charAt(0))) {
      return false;
    }
    boolean b = true;
    for (int i = 1; b && i < s.length(); i++) {
      b = Lex.isWordPart(s.charAt(i));
    }
    return b;
  }

  static boolean isExtendedId(String s) {
    if (s.length() == 0) {
      return false;
    }
    if (!Lex.isWordStart(s.charAt(0))) {
      return false;
    }
    boolean b = true;
    for (int i = 1; b && i < s.length(); i++) {
      b = Lex.isWordPart(s.charAt(i));
    }
    return b;
  }
}
