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

public class PTypeId extends PDefaultProgObj {
  int catOpt;
  String modId;
  String name;
  boolean ext;
  // Cstr resolvedModName;
  // PDefDict.TconProps tconProps;

  private PTypeId(Parser.SrcInfo srcInfo, PScope scope) {
    super(srcInfo, scope);
  }

  static PTypeId create(Parser.SrcInfo srcInfo, PScope scope, String modId, String name, boolean ext) {
    PTypeId id = new PTypeId(srcInfo, scope);
    id.modId = modId;
    if (modId == null) {
      id.catOpt = PDefDict.TID_CAT_VAR + PDefDict.TID_CAT_TCON;
    } else {
      id.catOpt = PDefDict.TID_CAT_TCON;
    }
    id.name = name;
    id.ext = ext;
    if (ext) {
      id.cutOffVar();
    }
    return id;
  }

  static PTypeId createVar(Parser.SrcInfo srcInfo, PScope scope, String name) {
    PTypeId id = create(srcInfo, scope, null, name, false);
    id.setVar();
    return id;
  }

  static PTypeId createFeature(Parser.SrcInfo srcInfo, PScope scope, String name) {
    PTypeId id = create(srcInfo, scope, null, name, false);
    id.setFeature();
    return id;
  }

  boolean isVar() { return this.isCat(PDefDict.TID_CAT_VAR); }

  boolean isTcon() { return this.isCat(PDefDict.TID_CAT_TCON); }

  boolean isFeature() { return this.isCat(PDefDict.TID_CAT_FEATURE); }

  boolean isCat(int cat) { return this.catOpt == cat; }

  boolean maybeVar() { return this.maybeCat(PDefDict.TID_CAT_VAR); }

  boolean maybeTcon() { return this.maybeCat(PDefDict.TID_CAT_TCON); }

  boolean maybeCat(int cat) { return (this.catOpt & cat) > 0; }

  void cutOffCatOpt(int cat) {
    this.catOpt &= ~cat;
  }

  void setVar() {
    this.setCat(PDefDict.TID_CAT_VAR);
  }

  void setTcon() {
    this.setCat(PDefDict.TID_CAT_TCON);
  }

  void setFeature() {
    this.setCat(PDefDict.TID_CAT_FEATURE);
  }

  void setCat(int cat) {
    this.catOpt = cat;
  }

  void cutOffVar() {
    this.cutOffCat(PDefDict.TID_CAT_VAR);
  }

  void cutOffTcon() {
    this.cutOffCat(PDefDict.TID_CAT_TCON);
  }

  void cutOffCat(int cat) {
    this.catOpt &= ~cat;
  }

  boolean isSimple() {
    return this.modId == null;
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
    if (this.isFeature()) {
      buf.append(",FX");
    }
    buf.append(",id=");
    buf.append(this.repr());
    buf.append("]");
    return buf.toString();
  }

  String repr() {
    return repr(this.modId, this.name, this.ext);
  }

  static String repr(String modId, String name, boolean ext) {
    StringBuffer buf = new StringBuffer();
    if (modId != null) {
      buf.append(modId);
      buf.append(".");
    }
    buf.append(name);
    if (ext) {
      buf.append("+");
    }
    return buf.toString();
  }

  public PTypeId copy(Parser.SrcInfo srcInfo, PScope scope, int extOpt, int concreteOpt) {
    PTypeId id = new PTypeId(srcInfo, scope);
    id.catOpt = this.catOpt;
    id.modId = this.modId;
    id.name = this.name;
    switch (extOpt) {
    case PType.COPY_EXT_OFF:
      id.ext = false;;
      break;
    case PType.COPY_EXT_ON:
      id.ext = true;;
      break;
    default:  // PType.COPY_EXT_KEEP
      id.ext = this.ext;
    }
    return id;
  }

  static PTypeId accept(ParserA.TokenReader reader, PScope scope, Option.Set<Parser.QualState> qual, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token word;
    if ((word = ParserA.acceptNormalWord(reader, spc)) == null) {
      return null;
    }
    Parser.SrcInfo si = word.getSrcInfo();
    String modId = null;
    String name = null;
    if (!qual.contains(Parser.WITH_QUAL) || ParserA.acceptToken(reader, LToken.DOT, ParserA.SPACE_DO_NOT_CARE) == null) {
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
    boolean ext = ParserA.acceptToken(reader, LToken.PLUS, ParserA.SPACE_DO_NOT_CARE) != null;
    return create(si, scope, modId, name, ext);
  }

  public void collectModRefs() throws CompileException {
    this.scope.referredModId(this.srcInfo, this.modId);
  }

  // void resolveModIdSimply() throws CompileException {
    // StringBuffer emsg;
    // if (this.modId != null) {
      // this.resolvedModName = this.scope.resolveModId(this.modId);
      // if (this.resolvedModName == null) {
        // emsg = new StringBuffer();
        // emsg.append("Module id \"");
        // emsg.append(this.modId);
        // emsg.append("\" not defined at ");
        // emsg.append(this.srcInfo);
        // emsg.append(".");
        // throw new CompileException(emsg.toString());
      // }
    // }
  // }

  public PType resolve() throws CompileException {
    throw new RuntimeException("PTypeId#resolve is called.");
  }

  // public PDefDict.TconProps getTconProps() {
    // throw new IllegalStateException("PTypeId#getTconProps should not called.");
  // }

  public void excludePrivateAcc() throws CompileException {
    throw new RuntimeException("PTypeId#excludePrivateAcc should not called.");
  }

  public PTypeSkel getSkel() {
    throw new RuntimeException("PTypeId#getSkel is called. " + this.toString());
  }
}
