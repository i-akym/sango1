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

import java.util.ArrayList;
import java.util.List;

class PDynamicInvEval extends PDefaultExprObj implements PEval {
  PExprObj funObj;
  PExprObj params[];

  private PDynamicInvEval(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("dynamic_inv[src=");
    buf.append(this.srcInfo);
    buf.append(",fun=");
    buf.append(this.funObj);
    buf.append(",params=[");
    for (int i = 0; i < this.params.length; i++) {
      buf.append(this.params[i]);
      buf.append(",");
    }
    buf.append("]]");
    return buf.toString();
  }

  static PDynamicInvEval create(Parser.SrcInfo srcInfo, PScope outerScope, PExprObj funObj, PExprObj[] params) {
    PDynamicInvEval e = new PDynamicInvEval(srcInfo, outerScope);
    e.funObj = funObj;
    e.params = params;
    return e;
  }

  static PDynamicInvEval acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("apply")) { return null; }
    ParserB.Elem e = elem.getFirstChild();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("No specification at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    ArrayList<PExprObj> ps = new ArrayList<PExprObj>();
    if (e.getName().equals("params")) {
      ParserB.Elem ee = e.getFirstChild();
      while (ee != null) {
        PExprObj expr = PExpr.acceptX(ee, outerScope);
        if (expr == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        ps.add(expr);
        ee = ee.getNextSibling();
      }
      e = e.getNextSibling();
    }
    if (e == null || !e.getName().equals("closure")) {
      emsg = new StringBuffer();
      emsg.append("Closure missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    ParserB.Elem ee = e.getFirstChild();
    if (ee == null) {
      emsg = new StringBuffer();
      emsg.append("Closure missing at ");
      emsg.append(e.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PExprObj closure = PEval.acceptX(ee, outerScope);
    if (closure == null) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(ee.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    return create(elem.getSrcInfo(), outerScope, closure, ps.toArray(new PExprObj[ps.size()]));
  }

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].collectModRefs();
    }
    this.funObj.collectModRefs();
  }

  public PDynamicInvEval resolve() throws CompileException {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i] = this.params[i].resolve();
    }
    this.funObj = this.funObj.resolve();
    return this;
  }

  public void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].normalizeTypes();
    }
    this.funObj.normalizeTypes();
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    this.typeGraphNode = graph.createDynamicInvNode(this, this.params.length);
    PTypeGraph.Node[] args = new PTypeGraph.Node[this.params.length];
    for (int i = 0; i < this.params.length; i++) {
      args[i] = this.params[i].setupTypeGraph(graph);
      ((PTypeGraph.DynamicInvNode)this.typeGraphNode).setParamNode(i, args[i]);
    }
    ((PTypeGraph.DynamicInvNode)this.typeGraphNode).setClosureNode(this.funObj.setupTypeGraph(graph));
    if (this.funObj instanceof PClosure) {
      ((PClosure)this.funObj).addArgTypeNodes(args);
    }
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.SeqNode node = flow.createNodeForInv(this.srcInfo);
    PTypeSkel[] pts = ((PTypeRefSkel)this.funObj.getFixedType()).params;
    PTypeSkelBindings bindings = ((PTypeGraph.DynamicInvNode)this.typeGraphNode).bindings;
    // List<PVarSlot> nvList = this.typeGraphNode.getNewvarList();
// /* DEBUG */ System.out.println("nvList " + nvList);
    // for (int i = 0; i < nvList.size(); i++) {
      // node.addChild(flow.createNodeForNewTvar(this.srcInfo, nvList.get(i), this.scope));
    // }
    for (int i = 0; i < this.params.length; i++) {
      node.addChild(this.params[i].setupFlow(flow));
    }
    node.addChild(this.funObj.setupFlow(flow));
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
