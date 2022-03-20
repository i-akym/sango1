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

class PEvalStmt extends PDefaultProgObj implements PFunDef {
  int availability;  // Module.AVAILABILITY_xxx
  PEVarDef[] params;
  String official;
  String[] aliases;
  int acc;  // Module.ACC_xxx
  PScope bodyScope;
  PRetDef retDef;
  PExpr[] implExprs;  // null means native impl

  private PEvalStmt() {}

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("eval[src=");
    buf.append(this.srcInfo);
    buf.append(",official=");
    buf.append(this.official);
    buf.append(",aliases=[");
    for (int i = 0; i < this.aliases.length; i++) {
      buf.append(this.aliases[i]);
      buf.append(",");
    }
    buf.append("],acc=");
    buf.append(this.acc);
    buf.append(",ret=");
    buf.append(this.retDef);
    if (this.implExprs != null) {
      buf.append(",exprs=[");
      for (int i = 0; i < this.implExprs.length; i++) {
        buf.append(this.implExprs[i]);
      buf.append(",");
      }
      buf.append("]");
    } else {
      buf.append(",NATIVE_IMPL");
    }
    buf.append("]");
    return buf.toString();
  }

  static class Builder {
    PEvalStmt eval;
    List<PEVarDef> paramList;
    List<String> aliasList;
    List<PExpr> implExprList;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.eval = new PEvalStmt();
      this.paramList = new ArrayList<PEVarDef>();
      this.aliasList = new ArrayList<String>();
      // implExprList will be initialized later
    }

    void setSrcInfo(Parser.SrcInfo si) {
      this.eval.srcInfo = si;
    }

    void setAvailability(int availability) {
      this.eval.availability = availability;
    }

    void addParam(PEVarDef param) {
      this.paramList.add(param);
    }

    void addParamList(List<PEVarDef> paramList) {
      for (int i = 0; i < paramList.size(); i++) {
        this.addParam(paramList.get(i));
      }
    }

    void setOfficial(String official) {
      this.eval.official = official;
    }

    void addAlias(String alias) {
      this.aliasList.add(alias);
    }

    void addAliasList(List<String> aliasList) {
      this.aliasList.addAll(aliasList);
    }

    void setAcc(int acc) {
      this.eval.acc = acc;
    }

    void setRetDef(PRetDef retDef) {
      this.eval.retDef = retDef;
    }
    void startImplExprSeq() {
      this.implExprList = new ArrayList<PExpr>();
    }

    void addImplExpr(PExpr expr) {
      this.implExprList.add(expr);
    }

    void addImplExprList(List<PExpr> exprList) {
      for (int i = 0; i < exprList.size(); i++) {
        this.addImplExpr(exprList.get(i));
      }
    }

    PEvalStmt create() throws CompileException {
      this.eval.params = this.paramList.toArray(new PEVarDef[this.paramList.size()]);
      this.eval.aliases = this.aliasList.toArray(new String[this.aliasList.size()]);
      if (this.implExprList != null) {
        this.eval.implExprs = this.implExprList.toArray(new PExpr[this.implExprList.size()]);
      }
      return this.eval;
    }
  }

  static PEvalStmt accept(ParserA.TokenReader reader) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptSpecifiedWord(reader, "eval", ParserA.SPACE_DO_NOT_CARE)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(t.getSrcInfo());
    builder.setAvailability(PModule.acceptAvailability(reader));
    builder.addParamList(acceptParamList(reader));
    PExprId official = PExprId.accept(reader, PExprId.ID_NO_QUAL, ParserA.SPACE_NEEDED);
    if (official == null) {
      emsg = new StringBuffer();
      emsg.append("Function official name missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setOfficial(official.name);
    builder.addAliasList(acceptAliasList(reader));
    builder.setAcc(PModule.acceptAcc(reader, PModule.ACC_OPTS_FOR_EVAL, Module.ACC_PRIVATE));
    if (ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\"->\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PRetDef retDef = PRetDef.accept(reader);
    builder.setRetDef(retDef);
    List<PExpr> impl = acceptImpl(reader);
    if (impl != null) {
      builder.startImplExprSeq();
      builder.addImplExprList(impl);
    } else {
      ;  // native impl
    }
    if (ParserA.acceptToken(reader, LToken.SEM_SEM, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\";;\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return builder.create();
  }

  static PEvalStmt acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("eval-def")) { return null; }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(elem.getSrcInfo());

    String official = elem.getAttrValueAsId("official");
    if (official == null) {
      emsg = new StringBuffer();
      emsg.append("Function official name missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setOfficial(official);

    builder.setAvailability(PModule.acceptXAvailabilityAttr(elem));
    int acc = PModule.acceptXAccAttr(elem, PModule.ACC_OPTS_FOR_EVAL, PModule.ACC_DEFAULT_FOR_EVAL);
    builder.setAcc(acc);

    ParserB.Elem e = elem.getFirstChild();

    if (e != null && e.getName().equals("aliases")) {
      ParserB.Elem ee = e.getFirstChild();
      while (ee != null) {
        if (!ee.getName().equals("alias")) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        String alias = ee.getAttrValueAsId("id");
        if (alias == null) {
          emsg = new StringBuffer();
          emsg.append("Alias identifier missing at ");
          emsg.append(ee.getSrcInfo().toString());
          emsg.append(". - ");
          emsg.append(ee.getName());
          throw new CompileException(emsg.toString());
        }
        builder.addAlias(alias);
        ee = ee.getNextSibling();
      }
      e = e.getNextSibling();
    }

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

    if (e != null && e.getName().equals("impl")) {
      builder.startImplExprSeq();
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
    }
    return builder.create();
  }

  private static List<PEVarDef> acceptParamList(ParserA.TokenReader reader) throws CompileException, IOException {
    List<PEVarDef> paramList = new ArrayList<PEVarDef>();
    PEVarDef param;
    while ((param = PEVarDef.accept(reader, PEVarDef.CAT_FUN_PARAM, PEVarDef.TYPE_NEEDED)) != null) {
      paramList.add(param);
    }
    return paramList;
  }

  private static List<String> acceptAliasList(ParserA.TokenReader reader) throws CompileException, IOException {
    List<String> aliasList = new ArrayList<String>();
    PExprId a = null;
    int state = 0;
    while (state >= 0) {
      if (ParserA.acceptToken(reader, LToken.VBAR, ParserA.SPACE_DO_NOT_CARE) != null) {
        state = 1;
      } else if (state == 1 && (a = PExprId.accept(reader, PExprId.ID_NO_QUAL, ParserA.SPACE_DO_NOT_CARE)) != null) {
        aliasList.add(a.name);
        state = 0;
      } else {
        state = -1;
      }
    }
    return aliasList;
  }

  static List<PExpr> acceptImpl(ParserA.TokenReader reader) throws CompileException, IOException {
    StringBuffer emsg;
    List<PExpr> impl = null;
    if (acceptImplNative(reader) != null) {
      ;
    } else if ((impl= acceptImplExprSeq(reader)) != null) {
      ;
    } else {
      emsg = new StringBuffer();
      emsg.append("Function body missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return impl;
  }

  private static ParserA.Token acceptImplNative(ParserA.TokenReader reader) throws CompileException, IOException {
    return ParserA.acceptSpecifiedWord(reader, PModule.IMPL_WORD_NATIVE, ParserA.SPACE_DO_NOT_CARE);
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
    if (scope == this.scope) { return; }
    this.scope = scope.defineFun(this);
    this.idResolved = false;
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].setupScope(this.scope);
    }
    if (this.implExprs != null) {
      this.bodyScope = scope.enterInner();
      for (int i = 0; i < this.implExprs.length; i++) {
        this.implExprs[i].setupScope(this.bodyScope);
      }
    }
    this.retDef.setupScope(this.scope);
  }

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].collectModRefs();
    }
    if (this.implExprs != null) {
      for (int i = 0; i < this.implExprs.length; i++) {
        this.implExprs[i].collectModRefs();
      }
    }
    this.retDef.collectModRefs();
  }

  public PEvalStmt resolve() throws CompileException {
    if (this.idResolved) { return this; }
    for (int i = 0; i < this.params.length; i++) {
      this.params[i] = this.params[i].resolve();
    }
    this.retDef = this.retDef.resolve();
    if (this.implExprs != null) {
      for (int i = 0; i < this.implExprs.length; i++) {
        this.implExprs[i] = this.implExprs[i].resolve();
      }
    }
    this.idResolved = true;
    return this;
  }

  void checkAcc() throws CompileException {
    if (this.acc == Module.ACC_PRIVATE) { return; }
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].excludePrivateAcc();
    }
    this.retDef.excludePrivateAcc();
  }

  public void normalizeTypes() throws CompileException {
    List<PDefDict.TconInfo> tis = new ArrayList<PDefDict.TconInfo>();
    if (this.params != null) {
      for (int i = 0; i < this.params.length; i++) {
        this.params[i].normalizeTypes();
        this.params[i].nTypeSkel.collectTconInfo(tis);
      }
    }
    this.retDef.normalizeTypes();
    this.retDef.nTypeSkel.collectTconInfo(tis);
    this.scope.addReferredTcons(tis);
    if (this.implExprs != null) {
      for (int i = 0; i < this.implExprs.length; i++) {
        this.implExprs[i].normalizeTypes();
      }
    }
  }

  void setupTypeGraph(PTypeGraph graph) {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].setupTypeGraph(graph);
    }
    if (this.implExprs != null) {
      PTypeGraph.Node n = null;
      for (int i = 0; i < this.implExprs.length; i++) {
        if (n == null) {
          n = this.implExprs[i].setupTypeGraph(graph);
        } else {
          PTypeGraph.SeqNode s = graph.createSeqNode(this.implExprs[i]);
          s.setLeadingTypeNode(n);
          s.setInNode(this.implExprs[i].setupTypeGraph(graph));
          n = s;
        }
      }
      PTypeGraph.RetNode rn = (PTypeGraph.RetNode)this.retDef.setupTypeGraph(graph);
      rn.setInNode(n);
    }
  }

  public Cstr getModName() {
    return this.scope.theMod.name;
  }

  public String getOfficialName() {
    return this.official;
  }

  public int getAvailability() { return this.availability; }

  public PTypeSkel[] getParamTypes() {
    PTypeSkel[] pts = new PTypeSkel[this.params.length];
    for (int i = 0; i < pts.length; i++) {
      pts[i] = this.params[i].getNormalizedType();
    }
    return pts;
  }

  public PTypeSkel getRetType() {
    return this.retDef.getNormalizedType();
  }
}
