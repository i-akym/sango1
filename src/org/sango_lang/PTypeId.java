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

public class PTypeId extends PDefaultProgElem implements PTypeDesc {
  static final int CAT_VAR = 1;
  static final int CAT_TCON = 2;

  public static final int SUBCAT_NOT_FOUND = 0;
  public static final int SUBCAT_DATA = 1;
  public static final int SUBCAT_EXTEND = 2;
  public static final int SUBCAT_ALIAS = 4;

  int catOpt;
  String mod;
  String name;
  boolean ext;
  PDefDict.TconInfo tconInfo;

  private PTypeId() {}

  static PTypeId create(Parser.SrcInfo srcInfo, String mod, String name, boolean ext) {
    PTypeId id = new PTypeId();
    id.srcInfo = srcInfo;
    id.mod = mod;
    if (mod == null) {
      id.catOpt = CAT_VAR + CAT_TCON;
    } else {
      id.catOpt = CAT_TCON;
    }
    id.name = name;
    id.ext = ext;
    if (ext) {
      id.cutOffVar();
    }
    return id;
  }

  static PTypeId createVar(Parser.SrcInfo srcInfo, String name) {
    PTypeId id = create(srcInfo, null, name, false);
    id.setVar();
    return id;
  }

  boolean isVar() { return this.isCat(CAT_VAR); }

  boolean isTcon() { return this.isCat(CAT_TCON); }

  boolean isCat(int cat) { return this.catOpt == cat; }

  boolean maybeVar() { return this.maybeCat(CAT_VAR); }

  boolean maybeTcon() { return this.maybeCat(CAT_TCON); }

  boolean maybeCat(int cat) { return (this.catOpt & cat) > 0; }

  void cutOffCatOpt(int cat) {
    this.catOpt &= ~cat;
  }

  void setVar() {
    this.setCat(CAT_VAR);
  }

  void setTcon() {
    this.setCat(CAT_TCON);
  }

  void setCat(int cat) {
    this.catOpt = cat;
  }

  void cutOffVar() {
    this.cutOffCat(CAT_VAR);
  }

  void cutOffTcon() {
    this.cutOffCat(CAT_TCON);
  }

  void cutOffCat(int cat) {
    this.catOpt &= ~cat;
  }

  boolean isSimple() {
    return this.mod == null;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("typeid[");
    if (this.srcInfo != null) {
      buf.append("src=");
      buf.append(this.srcInfo);
    }
    if (this.maybeVar()) {
      buf.append(",(VAR)");
    }
    if (this.maybeTcon()) {
      buf.append(",(TCON)");
    }
    buf.append(",id=");
    buf.append(this.toRepr());
    buf.append("]");
    return buf.toString();
  }

  String toRepr() {
    return repr(this.mod, this.name, this.ext);
  }

  static String repr(String mod, String name, boolean ext) {
    StringBuffer buf = new StringBuffer();
    if (mod != null) {
      buf.append(mod);
      buf.append(".");
    }
    buf.append(name);
    if (ext) {
      buf.append("+");
    }
    return buf.toString();
  }

  public PTypeId deepCopy(Parser.SrcInfo srcInfo) {
    PTypeId id = new PTypeId();
    id.srcInfo = srcInfo;
    id.catOpt = this.catOpt;
    id.mod = this.mod;
    id.name = this.name;
    id.ext = this.ext;
    return id;
  }

  static PTypeId accept(ParserA.TokenReader reader, int qual, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token word;
    if ((word = ParserA.acceptNormalWord(reader, spc)) == null) {
      return null;
    }
    Parser.SrcInfo si = word.getSrcInfo();
    String mod = null;
    String name = null;
    if (qual == PExprId.ID_NO_QUAL || ParserA.acceptToken(reader, LToken.DOT, ParserA.SPACE_DO_NOT_CARE) == null) {
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
    boolean ext = ParserA.acceptToken(reader, LToken.PLUS, ParserA.SPACE_DO_NOT_CARE) != null;
    return create(si, mod, name, ext);
  }

  public PTypeDesc setupScope(PScope scope) throws CompileException {
    StringBuffer emsg;
    if (scope == this.scope) { return this; }
    this.scope = scope;
    this.idResolved = false;
    if (this.mod != null && scope.resolveModId(this.mod) == null) {
      emsg = new StringBuffer();
      emsg.append("Module id \"");
      emsg.append(this.mod);
      emsg.append("\" not defined at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PTypeDesc ret = null;
    if (this.isSimple()) {
      PTVarSlot varSlot;
      if ((varSlot = scope.referSimpleTid(this.name)) != null) {
        if (!this.maybeCat(PTypeId.CAT_VAR)) {
          emsg = new StringBuffer();
          emsg.append("Variable not allowed at ");
          emsg.append(this.srcInfo);
          emsg.append(". - ");
          emsg.append(this.name);
          throw new CompileException(emsg.toString());
        }
        // if ((varSlot.varDef.cat & PVarDef.CAT_TYPE_PARAM) == 0) {
          // emsg = new StringBuffer();
          // emsg.append("Cannot refer either fun param or local var as type para at ");
          // emsg.append(this.srcInfo);
          // emsg.append(". - ");
          // emsg.append(this.name);
          // throw new CompileException(emsg.toString());
        // }
        ret = PTVarRef.create(this.srcInfo, this.name, varSlot).setupScope(scope);
      } else {
        ret = PTypeRef.create(this.srcInfo, this, new PTypeDesc[0]).setupScope(scope);
      }
    } else {
      ret = PTypeRef.create(this.srcInfo, this, new PTypeDesc[0]).setupScope(scope);
    }
    return ret;
  }

  public PTypeId resolveId() throws CompileException {
    StringBuffer emsg;
    // already determined to be tcon
    if (this.idResolved) { return this; }
    this.tconInfo = this.scope.resolveTcon(this.mod, this.name);
    if (this.tconInfo == null) {
      emsg = new StringBuffer();
      emsg.append("Id \"");
      emsg.append(this.toRepr());
      emsg.append("\" not found at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    this.idResolved = true;
    return this;
  }

  public PDefDict.TconInfo getTconInfo() {
    throw new IllegalStateException("PTypeId#getTconInfo should not called.");
  }

  public void excludePrivateAcc() throws CompileException {
    throw new RuntimeException("PTypeId#excludePrivateAcc should not called.");
  }

  public void normalizeTypes() {
    throw new RuntimeException("PTypeId#normalizeType is called.");
  }

  public PTypeSkel normalize() {
    throw new IllegalStateException("PTypeId#normalize is called.");
  }

  public PTypeSkel getSkel() {
    throw new RuntimeException("PTypeId#getSkel is called.");
  }
}
