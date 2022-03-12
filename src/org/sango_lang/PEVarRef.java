/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2021 AKIYAMA Isao                                         *
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

class PEVarRef extends PDefaultEvalAndPtnElem {
  String name;
  PEVarSlot varSlot;

  private PEVarRef() {}

  static PEVarRef create(Parser.SrcInfo srcInfo, String name, PEVarSlot varSlot) {
    PEVarRef v = new PEVarRef();
    v.srcInfo = srcInfo;
    v.name = name;
    v.varSlot = varSlot;
    return v;
  }

  static PExprId acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("var")) { return null; }
    String id = elem.getAttrValueAsId("id");
    if (id == null) {
      emsg = new StringBuffer();
      emsg.append("Variable name missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PExprId v = PExprId.create(elem.getSrcInfo(), null, id);
    v.setVar();
    return v;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("varref[");
    if (this.srcInfo != null) {
      buf.append("src=");
      buf.append(this.srcInfo);
    }
    buf.append(",name=");
    buf.append(this.name);
    if (this.varSlot != null) {
      buf.append(",slot=");
      buf.append(this.varSlot);
    }
    buf.append("]");
    return buf.toString();
  }

  public PTypeId deepCopy(Parser.SrcInfo srcInfo) {  // rollback to PTypeId
    return PTypeId.create(srcInfo, /* null, */ null, this.name, false);
  }

  public void setupScope(PScope scope) {
    if (scope == this.scope) { return; }
    this.scope = scope;
    this.idResolved = false;
  }

  public void collectModRefs() throws CompileException {}

  public PEVarRef resolve() throws CompileException {
    this.idResolved = true;
    return this;
  }

  public PDefDict.TconInfo getTconInfo() { return null; }

  public void excludePrivateAcc() throws CompileException {}

  public void normalizeTypes() {
    if (this.varSlot.varDef.nTypeSkel == null) {
      this.varSlot.varDef.normalizeTypes();
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
/* DEBUG */ if (this.scope == null) { System.out.println("null scope " + this); }
    return graph.createVarRefNode(this, name, this.varSlot.varDef.typeGraphNode);
  }

  public PTypeGraph.Node getTypeGraphNode() {
    return this.varSlot.varDef.typeGraphNode;
  }

  public PTypeSkel getFixedType() { return this.varSlot.varDef.getFixedType(); }

  public GFlow.Node setupFlow(GFlow flow) {
    return flow.createNodeForVarRef(this.srcInfo, this.varSlot);
  }
}
