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

class PEvalItem extends PDefaultProgElem {
  String name;
  PProgElem elem;

  private PEvalItem() {}

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("evalitem[src=");
    buf.append(this.srcInfo);
    if (this.name != null) {
      buf.append(",name=");
      buf.append(this.name);
    }
    buf.append(",elem=");
    buf.append(this.elem);
    buf.append("]");
    return buf.toString();
  }

  static PEvalItem create(Parser.SrcInfo srcInfo, String name, PProgElem elem) {
    PEvalItem i = new PEvalItem();
    i.srcInfo = srcInfo;
    i.name = name;
    i.elem = elem;
    return i;
  }

  static PEvalItem create(PProgElem elem) {
    return create(elem.getSrcInfo(), null, elem);
  }

  static PEvalItem accept(ParserA.TokenReader reader, int spc, int acceptables) throws CompileException, IOException {
    if (acceptables == PEval.ACCEPT_NOTHING) { return null; }
    StringBuffer emsg;
    Parser.SrcInfo srcInfo = reader.getCurrentSrcInfo();
    ParserA.Token token = reader.getToken();
    ParserA.Token next = reader.getNextToken();
    ParserA.Token name = null;
    Parser.SrcInfo srcInfoOfName = null;
    int space = spc;
    if (next.tagEquals(LToken.COL) && (name = ParserA.acceptNormalWord(reader, spc)) != null) {
      srcInfoOfName = srcInfo;
      ParserA.acceptToken(reader, LToken.COL, ParserA.SPACE_DO_NOT_CARE);
      srcInfo = reader.getCurrentSrcInfo();
      token = reader.getToken();
      next = null;
      space = ParserA.SPACE_DO_NOT_CARE;
    }
    PProgElem elem = null;
    if ((acceptables & PEval.ACCEPT_BYTE) > 0 && (elem = PByte.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_INT) > 0 && (elem = PInt.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_REAL) > 0 && (elem = PReal.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_CHAR) > 0 && (elem = PChar.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_LIST) > 0 && (elem = PList.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_TUPLE) > 0 && (elem = PTuple.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_STRING) > 0 && (elem = PString.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_FUN_REF) > 0 && (elem = PFunRef.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_CLOSURE) > 0 && (elem = PClosure.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_IF_BLOCK) > 0 && (elem = PIfBlock.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_CASE_BLOCK) > 0 && (elem = PCaseBlock.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_ID) > 0 && (elem = PExprId.accept(reader, PExprId.ID_MAYBE_QUAL, space)) != null) {  // must be after 'if' 'case'
      ;
    } else if ((acceptables & PEval.ACCEPT_DYNAMIC_INV) > 0 && (elem = PDynamicInv.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_SELF_INV) > 0 && (elem = PSelfInv.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_DATA_CONSTR_USING) > 0 && (elem = PDataConstrUsing.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_EVAL) > 0 &&  (elem = PEval.acceptEnclosed(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_PIPE) > 0 &&  (elem = PPipe.accept(reader, space)) != null) {
      ;
    }
    String an = null;
    if (name != null) {
      if (elem == null) {
        emsg = new StringBuffer();
        emsg.append("Data attribute missing for \"");
        emsg.append(name.value.token);
        emsg.append("\" missing at ");
        emsg.append(srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (elem instanceof PByte
          || elem instanceof PInt
          || elem instanceof PReal
          || elem instanceof PChar
          || elem instanceof PList
          || elem instanceof PTuple
          || elem instanceof PString
          || elem instanceof PExprId
          || elem instanceof PFunRef
          || elem instanceof PClosure
          || elem instanceof PIfBlock
          || elem instanceof PEvalElem) {
        ;
      } else {
        emsg = new StringBuffer();
        emsg.append("Invalid data attribute at ");
        emsg.append(elem.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      an = name.value.token;
    }
    return (elem != null)? create(srcInfo, an, elem): null;
  }

  void fixAsParam() {
    if (this.elem instanceof PExprId) {
      this.elem = PUndetEval.create(this.srcInfo, (PExprId)this.elem, new PEvalElem[0]);
    }
  }

  public PEvalItem setupScope(PScope scope) throws CompileException {
    if (scope == this.scope) { return this; }
    this.scope = scope;
    this.idResolved = false;
    this.elem = this.elem.setupScope(scope);
    return this;
  }

  public PEvalItem resolveId() throws CompileException {
    if (this.idResolved) { return this; }
    this.elem = this.elem.resolveId();
    this.idResolved = true;
    return this;
  }

  public void normalizeTypes() throws CompileException {
    this.elem.normalizeTypes();
  }
}
