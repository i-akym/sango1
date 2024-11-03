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

class PTypeVarRef extends PDefaultTypedObj implements PType {
  PTypeVarDef def;

  private PTypeVarRef(Parser.SrcInfo srcInfo, PScope scope) {
    super(srcInfo, scope);
  }

  static PTypeVarRef create(Parser.SrcInfo srcInfo, PScope scope, PTypeVarDef def) {
    PTypeVarRef v = new PTypeVarRef(srcInfo, scope);
    v.def = def;
    return v;
  }

  static PTid acceptXTvar(ParserB.Elem elem, PScope scope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("var")) { return null; }
    String id = elem.getAttrValueAsId("id");
    if (id == null) {
      emsg = new StringBuffer();
      emsg.append("Variable name missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return PTid.createVar(elem.getSrcInfo(), scope, id);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("varref[");
    if (this.srcInfo != null) {
      buf.append("src=");
      buf.append(this.srcInfo);
    }
    buf.append(",name=");
    buf.append(this.def.name);
    buf.append("]");
    return buf.toString();
  }

  public PType.Undet unresolvedCopy(Parser.SrcInfo srcInfo, PScope scope, int extOpt, int concreteOpt) {
    return PType.Undet.create(PTid.create(srcInfo, scope, null, this.def.name, false));
  }

  public void collectModRefs() throws CompileException {}

  public PTypeVarRef resolve() throws CompileException {
    return this;
  }

  public void excludePrivateAcc() throws CompileException {}

  public PTypeSkel toSkel() {
    /* DEBUG */ if (this.scope == null) { throw new RuntimeException("scope is null " + this.toString()); }
    return this.def.toSkel();
  }

  public PTypeVarSkel getNormalizedSkel() {  // called when top level
    return (PTypeVarSkel)this.toSkel();
  }

  public PTypeSkel getFixedType() { return this.def.getFixedType(); }
}
