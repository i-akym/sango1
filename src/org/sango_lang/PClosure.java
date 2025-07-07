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
  PExprVarDef[] params;
  PRetDef retDef;
  PExprList.Seq implExprs;
  PScope outerScope;
  PScope bodyScope;

  private PClosure(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope.prepareClosure());
    this.scope.defineClosure(this);
  }

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
    buf.append(this.implExprs);
    buf.append("]]");
    return buf.toString();
  }

  static class Builder {
    PClosure closure;
    PScope bodyScope;
    List<PExprVarDef> paramList;
    List<PExpr> implExprList;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.closure = new PClosure(srcInfo, outerScope);
      this.bodyScope = this.closure.scope.startInnerScope();
      this.paramList = new ArrayList<PExprVarDef>();
      this.implExprList = new ArrayList<PExpr>();
    }

    PScope getDefScope() { return this.closure.scope; }

    PScope getBodyScope() { return this.bodyScope; }

    void addParam(PExprVarDef param) {
      this.paramList.add(param);
    }

    void addParamList(List<PExprVarDef> paramList) {
      for (int i = 0; i < paramList.size(); i++) {
        this.addParam(paramList.get(i));
      }
    }

    void setRetDef(PRetDef retDef) {
      this.closure.retDef = retDef;
    }

    void setImplExprs(PExprList.Seq seq) {
      this.closure.implExprs = seq;
    }

    PClosure create() {
      this.closure.params = this.paramList.toArray(new PExprVarDef[this.paramList.size()]);
      return this.closure;
    }
  }

  static PClosure accept(ParserA.TokenReader reader, PScope outerScope, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    PClosure closure = null;
    if ((closure = acceptNoArg(reader, outerScope, spc)) != null) {
      ;
    } else if ((closure = acceptWithArgs(reader, outerScope, spc)) != null) {
      ;
    }
    return closure;
  }

  static PClosure acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("closure-def")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo(), outerScope);
    PScope defScope = builder.getDefScope();
    PScope bodyScope = builder.getBodyScope();
    ParserB.Elem e = elem.getFirstChild();

    if (e != null && e.getName().equals("params")) {
      ParserB.Elem ee = e.getFirstChild();
      while (ee != null) {
        PExprVarDef var = PExprVarDef.acceptX(ee, defScope, PExprVarDef.CAT_FUN_PARAM, PExprVarDef.TYPE_NEEDED);
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
    PRetDef ret = PRetDef.acceptX(e, defScope);
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
    Parser.SrcInfo si = e.getSrcInfo();
    List<PExpr> ies = new ArrayList<PExpr>();
    ParserB.Elem ee = e.getFirstChild();
    if (ee == null) {
      ies.add(PExpr.createDummyVoidExpr(si, bodyScope));
    } else {
      while (ee != null) {
        PExpr expr = PExpr.acceptX(ee, bodyScope);
        if (expr == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        ies.add(expr);
        ee = ee.getNextSibling();
      }
    }
    builder.setImplExprs(PExprList.Seq.create(si, bodyScope, ies));
    e = e.getNextSibling();
    return builder.create();
  }

  private static PClosure acceptNoArg(ParserA.TokenReader reader, PScope outerScope, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.BKSLASH_BKSLASH, spc)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(t.getSrcInfo(), outerScope);
    PScope defScope = builder.getDefScope();
    PScope bodyScope = builder.getBodyScope();
    builder.setRetDef(PRetDef.accept(reader, defScope));
    PExprList.Seq implExprList;
    if ((implExprList = acceptImplExprSeq(reader, bodyScope)) == null) {
      emsg = new StringBuffer();
      emsg.append("Function body missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setImplExprs(implExprList);
    return builder.create();
  }

  private static PClosure acceptWithArgs(ParserA.TokenReader reader, PScope outerScope, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.BKSLASH, spc)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(t.getSrcInfo(), outerScope);
    PScope defScope = builder.getDefScope();
    PScope bodyScope = builder.getBodyScope();
    PExprVarDef param;
    while ((param = PExprVarDef.accept(reader, defScope, PExprVarDef.CAT_FUN_PARAM, PExprVarDef.TYPE_NEEDED)) != null) {
      builder.addParam(param);
    }
    if (ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\"->\"missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setRetDef(PRetDef.accept(reader, defScope));
    PExprList.Seq implExprList;
    if ((implExprList = acceptImplExprSeq(reader, bodyScope)) == null) {
      emsg = new StringBuffer();
      emsg.append("Function body missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setImplExprs(implExprList);
    return builder.create();
  }

  private static PExprList.Seq acceptImplExprSeq(ParserA.TokenReader reader, PScope bodyScope) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptToken(reader, LToken.LBRACE, ParserA.SPACE_DO_NOT_CARE)) == null) {
      return null;
    }
    PExprList.Seq exprList = PExprList.acceptSeq(reader, t.getSrcInfo(), bodyScope, true);
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

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].collectModRefs();
    }
    this.implExprs.collectModRefs();
    retDef.collectModRefs();
  }

  public PClosure resolve() throws CompileException {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i] = this.params[i].resolve();
    }
    this.implExprs = this.implExprs.resolve();
    this.retDef.scope.doCopy();
    this.retDef = this.retDef.resolve();
    return this;
  }

  public void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].normalizeTypes();
    }
    this.implExprs.normalizeTypes();
    retDef.normalizeTypes();
  }

  public PTypeSkel[] getParamDefinedTypes() throws CompileException {
    PTypeSkel[] pts = new PTypeSkel[this.params.length];
    for (int i = 0; i < pts.length; i++) {
      pts[i] = this.params[i].getNormalizedType();
    }
    return pts;
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    this.typeGraphNode = graph.createClosureNode(this, this.params.length);
    for (int i = 0; i < this.params.length; i++) {
      ((PTypeGraph.ClosureNode)this.typeGraphNode).setParamNode(i, this.params[i].setupTypeGraph(graph));
    }
    PTypeGraph.Node rn = this.retDef.setupTypeGraph(graph);
    rn.setInNode(this.implExprs.setupTypeGraph(graph));
    ((PTypeGraph.ClosureNode)this.typeGraphNode).setRetNode(rn);
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
    PExprVarSlot[] paramVarSlots = new PExprVarSlot[this.params.length];
    PTypeSkel[] paramTypes = new PTypeSkel[this.params.length];
    for (int i = 0; i < this.params.length; i++) {
      paramVarSlots[i] = this.params[i]._resolved_varSlot;
      paramTypes[i] = this.params[i].getFinalizedType();
    }
    GFlow.RootNode implNode = flow.createNodeForClosureImpl(this.srcInfo, name, paramVarSlots, paramTypes);
    for (int i = 0; i < implExprs.exprs.length - 1; i++) {
      implNode.addChild(this.implExprs.exprs[i].setupFlow(flow));
      implNode.addChild(flow.createSinkNode(this.implExprs.exprs[i].getSrcInfo()));
    }
    implNode.addChild(this.implExprs.exprs[this.implExprs.exprs.length - 1].setupFlow(flow));
    GFlow.SeqNode constrNode = flow.createNodeForClosureConstr(this.srcInfo, implNode);
    return constrNode;
  }
}
