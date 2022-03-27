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

class PExtendStmt extends PDefaultProgObj implements PDataDef {
  int availability;
  String baseMod;
  String baseTcon;
  String tcon;
  PTypeVarDef[] tparams;
  PType sig;
  int acc;
  PDataConstrDef[] constrs;
  PDefDict.TconInfo baseTconInfo;

  PExtendStmt(Parser.SrcInfo srcInfo) {
    super(srcInfo);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("extend[src=");
    buf.append(this.srcInfo);
    buf.append(",basemod=");
    buf.append(this.baseMod);
    buf.append(",basetcon=");
    buf.append(this.baseTcon);
    buf.append(",sig=");
    buf.append(this.sig);
    buf.append("],acc=");
    buf.append(this.acc);
    buf.append(",constrs=[");
    for (int i = 0; i < this.constrs.length; i++) {
      buf.append(this.constrs[i]);
      buf.append(",");
    }
    buf.append("]]");
    return buf.toString();
  }

  static class Builder {
    PExtendStmt ext;
    PType base;
    String rename;
    List<PDataConstrDef> constrList;

    static Builder newInstance(Parser.SrcInfo srcInfo) {
      return new Builder(srcInfo);
    }

    Builder(Parser.SrcInfo srcInfo) {
      this.ext = new PExtendStmt(srcInfo);
      this.constrList = new ArrayList<PDataConstrDef>();
    }

    // void setSrcInfo(Parser.SrcInfo si) {
      // this.ext.srcInfo = si;
    // }

    void setBase(PType base) {
      this.base = base;
    }

    void setRename(String rename) {
      this.rename = rename;
    }

    void setAvailability(int availability) {
      this.ext.availability = availability;
    }

    void setAcc(int acc) {
      this.ext.acc = acc;
    }

    void addConstr(PDataConstrDef constr) {
      this.constrList.add(constr);
    }

    void addConstrList(List<PDataConstrDef> constrList) {
      for (int i = 0; i < constrList.size(); i++) {
        this.addConstr(constrList.get(i));
      }
    }

    PExtendStmt create() throws CompileException {
      if (this.base instanceof PType.Undet) {
        PType.Undet u = (PType.Undet)this.base;
        this.ext.baseMod = (u.id.mod != null)? u.id.mod: PModule.MOD_ID_LANG;
        this.ext.baseTcon = u.id.name;
        this.ext.tcon = (this.rename != null)? this.rename: this.ext.baseTcon;
        this.ext.tparams = new PTypeVarDef[0];
        PType.Builder sigBuilder = PType.Builder.newInstance();
        sigBuilder.setSrcInfo(u.id.srcInfo);
        sigBuilder.addItem(PTypeId.create(
          u.id.srcInfo,
          null,
          this.ext.tcon,
          false));
        this.ext.sig = sigBuilder.create();
      } else if (this.base instanceof PTypeRef) {
        PTypeRef tr = (PTypeRef)this.base;
        this.ext.baseMod = (tr.mod != null)? tr.mod: PModule.MOD_ID_LANG;
        this.ext.baseTcon = tr.tcon;
        this.ext.tcon = (this.rename != null)? this.rename: this.ext.baseTcon;
        this.ext.tparams = new PTypeVarDef[tr.params.length];
        for (int i = 0; i < tr.params.length; i++) {
          if (tr.params[i] instanceof PTypeVarDef) {
            this.ext.tparams[i] = (PTypeVarDef)tr.params[i];
          } else {
            throw new RuntimeException("Unexpected type.");
          }
        }
        this.ext.sig = PTypeRef.create(
          tr.srcInfo,
          PTypeId.create(tr.srcInfo, null, this.ext.tcon, false),
          tr.params);
      } else {
        throw new RuntimeException("Unexpected type.");
      }
      // /* DEBUG */ System.out.print("extend sig init "); System.out.println(this.ext.sig);
      this.ext.constrs = this.constrList.toArray(new PDataConstrDef[this.constrList.size()]);
      return this.ext;
    }
  }

