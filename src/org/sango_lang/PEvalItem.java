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

abstract class PEvalItem extends PDefaultExprObj {

  private PEvalItem(Parser.SrcInfo srcInfo) {
    this.srcInfo = srcInfo;
  }

  abstract boolean isObjItem();
  abstract PExprObj getObj();
  abstract String getName();
  abstract PProgElem getSym();

  static ObjItem create(PExprId id) {
    return ObjItem.create(id.getSrcInfo(), null, id);
  }

  static ObjItem create(PCaseBlock caseBlock) {
    return ObjItem.create(caseBlock.getSrcInfo(), null, caseBlock);
  }

  static ObjItem create(PEval eval) {
    return create(PExpr.create(eval));
  }

  static ObjItem create(PExpr expr) {
    return ObjItem.create(expr.getSrcInfo(), null, expr);
  }

  static class ObjItem extends PEvalItem {
    String name;
    PExprObj obj;

    static ObjItem create(Parser.SrcInfo srcInfo, String name, PExprObj obj) {
      ObjItem oi = new ObjItem(srcInfo);
      oi.name = name;
      oi.obj = obj;
      return oi;
    }

    private ObjItem(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    boolean isObjItem() { return true; }

    PExprObj getObj() { return this.obj; }

    String getName() { return this.name; }

    PProgElem getSym() { return null; }

    ObjItem shallowCopyNoName() {
      return create(this.srcInfo, null, this.obj);
    }

    void fixAsParam() {
      if (this.obj instanceof PExprId) {
        this.obj = PUndetEval.create(this.srcInfo, (PExprId)this.obj, new PEvalItem.ObjItem[0]);
      }
    }

    public void setupScope(PScope scope) {
      if (scope == this.scope) { return; }
      this.scope = scope;
      this.idResolved = false;
      this.obj.setupScope(scope);
    }

    public void collectModRefs() throws CompileException {
      this.obj.collectModRefs();
    }

    public ObjItem resolve() throws CompileException {
      if (this.idResolved) { return this; }
      this.obj = this.obj.resolve();
      this.idResolved = true;
      return this;
    }

    public void normalizeTypes() throws CompileException {
      this.obj.normalizeTypes();
    }

    public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
      return this.obj.setupTypeGraph(graph);
    }

    public GFlow.Node setupFlow(GFlow flow) {
      return this.obj.setupFlow(flow);
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("evalitem[src=");
      buf.append(this.srcInfo);
      if (this.name != null) {
        buf.append(",name=");
        buf.append(this.name);
      }
      buf.append(",obj=");
      buf.append(this.obj);
      buf.append("]");
      return buf.toString();
    }
  }

  static class SymItem extends PEvalItem {
    PProgElem sym;

    static SymItem create(Parser.SrcInfo srcInfo, PProgElem sym) {
      SymItem si = new SymItem(srcInfo);
      si.sym = sym;
      return si;
    }

    private SymItem(Parser.SrcInfo srcInfo) {
      super(srcInfo);
    }

    boolean isObjItem() { return false; }

    PExprObj getObj() { return null; }

    String getName() { return null; }

    PProgElem getSym() { return this.sym; }


    public void setupScope(PScope scope) {}

    public void collectModRefs() {}

    public SymItem resolve() throws CompileException {
      return this;
    }

    public void normalizeTypes() throws CompileException {}

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("evalitem[src=");
      buf.append(this.srcInfo);
      buf.append(",sym=");
      buf.append(this.sym);
      buf.append("]");
      return buf.toString();
    }
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
    PProgElem sym = null;
    PExprObj obj = null;
    if ((acceptables & PEval.ACCEPT_BYTE) > 0 && (obj = PByte.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_INT) > 0 && (obj = PInt.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_REAL) > 0 && (obj = PReal.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_CHAR) > 0 && (obj = PChar.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_LIST) > 0 && (obj = PList.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_TUPLE) > 0 && (obj = PTuple.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_STRING) > 0 && (obj = PString.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_FUN_REF) > 0 && (obj = PFunRef.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_CLOSURE) > 0 && (obj = PClosure.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_IF_EVAL) > 0 && (obj = PIfEval.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_CASE_BLOCK) > 0 && (obj = PCaseBlock.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_ID) > 0 && (obj = PExprId.accept(reader, PExprId.ID_MAYBE_QUAL, space)) != null) {  // must be after 'if' 'case'
      ;
    } else if ((acceptables & PEval.ACCEPT_DYNAMIC_INV) > 0 && (sym = PDynamicInv.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_SELF_INV) > 0 && (sym = PSelfInv.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_DATA_CONSTR_USING) > 0 && (sym = PDataConstrUsing.accept(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_ENCLOSED) > 0 &&  (obj = PExpr.acceptEnclosed(reader, space)) != null) {
      ;
    } else if ((acceptables & PEval.ACCEPT_PIPE) > 0 &&  (sym = PPipe.accept(reader, space)) != null) {
      ;
    }
    String an = null;
    if (name != null) {
      if (obj == null) {
        emsg = new StringBuffer();
        emsg.append("Data attribute missing for \"");
        emsg.append(name.value.token);
        emsg.append("\" missing at ");
        emsg.append(srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      // if (elem instanceof PByte
          // || elem instanceof PInt
          // || elem instanceof PReal
          // || elem instanceof PChar
          // || elem instanceof PList
          // || elem instanceof PTuple
          // || elem instanceof PString
          // || elem instanceof PExprId
          // || elem instanceof PFunRef
          // || elem instanceof PClosure
          // || elem instanceof PIfBlock
          // || elem instanceof PExprObj) {
      if (sym != null) {
        emsg = new StringBuffer();
        emsg.append("Invalid data attribute at ");
        emsg.append(sym.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      an = name.value.token;
    }
    PEvalItem i;
    if (obj != null) {
      i = ObjItem.create(srcInfo, an, obj);
    } else if (sym != null) {
      i = SymItem.create(srcInfo, sym);
    } else {
      i = null;
    }
    return i;
  }
}
