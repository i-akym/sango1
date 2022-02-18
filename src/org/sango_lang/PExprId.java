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

class PExprId extends PDefaultEvalAndPtnElem {
  static final int ID_NO_QUAL = 0;
  static final int ID_MAYBE_QUAL = 1;

  static final int CAT_NOT_FOUND = 0;
  static final int CAT_VAR = 1;
  static final int CAT_DCON_EVAL = 2;
  static final int CAT_DCON_PTN = 4;
  static final int CAT_DCON = CAT_DCON_EVAL + CAT_DCON_PTN;
  static final int CAT_FUN_OFFICIAL = 8;
  static final int CAT_FUN_ALIAS = 16;
  static final int CAT_FUN = CAT_FUN_OFFICIAL + CAT_FUN_ALIAS;

  int catOpt;
  String mod;
  String name;
  PDefDict.EidProps props;

  private PExprId() {}

  static PExprId create(Parser.SrcInfo srcInfo, String mod, String name) {
    PExprId id = new PExprId();
    id.srcInfo = srcInfo;
    id.mod = mod;
    id.name = name;
    if (id.mod == null) {
      id.catOpt = CAT_VAR + CAT_FUN + CAT_DCON;
    } else {
      id.catOpt = CAT_FUN + CAT_DCON;
    }
    return id;
  }

  boolean isVar() { return this.isCat(CAT_VAR); }

  boolean isDcon() { return this.isCat(CAT_DCON); }

  // boolean isFun() { return this.isCat(CAT_FUN); }

  boolean isCat(int cat) { return this.catOpt == cat; }

  boolean maybeVar() { return this.maybeCat(CAT_VAR); }

  boolean maybeDcon() { return this.maybeCat(CAT_DCON); }

  boolean maybeFun() { return this.maybeCat(CAT_FUN); }

  boolean maybeCat(int cat) { return (this.catOpt & cat) > 0; }

  void cutOffVar() {
    this.cutOffCat(CAT_VAR);
  }

  void cutOffDcon() {
    this.cutOffCat(CAT_DCON);
  }

  void cutOffFun() {
    this.cutOffCat(CAT_FUN);
  }

  void cutOffCat(int cat) {
    this.catOpt &= ~cat;
  }

  void setVar() {
    this.setCat(CAT_VAR);
  }

  void setDcon() {
    this.setCat(CAT_DCON);
  }

  void setFun() {
    this.setCat(CAT_FUN);
  }

  void setCat(int cat) {
    this.catOpt = cat;
  }

  boolean isSimple() {
    return this.mod == null;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("exprid[");
    if (this.srcInfo != null) {
      buf.append("src=");
      buf.append(this.srcInfo);
    }
    if (this.maybeCat(CAT_VAR)) {
      buf.append(",(VAR)");
    }
    if (this.maybeCat(CAT_DCON_EVAL)) {
      buf.append(",(DCON_EVAL)");
    }
    if (this.maybeCat(CAT_DCON_PTN)) {
      buf.append(",(DCON_PTN)");
    }
    if (this.maybeCat(CAT_FUN_OFFICIAL)) {
      buf.append(",(FUN_OFFICIAL)");
    }
    if (this.maybeCat(CAT_FUN_ALIAS)) {
      buf.append(",(FUN_ALIAS)");
    }
    buf.append(",id=");
    buf.append(this.toRepr());
    buf.append("]");
    return buf.toString();
  }

  String toRepr() {
    return repr(this.mod, this.name);
  }

  static String repr(String mod, String name) {
    return (mod != null)? mod + "." + name: name;
  }

  static PExprId accept(ParserA.TokenReader reader, int qual, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token word;
    if ((word = ParserA.acceptNormalWord(reader, spc)) == null) {
      return null;
    }
    Parser.SrcInfo si = word.getSrcInfo();
    String mod = null;
    String name = null;
    if (qual == ID_NO_QUAL || ParserA.acceptToken(reader, LToken.DOT, ParserA.SPACE_DO_NOT_CARE) == null) {
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
      mod = word.value.token;
      name = word2.value.token;
    }
    return create(si, mod, name);
  }

  public PEvalAndPtnElem setupScope(PScope scope) throws CompileException {
    StringBuffer emsg;
    if (scope == this.scope) { return this; }
    this.scope = scope;
    this.idResolved = false;
    PEvalAndPtnElem ret = this;
    if (this.isSimple()) {
      PEVarDef v;
      if ((v = scope.referSimpleEid(this.name)) != null) {
        if (!this.maybeVar()) {
          emsg = new StringBuffer();
          emsg.append("Variable name \"");
          emsg.append(this.name);
          emsg.append("\" not allowed at ");
          emsg.append(this.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        ret = PEVarRef.create(this.srcInfo, this.name, v.varSlot).setupScope(scope);
      } else if (this.maybeDcon() || this.maybeFun()) {
        this.cutOffVar();
      } else {
        emsg = new StringBuffer();
        emsg.append("Variable \"");
        emsg.append(this.name);
        emsg.append("\" not defined at ");
        emsg.append(this.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    } else if (scope.resolveModId(this.mod) == null) {
      emsg = new StringBuffer();
      emsg.append("Module id \"");
      emsg.append(this.mod);
      emsg.append("\" not defined at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return ret;
  }

  public PExprId resolveId() throws CompileException {
    StringBuffer emsg;
    if (this.idResolved) { return this; }
    if (this.maybeVar()) {
      throw new IllegalStateException("Possibility of being variable stays.");
    }
    this.props = this.scope.resolveEid(this);
    if (this.props == null) {
      emsg = new StringBuffer();
      emsg.append("Id \"");
      emsg.append(this.toRepr());
      emsg.append("\" not found at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if ((this.props.cat & this.catOpt) == 0) {
      emsg = new StringBuffer();
      emsg.append("Misusing \"");
      emsg.append(this.toRepr());
      emsg.append("\" at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    this.catOpt &= this.props.cat;
    this.idResolved = true;
    return this;
  }

  public void normalizeTypes() {
    throw new RuntimeException("PExprId#normalizeType should not called. - " + this.toString());
  }
}
