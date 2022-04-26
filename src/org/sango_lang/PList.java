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
import java.util.ArrayList;
import java.util.List;

abstract class PList extends PDefaultExprObj {

  PList(Parser.SrcInfo srcInfo) {
    super(srcInfo);
  }

  static class Builder {
    Parser.SrcInfo srcInfo;
    List<PExpr> elemSeq;
    PExprObj tail;

    static Builder newInstance(Parser.SrcInfo srcInfo) {
      return new Builder(srcInfo);
    }

    Builder(Parser.SrcInfo srcInfo) {
      this.srcInfo = srcInfo;
      this.elemSeq = new ArrayList<PExpr>();
    }

    void addElem(PExpr elem) {
      this.elemSeq.add(elem);
    }

    void addElemSeq(PExprList.Elems elemSeq) {
      for (int i = 0; i < elemSeq.exprs.length; i++) {
        this.elemSeq.add(elemSeq.exprs[i]);
      }
      // this.elemSeq.addAll(elemSeq);
    }

    void setTail(PExprObj tail) {
      this.tail = tail;
    }

    PList create() {
      if (this.elemSeq.isEmpty()) {
        return PEmptyList.create(this.srcInfo);
      }
      PExprObj t = (this.tail != null)? this.tail: PEmptyList.create(this.srcInfo);
      PList L = null;
      for (int i = this.elemSeq.size() - 1; i >= 0; i--) {
        L = PListConstr.create(this.srcInfo, this.elemSeq.get(i), t);
        t = L;
      }
      return L;
    }
  }

  static PList accept(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.LBRACKET, spc)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(t.getSrcInfo());
    PExprList.Elems elemSeq;
    if ((elemSeq = PExprList.acceptElems(reader, t.getSrcInfo(), 0)) != null) {
      builder.addElemSeq(elemSeq);
      if (elemSeq.exprs.length > 0 && ParserA.acceptToken(reader, LToken.SEM, ParserA.SPACE_DO_NOT_CARE) != null) {
        PExpr tail;
        if ((tail = PExpr.accept(reader)) == null) {
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
        builder.setTail(tail);
      }
    }
    if ((t = ParserA.acceptToken(reader, LToken.RBRACKET, ParserA.SPACE_DO_NOT_CARE)) == null) {
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

  static PList acceptX(ParserB.Elem elem) throws CompileException {
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
    PExpr h = PExpr.acceptX(e);
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
    PExpr t = PExpr.acceptX(e);
    if (t == null) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(e.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    return PListConstr.create(elem.getSrcInfo(), h, t);
  }
}
