/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2022 AKIYAMA Isao                                         *
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

class PExprVarDef extends PDefaultExprObj {
  static final int CAT_FUN_PARAM = 1;
  static final int CAT_LOCAL_VAR = 2;

  static final int TYPE_NOT_ALLOWED = 1;
  static final int TYPE_MAYBE_SPECIFIED = 2;
  static final int TYPE_NEEDED = 3;

  String name;
  int cat;
  PExprVarSlot _resolved_varSlot;

  private PExprVarDef(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  static PExprVarDef create(Parser.SrcInfo srcInfo, PScope outerScope, int cat, PType type, String name) {
    PExprVarDef var = new PExprVarDef(srcInfo, outerScope);
    var.cat = cat;
    var.type = type;
    var.name = name;
    return var;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("newvar[");
    buf.append("src=");
    buf.append(this.srcInfo);
     buf.append(",cat=");
    buf.append(this.cat);
    buf.append(",name=");
    buf.append(this.name);
    if (this.type != null) {
      buf.append(",type=");
      buf.append(this.type);
    }
    if (this._resolved_varSlot != null) {
      buf.append(",slot=");
      buf.append(this._resolved_varSlot);
    }
    buf.append("]");
    return buf.toString();
  }

  static PExprVarDef accept(ParserA.TokenReader reader, PScope outerScope, int cat, int typeSpec) throws CompileException, IOException {
    StringBuffer emsg;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    PType type = null;
    if (typeSpec == TYPE_MAYBE_SPECIFIED || typeSpec == TYPE_NEEDED) {
      type = PType.accept(reader, outerScope, ParserA.SPACE_DO_NOT_CARE);
    }
    ParserA.Token varSym = ParserA.acceptToken(reader, LToken.AST, ParserA.SPACE_DO_NOT_CARE);
    if (type == null && varSym == null) {
      return null;
    }
    if (type != null && varSym == null) {
      emsg = new StringBuffer();
      emsg.append("\"*\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (typeSpec == TYPE_NEEDED && type == null) {
      emsg = new StringBuffer();
      emsg.append("Type missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    ParserA.Token varId;
    if ((varId = ParserA.acceptNormalWord(reader, ParserA.SPACE_DO_NOT_CARE))== null) {
      emsg = new StringBuffer();
      emsg.append("Variable/parameter name missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return create(si, outerScope, cat, type, varId.value.token);
  }

  static PExprVarDef acceptX(ParserB.Elem elem, PScope outerScope, int cat, int typeSpec) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("newvar")) { return null; }
    String id = elem.getAttrValueAsId("id");
    if (id == null) {
      emsg = new StringBuffer();
      emsg.append("Variable name missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PType type = null;
    if (typeSpec == TYPE_NEEDED || typeSpec == TYPE_MAYBE_SPECIFIED) {
      ParserB.Elem ee = elem.getFirstChild();
      type = (ee != null)? PType.acceptX(ee, outerScope): null;
    }
    if (typeSpec == TYPE_NEEDED && type == null) {
      emsg = new StringBuffer();
      emsg.append("Type missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return create(elem.getSrcInfo(), outerScope, cat, type, id);
  }

  public void collectModRefs() throws CompileException {
    if (this.type != null) {
      this.type.collectModRefs();
    }
  }

  public PExprVarDef resolve() throws CompileException {
    StringBuffer emsg;
    if (this._resolved_varSlot != null) { return this; }
    if (!this.scope.canDefineEVar(this)) {
      emsg = new StringBuffer();
      emsg.append("Cannot define variable at ");
      emsg.append(this.srcInfo);
      emsg.append(". - ");
      emsg.append(this.name);
      /* DEBUG */ // throw new RuntimeException(emsg.toString());
      throw new CompileException(emsg.toString());
    }
    this._resolved_varSlot = this.scope.defineEVar(this);
    if (this.type != null) {
      this.type = (PType)this.type.resolve();
    }
    return this;
  }

  public void excludePrivateAcc() throws CompileException {
    if (this.type != null) {
      this.type.excludePrivateAcc();
    }
  }

  public void normalizeTypes() throws CompileException {
    if (this.type != null && this._normalized_typeSkel == null) {
      this._normalized_typeSkel = this.type.toSkel().normalize();
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    this.typeGraphNode = graph.createVarNode(this, this.name, this.cat);
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    return flow.createNodeForVarDef(this.srcInfo, this._resolved_varSlot);
  }
}
