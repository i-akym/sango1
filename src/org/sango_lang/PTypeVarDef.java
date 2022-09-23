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
  String name;
  int variance;
  boolean requiresConcrete;
  PFeature.List features;
  PType constraint;  // maybe null, guaranteed to be PTypeRef later
  PTypeSkel nConstraint;
  PTypeVarSlot varSlot;  // setup later

  private PTypeVarDef(Parser.SrcInfo srcInfo, PScope scope) {
    super(srcInfo, scope);
  }

  static PTypeVarDef create(Parser.SrcInfo srcInfo, PScope scope, String name, int variance, boolean requiresConcrete, PTypeRef constraint) {
    PTypeVarDef var = new PTypeVarDef(srcInfo, scope);
    var.name = name;
    var.variance = variance;
    var.requiresConcrete = requiresConcrete;
    var.constraint = constraint;
    return var;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("newvar[");
    buf.append("src=");
    buf.append(this.srcInfo);
    buf.append(",name=");
    buf.append(this.name);
    buf.append(",variance=");
    buf.append(this.variance);
    if (this.requiresConcrete) {
      buf.append("!");
    }
    if (this.features != null) {
      buf.append(",features=");
      buf.append(this.features);
    }
    if (this.constraint != null) {
      buf.append(",constraint=");
      buf.append(this.constraint);
    }
    if (this.varSlot != null) {
      buf.append(",slot=");
      buf.append(this.varSlot);
    }
    buf.append("]");
    return buf.toString();
  }

  public PTypeVarDef deepCopy(Parser.SrcInfo srcInfo, PScope scope, int extOpt, int varianceOpt, int concreteOpt) {
    PTypeVarDef v = new PTypeVarDef(srcInfo, scope);
    v.name = this.name;
    // v.varSlot = this.varSlot;  // not copied
    switch (varianceOpt) {
    case PType.COPY_VARIANCE_INVARIANT:
      v.variance = Module.INVARIANT;
      break;
    case PType.COPY_VARIANCE_COVARIANT:
      v.variance = Module.COVARIANT;
      break;
    case PType.COPY_VARIANCE_CONTRAVARIANT:
      v.variance = Module.CONTRAVARIANT;
      break;
    default:  // PType.COPY_VARIANCE_KEEP
      v.variance = this.variance;
    }
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
      throw new RuntimeException("Feature not implemented in PTypeVarDef.");
    }
    if (this.constraint != null) {
      try {
        PType.Builder b = PType.Builder.newInstance(srcInfo, scope);
        b.addItem(this.constraint.deepCopy(srcInfo, scope, extOpt, varianceOpt, concreteOpt));
        v.constraint = b.create();
      } catch (Exception ex) {
        throw new RuntimeException("Internal error. " + ex.toString());
      }
    }
    return v;
  }

  static PTypeVarDef acceptSimple(ParserA.TokenReader reader, PScope scope) throws CompileException, IOException {
    StringBuffer emsg;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    ParserA.Token varSym = ParserA.acceptToken(reader, LToken.AST, ParserA.SPACE_DO_NOT_CARE);
    if (varSym == null) { return null; }
    int variance;
    if (ParserA.acceptToken(reader, LToken.PLUS, ParserA.SPACE_DO_NOT_CARE) != null) {
      variance = Module.COVARIANT;
    } else if (ParserA.acceptToken(reader, LToken.MINUS, ParserA.SPACE_DO_NOT_CARE) != null) {
      variance = Module.CONTRAVARIANT;
    } else {
      variance = Module.INVARIANT;
    }
    ParserA.Token varId;
    if ((varId = ParserA.acceptNormalWord(reader, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("Variable/parameter name missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    boolean requiresConcrete = ParserA.acceptToken(reader, LToken.EXCLA, ParserA.SPACE_DO_NOT_CARE) != null;
    return create(si, scope, varId.value.token, variance, requiresConcrete, null);
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
    return create(elem.getSrcInfo(), scope, id, Module.INVARIANT, false, null);  // HERE
  }

  public void collectModRefs() throws CompileException {
    if (this.features != null) {
      throw new RuntimeException("Feature not implemented in PTypeVarDef.");
    }
    if (this.constraint != null) {
      this.constraint.collectModRefs();
    }
  }

  public PTypeVarDef resolve() throws CompileException {
    StringBuffer emsg;
    if (this.varSlot != null) { return this; }
    if (!this.scope.canDefineTVar(this)) {
      emsg = new StringBuffer();
      emsg.append("Cannot define variable at ");
      emsg.append(this.srcInfo);
      emsg.append(". - ");
      emsg.append(this.name);
      throw new CompileException(emsg.toString());
    }
    this.varSlot = this.scope.defineTVar(this);
    if (this.constraint != null) {
      this.constraint = this.constraint.resolve();
    }
    return this;
  }

  public PDefDict.TconInfo getTconInfo() { return null; }

  public void excludePrivateAcc() throws CompileException {}

  public void normalizeTypes() {
    if (this.features != null) {
      throw new RuntimeException("Feature not implemented in PTypeVarDef.");
    }
    if (this.constraint != null) {
      this.nConstraint = this.constraint.normalize();
    }
    this.nTypeSkel = PTypeVarSkel.create(this.srcInfo, this.name, this.varSlot, this.nConstraint);
  }

  public PTypeVarSkel normalize() {
    return (PTypeVarSkel)this.getSkel();
  }

  public PTypeVarSkel getSkel() {
    if (this.nTypeSkel == null) {
      this.normalizeTypes();
    }
    return (PTypeVarSkel)this.nTypeSkel;
  }
}
