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
import java.util.ArrayList;

abstract class PListPtn extends PDefaultExprObj {

  PListPtn(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  static class Builder {
    int context;  // PPtnMatch.CONTEXT_*
    Parser.SrcInfo srcInfo;
    PScope scope;
    List<PPtnMatch> elemSeq;
    PPtnMatch tail;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.srcInfo = srcInfo;
      this.scope = scope;
      this.elemSeq = new ArrayList<PPtnMatch>();
    }

    void setContext(int context) {
    }

    void addElem(PPtnMatch elem) {
      this.elemSeq.add(elem);
    }

    void addElemSeq(List<PPtnMatch> elemSeq) {
      this.elemSeq.addAll(elemSeq);
    }

    void setTail(PPtnMatch tail) {
      this.tail = tail;
    }

    PListPtn create() {
      if (this.elemSeq.isEmpty()) {
        return PEmptyListPtn.create(this.srcInfo, this.scope);
      }
      PPtnMatch t = (this.tail != null)?
        this.tail:
        PPtnMatch.create(this.srcInfo, this.scope, this.context, null, PEmptyListPtn.create(this.srcInfo, this.scope));
      PListPtn L = null;
      for (int i = this.elemSeq.size() - 1; i >= 0; i--) {
        L = PListConstrPtn.create(this.srcInfo, this.scope, this.elemSeq.get(i), t);
        t = PPtnMatch.create(this.srcInfo, this.scope, this.context, null, L);
      }
      return L;
    }
  }

  static PListPtn accept(ParserA.TokenReader reader, PScope outerScope, int spc, int context) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.LBRACKET, spc)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(t.getSrcInfo(), outerScope);
    builder.setContext(context);
    List<PPtnMatch> elemSeq;
    if ((elemSeq = PPtnMatch.acceptPosdSeq(reader, outerScope, 0, context)) != null) {
      builder.addElemSeq(elemSeq);
      if (!elemSeq.isEmpty() && ParserA.acceptToken(reader, LToken.SEM, ParserA.SPACE_DO_NOT_CARE) != null) {
        PPtnMatch tail;
        if ((tail = PPtnMatch.accept(reader, outerScope, context)) == null) {
          emsg = new StringBuffer();
          emsg.append("List tail not found at ");
          emsg.append(reader.getCurrentSrcInfo());
          emsg.append(".");
          ParserA.Token et = reader.getToken();
          if (et.value != null) {
            emsg.append(" - ");
            emsg.append(et.value.token);
          }
          throw new CompileException(emsg.toString());
        }
        builder.setTail(tail);
      }
    }
    if (ParserA.acceptToken(reader, LToken.RBRACKET, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("Syntax error at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      ParserA.Token et = reader.getToken();
      if (et.value != null) {
        emsg.append(" - ");
        emsg.append(et.value.token);
      }
      throw new CompileException(emsg.toString());
    }
    return builder.create();
  }

  static PListPtn acceptX(ParserB.Elem elem, PScope outerScope, int context) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("list")) { return null; }
    ParserB.Elem e = elem.getFirstChild();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Head missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PPtnMatch h = PPtnMatch.acceptX(e, outerScope, context);
    if (h == null) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(e.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    e = e.getNextSibling();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Tail missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PPtnMatch t = PPtnMatch.acceptX(e, outerScope, context);
    if (t == null) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(e.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    return PListConstrPtn.create(elem.getSrcInfo(), outerScope, h, t);
  }
}
