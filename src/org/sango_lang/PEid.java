/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2024 AKIYAMA Isao                                         *
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

class PEid extends PDefaultExprObj {
  int catOpt;
  String modId;
  String name;
  // PDefDict.EidProps props;

  private PEid(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  static PEid create(Parser.SrcInfo srcInfo, PScope outerScope, String modId, String name) {
    PEid id = new PEid(srcInfo, outerScope);
    id.modId = modId;
    id.name = name;
    if (id.modId == null) {
      id.catOpt = PDefDict.EID_CAT_VAR + PDefDict.EID_CAT_FUN + PDefDict.EID_CAT_DCON;
    } else {
      id.catOpt = PDefDict.EID_CAT_FUN + PDefDict.EID_CAT_DCON;
    }
    return id;
  }

  boolean isVar() { return this.isCat(PDefDict.EID_CAT_VAR); }

  boolean isDconEval() { return this.isCat(PDefDict.EID_CAT_DCON_EVAL); }

  boolean isDconPtn() { return this.isCat(PDefDict.EID_CAT_DCON_PTN); }

  boolean isCat(int cat) { return this.catOpt == cat; }

  boolean maybeVar() { return this.maybeCat(PDefDict.EID_CAT_VAR); }

  boolean maybeDcon() { return this.maybeCat(PDefDict.EID_CAT_DCON); }

  boolean maybeDconEval() { return this.maybeCat(PDefDict.EID_CAT_DCON_EVAL); }

  boolean maybeDconPtn() { return this.maybeCat(PDefDict.EID_CAT_DCON_PTN); }

  boolean maybeFun() { return this.maybeCat(PDefDict.EID_CAT_FUN); }

  boolean maybeCat(int cat) { return (this.catOpt & cat) > 0; }

  void cutOffVar() {
    this.cutOffCat(PDefDict.EID_CAT_VAR);
  }

  void cutOffDcon() {
    this.cutOffCat(PDefDict.EID_CAT_DCON);
  }

  void cutOffDconEval() {
    this.cutOffCat(PDefDict.EID_CAT_DCON_EVAL);
  }

  void cutOffDconPtn() {
    this.cutOffCat(PDefDict.EID_CAT_DCON_PTN);
  }

  void cutOffFun() {
    this.cutOffCat(PDefDict.EID_CAT_FUN);
  }

  void cutOffCat(int cat) {
    this.catOpt &= ~cat;
  }

  void setVar() {
    this.setCat(PDefDict.EID_CAT_VAR);
  }

  void setDconEval() {
    this.setCat(PDefDict.EID_CAT_DCON_EVAL);
  }

  void setDconPtn() {
    this.setCat(PDefDict.EID_CAT_DCON_PTN);
  }

  void setFun() {
    this.setCat(PDefDict.EID_CAT_FUN);
  }

  void setCat(int cat) {
    this.catOpt = cat;
  }

  // boolean isSimple() {
    // return this.modId == null;
  // }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("exprid[");
    if (this.srcInfo != null) {
      buf.append("src=");
      buf.append(this.srcInfo);
    }
    if (this.maybeCat(PDefDict.EID_CAT_VAR)) {
      buf.append(",(VAR)");
    }
    if (this.maybeCat(PDefDict.EID_CAT_DCON_EVAL)) {
      buf.append(",(DCON_EVAL)");
    }
    if (this.maybeCat(PDefDict.EID_CAT_DCON_PTN)) {
      buf.append(",(DCON_PTN)");
    }
    if (this.maybeCat(PDefDict.EID_CAT_FUN_OFFICIAL)) {
      buf.append(",(FUN_OFFICIAL)");
    }
    if (this.maybeCat(PDefDict.EID_CAT_FUN_ALIAS)) {
      buf.append(",(FUN_ALIAS)");
    }
    buf.append(",id=");
    buf.append(this.repr());
    buf.append("]");
    return buf.toString();
  }

  String repr() {
    return repr(this.modId, this.name);
  }

  static String repr(String modId, String name) {
    return (modId != null)? modId + "." + name: name;
  }

