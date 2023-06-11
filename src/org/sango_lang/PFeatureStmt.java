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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class PFeatureStmt extends PDefaultProgObj implements PFeatureDef {
  Module.Availability availability;
  Module.Access acc;
  PTypeVarDef obj;
  PFeature sig;
  PTypeRef impl;

  PFeatureStmt(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope.enterInner());
    this.scope.startDef();
    this.availability = Module.AVAILABILITY_GENERAL;  // default
    this.acc = Module.ACC_PRIVATE;  // default
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("feature[src=");
    buf.append(this.srcInfo);
    buf.append(",acc=");
    buf.append(this.acc);
    buf.append(",obj=");
    buf.append(this.obj);
    buf.append(",sig=");
    buf.append(this.sig);
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

    void setAvailability(Module.Availability availability) {
      this.feature.availability = availability;
    }

    void setAcc(Module.Access acc) {
      this.feature.acc = acc;
    }

    void setObjType(PTypeVarDef t) {
      this.feature.obj = t;
    }

    void setSig(PFeature sig) {
      this.feature.sig = sig;
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

    PTypeVarDef objType;
    if ((t = ParserA.acceptToken(reader, LToken.LT, ParserA.SPACE_NEEDED)) == null) {
      emsg = new StringBuffer();
      emsg.append("Object type missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if ((objType = PTypeVarDef.accept(reader, defScope)) == null) {
      emsg = new StringBuffer();
      emsg.append("Invalid object type at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if ((t = ParserA.acceptToken(reader, LToken.GT, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("Systax error at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setObjType(objType);

    PFeature sig;
    if ((sig = PFeature.acceptSig(reader, defScope)) == null) {
      emsg = new StringBuffer();
      emsg.append("Feature signature missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setSig(sig);

    builder.setAcc(PModule.acceptAcc(reader, PModule.ACC_OPTS_FOR_FEATURE, PModule.ACC_DEFAULT_FOR_FEATURE));

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
      emsg.append("Invalid implementation data type at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(". - ");
      emsg.append(implType.toString());
      throw new CompileException(emsg.toString());
    }
    PTypeRef itr = (PTypeRef)implType;
    // following check will be done in data stmt generation
    // for (int i = 0; i < itr.params.length; i++) {
      // if (itr.params[i] instanceof PType.Undet) {  // var ref or type ref w/o params
        // ;
      // } else if (itr.params[i] instanceof PTypeVarDef) {
        // ;
      // } else {  // PTypeRef
        // emsg = new StringBuffer();
        // emsg.append("Invalid feature implementation type paramter at ");
        // emsg.append(itr.params[i].getSrcInfo());
        // emsg.append(". parameter ");
        // emsg.append(i + 1);
        // throw new CompileException(emsg.toString());
      // }
    // }
    builder.setImplType(itr);

    if (ParserA.acceptToken(reader, LToken.SEM_SEM, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\";;\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }

    return builder.create();
  }

  static PFeatureStmt acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("feature-def")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo(), outerScope);
    PScope defScope = builder.getDefScope();

    builder.setAvailability(PModule.acceptXAvailabilityAttr(elem));
    Module.Access acc = PModule.acceptXAccAttr(elem, PModule.ACC_OPTS_FOR_EXTEND, PModule.ACC_DEFAULT_FOR_EXTEND);
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
    this.obj = this.obj.resolve();
    this.sig = this.sig.resolve();
    this.impl = this.impl.resolve();

    Set<String> ivs = new HashSet<String>();  // var refs in impl
    for (int i = 0; i < this.impl.params.length; i++) {
      if (this.impl.params[i] instanceof PTypeVarRef) {
        ivs.add(((PTypeVarRef)this.impl.params[i]).def.name);
        ;
      } else if (this.impl.params[i] instanceof PTypeVarDef) {
        ;
      } else {
        emsg = new StringBuffer();
        emsg.append("Invalid feature implementation type paramter at ");
        emsg.append(this.impl.params[i].getSrcInfo());
        emsg.append(". parameter ");
        emsg.append(i + 1);
        throw new CompileException(emsg.toString());
      }
    }
    if (ivs.size() != 1 + this.sig.params.length) {
      emsg = new StringBuffer();
      emsg.append("Insufficient variable references in feature implementation type at ");
      emsg.append(this.impl.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return this;
  }

  public Module.Availability getAvailability() { return this.availability; }

  public Module.Access getAcc() { return this.acc; }

  public int getParamCount() { return this.sig.params.length; }

  void checkAcc() throws CompileException {
    if (this.acc == Module.ACC_PRIVATE) { return; }
    this.impl.excludePrivateAcc();
    return;
  }

  public void setupExtensionGraph(PDefDict.ExtGraph g) throws CompileException {}

  public void checkConcreteness() throws CompileException {}

  List<PEvalStmt> generateFuns(PModule mod) throws CompileException {
    List<PEvalStmt> funs = new ArrayList<PEvalStmt>();

/* DEBUG */ System.out.println(this.generateFeatureFun(mod));
    // funs.add(this.generateFeatureFun(mod));
    // funs.add(this.generateFeatureBuiltinFun(mod));
    return funs;
  }

  PEvalStmt generateFeatureFun(PModule mod) throws CompileException {
    // eval <*T[ FEAT ]> *X _feature_FEAT @xxx -> < IMPL > {
    //   X _builtin_feature_get_FEAT
    // }
    Parser.SrcInfo si = this.srcInfo.appendPostfix("_feature");
    PScope modScope = this.scope.theMod.scope;
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, modScope);
    PScope defScope = evalStmtBuilder.getDefScope();
    PScope bodyScope = evalStmtBuilder.getBodyScope();
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, defScope);
    evalStmtBuilder.setOfficial("_feature_" + this.sig.fname.name);
    evalStmtBuilder.setAcc(this.acc);
    PType.Builder paramTypeBuilder = PType.Builder.newInstance(si, defScope);
    PFeature.ListBuilder paramFeaturesBuilder = PFeature.ListBuilder.newInstance(si, defScope);
    paramFeaturesBuilder.addFeature(
      this.sig.deepCopy(si, defScope, PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_KEEP));  // HERE
    paramTypeBuilder.addItem(
      PTypeVarDef.create(si, defScope, "T", false, paramFeaturesBuilder.create(), null));
    evalStmtBuilder.addParam(
      PExprVarDef.create(si, defScope, PExprVarDef.CAT_FUN_PARAM, paramTypeBuilder.create(), "X"));

// HERE

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
    return evalStmtBuilder.create();
  }
}
