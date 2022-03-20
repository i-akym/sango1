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

import java.util.List;
import java.util.ArrayList;

class PStaticInvEval extends PDefaultExprObj implements PEval {
  PExprId funId;  // HERE: official name needed
  PExprObj params[];

  private PStaticInvEval() {}

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("static_inv[src=");
    buf.append(this.srcInfo);
    buf.append(",fun=");
    buf.append(this.funId);
    buf.append(",params=[");
    for (int i = 0; i < this.params.length; i++) {
      buf.append(this.params[i]);
      buf.append(",");
    }
    buf.append("]]");
    return buf.toString();
  }

  static PStaticInvEval create(Parser.SrcInfo srcInfo, PExprId funId, PExprObj[] params) {
    PStaticInvEval e = new PStaticInvEval();
    e.srcInfo = srcInfo;
    e.funId = funId;
    e.params = params;
    return e;
  }

  static PEval acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("invoke")) { return null; }
    String id = elem.getAttrValueAsExtendedId("id");
    if (id == null) {
      emsg = new StringBuffer();
      emsg.append("Id missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    String mid = elem.getAttrValueAsId("mid");
    PExprId eid = null;
    if (id.equals("@SELF")) {
      if (mid != null) {
        emsg = new StringBuffer();
        emsg.append("Module id is not allowed for self reference at ");
        emsg.append(elem.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    } else if (Parser.isNormalId(id)) {
      eid = PExprId.create(elem.getSrcInfo(), mid, id);;
    } else {
      emsg = new StringBuffer();
      emsg.append("Invalid id at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(". - ");
      emsg.append(id);
      throw new CompileException(emsg.toString());
    }
    ArrayList<PExprObj> ps = new ArrayList<PExprObj>();
    ParserB.Elem e = elem.getFirstChild();
    while (e != null) {
      PExprObj expr = PExpr.acceptX(e);
      if (expr == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(e.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
      }
      ps.add(expr);
      e = e.getNextSibling();
    }
    Parser.SrcInfo si = elem.getSrcInfo();
    PExprObj[] params = ps.toArray(new PExprObj[ps.size()]);
    return (eid != null)?
      create(si, eid, params):
      PDynamicInvEval.create(si, PFunRef.createSelf(si), params);
  }

  public void setupScope(PScope scope) {
    if (scope == this.scope) { return; }
    this.scope = scope;
    this.idResolved = false;
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].setupScope(scope);
    }
    this.funId.setupScope(scope);
  }

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].collectModRefs();
    }
    this.funId.collectModRefs();
  }

  public PStaticInvEval resolve() throws CompileException {
    if (this.idResolved) { return this; }
    for (int i = 0; i < this.params.length; i++) {
      this.params[i] = this.params[i].resolve();
    }
    this.funId = (PExprId)this.funId.resolve();
    return this;
  }

  public void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].normalizeTypes();
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    this.typeGraphNode = graph.createStaticInvNode(this, this.funId, this.params.length);
    for (int i = 0; i < this.params.length; i++) {
      ((PTypeGraph.StaticInvNode)this.typeGraphNode).setParamNode(i, this.params[i].setupTypeGraph(graph));
    }
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.SeqNode node = flow.createNodeForInv(this.srcInfo);
    PFunDef fd = ((PTypeGraph.StaticInvNode)this.typeGraphNode).funDef;
    PTypeSkel[] pts = fd.getParamTypes();
    PTypeSkelBindings bindings = ((PTypeGraph.StaticInvNode)this.typeGraphNode).bindings;
// /* DEBUG */ System.out.println("static inv to " + fd.getOfficialName() + " with " + bindings);
    // List<PVarSlot> nvList = this.typeGraphNode.getNewvarList();
// /* DEBUG */ System.out.println("nvList " + nvList);
    // for (int i = 0; i < nvList.size(); i++) {
      // node.addChild(flow.createNodeForNewTvar(this.srcInfo, nvList.get(i), this.scope));
    // }
    for (int i = 0; i < this.params.length; i++) {
      node.addChild(this.params[i].setupFlow(flow));
    }
    node.addChild(flow.createNodeForFunRefBody(
      this.srcInfo, this.scope.theMod.modNameToModRefIndex(fd.getModName()), fd.getOfficialName()));
    // List<PVarSlot> extracted = new ArrayList<PVarSlot>();
    // int varIndex = 0;
    // for (int i = 0; i < this.params.length; i++) {
      // List<PVarSlot> tvList = pts[i].extractVars(extracted);
      // if (tvList != null) {
        // for (int j = 0; j < tvList.size(); j++) {
          // PVarSlot v = tvList.get(j);
          // if (!bindings.isGivenTvar(v)) {
            // PTypeSkel t = bindings.resolveShallow(v);
            // if (t == null) { throw new RuntimeException("Unbound. " + v + bindings); }
            // node.addChild(flow.createNodeForClosureCast(
              // this.srcInfo,
              // varIndex,
	      // t.setupFlow(flow, this.scope, bindings)));
          // }
          // extracted.addAll(tvList);
        // }
      // }
    // }
    return node;
  }
}
