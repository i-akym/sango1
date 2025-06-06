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

class PTypeRef extends PDefaultProgObj implements PType {
  PTid tcon;
  PType[] params;  // empty array if no params
  PDefDict.TidProps _resolved_tconProps;

  private PTypeRef(Parser.SrcInfo srcInfo, PScope scope) {
    super(srcInfo, scope);
  }

  static PTypeRef create(Parser.SrcInfo srcInfo, PScope scope, PTid tcon, PType[] param) {
    PTypeRef t = new PTypeRef(srcInfo, scope);
    t.tcon = tcon;
    t.params = (param != null)? param: new PType[0];
    return t;
  }

  static PTypeRef acceptX(ParserB.Elem elem, PScope scope, int acceptables) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("type")) { return null; }
    PType.Builder builder = PType.Builder.newInstance(elem.getSrcInfo(), scope);

    String tcon = elem.getAttrValueAsId("tcon");
    if (tcon == null) {
      emsg = new StringBuffer();
      emsg.append("Type constructor missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    String mid = elem.getAttrValueAsId("mid");
    boolean ext = elem.getAttrValueAsYesNoSwitch("ext", false);
    PTid tconItem = PTid.create(elem.getSrcInfo(), scope, mid, tcon, ext);
    tconItem.setTcon();
    ParserB.Elem e = elem.getFirstChild();
    while (e != null) {
      PProgObj t = PType.acceptXItem(e, scope, acceptables);
      if (t == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(e.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
      }
      builder.addItem(t);
      e = e.getNextSibling();
    }
    builder.addItem(tconItem);
    return (PTypeRef)builder.create();
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    if (this.srcInfo != null) {
      buf.append("typeref[src=");
      buf.append(this.srcInfo);
      buf.append(",");
    }
    String sep = "";
    buf.append("<");
    for (int i = 0; i < this.params.length; i++) {
      buf.append(sep);
      buf.append(this.params[i]);
      sep = " ";
    }
    buf.append(sep);
    buf.append(this.tcon.repr());
    buf.append(">");
    if (this.srcInfo != null) {
      buf.append("]");
    }
    return buf.toString();
  }

  public PTypeRef unresolvedCopy(Parser.SrcInfo srcInfo, PScope scope, int extOpt, int concreteOpt) {
    PTypeRef t = new PTypeRef(srcInfo, scope);
    t.tcon = this.tcon.copy(srcInfo, scope, extOpt, concreteOpt);
    t.params = new PType[this.params.length];
    for (int i = 0; i < this.params.length; i++) {
      try {
        PType.Builder b = PType.Builder.newInstance(srcInfo, scope);
        b.addItem(this.params[i].unresolvedCopy(srcInfo, scope, extOpt, concreteOpt));
        t.params[i] = b.create();
      } catch (Exception ex) {
        throw new RuntimeException("Internal error. " + ex.toString());
      }
    }
    return t;
  }

  static PTypeRef getLangDefinedType(Parser.SrcInfo srcInfo, PScope scope, String tcon, PType[] paramTypeDescs) {
    return  create(srcInfo, scope,
      PTid.create(srcInfo, scope, PModule.MOD_ID_LANG, tcon, false),
      paramTypeDescs);
  }

  public void collectModRefs() throws CompileException {
    this.scope.referredModId(this.srcInfo, this.tcon.modId);
    for (int i = 0; i < this.params.length; i++) {
      this.params[i].collectModRefs();
    }
  }

  public PTypeRef resolve() throws CompileException {
    StringBuffer emsg;
    /* DEBUG */ if (this.scope == null) { System.out.print("scope is null "); System.out.println(this); }

    this._resolved_tconProps = this.scope.resolveTcon(this.tcon);
    if (this._resolved_tconProps == null) {
      emsg = new StringBuffer();
      emsg.append("Type constructor \"");
      emsg.append(this.tcon.repr());
      emsg.append("\" not defined at ");
      emsg.append(this.tcon.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    int paramCount = -2;
    if ((this._resolved_tconProps.cat & PDefDict.TID_CAT_TCON_DATAEXT) > 0) {
      PDataDef dataDef = this.scope.getCompiler().defDict.getDataDef(this.scope.theMod.name, this._resolved_tconProps.key);
      if (dataDef == null) { throw new RuntimeException("Unexpected. " + this.tcon); }  // checked before
      paramCount = dataDef.getParamCount();
      // PDefDict.TparamProps[] pps = dataDef.getParamPropss();
      // paramCount = (pps == null)? -1: pps.length;
    } else if ((this._resolved_tconProps.cat & PDefDict.TID_CAT_TCON_ALIAS) > 0) {
      PAliasTypeDef aliasTypeDef = this.scope.getCompiler().defDict.getAliasTypeDef(this.scope.theMod.name, this._resolved_tconProps.key);
      if (aliasTypeDef == null) { throw new RuntimeException("Unexpected. " + this.tcon); }  // checked before
      paramCount = aliasTypeDef.getParamVarSlots().length;
    } else {
      throw new RuntimeException("Unexpected cat. " + this.tcon + " " + this._resolved_tconProps.cat);
    }
    if (paramCount >= 0 && this.params.length != paramCount) {
      emsg = new StringBuffer();
      emsg.append("Parameter count of \"");
      emsg.append(this.tcon.repr());
      emsg.append("\" mismatch at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString()) ;
    }
    for (int i = 0; i < this.params.length; i++) {
      PType p = (PType)this.params[i].resolve();
      this.params[i] = p;
    }
    return this;
  }

  public void excludePrivateAcc() throws CompileException {
    StringBuffer emsg;
    if (this._resolved_tconProps.acc == Module.ACC_PRIVATE) {
      emsg = new StringBuffer();
      emsg.append("\"");
      emsg.append(this.tcon.repr());
      emsg.append("\" should not be private at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    for (int i = 0; i < this.params.length; i++) {  // needed only alias def?
      this.params[i].excludePrivateAcc();
    }
  }

  public PTypeRefSkel toSkel() {
    PTypeSkel t;
    PTypeSkel[] ps = new PTypeSkel[this.params.length];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = this.params[i].toSkel();
    }
    return PTypeRefSkel.create(this.scope.getCompiler(), this.srcInfo, this._resolved_tconProps.key, this.tcon.ext, ps);
  }

  // public PTypeSkel getNormalizedSkel() throws CompileException {
    // PTypeSkel t;
    // PTypeSkel[] ps = new PTypeSkel[this.params.length];
    // for (int i = 0; i < ps.length; i++) {
      // ps[i] = this.params[i].getNormalizedSkel();
    // }

    // if ((this._resolved_tconProps.cat & PDefDict.TID_CAT_TCON_ALIAS) > 0) {
    // // if ((a = this._resolved_tconProps.defGetter.getAliasTypeDef()) != null) {
      // PAliasTypeDef a = this.scope.getCompiler().defDict.getAliasTypeDef(this.scope.theMod.name, this._resolved_tconProps.key);
      // t = a.unalias(ps);
    // } else {
      // t = PTypeRefSkel.create(this.scope.getCompiler(), this.srcInfo, this._resolved_tconProps.key, this.tcon.ext, ps);
    // }
    // return t;
  // }
}
