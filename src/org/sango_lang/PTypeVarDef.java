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

class PTypeVarDef extends PDefaultTypedObj implements PType {
  String name;  // null if anonymous
  boolean requiresConcrete;
  PFeature.List features;  // maybe null
  PTypeVarSlot _resolved_varSlot;
  PTypeVarSkel _once_skel;

  private PTypeVarDef(Parser.SrcInfo srcInfo, PScope scope) {
    super(srcInfo, scope);
  }

  static PTypeVarDef create(Parser.SrcInfo srcInfo, PScope scope,
      String name, boolean requiresConcrete, PFeature.List features) {
    PTypeVarDef var = new PTypeVarDef(srcInfo, scope);
    var.name = name;
    var.requiresConcrete = requiresConcrete;
    var.features = features;
    return var;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("newvar[");
    buf.append("src=");
    buf.append(this.srcInfo);
    buf.append(",name=");
    buf.append((this.name != null)? this.name: "(ANONYM)");
    if (this.requiresConcrete) {
      buf.append("!");
    }
    if (this._resolved_varSlot != null) {
      buf.append(",slot=");
      buf.append(this._resolved_varSlot);
    }
    if (this.features != null) {
      buf.append(",features=");
      buf.append(this.features);
    }
    buf.append("]");
    return buf.toString();
  }

  public PTypeVarDef unresolvedCopy(Parser.SrcInfo srcInfo, PScope scope, int extOpt, int concreteOpt) {
    PTypeVarDef v = new PTypeVarDef(srcInfo, scope);
    v.name = this.name;
    switch (concreteOpt) {
    case PType.COPY_CONCRETE_OFF:
      v.requiresConcrete = false;;
      break;
    case PType.COPY_CONCRETE_ON:
      v.requiresConcrete = true;;
      break;
    default:  // PType.COPY_CONCRETE_KEEP
      v.requiresConcrete = this.requiresConcrete;
    }
    if (this.features != null) {
      v.features = this.features.unresolvedCopy(srcInfo, scope, extOpt, concreteOpt);
    }
    return v;
  }

  static PTypeVarDef accept(ParserA.TokenReader reader, PScope scope, int spc, boolean acceptsVarDef) throws CompileException, IOException {
    StringBuffer emsg;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    ParserA.Token varSym;
    String name;
    boolean requiresConcrete;
    PFeature.List fs;
    if (acceptsVarDef && (varSym  = ParserA.acceptToken(reader, LToken.AST, spc)) != null) {
      ParserA.Token varId;
      if ((varId = ParserA.acceptNormalWord(reader, ParserA.SPACE_DO_NOT_CARE)) == null) {
        emsg = new StringBuffer();
        emsg.append("Variable/parameter name missing at ");
        emsg.append(reader.getCurrentSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      name = varId.value.token;
      requiresConcrete = ParserA.acceptToken(reader, LToken.EXCLA, ParserA.SPACE_DO_NOT_CARE) != null;
      fs = PFeature.List.accept(reader, scope, acceptsVarDef);
    } else if ((varSym  = ParserA.acceptToken(reader, LToken.AST_AST, spc)) != null) {
      name = null;
      requiresConcrete = false;
      fs = PFeature.List.accept(reader, scope, acceptsVarDef);
      if (fs == null) {
        emsg = new StringBuffer();
        emsg.append("Feature(s) missing at ");
        emsg.append(reader.getCurrentSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    } else {
      return null;
    }
    return create(si, scope, name, requiresConcrete, fs);
  }

  static PTypeVarDef acceptForDefHeader(ParserA.TokenReader reader, PScope scope, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    ParserA.Token varSym = ParserA.acceptToken(reader, LToken.AST, spc);
    if (varSym == null) { return null; }
    ParserA.Token varId;
    if ((varId = ParserA.acceptNormalWord(reader, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("Variable/parameter name missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return create(si, scope, varId.value.token, false, null);
  }

  static PTypeVarDef acceptX(ParserB.Elem elem, PScope scope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("newvar")) { return null; }
    String id = elem.getAttrValueAsId("id");
    if (id == null) {
      emsg = new StringBuffer();
      emsg.append("Variable name missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return create(elem.getSrcInfo(), scope, id, false, null);  // HERE
  }

  public void collectModRefs() throws CompileException {
    if (this.features != null) {
      this.features.collectModRefs();
    }
  }

  public PTypeVarDef resolve() throws CompileException {
// /* DEBUG */ System.out.print("VARDEF "); System.out.print(this); System.out.println(this.scope);
    StringBuffer emsg;
    if (this._resolved_varSlot != null) { return this; }
    if (!this.scope.canDefineTVar(this)) {  // successful if name == null
      emsg = new StringBuffer();
      emsg.append("Cannot define variable at ");
      emsg.append(this.srcInfo);
      emsg.append(". - ");
      emsg.append(this.name);
      throw new CompileException(emsg.toString());
    }
    this.scope.defineTVar(this);
    if (this.features != null) {
      this.features = this.features.resolve();
    }
    return this;
  }

  public void excludePrivateAcc() throws CompileException {
    if (this.features != null) {
      this.features.excludePrivateAcc();
    }
  }

  public void normalizeTypes() throws CompileException {
    if (this.type != null && this._normalized_typeSkel == null) {
      this._normalized_typeSkel = this.type.toSkel().normalize();
    }
  }

  public PTypeVarSkel toSkel() {
    if (this._once_skel == null) {
      PFeatureSkel.List fs = (this.features != null)?  this.features.toSkel(): null;
      this._once_skel = PTypeVarSkel.create(this.scope.theMod.theCompiler, this.srcInfo, this.name, this._resolved_varSlot, this.requiresConcrete, fs);
    }
    return this._once_skel;
  }
}
