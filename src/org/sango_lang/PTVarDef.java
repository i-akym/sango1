/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2021 AKIYAMA Isao                                         *
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

class PTVarDef extends PDefaultPtnElem implements PTypeDesc {
  String name;
  int variance;
  boolean requiresConcrete;
  PTypeRef constraint;  // maybe null
  PTVarSlot varSlot;  // setup later

  private PTVarDef() {}

  static PTVarDef create(Parser.SrcInfo srcInfo, String name, int variance, boolean requiresConcrete, PTypeRef constraint) {
    PTVarDef var = new PTVarDef();
    var.srcInfo = srcInfo;
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

  public PTVarDef deepCopy(Parser.SrcInfo srcInfo, int extOpt, int varianceOpt, int concreteOpt) {
    PTVarDef v = new PTVarDef();
    v.srcInfo = srcInfo;
    v.name = this.name;
    // v.varSlot = this.varSlot;  // not copied
    switch (varianceOpt) {
    case PTypeDesc.COPY_VARIANCE_INVARIANT:
      v.variance = Module.INVARIANT;
      break;
    case PTypeDesc.COPY_VARIANCE_COVARIANT:
      v.variance = Module.COVARIANT;
      break;
    case PTypeDesc.COPY_VARIANCE_CONTRAVARIANT:
      v.variance = Module.CONTRAVARIANT;
      break;
    default:  // PTypeDesc.COPY_VARIANCE_KEEP
      v.variance = this.variance;
    }
    switch (concreteOpt) {
    case PTypeDesc.COPY_CONCRETE_OFF:
      v.requiresConcrete = false;;
      break;
    case PTypeDesc.COPY_CONCRETE_ON:
      v.requiresConcrete = true;;
      break;
    default:  // PTypeDesc.COPY_CONCRETE_KEEP
      v.requiresConcrete = this.requiresConcrete;
    }
    return v;
  }

  static PTVarDef acceptSimple(ParserA.TokenReader reader) throws CompileException, IOException {
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
    return create(si, varId.value.token, variance, requiresConcrete, null);
  }

  static PTVarDef acceptX(ParserB.Elem elem) throws CompileException {
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
    return create(elem.getSrcInfo(), id, Module.INVARIANT, false, null);  // HERE
  }

  public PTVarDef setupScope(PScope scope) throws CompileException {
    StringBuffer emsg;
    if (scope == this.scope) { return this; }
    this.scope = scope;
    this.idResolved = false;
    if (this.constraint != null) {
      this.constraint = this.constraint.setupScope(scope);
    }
    if (!scope.canDefineTVar(this)) {
      emsg = new StringBuffer();
      emsg.append("Cannot define variable at ");
      emsg.append(this.srcInfo);
      emsg.append(". - ");
      emsg.append(this.name);
      throw new CompileException(emsg.toString());
    }
    this.varSlot = scope.defineTVar(this);
// /* DEBUG */ System.out.println(this);
    return this;
  }

  public PTVarDef resolveId() throws CompileException {
    if (this.idResolved) { return this; }
    if (this.constraint != null) {
      this.constraint = this.constraint.resolveId();
    }
    this.idResolved = true;
    return this;
  }

  public PDefDict.TconInfo getTconInfo() { return null; }

  public void excludePrivateAcc() throws CompileException {}

  public void checkRequiringConcreteIn() throws CompileException {}

  public void checkRequiringConcreteOut() throws CompileException {
    if (this.requiresConcrete) {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Requiring concrete is not allowed at ");
      emsg.append(this.srcInfo);
      emsg.append(". - ");
      emsg.append(this.name);
      throw new CompileException(emsg.toString());
    }
  }

  public void normalizeTypes() {
    this.nTypeSkel = this.scope.getLangPrimitiveType(this.srcInfo, "type").getSkel();
  }

  public PTypeVarSkel normalize() {
    return (PTypeVarSkel)this.getSkel();
  }

  public PTypeVarSkel getSkel() {
    return PTypeVarSkel.create(this.srcInfo, this.name, this.varSlot,
    (this.constraint != null)? (PTypeRefSkel)this.constraint.getSkel(): null);
  }
}