  static PEid accept(ParserA.TokenReader reader, PScope outerScope,
      Option.Set<Parser.QualState> qual, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token word;
    if ((word = ParserA.acceptNormalWord(reader, spc)) == null) {
      return null;
    }
    Parser.SrcInfo si = word.getSrcInfo();
    String modId = null;
    String name = null;
    if (!qual.contains(Parser.WITH_QUAL)
        || ParserA.acceptToken(reader, LToken.DOT, ParserA.SPACE_DO_NOT_CARE) == null) {
      name = word.value.token;
    } else {
      ParserA.Token word2;
      if ((word2 = ParserA.acceptNormalWord(reader, ParserA.SPACE_DO_NOT_CARE)) == null) {
        emsg = new StringBuffer();
        emsg.append("Id after \".\" missing at ");
        emsg.append(reader.getCurrentSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      modId = word.value.token;
      name = word2.value.token;
    }
    return create(si, outerScope, modId, name);
  }

  public void collectModRefs() throws CompileException {
    this.scope.referredModId(this.srcInfo, this.modId);
  }

  public PExprObj resolve() throws CompileException {
    throw new RuntimeException("PEid#resolve is called. " + this.toString());
    // StringBuffer emsg;
    // PExprObj e;
    // PDefDict.EidProps ep = this.scope.resolveEid(this);
    // if (ep.cat == PDefDict.EID_CAT_VAR) {
      // if (!this.maybeVar()) {
        // emsg = new StringBuffer();
        // emsg.append("Variable name \"");
        // emsg.append(this.name);
        // emsg.append("\" not allowed at ");
        // emsg.append(this.srcInfo);
        // emsg.append(".");
        // throw new CompileException(emsg.toString());
      // }
      // this.setVar();
      // e = this.scope.lookupEVar(this.name);
      // e = e.resolve();
    // } else if (ep.cat == PDefDict.EID_CAT_DCON_EVAL) {
      // if (!this.maybeDconEval()) {
        // emsg = new StringBuffer();
        // emsg.append("Invalid data constructor \"");
        // emsg.append(this.repr());
        // emsg.append("\" at ");
        // emsg.append(this.srcInfo);
        // emsg.append(".");
        // throw new CompileException(emsg.toString());
      // }
      // this.setDconEval();
    // }

    // PExprObj ret = this;
    // if (this.modId == null) {
      // PExprVarDef v;
      // if ((v = this.scope.referSimpleEid(this.name)) != null) {
        // if (!this.maybeVar()) {
          // emsg = new StringBuffer();
          // emsg.append("Variable name \"");
          // emsg.append(this.name);
          // emsg.append("\" not allowed at ");
          // emsg.append(this.srcInfo);
          // emsg.append(".");
          // throw new CompileException(emsg.toString());
        // }
        // ret = PExprVarRef.create(this.srcInfo, this.scope, this.name, v.varSlot);
        // ret = ret.resolve();
      // } else if (this.maybeDcon() || this.maybeFun()) {
        // this.cutOffVar();
      // } else {
        // emsg = new StringBuffer();
        // emsg.append("Variable \"");
        // emsg.append(this.name);
        // emsg.append("\" not defined at ");
        // emsg.append(this.srcInfo);
        // emsg.append(".");
        // throw new CompileException(emsg.toString());
      // }
    // } else if (this.scope.resolveModId(this.modId) == null) {
      // emsg = new StringBuffer();
      // emsg.append("Module id \"");
      // emsg.append(this.modId);
      // emsg.append("\" not defined at ");
      // emsg.append(this.srcInfo);
      // emsg.append(".");
      // throw new CompileException(emsg.toString());
    // }

    // if (ret == this) {
      // if (this.maybeVar()) {
        // throw new IllegalStateException("Possibility of being variable stays.");
      // }
      // PDefDict.EidProps props = this.scope.resolveEid(this);
      // if (props == null) {
        // emsg = new StringBuffer();
        // emsg.append("Id \"");
        // emsg.append(this.repr());
        // emsg.append("\" not found at ");
        // emsg.append(this.srcInfo);
        // emsg.append(".");
        // throw new CompileException(emsg.toString());
      // }
      // if ((props.cat & this.catOpt) == 0) {
        // emsg = new StringBuffer();
        // emsg.append("Misusing \"");
        // emsg.append(this.repr());
        // emsg.append("\" at ");
        // emsg.append(this.srcInfo);
        // emsg.append(".");
        // throw new CompileException(emsg.toString());
      // }
      // this.catOpt &= props.cat;
    // }
    // return ret;
  }

  public void normalizeTypes() throws CompileException {
    throw new RuntimeException("PEid#noralizeTypes is called.");
  }
}
