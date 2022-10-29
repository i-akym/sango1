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
import java.util.ArrayList;
import java.util.List;

class PFeatureStmt extends PDefaultProgObj implements PFeatureDef {
  PFeature sig;
  int availability;
  int acc;
  PType obj;  // PTypeVarDef or PTypeVarRef
  PTypeRef impl;

  PFeatureStmt(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope.enterInner());
    this.scope.startDef();
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("feature[src=");
    buf.append(this.srcInfo);
    buf.append(",sig=");
    buf.append(this.sig);
    buf.append(",acc=");
    buf.append(this.acc);
    buf.append(",obj=");
    buf.append(this.obj);
    buf.append(",impl=");
    buf.append(this.impl);
    buf.append("]");
    return buf.toString();
  }

  static class Builder {
    PFeatureStmt feature;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.feature = new PFeatureStmt(srcInfo, outerScope);
    }

    PScope getDefScope() { return this.feature.scope; }

    void setAvailability(int availability) {
      this.feature.availability = availability;
    }

    void setSig(PFeature sig) {
      this.feature.sig = sig;
    }

    void setAcc(int acc) {
      this.feature.acc = acc;
    }

    void setObjType(PType t) {
      this.feature.obj = t;
    }

    void setImplType(PTypeRef tr) {
      this.feature.impl = tr;
    }

    PFeatureStmt create() throws CompileException {
      return this.feature;
    }
  }

  static PFeatureStmt accept(ParserA.TokenReader reader, PScope outerScope) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptSpecifiedWord(reader, "feature", ParserA.SPACE_DO_NOT_CARE)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance(t.getSrcInfo(), outerScope);
    PScope defScope = builder.getDefScope();
    builder.setAvailability(PModule.acceptAvailability(reader));
    PFeature sig;
    if ((sig = PFeature.acceptSig(reader, defScope)) == null) {
      emsg = new StringBuffer();
      emsg.append("Feature sig missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setSig(sig);

    builder.setAcc(PModule.acceptAcc(reader, PModule.ACC_OPTS_FOR_FEATURE, PModule.ACC_DEFAULT_FOR_FEATURE));

    if (ParserA.acceptToken(reader, LToken.COL_EQ, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\":=\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }

    PType objType;
    if ((objType = PType.accept(reader, defScope, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("Object type missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    // HERE: obj type check
    builder.setObjType(objType);

    if (ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\"->\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }

    PType implType;
    if ((implType = PType.accept(reader, defScope, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("Implementation type missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (!(implType instanceof PTypeRef)) {
      emsg = new StringBuffer();
      emsg.append("Invalid implementation type at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(". - ");
      emsg.append(implType.toString());
      throw new CompileException(emsg.toString());
    }
    builder.setImplType((PTypeRef)implType);

    return builder.create();
  }

  static PFeatureStmt acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("feature-def")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo(), outerScope);
    PScope defScope = builder.getDefScope();

    builder.setAvailability(PModule.acceptXAvailabilityAttr(elem));
    int acc = PModule.acceptXAccAttr(elem, PModule.ACC_OPTS_FOR_EXTEND, PModule.ACC_DEFAULT_FOR_EXTEND);
    builder.setAcc(acc);

    PType.Builder tb = PType.Builder.newInstance(elem.getSrcInfo(), defScope);
    ParserB.Elem e = elem.getFirstChild();
    if (e != null && e.getName().equals("params")) {
      ParserB.Elem ee = e.getFirstChild();
      while (ee != null) {
        PTypeVarDef var = PTypeVarDef.acceptX(ee, defScope);
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

// HERE
    return builder.create();
  }

  public void collectModRefs() throws CompileException {
    this.sig.collectModRefs();
    this.impl.collectModRefs();
  }

  public PFeatureStmt resolve() throws CompileException {
    StringBuffer emsg;
    this.sig = this.sig.resolve();
    this.obj = this.obj.resolve();
    this.impl = this.impl.resolve();
    return this;
  }

  public int getAvailability() { return this.availability; }

  public int getAcc() { return this.acc; }

  public int getParamCount() { return this.sig.params.length; }

  void checkAcc() throws CompileException {
    if (this.acc == Module.ACC_PRIVATE) { return; }
    // this.impl.checkAcc();  // HERE
    return;
  }

  public void normalizeTypes() throws CompileException {
// HERE
    // List<PDefDict.TconInfo> tis = new ArrayList<PDefDict.TconInfo>();
    // for (int i = 0; i < this.constrs.length; i++) {
      // this.constrs[i].normalizeTypes();
      // for (int j = 0; j < constrs[i].attrs.length; j++) {
        // constrs[i].attrs[j].nTypeSkel.checkVariance(PTypeSkel.WIDER);
        // constrs[i].attrs[j].nTypeSkel.collectTconInfo(tis);
      // }
    // }
    // this.scope.addReferredTcons(tis);
  }

  public void setupExtensionGraph(PDefDict.ExtGraph g) throws CompileException {}

  public void checkConcreteness() throws CompileException {}

  List<PEvalStmt> generateFuns(PModule mod) throws CompileException {
    List<PEvalStmt> funs = new ArrayList<PEvalStmt>();
    return funs;
  }

  // PEvalStmt generateCallHashFun(PModule mod) throws CompileException {
    // eval <*T0 *T1 .. TCON> *X _call_hash_TCON @private -> <int> {
    //   X _hash_TCON
    // }

    // if (!mod.funOfficialDict.containsKey("_hash_" + this.tcon)) { return null; }
    // Parser.SrcInfo si = this.srcInfo.appendPostfix("_hash");
    // PScope modScope = this.scope.theMod.scope;
    // PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, modScope);
    // PScope defScope = evalStmtBuilder.getDefScope();
    // // PScope retScope = evalStmtBuilder.getRetScope();
    // PScope bodyScope = evalStmtBuilder.getBodyScope();
    // PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, defScope);
    // PScope retScope = retDefBuilder.getScope();
    // evalStmtBuilder.setOfficial("_call_hash_" + this.tcon);
    // evalStmtBuilder.setAcc(Module.ACC_PRIVATE);
    // PType.Builder paramTypeBuilder = PType.Builder.newInstance(si, defScope);
    // String[] paramNames = PModule.generateIds("T", this.tparams.length);
    // for (int i = 0; i < paramNames.length; i++) {
      // paramTypeBuilder.addItem(PTypeVarDef.create(si, defScope, paramNames[i], Module.INVARIANT, false, null));
    // }
    // paramTypeBuilder.addItem(PTypeId.create(si, defScope, null, this.tcon, false));
    // evalStmtBuilder.addParam(PExprVarDef.create(si, defScope, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));
    // PType.Builder retTypeBuilder = PType.Builder.newInstance(si, retScope);
    // retTypeBuilder.addItem(PTypeId.create(si, retScope, PModule.MOD_ID_LANG, "int", false));
    // retDefBuilder.setType(retTypeBuilder.create());
    // evalStmtBuilder.setRetDef(retDefBuilder.create());
    // PEval.Builder callEvalBuilder = PEval.Builder.newInstance(si, bodyScope);
    // callEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, bodyScope, null, "X")));
    // callEvalBuilder.addItem(PEvalItem.create(PExprId.create(si, bodyScope, null, "_hash_" + this.tcon)));
    // List<PExpr> ies = new ArrayList<PExpr>();
    // ies.add(PExpr.create(callEvalBuilder.create()));
    // evalStmtBuilder.setImplExprs(PExprList.Seq.create(si, bodyScope, ies));
    // return evalStmtBuilder.create();
  // }

}