  static PExtendStmt accept(ParserA.TokenReader reader) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptSpecifiedWord(reader, "extend", ParserA.SPACE_DO_NOT_CARE)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(t.getSrcInfo());
    // builder.setSrcInfo(t.getSrcInfo());
    builder.setAvailability(PModule.acceptAvailability(reader));
    PType base;
    if ((base = PType.acceptSig1(reader, PExprId.ID_MAYBE_QUAL)) == null) {
      emsg = new StringBuffer();
      emsg.append("Type description missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setBase(base);
    if (ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE) != null) {
      PTypeId rename;
      if ((rename = PTypeId.accept(reader, PExprId.ID_NO_QUAL, ParserA.SPACE_DO_NOT_CARE)) == null) {
        emsg = new StringBuffer();
        emsg.append("New name missing at ");
        emsg.append(reader.getCurrentSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      builder.setRename(rename.name);
    }
    builder.setAcc(PModule.acceptAcc(reader, PModule.ACC_OPTS_FOR_EXTEND, PModule.ACC_DEFAULT_FOR_EXTEND));
    if (ParserA.acceptToken(reader, LToken.COL_EQ, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\":=\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PDataStmt.DataDefBody body;
    if ((body = PDataStmt.acceptDataDefBody(reader)) == null) {
      emsg = new StringBuffer();
      emsg.append("Data definition body missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.addConstrList(body.constrList);
    if (ParserA.acceptToken(reader, LToken.SEM_SEM, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\";;\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return builder.create();
  }

  static PExtendStmt acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("extend-def")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo());
    // builder.setSrcInfo(elem.getSrcInfo());

    String btcon = elem.getAttrValueAsId("base-tcon");
    if (btcon == null) {
      emsg = new StringBuffer();
      emsg.append("Base type constructor missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PTypeId btconItem = PTypeId.create(elem.getSrcInfo(), elem.getAttrValueAsId("base-mid"), btcon, false);
    btconItem.setTcon();

    String renamed = elem.getAttrValueAsId("renamed-tcon");
    if (renamed != null) {
      builder.setRename(renamed);
    }

    builder.setAvailability(PModule.acceptXAvailabilityAttr(elem));
    int acc = PModule.acceptXAccAttr(elem, PModule.ACC_OPTS_FOR_EXTEND, PModule.ACC_DEFAULT_FOR_EXTEND);
    builder.setAcc(acc);

    PType.Builder tb = PType.Builder.newInstance();
    ParserB.Elem e = elem.getFirstChild();
    if (e != null && e.getName().equals("params")) {
      ParserB.Elem ee = e.getFirstChild();
      while (ee != null) {
        PTypeVarDef var = PTypeVarDef.acceptX(ee);
        if (var == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        tb.addItem(var);
        ee = ee.getNextSibling();
      }
      e = e.getNextSibling();
    }
    tb.addItem(btconItem);
    builder.setBase(tb.create());

    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Data definition body missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PDataStmt.DataDefBody body = PDataStmt.acceptXDataDefBody(e);
    if (body == null) {
      emsg = new StringBuffer();
      emsg.append("Data definition body missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.addConstrList(body.constrList);
    return builder.create();
  }

  public void setupScope(PScope scope) {
    StringBuffer emsg;
    PScope s = scope.start();
    if (s == this.scope) { return; }
    this.scope = s;
    this.idResolved = false;
    // /* DEBUG */ System.out.print("extend sig setupscope "); System.out.println(this.sig);
    this.sig.setupScope(s);
    for (int i = 0; i < this.constrs.length; i++) {
      this.constrs[i].setupScope(this.scope);
    }
  }

  public void collectModRefs() throws CompileException {
    this.sig.collectModRefs();
    for (int i = 0; i < this.constrs.length; i++) {
      this.constrs[i].collectModRefs();
    }
  }
  public PExtendStmt resolve() throws CompileException {
    StringBuffer emsg;
    if (this.idResolved) { return this; }
    if (this.baseMod != null && this.scope.resolveModId(this.baseMod) == null) {  // refer base mod id in order to register foreign mod
      emsg = new StringBuffer();
      emsg.append("Module id \"");
      emsg.append(this.baseMod);
      emsg.append("\" not defined at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    // /* DEBUG */ System.out.print("extend sig resolveid "); System.out.println(this.sig);
    this.sig = this.sig.resolve();
    for (int i = 0; i < this.tparams.length; i++) {
      this.tparams[i] = this.tparams[i].resolve();
    }
    for (int i = 0; i < this.constrs.length; i++) {
      this.constrs[i] = this.constrs[i].resolve();
      this.constrs[i].setDataType(this.sig);
    }
    // /* DEBUG */ System.out.println("resolve " + this.baseMod + "." + this.baseTcon);
    if ((this.baseTconInfo = this.scope.resolveTcon(this.baseMod, this.baseTcon)) == null) {
      emsg = new StringBuffer();
      emsg.append("Base type constructor not found at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString()) ;
    }
    // /* DEBUG */ System.out.println("extend base tcon info " + this.baseTconInfo);
    if (this.baseTconInfo.props.paramCount() >= 0 && this.tparams.length != this.baseTconInfo.props.paramCount()) {
      emsg = new StringBuffer();
      emsg.append("Parameter count of 'extend' definition mismatch at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString()) ;
    }
    if (this.baseTconInfo.props.paramProps != null) {
      for (int i = 0; i < this.tparams.length; i++) {
        if (this.tparams[i].variance != this.baseTconInfo.props.paramProps[i].variance) {
          emsg = new StringBuffer();
          emsg.append("Variance of *");
          emsg.append(this.tparams[i].name);
          emsg.append(" mismatch with that of base definition at ");
          emsg.append(this.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString()) ;
        }
      }
    }
    this.idResolved = true;
    return this;
  }

  public String getFormalTcon() { return this.tcon; }

  public int getParamCount() { return this.tparams.length; }

  public PTypeRefSkel getTypeSig() {
    return (PTypeRefSkel)this.sig.getSkel();
  }

  public int getAvailability() { return this.availability; }

  public int getAcc() { return this.acc; }

  public int getConstrCount() { return this.constrs.length; }

  public PDataDef.Constr getConstr(String dcon) {
    PDataDef.Constr c = null;
    for (int i = 0; i < this.constrs.length; i++) {
      if (this.constrs[i].dcon.equals(dcon)) {
        c = this.constrs[i];
        break;
      }
    }
    return c;
  }

  public PDataDef.Constr getConstrAt(int index) { return this.constrs[index]; }

  void checkAcc() throws CompileException {
    if (this.acc == Module.ACC_PRIVATE) { return; }
    for (int i = 0; i < this.tparams.length; i++) {
      this.tparams[i].excludePrivateAcc();
    }
    if (this.acc != Module.ACC_OPAQUE) {
      for (int i = 0; i < this.constrs.length; i++) {
        this.constrs[i].excludePrivateAcc();
      }
    }
  }

  public void normalizeTypes() throws CompileException {
    List<PDefDict.TconInfo> tis = new ArrayList<PDefDict.TconInfo>();
    for (int i = 0; i < this.constrs.length; i++) {
      this.constrs[i].normalizeTypes();
      for (int j = 0; j < constrs[i].attrs.length; j++) {
        constrs[i].attrs[j].nTypeSkel.checkVariance(PTypeSkel.WIDER);
        constrs[i].attrs[j].nTypeSkel.collectTconInfo(tis);
      }
    }
    this.scope.addReferredTcons(tis);
  }

  public void setupExtensionGraph(PDefDict.ExtGraph g) throws CompileException {
    g.addExtension(this.baseTconInfo.key, PDefDict.TconKey.create(this.scope.myModName(), this.tcon));
  }

  public void checkConcreteness() throws CompileException {
    for (int i = 0; i < this.constrs.length; i++) {
      this.constrs[i].checkConcreteness();
    }
  }

  public PDefDict.TconKey getBaseTconKey() { return this.baseTconInfo.key; }

  List<PEvalStmt> generateFuns(PModule mod) throws CompileException {
    List<PEvalStmt> funs = new ArrayList<PEvalStmt>();
    PEvalStmt e;
    List<PEvalStmt> es;
    if ((e = this.generateCallHashFun(mod)) != null) { funs.add(e); }
    if ((e = this.generateCallDebugReprFun(mod)) != null) { funs.add(e); }
    if ((e = this.generateInFun(mod)) != null) { funs.add(e); }
    if ((e = this.generateNarrowFun(mod)) != null) { funs.add(e); }
    if ((es = this.generateAttrFuns(mod)) != null) { funs.addAll(es); }
    return funs;
  }

  PEvalStmt generateCallHashFun(PModule mod) throws CompileException {
    // eval <*T0 *T1 .. TCON> *X _call_hash_TCON @private -> <int> {
    //   X _hash_TCON
    // }
    if (!mod.funOfficialDict.containsKey("_hash_" + this.tcon)) { return null; }
    Parser.SrcInfo si = this.srcInfo.appendPostfix("_hash");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si);
    // evalStmtBuilder.setSrcInfo(si);
    evalStmtBuilder.setOfficial("_call_hash_" + this.tcon);
    evalStmtBuilder.setAcc(Module.ACC_PRIVATE);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance();
    paramTypeBuilder.setSrcInfo(si);
    String[] paramNames = PModule.generateIds("T", this.tparams.length);
    for (int i = 0; i < paramNames.length; i++) {
      paramTypeBuilder.addItem(PTypeVarDef.create(si, paramNames[i], Module.INVARIANT, false, null));
    }
    paramTypeBuilder.addItem(PTypeId.create(si, null, this.tcon, false));
    evalStmtBuilder.addParam(PExprVarDef.create(si, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));
    PType.Builder retTypeBuilder = PType.Builder.newInstance();
    retTypeBuilder.setSrcInfo(si);
    retTypeBuilder.addItem(PTypeId.create(si, PModule.MOD_ID_LANG, "int", false));
    evalStmtBuilder.setRetDef(PRetDef.create(retTypeBuilder.create()));
    evalStmtBuilder.startImplExprSeq();
    PEval.Builder callEvalBuilder = PEval.Builder.newInstance();
    callEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, null, "X")));
    callEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, null, "_hash_" + this.tcon)));
    evalStmtBuilder.addImplExpr(PExpr.create(callEvalBuilder.create()));
    return evalStmtBuilder.create();
  }

  PEvalStmt generateCallDebugReprFun(PModule mod) throws CompileException {
    // eval <*T0 *T1 .. TCON> *X _call_debug_repr_TCON @private -> <cstr> {
    //   X _debug_repr_TCON
    // }
    if (!mod.funOfficialDict.containsKey("_debug_repr_" + this.tcon)) { return null; }
    if (!mod.funOfficialDict.containsKey("_debug_repr_" + this.tcon)) { return null; }
    Parser.SrcInfo si = this.srcInfo.appendPostfix("_debug_repr");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si);
    // evalStmtBuilder.setSrcInfo(si);
    evalStmtBuilder.setOfficial("_call_debug_repr_" + this.tcon);
    evalStmtBuilder.setAcc(Module.ACC_PRIVATE);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance();
    paramTypeBuilder.setSrcInfo(si);
    String[] paramNames = PModule.generateIds("T", this.tparams.length);
    for (int i = 0; i < paramNames.length; i++) {
      paramTypeBuilder.addItem(PTypeVarDef.create(si, paramNames[i], Module.INVARIANT, false, null));
    }
    paramTypeBuilder.addItem(PTypeId.create(si, null, this.tcon, false));
    evalStmtBuilder.addParam(PExprVarDef.create(si, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));
    PType.Builder retTypeBuilder = PType.Builder.newInstance();
    retTypeBuilder.setSrcInfo(si);
    retTypeBuilder.addItem(PTypeId.create(si, PModule.MOD_ID_LANG, "cstr", false));
    evalStmtBuilder.setRetDef(PRetDef.create(retTypeBuilder.create()));
    evalStmtBuilder.startImplExprSeq();
    PEval.Builder callEvalBuilder = PEval.Builder.newInstance();
    callEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, null, "X")));
    callEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, null, "_debug_repr_" + this.tcon)));
    evalStmtBuilder.addImplExpr(PExpr.create(callEvalBuilder.create()));
    return evalStmtBuilder.create();
  }

  PEvalStmt generateInFun(PModule mod) throws CompileException {
    // eval @availability <*T0 *T1 .. TCON+> *X _in_TCON? | TCON? @public -> <bool> {
    //   X case {
    //   ; *** DCON0 -> true$
    //   ; *** DCON1 -> true$
    //   ;  :
    //   ; ** -> X base._in_TCON?
    //   }
    // }
    if (this.acc != Module.ACC_PUBLIC && this.acc != Module.ACC_PROTECTED) { return null; }
    String[] names = PModule.generateInFunNames(this.tcon);  // official name and aliases
    if (mod.funOfficialDict.containsKey(names[0])) { return null; }
    Parser.SrcInfo si = srcInfo.appendPostfix("_in");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si);
    // evalStmtBuilder.setSrcInfo(si);
    evalStmtBuilder.setAvailability(this.availability);
    evalStmtBuilder.setOfficial(names[0]);
    evalStmtBuilder.addAlias(names[1]);
    evalStmtBuilder.setAcc(Module.ACC_PUBLIC);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance();
    paramTypeBuilder.setSrcInfo(si);
    String[] paramNames = PModule.generateIds("T", this.tparams.length);
    for (int i = 0; i < paramNames.length; i++) {
      PTypeVarDef p = (PTypeVarDef)this.tparams[i].deepCopy(
        si, PType.COPY_EXT_KEEP, PType.COPY_VARIANCE_INVARIANT, PType.COPY_CONCRETE_KEEP);
      paramTypeBuilder.addItem(p);
    }
    paramTypeBuilder.addItem(PTypeId.create(si, null, this.tcon, true));
    evalStmtBuilder.addParam(PExprVarDef.create(si, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));
    PType.Builder retTypeBuilder = PType.Builder.newInstance();
    retTypeBuilder.setSrcInfo(si);
    retTypeBuilder.addItem(PTypeId.create(si, PModule.MOD_ID_LANG, "bool", false));
    evalStmtBuilder.setRetDef(PRetDef.create(retTypeBuilder.create()));
    evalStmtBuilder.startImplExprSeq();
    PEval.Builder caseEvalBuilder = PEval.Builder.newInstance();
    caseEvalBuilder.setSrcInfo(si);
    caseEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, null, "X")));
    PCaseBlock.Builder caseBlockBuilder = PCaseBlock.Builder.newInstance(si);
    // caseBlockBuilder.setSrcInfo(si);
    for (int i = 0; i < this.constrs.length; i++) {
      PDataDef.Constr constr = this.constrs[i];
      PCaseClause.Builder caseClauseBuilder = PCaseClause.Builder.newInstance(si);
      // caseClauseBuilder.setSrcInfo(si);
      PCasePtnMatch.Builder casePtnMatchBuilder = PCasePtnMatch.Builder.newInstance(si);
      // casePtnMatchBuilder.setSrcInfo(si);
      PPtn.Builder ptnBuilder = PPtn.Builder.newInstance();
      ptnBuilder.setSrcInfo(si);
      ptnBuilder.setContext(PPtnMatch.CONTEXT_TRIAL);
      ptnBuilder.addItem(PPtnItem.create(si, PPtnMatch.CONTEXT_TRIAL, null, PWildCards.create(si)));
      ptnBuilder.addItem(PPtnItem.create(si, PPtnMatch.CONTEXT_TRIAL, null, PExprId.create(si, null, constr.getDcon())));
      casePtnMatchBuilder.setPtnMatch(PPtnMatch.create(si, PPtnMatch.CONTEXT_TRIAL, null, ptnBuilder.create()));
      caseClauseBuilder.addPtnMatch(casePtnMatchBuilder.create());
      PEval.Builder trueTermEvalBuilder = PEval.Builder.newInstance();
      trueTermEvalBuilder.setSrcInfo(si);
      trueTermEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, PModule.MOD_ID_LANG, "true$")));
      caseClauseBuilder.addActionExpr(PExpr.create(trueTermEvalBuilder.create()));
      caseBlockBuilder.addClause(caseClauseBuilder.create());
    }
    PCaseClause.Builder otherwiseCaseClauseBuilder = PCaseClause.Builder.newInstance(si);
    // otherwiseCaseClauseBuilder.setSrcInfo(si);
    PCasePtnMatch.Builder otherwisePtnMatchBuilder = PCasePtnMatch.Builder.newInstance(si);
    // otherwisePtnMatchBuilder.setSrcInfo(si);
    PPtn.Builder otherwisePtnBuilder = PPtn.Builder.newInstance();
    otherwisePtnBuilder.setSrcInfo(si);
    otherwisePtnBuilder.setContext(PPtnMatch.CONTEXT_TRIAL);
    otherwisePtnBuilder.addItem(PPtnItem.create(si, PPtnMatch.CONTEXT_TRIAL, null, PWildCard.create(si)));
    otherwisePtnMatchBuilder.setPtnMatch(PPtnMatch.create(si, PPtnMatch.CONTEXT_TRIAL, null, otherwisePtnBuilder.create()));
    otherwiseCaseClauseBuilder.addPtnMatch(otherwisePtnMatchBuilder.create());
    String[] baseNames = PModule.generateInFunNames(this.baseTcon);
    PEval.Builder forwardingEvalBuilder = PEval.Builder.newInstance();
    forwardingEvalBuilder.setSrcInfo(si);
    forwardingEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, null, "X")));
    forwardingEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, this.baseMod, baseNames[0])));
    otherwiseCaseClauseBuilder.addActionExpr(PExpr.create(forwardingEvalBuilder.create()));
    caseBlockBuilder.addClause(otherwiseCaseClauseBuilder.create());
    caseEvalBuilder.addItem(PEvalItem.create(caseBlockBuilder.create()));
    evalStmtBuilder.addImplExpr(PExpr.create(caseEvalBuilder.create()));
    return evalStmtBuilder.create();
  }

  PEvalStmt generateNarrowFun(PModule mod) throws CompileException {
    // eval @availability <*T0 *T1 .. TCON+> *X _narrow_TCON | narrow @public -> <<T0 T1 .. TCON> maybe> {
    //   X case {
    //   ; *V0 *V1 .. DCON0 -> (V0 V1 .. DCON0) value$
    //   ; *V0 *V1 .. DCON1 -> (V0 V1 .. DCON1) value$
    //   ;  :
    //   ; ** -> X base._narrow_TCON
    //   }
    // }
    if (this.acc != Module.ACC_PUBLIC && this.acc != Module.ACC_PROTECTED) { return null; }
    String[] names = PModule.generateNarrowFunNames(this.tcon);
    if (mod.funOfficialDict.containsKey(names[0])) { return null; }
    Parser.SrcInfo si = srcInfo.appendPostfix("_narrow");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si);
    // evalStmtBuilder.setSrcInfo(si);
    evalStmtBuilder.setAvailability(this.availability);
    evalStmtBuilder.setOfficial(names[0]);
    evalStmtBuilder.addAlias(names[1]);
    evalStmtBuilder.setAcc(Module.ACC_PUBLIC);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance();
    paramTypeBuilder.setSrcInfo(si);
    String[] paramNames = PModule.generateIds("T", this.tparams.length);
    for (int i = 0; i < paramNames.length; i++) {
      PTypeVarDef p = (PTypeVarDef)this.tparams[i].deepCopy(
        si, PType.COPY_EXT_KEEP, PType.COPY_VARIANCE_INVARIANT, PType.COPY_CONCRETE_KEEP);
      paramNames[i] = p.name;
      paramTypeBuilder.addItem(p);
    }
    paramTypeBuilder.addItem(PTypeId.create(si, null, this.tcon, true));
    evalStmtBuilder.addParam(PExprVarDef.create(si, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));
    PType.Builder retMaybeTypeBuilder = PType.Builder.newInstance();
    retMaybeTypeBuilder.setSrcInfo(si);
    PType.Builder retDataTypeBuilder = PType.Builder.newInstance();
    retDataTypeBuilder.setSrcInfo(si);
    for (int i = 0; i < paramNames.length; i++) {
      retDataTypeBuilder.addItem(PTypeId.createVar(si, paramNames[i]));
    }
    retDataTypeBuilder.addItem(PTypeId.create(si, null, this.tcon, false));
    retMaybeTypeBuilder.addItem(retDataTypeBuilder.create());
    retMaybeTypeBuilder.addItem(PTypeId.create(si, PModule.MOD_ID_LANG, "maybe", false));
    evalStmtBuilder.setRetDef(PRetDef.create(retMaybeTypeBuilder.create()));
    evalStmtBuilder.startImplExprSeq();
    PEval.Builder caseEvalBuilder = PEval.Builder.newInstance();
    caseEvalBuilder.setSrcInfo(si);
    caseEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, null, "X")));
    PCaseBlock.Builder caseBlockBuilder = PCaseBlock.Builder.newInstance(si);
    // caseBlockBuilder.setSrcInfo(si);
    for (int i = 0; i < this.constrs.length; i++) {
      PDataDef.Constr constr = this.constrs[i];
      PCaseClause.Builder caseClauseBuilder = PCaseClause.Builder.newInstance(si);
      // caseClauseBuilder.setSrcInfo(si);
      PCasePtnMatch.Builder casePtnMatchBuilder = PCasePtnMatch.Builder.newInstance(si);
      // casePtnMatchBuilder.setSrcInfo(si);
      PPtn.Builder ptnBuilder = PPtn.Builder.newInstance();
      ptnBuilder.setSrcInfo(si);
      ptnBuilder.setContext(PPtnMatch.CONTEXT_TRIAL);
      String[] attrs = PModule.generateIds("V", constr.getAttrCount());
      for (int j = 0; j < constr.getAttrCount(); j++) {
        ptnBuilder.addItem(PPtnItem.create(si, PPtnMatch.CONTEXT_TRIAL, null, PExprVarDef.create(si, PExprVarDef.CAT_LOCAL_VAR, null, attrs[j])));
      }
      ptnBuilder.addItem(PPtnItem.create(si, PPtnMatch.CONTEXT_TRIAL, null, PExprId.create(si, null, constr.getDcon())));
      casePtnMatchBuilder.setPtnMatch(PPtnMatch.create(si, PPtnMatch.CONTEXT_TRIAL, null, ptnBuilder.create()));
      caseClauseBuilder.addPtnMatch(casePtnMatchBuilder.create());
      PEval.Builder narrowedResBuilder = PEval.Builder.newInstance();
      narrowedResBuilder.setSrcInfo(si);
      PEval.Builder narrowedValueBuilder = PEval.Builder.newInstance();
      narrowedValueBuilder.setSrcInfo(si);
      for (int j = 0; j < constr.getAttrCount(); j++) {
        narrowedValueBuilder.addItem(PEvalItem.create(PExprId.create(si, null, attrs[j])));
      }
      narrowedValueBuilder.addItem(PEvalItem.create(PExprId.create(si, null, constr.getDcon())));
      narrowedResBuilder.addItem(PEvalItem.create(narrowedValueBuilder.create()));
      narrowedResBuilder.addItem(PEvalItem.create(PExprId.create(si, PModule.MOD_ID_LANG, "value$")));
      caseClauseBuilder.addActionExpr(PExpr.create(narrowedResBuilder.create()));
      caseBlockBuilder.addClause(caseClauseBuilder.create());
    }
    PCaseClause.Builder otherwiseCaseClauseBuilder = PCaseClause.Builder.newInstance(si);
    // otherwiseCaseClauseBuilder.setSrcInfo(si);
    PCasePtnMatch.Builder otherwisePtnMatchBuilder = PCasePtnMatch.Builder.newInstance(si);
    // otherwisePtnMatchBuilder.setSrcInfo(si);
    PPtn.Builder otherwisePtnBuilder = PPtn.Builder.newInstance();
    otherwisePtnBuilder.setSrcInfo(si);
    otherwisePtnBuilder.setContext(PPtnMatch.CONTEXT_TRIAL);
    otherwisePtnBuilder.addItem(PPtnItem.create(si, PPtnMatch.CONTEXT_TRIAL, null, PWildCard.create(si)));
    otherwisePtnMatchBuilder.setPtnMatch(PPtnMatch.create(si, PPtnMatch.CONTEXT_TRIAL, null, otherwisePtnBuilder.create()));
    otherwiseCaseClauseBuilder.addPtnMatch(otherwisePtnMatchBuilder.create());
    String[] baseNames = PModule.generateNarrowFunNames(this.baseTcon);
    PEval.Builder forwardingEvalBuilder = PEval.Builder.newInstance();
    forwardingEvalBuilder.setSrcInfo(si);
    forwardingEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, null, "X")));
    forwardingEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, this.baseMod, baseNames[0])));
    otherwiseCaseClauseBuilder.addActionExpr(PExpr.create(forwardingEvalBuilder.create()));
    caseBlockBuilder.addClause(otherwiseCaseClauseBuilder.create());
    caseEvalBuilder.addItem(PEvalItem.create(caseBlockBuilder.create()));
    evalStmtBuilder.addImplExpr(PExpr.create((caseEvalBuilder.create())));
    return evalStmtBuilder.create();
  }

  List<PEvalStmt> generateAttrFuns(PModule mod) throws CompileException {
    List<PEvalStmt> es = new ArrayList<PEvalStmt>();
    PEvalStmt e;
    if (this.constrs.length == 1) {
      PDataConstrDef constr = this.constrs[0];
      for (int i = 0; i < constr.attrs.length; i++) {
        PDataAttrDef attr = constr.attrs[i];
        if ((e = this.generateAttrFun1(mod, constr, attr)) != null) { es.add(e); }
      }
    } else {
      for (int i = 0; i < this.constrs.length; i++) {
        PDataConstrDef constr = this.constrs[i];
        for (int j = 0; j < constr.attrs.length; j++) {
          PDataAttrDef attr = constr.attrs[j];
          if ((e = this.generateAttrFun2(mod, constr, attr)) != null) { es.add(e); }
        }
      }
    }
    return es;
  }

  PEvalStmt generateAttrFun1(PModule mod, PDataConstrDef constr, PDataAttrDef attr) throws CompileException {
    // eval @availability <*T0 *T1 .. TCON> *X _attr_TCON_A | A @xxx -> <A's type> {
    //   X = A: *V *** dcon$,
    //   V
    // }
    if (attr.name == null) { return null; }
    String[] names = PModule.generateAttrFunNames(this.tcon, attr.name);
    if (mod.funOfficialDict.containsKey(names[0])) { return null; }
    Parser.SrcInfo si = this.srcInfo.appendPostfix("_attr");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si);
    // evalStmtBuilder.setSrcInfo(si);
    evalStmtBuilder.setAvailability(this.availability);
    evalStmtBuilder.setOfficial(names[0]);
    evalStmtBuilder.addAlias(names[1]);
    evalStmtBuilder.setAcc((this.acc == Module.ACC_PUBLIC || this.acc == Module.ACC_PROTECTED)? Module.ACC_PUBLIC: Module.ACC_PRIVATE);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance();
    paramTypeBuilder.setSrcInfo(si);
    for (int i = 0; i < this.tparams.length; i++) {
      PTypeVarDef p = (PTypeVarDef)this.tparams[i].deepCopy(
        si, PType.COPY_EXT_KEEP, PType.COPY_VARIANCE_INVARIANT, PType.COPY_CONCRETE_KEEP);
      paramTypeBuilder.addItem(p);
    }
    paramTypeBuilder.addItem(PTypeId.create(si, null, this.tcon, false));
    evalStmtBuilder.addParam(PExprVarDef.create(si, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));
    PType.Builder retTypeBuilder = PType.Builder.newInstance();
    retTypeBuilder.setSrcInfo(si);
    retTypeBuilder.addItem(attr.type.deepCopy(
      si, PType.COPY_EXT_KEEP, PType.COPY_VARIANCE_INVARIANT, PType.COPY_CONCRETE_OFF));
    evalStmtBuilder.setRetDef(PRetDef.create(retTypeBuilder.create()));
    evalStmtBuilder.startImplExprSeq();
    PEval.Builder matchEvalBuilder = PEval.Builder.newInstance();
    matchEvalBuilder.setSrcInfo(si);
    matchEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, null, "X")));
    PPtn.Builder matchPtnBuilder = PPtn.Builder.newInstance();
    matchPtnBuilder.setSrcInfo(si);
    matchPtnBuilder.setContext(PPtnMatch.CONTEXT_TRIAL);
    matchPtnBuilder.addItem(PPtnItem.create(si, PPtnMatch.CONTEXT_TRIAL, attr.name, PExprVarDef.create(si, PExprVarDef.CAT_LOCAL_VAR, null, "V")));
    matchPtnBuilder.addItem(PPtnItem.create(si, PPtnMatch.CONTEXT_TRIAL, null, PWildCards.create(si)));
    matchPtnBuilder.addItem(PPtnItem.create(si, PPtnMatch.CONTEXT_TRIAL, null, PExprId.create(si, null, constr.dcon)));
    evalStmtBuilder.addImplExpr(PExpr.create(si, matchEvalBuilder.create(), PPtnMatch.create(si, PPtnMatch.CONTEXT_TRIAL, null, matchPtnBuilder.create())));
    PEval.Builder retEvalBuilder = PEval.Builder.newInstance();
    retEvalBuilder.setSrcInfo(si);
    retEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, null, "V")));
    evalStmtBuilder.addImplExpr(PExpr.create(retEvalBuilder.create()));
    return evalStmtBuilder.create();
  }

  PEvalStmt generateAttrFun2(PModule mod, PDataConstrDef constr, PDataAttrDef attr) throws CompileException {
    // eval @availability <*T0 *T1 .. TCON> *X _maybe_attr_TCON_A | maybe_A @xxx -> <<A's type> maybe> {
    //   X case {
    //   ; A: *V *** DCON -> V value$
    //   ; ** -> none$
    //   }
    // }
    if (attr.name == null) { return null; }
    String[] names = PModule.generateMaybeAttrFunNames(this.tcon, attr.name);
    if (mod.funOfficialDict.containsKey(names[0])) { return null; }
    Parser.SrcInfo si = srcInfo.appendPostfix("_maybe_attr");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si);
    // evalStmtBuilder.setSrcInfo(si);
    evalStmtBuilder.setAvailability(this.availability);
    evalStmtBuilder.setOfficial(names[0]);
    evalStmtBuilder.addAlias(names[1]);
    evalStmtBuilder.setAcc((this.acc == Module.ACC_PUBLIC || this.acc == Module.ACC_PROTECTED)? Module.ACC_PUBLIC: Module.ACC_PRIVATE);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance();
    paramTypeBuilder.setSrcInfo(si);
    for (int i = 0; i < this.tparams.length; i++) {
      PTypeVarDef p = (PTypeVarDef)this.tparams[i].deepCopy(
        si, PType.COPY_EXT_KEEP, PType.COPY_VARIANCE_INVARIANT, PType.COPY_CONCRETE_KEEP);
      paramTypeBuilder.addItem(p);
    }
    paramTypeBuilder.addItem(PTypeId.create(si, null, this.tcon, false));
    evalStmtBuilder.addParam(PExprVarDef.create(si, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));
    PType.Builder retTypeBuilder = PType.Builder.newInstance();
    retTypeBuilder.setSrcInfo(si);
    retTypeBuilder.addItem(attr.type.deepCopy(
      si, PType.COPY_EXT_KEEP, PType.COPY_VARIANCE_INVARIANT, PType.COPY_CONCRETE_OFF));
    retTypeBuilder.addItem(PTypeId.create(si, PModule.MOD_ID_LANG, "maybe", false));
    evalStmtBuilder.setRetDef(PRetDef.create(retTypeBuilder.create()));
    evalStmtBuilder.startImplExprSeq();
    PEval.Builder caseEvalBuilder = PEval.Builder.newInstance();
    caseEvalBuilder.setSrcInfo(si);
    caseEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, null, "X")));
    PCaseBlock.Builder caseBlockBuilder = PCaseBlock.Builder.newInstance(si);
    // caseBlockBuilder.setSrcInfo(si);
    PCaseClause.Builder caseClauseBuilder = PCaseClause.Builder.newInstance(si);
    // caseClauseBuilder.setSrcInfo(si);
    PCasePtnMatch.Builder casePtnMatchBuilder = PCasePtnMatch.Builder.newInstance(si);
    // casePtnMatchBuilder.setSrcInfo(si);
    PPtn.Builder ptnBuilder = PPtn.Builder.newInstance();
    ptnBuilder.setSrcInfo(si);
    ptnBuilder.setContext(PPtnMatch.CONTEXT_TRIAL);
    ptnBuilder.addItem(PPtnItem.create(si, PPtnMatch.CONTEXT_TRIAL, attr.name, PExprVarDef.create(si, PExprVarDef.CAT_LOCAL_VAR, null, "V")));
    ptnBuilder.addItem(PPtnItem.create(si, PPtnMatch.CONTEXT_TRIAL, null, PWildCards.create(si)));
    ptnBuilder.addItem(PPtnItem.create(si, PPtnMatch.CONTEXT_TRIAL, null, PExprId.create(si, null, constr.dcon)));
    casePtnMatchBuilder.setPtnMatch(PPtnMatch.create(si, PPtnMatch.CONTEXT_TRIAL, null, ptnBuilder.create()));
    caseClauseBuilder.addPtnMatch(casePtnMatchBuilder.create());
    PEval.Builder valueEvalBuilder = PEval.Builder.newInstance();
    valueEvalBuilder.setSrcInfo(si);
    valueEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, null, "V")));
    valueEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, PModule.MOD_ID_LANG, "value$")));
    caseClauseBuilder.addActionExpr(PExpr.create(valueEvalBuilder.create()));
    caseBlockBuilder.addClause(caseClauseBuilder.create());
    PCaseClause.Builder otherwiseCaseClauseBuilder = PCaseClause.Builder.newInstance(si);
    // otherwiseCaseClauseBuilder.setSrcInfo(si);
    PCasePtnMatch.Builder otherwisePtnMatchBuilder = PCasePtnMatch.Builder.newInstance(si);
    // otherwisePtnMatchBuilder.setSrcInfo(si);
    PPtn.Builder otherwisePtnBuilder = PPtn.Builder.newInstance();
    otherwisePtnBuilder.setSrcInfo(si);
    otherwisePtnBuilder.setContext(PPtnMatch.CONTEXT_TRIAL);
    otherwisePtnBuilder.addItem(PPtnItem.create(si, PPtnMatch.CONTEXT_TRIAL, null, PWildCard.create(si)));
    otherwisePtnMatchBuilder.setPtnMatch(PPtnMatch.create(si, PPtnMatch.CONTEXT_TRIAL, null, otherwisePtnBuilder.create()));
    otherwiseCaseClauseBuilder.addPtnMatch(otherwisePtnMatchBuilder.create());
    PEval.Builder noneEvalBuilder = PEval.Builder.newInstance();
    noneEvalBuilder.setSrcInfo(si);
    noneEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, PModule.MOD_ID_LANG, "none$")));
    otherwiseCaseClauseBuilder.addActionExpr(PExpr.create(noneEvalBuilder.create()));
    caseBlockBuilder.addClause(otherwiseCaseClauseBuilder.create());
    caseEvalBuilder.addItem(PEvalItem.create(caseBlockBuilder.create()));
    evalStmtBuilder.addImplExpr(PExpr.create(caseEvalBuilder.create()));
    return evalStmtBuilder.create();
  }
}
