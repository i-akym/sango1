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

class PClosure extends PDefaultExprObj {
  PEVarDef[] params;
  PRetDef retDef;
  PExpr[] implExprs;
  PScope outerScope;
  PScope bodyScope;

  private PClosure() {}

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("closure[src=");
    buf.append(this.srcInfo);
    buf.append(",params=[");
    for (int i = 0; i < this.params.length; i++) {
      buf.append(this.params[i]);
      buf.append(",");
    }
    buf.append("],ret=");
    buf.append(this.retDef);
    buf.append(",exprs=[");
    for (int i = 0; i < this.implExprs.length; i++) {
      buf.append(this.implExprs[i]);
      buf.append(",");
    }
    buf.append("]]");
    return buf.toString();
  }

  static class Builder {
    PClosure closure;
    List<PEVarDef> paramList;
    List<PExpr> implExprList;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.closure = new PClosure();
      this.paramList = new ArrayList<PEVarDef>();
      this.implExprList = new ArrayList<PExpr>();
    }

    void setSrcInfo(Parser.SrcInfo si) {
      this.closure.srcInfo = si;
    }

    void addParam(PEVarDef param) {
      this.paramList.add(param);
    }

    void addParamList(List<PEVarDef> paramList) {
      for (int i = 0; i < paramList.size(); i++) {
        this.addParam(paramList.get(i));
      }
    }

    void setRetDef(PRetDef retDef) {
      this.closure.retDef = retDef;
    }

    void addImplExpr(PExpr expr) {
      this.implExprList.add(expr);
    }

    void addImplExprList(List<PExpr> exprList) {
      for (int i = 0; i < exprList.size(); i++) {
        this.addImplExpr(exprList.get(i));
      }
    }

    PClosure create() {
      this.closure.params = this.paramList.toArray(new PEVarDef[this.paramList.size()]);
      this.closure.implExprs = this.implExprList.toArray(new PExpr[this.implExprList.size()]);
      return this.closure;
    }
  }

  static PClosure accept(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    PClosure closure = null;
    if ((closure = acceptNoArg(reader, spc)) != null) {
      ;
    } else if ((closure = acceptWithArgs(reader, spc)) != null) {
      ;
    }
    return closure;
  }

  static PClosure acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("closure-def")) { return null; }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(elem.getSrcInfo());
    ParserB.Elem e = elem.getFirstChild();

    if (e != null && e.getName().equals("params")) {
      ParserB.Elem ee = e.getFirstChild();
      while (ee != null) {
        PEVarDef var = PEVarDef.acceptX(ee, PEVarDef.CAT_FUN_PARAM, PEVarDef.TYPE_NEEDED);
        if (var == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        builder.addParam(var);
        ee = ee.getNextSibling();
      }
      e = e.getNextSibling();
    }

    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Return type missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PRetDef ret = PRetDef.acceptX(e);
    if (ret == null) {
      emsg = new StringBuffer();
      emsg.append("Return type missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setRetDef(ret);
    e = e.getNextSibling();

    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Implemenation missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (!e.getName().equals("impl")) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(e.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    ParserB.Elem ee = e.getFirstChild();
    if (ee == null) {
      builder.addImplExpr(PExpr.createDummyVoidExpr(e.getSrcInfo()));
    } else {
      while (ee != null) {
        PExpr expr = PExpr.acceptX(ee);
        if (expr == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        builder.addImplExpr(expr);
        ee = ee.getNextSibling();
      }
    }
    e = e.getNextSibling();
    return builder.create();
  }

  private static PClosure acceptNoArg(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.BKSLASH_BKSLASH, spc)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(t.getSrcInfo());
    builder.setRetDef(PRetDef.accept(reader));
    List<PExpr> implExprList;
    if ((implExprList = acceptImplExprSeq(reader)) == null) {
      emsg = new StringBuffer();
      emsg.append("Function body missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.addImplExprList(implExprList);
    return builder.create();
  }

  private static PClosure acceptWithArgs(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.BKSLASH, spc)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(t.getSrcInfo());
    PEVarDef param;
    while ((param = PEVarDef.accept(reader, PEVarDef.CAT_FUN_PARAM, PEVarDef.TYPE_NEEDED)) != null) {
      builder.addParam(param);
    }
    if (ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\"->\"missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setRetDef(PRetDef.accept(reader));
    List<PExpr> implExprList;
    if ((implExprList = acceptImplExprSeq(reader)) == null) {
      emsg = new StringBuffer();
      emsg.append("Function body missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.addImplExprList(implExprList);
    return builder.create();
  }

  private static List<PExpr> acceptImplExprSeq(ParserA.TokenReader reader) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.LBRACE, ParserA.SPACE_DO_NOT_CARE)) == null) {
      return null;
    }
    List<PExpr> exprList = PExpr.acceptSeq(reader, true);
    if (ParserA.acceptToken(reader, LToken.RBRACE, ParserA.SPACE_DO_NOT_CARE) == null) {
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
    return exprList;
  }

  public void setupScope(PScope scope) {
    if (scope == this.outerScope) { return; }
    this.outerScope = scope;
    this.scope = scope.enterClosure(this);
    this.idResolved = false;
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].setupScope(this.scope);
    }
    this.bodyScope = this.scope.enterInner();
    for (int i = 0; i < this.implExprs.length; i++) {
      this.implExprs[i].setupScope(this.bodyScope);
    }
    retDef.setupScope(this.scope);
  }

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].collectModRefs();
    }
    for (int i = 0; i < this.implExprs.length; i++) {
      this.implExprs[i].collectModRefs();
    }
    retDef.collectModRefs();
  }

  public PClosure resolve() throws CompileException {
    if (this.idResolved) { return this; }
    for (int i = 0; i < this.params.length; i++) {
      this.params[i] = this.params[i].resolve();
    }
    this.retDef = this.retDef.resolve();
    for (int i = 0; i < this.implExprs.length; i++) {
      this.implExprs[i] = this.implExprs[i].resolve();
    }
    this.idResolved = true;
    return this;
  }

  public void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].normalizeTypes();
    }
    this.retDef.normalizeTypes();
    for (int i = 0; i < this.implExprs.length; i++) {
      this.implExprs[i].normalizeTypes();
    }
  }

  public PTypeSkel[] getParamDefinedTypes() {
    PTypeSkel[] pts = new PTypeSkel[this.params.length];
    for (int i = 0; i < pts.length; i++) {
      pts[i] = this.params[i].getNormalizedType();
    }
    return pts;
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    this.typeGraphNode = graph.createClosureNode(this, this.params.length);
    for (int i = 0; i < this.params.length; i++) {
      ((PTypeGraph.ClosureNode)this.typeGraphNode).setParamNode(i, this.params[i].setupTypeGraph(graph));
    }
    PTypeGraph.Node n = null;
    for (int i = 0; i < this.implExprs.length; i++) {  // guaranteed not to be empty
      if (n == null) {
        n = this.implExprs[i].setupTypeGraph(graph);
      } else {
        PTypeGraph.SeqNode s = graph.createSeqNode(this.implExprs[i]);
        s.setLeadingTypeNode(n);
        s.setInNode(this.implExprs[i].setupTypeGraph(graph));
        n = s;
      }
    }
    PTypeGraph.Node r = this.retDef.setupTypeGraph(graph);
    r.setInNode(n);
    ((PTypeGraph.ClosureNode)this.typeGraphNode).setRetNode(r);
    return this.typeGraphNode;
  }

  void addArgTypeNodes(PTypeGraph.Node[] args) {
    if (args.length != this.params.length) {
      throw new IllegalArgumentException("Count of args is not guaranteed to be equal to that of params.");
    }
    for (int i = 0; i < args.length; i++) {
      this.params[i].getTypeGraphNode().setInNode(args[i]);
    }
  }

  public GFlow.Node setupFlow(GFlow flow) {
    String name = this.scope.getFunOfficial() + ":" + this.scope.generateId();
    PEVarSlot[] paramVarSlots = new PEVarSlot[this.params.length];
    PTypeSkel[] paramTypes = new PTypeSkel[this.params.length];
    for (int i = 0; i < this.params.length; i++) {
      paramVarSlots[i] = this.params[i].varSlot;
      paramTypes[i] = this.params[i].getFixedType();
    }
    GFlow.RootNode implNode = flow.createNodeForClosureImpl(this.srcInfo, name, paramVarSlots, paramTypes);
    for (int i = 0; i < implExprs.length - 1; i++) {
      implNode.addChild(this.implExprs[i].setupFlow(flow));
      implNode.addChild(flow.createSinkNode(this.implExprs[i].getSrcInfo()));
    }
    implNode.addChild(this.implExprs[this.implExprs.length - 1].setupFlow(flow));
    GFlow.SeqNode constrNode = flow.createNodeForClosureConstr(this.srcInfo, implNode /*, envList.size()*/);
    return constrNode;
  }
}
