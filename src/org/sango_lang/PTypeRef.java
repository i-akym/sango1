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

class PTypeRef extends PDefaultProgElem implements PTypeDesc {
  Parser.SrcInfo tconSrcInfo;
  String mod;
  Cstr modName;
  String tcon;
  boolean ext;
  PTypeDesc[] params;  // empty array if no params
  PDefDict.TconInfo tconInfo;

  private PTypeRef() {}

  static PTypeRef create(Parser.SrcInfo srcInfo, PTypeId id, PTypeDesc[] param) {
    PTypeRef t = new PTypeRef();
    t.srcInfo = srcInfo;
    t.tconSrcInfo = id.srcInfo;
    t.mod = id.mod;
    t.tcon = id.name;
    t.ext = id.ext;
    t.params = (param != null)? param: new PTypeDesc[0];
    return t;
  }

  static PTypeRef acceptX(ParserB.Elem elem, int acceptables) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("type")) { return null; }
    PType.Builder builder = PType.Builder.newInstance();
    builder.setSrcInfo(elem.getSrcInfo());

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
    PTypeId tconItem = PTypeId.create(elem.getSrcInfo(), mid, tcon, ext);
    tconItem.setTcon();
    ParserB.Elem e = elem.getFirstChild();
    while (e != null) {
      PTypeDesc t = PType.acceptXItem(e, acceptables);
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
    buf.append(PTypeId.repr(this.mod, this.tcon, this.ext));
    buf.append(">");
    if (this.srcInfo != null) {
      buf.append("]");
    }
    return buf.toString();
  }

  public PTypeRef deepCopy(Parser.SrcInfo srcInfo, int extOpt, int varianceOpt, int concreteOpt) {
    PTypeRef t = new PTypeRef();
    t.srcInfo = srcInfo;
    t.mod = this.mod;
    t.modName = this.modName;
    t.tcon = this.tcon;
    switch (extOpt) {
    case PTypeDesc.COPY_EXT_OFF:
      t.ext = false;;
      break;
    case PTypeDesc.COPY_EXT_ON:
      t.ext = true;;
      break;
    default:  // PTypeDesc.COPY_EXT_KEEP
      t.ext = this.ext;
    }
    t.params = new PTypeDesc[this.params.length];
    for (int i = 0; i < this.params.length; i++) {
      t.params[i] = this.params[i].deepCopy(srcInfo, extOpt, varianceOpt, concreteOpt);
    }
    return t;
  }

  static PTypeRef getLangDefinedType(Parser.SrcInfo srcInfo, String tcon, PTypeDesc[] paramTypeDescs) {
    return  create(
      srcInfo,
      PTypeId.create(srcInfo, PModule.MOD_ID_LANG, tcon, false),
      paramTypeDescs);
  }

  public PTypeRef setupScope(PScope scope) throws CompileException {
    StringBuffer emsg;
    if (scope == this.scope) { return this; }
    this.scope = scope;
    this.idResolved = false;
    for (int i = 0; i < this.params.length; i++) {
      this.params[i] = (PTypeDesc)this.params[i].setupScope(scope);
    }
    if (this.mod != null) {
      this.modName = scope.resolveModId(this.mod);
      if (this.modName == null) {
        emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(this.mod);
        emsg.append("\" not defined at ");
        emsg.append(this.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }
    return this;
  }

  public PTypeRef resolveId() throws CompileException {
    StringBuffer emsg;
    if (this.idResolved) { return this; }
    /* DEBUG */ if (this.scope == null) { System.out.print("scope is null "); System.out.println(this); }
    if ((this.tconInfo = this.scope.resolveTcon(this.mod, this.tcon)) == null) {
      emsg = new StringBuffer();
      emsg.append("Type constructor \"");
      emsg.append(PTypeId.repr(this.mod, this.tcon, false));
      emsg.append("\" not defined at ");
      emsg.append(this.tconSrcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (this.tconInfo.props.paramCount() >= 0 && this.params.length != this.tconInfo.props.paramCount()) {
      emsg = new StringBuffer();
      emsg.append("Parameter count of \"");
      emsg.append(PTypeId.repr(this.mod, this.tcon, false));
      emsg.append("\" mismatch at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString()) ;
    }
    for (int i = 0; i < this.params.length; i++) {
      PTypeDesc p = (PTypeDesc)this.params[i].resolveId();
      this.params[i] = p;
    }
    this.idResolved = true;
    return this;
  }

  public PDefDict.TconInfo getTconInfo() {
    if (this.tconInfo == null) {
      throw new IllegalStateException("Tcon info not set up.");
    }
    return this.tconInfo;
  }

  public void excludePrivateAcc() throws CompileException {
    StringBuffer emsg;
    if (this.tconInfo.props.acc == Module.ACC_PRIVATE) {
      emsg = new StringBuffer();
      emsg.append("\"");
      emsg.append(PTypeId.repr(this.mod, this.tcon, false));
      emsg.append("\" should not be private at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    for (int i = 0; i < this.params.length; i++) {  // needed only alias def?
      this.params[i].excludePrivateAcc();
    }
  }

  public void normalizeTypes() {
    throw new RuntimeException("PTypeRef#normalizeTypes should not be called.");
  }

  public PTypeSkel normalize() {
    PTypeSkel t;
    // if (isBottom(this)) {
      // t = PBottomSkel.create(this.srcInfo);
    // } else {
      PAliasDef a;
      PTypeSkel[] ps = new PTypeSkel[this.params.length];
      for (int i = 0; i < ps.length; i++) {
        ps[i] = this.params[i].normalize();
      }
      if ((a = this.tconInfo.props.defGetter.getAliasDef()) != null) {
        t = a.unalias(ps);
      } else {
        t = PTypeRefSkel.create(this.scope.getCompiler(), this.srcInfo, this.tconInfo, this.ext, ps);
      }
    // }
    return t;
  }

  static boolean isExposed(PTypeDesc type) {
    return isLangType(type, Module.TCON_EXPOSED);
  }

  static boolean isBottom(PTypeDesc type) {
    return isLangType(type, Module.TCON_BOTTOM);
  }

  // static boolean willNotReturn(PTypeDesc type) {
    // return isLangType(type, Module.TCON_BOTTOM);
  // }

  static boolean isList(PTypeDesc type) {
    return isLangType(type, Module.TCON_LIST);
  }

  static boolean isFun(PTypeDesc type) {
    return isLangType(type, Module.TCON_FUN);
  }

  static boolean isLangType(PTypeDesc type, String tcon) {
    boolean b;
    if (type instanceof PTypeRef) {
      PTypeRef tr = (PTypeRef)type;
      if (tr.tconInfo == null) { throw new IllegalArgumentException("Tcon not resolved. " + tr.toString()); }
      b = tr.tconInfo.key.modName.equals(Module.MOD_LANG) && tr.tconInfo.key.tcon.equals(tcon);
    } else {
      b = false;
    }
    return b;
  }

  public PTypeSkel getSkel() {
    PTypeSkel t;
    // if (isBottom(this)) {
      // t = PBottomSkel.create(this.srcInfo);
    // } else {
      PTypeSkel[] ps = new PTypeSkel[this.params.length];
      for (int i = 0; i < ps.length; i++) {
        ps[i] = this.params[i].getSkel();
      }
      t =  PTypeRefSkel.create(this.scope.getCompiler(), this.srcInfo, this.tconInfo, this.ext, ps);
    // }
    return t;
  }
}
