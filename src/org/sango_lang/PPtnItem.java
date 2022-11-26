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

class PPtnItem extends PDefaultExprObj {
  int context;  // PPtnMatch.CONTEXT_*
  String name;
  PProgObj elem;

  private PPtnItem(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("ptnitem[src=");
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

  static PPtnItem create(Parser.SrcInfo srcInfo, PScope outerScope, int context, String name, PProgObj elem) {
    PPtnItem i = new PPtnItem(srcInfo, outerScope);
    i.context = context;
    i.name = name;
    i.elem = elem;
    return i;
  }

  static PPtnItem accept(ParserA.TokenReader reader, PScope outerScope, int spc, int acceptables, int context) throws CompileException, IOException {
    if (acceptables == PPtn.ACCEPT_NOTHING) { return null; }
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
    PExprObj elem = null;
    if ((acceptables & PPtn.ACCEPT_BYTE) > 0 && (elem = PByte.accept(reader, outerScope, space)) != null) {
      ;
    } else if ((acceptables & PPtn.ACCEPT_INT) > 0 && (elem = PInt.accept(reader, outerScope, space)) != null) {
      ;
    } else if ((acceptables & PPtn.ACCEPT_REAL) > 0 && (elem = PReal.accept(reader, outerScope, space)) != null) {
      ;
    } else if ((acceptables & PPtn.ACCEPT_CHAR) > 0 && (elem = PChar.accept(reader, outerScope, space)) != null) {
      ;
    } else if ((acceptables & PPtn.ACCEPT_LIST) > 0 && (elem = PListPtn.accept(reader, outerScope, space, context)) != null) {
      ;
    } else if ((acceptables & PPtn.ACCEPT_TUPLE) > 0 && (elem = PTuplePtn.accept(reader, outerScope, space, context)) != null) {
      ;
    } else if ((acceptables & PPtn.ACCEPT_STRING) > 0 && (elem = PStringPtn.accept(reader, outerScope, space, context)) != null) {
      ;
    } else if ((acceptables & PPtn.ACCEPT_ID) > 0 && (elem = PExprId.accept(reader, outerScope, Parser.QUAL_MAYBE, space)) != null) {
      ;
    } else if ((acceptables & PPtn.ACCEPT_VARDEF_NOT_CASTED) > 0 && (elem = PExprVarDef.accept(reader, outerScope, PExprVarDef.CAT_LOCAL_VAR, PExprVarDef.TYPE_NOT_ALLOWED)) != null) {
      ;
    } else if ((acceptables & PPtn.ACCEPT_VARDEF_CASTED) > 0 && (elem = PExprVarDef.accept(reader, outerScope, PExprVarDef.CAT_LOCAL_VAR, PExprVarDef.TYPE_NEEDED)) != null) {
      ;
    } else if ((acceptables & PPtn.ACCEPT_WILD_CARD) > 0 && (elem = PWildCard.accept(reader, outerScope, space)) != null) {
      ;
    } else if ((acceptables & PPtn.ACCEPT_WILD_CARDS) > 0 && (elem = PWildCards.accept(reader, outerScope, space)) != null) {
      ;
    } else if ((acceptables & PPtn.ACCEPT_PTN_MATCH) > 0 &&  (elem = PPtnMatch.acceptEnclosed(reader, outerScope, space, context)) != null) {
      ;
    }
    String an = null;
    if (name != null) {
      if (elem == null) {
        emsg = new StringBuffer();
        emsg.append("Data attribute missing at ");
        emsg.append(srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (elem instanceof PByte
          || elem instanceof PInt
          || elem instanceof PReal
          || elem instanceof PChar
          || elem instanceof PListPtn
          || elem instanceof PTuplePtn
          || elem instanceof PStringPtn
          || elem instanceof PExprId
          || elem instanceof PExprVarDef
          || elem instanceof PWildCard
          || elem instanceof PPtnMatch) {
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
    return (elem != null)? create(srcInfo, outerScope, context, an, elem): null;
  }

  void fixAsAttr() {
    if (this.elem instanceof PExprId) {
      this.elem = PUndetPtn.create(this.srcInfo, this.scope, this.context, (PExprId)this.elem);
    }
  }

  // public void setupScope(PScope scope) {
    // if (scope == this.scope) { return; }
    // this.scope = scope;
    // this.idResolved = false;
    // this.elem.setupScope(scope);
  // }

  public void collectModRefs() throws CompileException {
    this.elem.collectModRefs();
  }

  public PPtnItem resolve() throws CompileException {
    // if (this.idResolved) { return this; }
    this.elem = this.elem.resolve();
    // this.idResolved = true;
    return this;
  }

  public void normalizeTypes() throws CompileException {
    this.elem.normalizeTypes();
  }
}
