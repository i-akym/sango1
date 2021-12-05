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
import java.util.ArrayList;
import java.util.List;

class PExtendStmt extends PDefaultProgElem implements PDataDef {
  int availability;
  String baseMod;
  String baseTcon;
  String tcon;
  PTVarDef[] tparams;
  PTypeDesc sig;
  int acc;
  PDataConstrDef[] constrs;
  PDefDict.TconInfo baseTconInfo;

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("extend[src=");
    buf.append(this.srcInfo);
    buf.append(",basemod=");
    buf.append(this.baseMod);
    buf.append(",basetcon=");
    buf.append(this.baseTcon);
    buf.append(",sig=");
    buf.append(this.sig);
    buf.append("],acc=");
    buf.append(this.acc);
    buf.append(",constrs=[");
    for (int i = 0; i < this.constrs.length; i++) {
      buf.append(this.constrs[i]);
      buf.append(",");
    }
    buf.append("]]");
    return buf.toString();
  }

  static class Builder {
    PExtendStmt ext;
    PTypeDesc base;
    String rename;
    List<PDataConstrDef> constrList;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.ext = new PExtendStmt();
      this.constrList = new ArrayList<PDataConstrDef>();
    }

    void setSrcInfo(Parser.SrcInfo si) {
      this.ext.srcInfo = si;
    }

    void setBase(PTypeDesc base) {
      this.base = base;
    }

    void setRename(String rename) {
      this.rename = rename;
    }

    void setAvailability(int availability) {
      this.ext.availability = availability;
    }

    void setAcc(int acc) {
      this.ext.acc = acc;
    }

    void addConstr(PDataConstrDef constr) {
      this.constrList.add(constr);
    }

    void addConstrList(List<PDataConstrDef> constrList) {
      for (int i = 0; i < constrList.size(); i++) {
        this.addConstr(constrList.get(i));
      }
    }

    PExtendStmt create() throws CompileException {
      if (this.base instanceof PTypeId) {
        PTypeId ti = (PTypeId)this.base;
        this.ext.baseMod = (ti.mod != null)? ti.mod: PModule.MOD_ID_LANG;
        this.ext.baseTcon = ti.name;
        this.ext.tcon = (this.rename != null)? this.rename: this.ext.baseTcon;
        this.ext.tparams = new PTVarDef[0];
        this.ext.sig = PTypeId.create(
          ti.srcInfo,
          null,
          this.ext.tcon,
          false);
      } else if (this.base instanceof PTypeRef) {
        PTypeRef tr = (PTypeRef)this.base;
        this.ext.baseMod = (tr.mod != null)? tr.mod: PModule.MOD_ID_LANG;
        this.ext.baseTcon = tr.tcon;
        this.ext.tcon = (this.rename != null)? this.rename: this.ext.baseTcon;
        this.ext.tparams = new PTVarDef[tr.params.length];
        for (int i = 0; i < tr.params.length; i++) {
          this.ext.tparams[i] = (PTVarDef)tr.params[i];
        }
        this.ext.sig = PTypeRef.create(
          tr.srcInfo,
          PTypeId.create(
            tr.srcInfo,
            null,
            this.ext.tcon,
            false),
          tr.params,
          null);  // HERE: bound tvar
      } else {
        throw new RuntimeException("Unexpected type.");
      }
      // /* DEBUG */ System.out.print("extend sig init "); System.out.println(this.ext.sig);
      this.ext.constrs = this.constrList.toArray(new PDataConstrDef[this.constrList.size()]);
      return this.ext;
    }
  }

  static PExtendStmt accept(ParserA.TokenReader reader) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptSpecifiedWord(reader, "extend", ParserA.SPACE_DO_NOT_CARE)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(t.getSrcInfo());
    builder.setAvailability(PModule.acceptAvailability(reader));
    PTypeDesc base;
    if ((base = PType.acceptSig(reader, PExprId.ID_MAYBE_QUAL, PType.ALLOW_REQUIRE_CONCRETE)) == null) {
      emsg = new StringBuffer();
      emsg.append("Type description missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setBase(base);
    if (ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE) != null) {
      PTypeId rename;
      if ((rename = PTypeId.accept(reader, PExprId.ID_NO_QUAL, ParserA.SPACE_DO_NOT_CARE)) == null) {
        emsg = new StringBuffer();
        emsg.append("New name missing at ");
        emsg.append(reader.getCurrentSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      builder.setRename(rename.name);
    }
    builder.setAcc(PModule.acceptAcc(reader, PModule.ACC_OPTS_FOR_EXTEND, PModule.ACC_DEFAULT_FOR_EXTEND));
    if (ParserA.acceptToken(reader, LToken.COL_EQ, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\":=\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PDataStmt.DataDefBody body;
    if ((body = PDataStmt.acceptDataDefBody(reader)) == null) {
      emsg = new StringBuffer();
      emsg.append("Data definition body missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.addConstrList(body.constrList);
    if (ParserA.acceptToken(reader, LToken.SEM_SEM, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\";;\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return builder.create();
  }

  static PExtendStmt acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("extend-def")) { return null; }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(elem.getSrcInfo());

    String btcon = elem.getAttrValueAsId("base-tcon");
    if (btcon == null) {
      emsg = new StringBuffer();
      emsg.append("Base type constructor missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PTypeId btconItem = PTypeId.create(elem.getSrcInfo(), elem.getAttrValueAsId("base-mid"), btcon, false);
    btconItem.setTcon();

    String renamed = elem.getAttrValueAsId("renamed-tcon");
    if (renamed != null) {
      builder.setRename(renamed);
    }

    builder.setAvailability(PModule.acceptXAvailabilityAttr(elem));
    int acc = PModule.acceptXAccAttr(elem, PModule.ACC_OPTS_FOR_EXTEND, PModule.ACC_DEFAULT_FOR_EXTEND);
    builder.setAcc(acc);

    PType.Builder tb = PType.Builder.newInstance();
    ParserB.Elem e = elem.getFirstChild();
    if (e != null && e.getName().equals("params")) {
      ParserB.Elem ee = e.getFirstChild();
      while (ee != null) {
        PTVarDef var = PTVarDef.acceptX(ee);
        if (var == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        tb.addItem(var);
        ee = ee.getNextSibling();
      }
      e = e.getNextSibling();
    }
    tb.addItem(btconItem);
    builder.setBase(tb.create());

    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Data definition body missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PDataStmt.DataDefBody body = PDataStmt.acceptXDataDefBody(e);
    if (body == null) {
      emsg = new StringBuffer();
      emsg.append("Data definition body missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.addConstrList(body.constrList);
    return builder.create();
  }

  public PExtendStmt setupScope(PScope scope) throws CompileException {
    StringBuffer emsg;
    PScope s = scope.start();
    if (s == this.scope) { return this; }
    this.scope = s;
    this.idResolved = false;
    // /* DEBUG */ System.out.print("extend sig setupscope "); System.out.println(this.sig);
    if (this.baseMod != null && scope.resolveModId(this.baseMod) == null) {  // refer base mod id in order to register foreign mod
      emsg = new StringBuffer();
      emsg.append("Module id \"");
      emsg.append(this.baseMod);
      emsg.append("\" not defined at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    this.sig = this.sig.setupScope(s);
    for (int i = 0; i < this.constrs.length; i++) {
      this.constrs[i] = this.constrs[i].setupScope(this.scope);
      this.constrs[i].setDataType(this.sig);
    }
    return this;
  }

  public PExtendStmt resolveId() throws CompileException {
    StringBuffer emsg;
    if (this.idResolved) { return this; }
    // /* DEBUG */ System.out.print("extend sig resolveid "); System.out.println(this.sig);
    this.sig = this.sig.resolveId();
    for (int i = 0; i < this.tparams.length; i++) {
      this.tparams[i] = this.tparams[i].resolveId();
    }
    for (int i = 0; i < this.constrs.length; i++) {
      this.constrs[i] = this.constrs[i].resolveId();
    }
    // /* DEBUG */ System.out.println("resolve " + this.baseMod + "." + this.baseTcon);
    if ((this.baseTconInfo = this.scope.resolveTcon(this.baseMod, this.baseTcon)) == null) {
      emsg = new StringBuffer();
      emsg.append("Base type constructor not found at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString()) ;
    }
    // /* DEBUG */ System.out.println("extend base tcon info " + this.baseTconInfo);
    if (this.baseTconInfo.props.paramCount() >= 0 && this.tparams.length != this.baseTconInfo.props.paramCount()) {
      emsg = new StringBuffer();
      emsg.append("Parameter count of 'extend' definition mismatch at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString()) ;
    }
    if (this.baseTconInfo.props.paramProps != null) {
      for (int i = 0; i < this.tparams.length; i++) {
        if (this.tparams[i].variance != this.baseTconInfo.props.paramProps[i].variance) {
          emsg = new StringBuffer();
          emsg.append("Variance of *");
          emsg.append(this.tparams[i].name);
          emsg.append(" mismatch with that of base definition at ");
          emsg.append(this.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString()) ;
        }
      }
    }
    this.idResolved = true;
    return this;
  }

  public String getFormalTcon() { return this.tcon; }

  public PTVarSlot[] getParamVarSlots() {
    PTVarSlot[] pvs = new PTVarSlot[this.tparams.length];
    for (int i = 0; i < this.tparams.length; i++) {
      pvs[i] = this.tparams[i].varSlot;
    }
    return pvs;
  }

  public PTypeRefSkel getTypeSig() {
    return (PTypeRefSkel)this.sig.getSkel();
  }

  public int getAvailability() { return this.availability; }

  public int getAcc() { return this.acc; }

  public int getConstrCount() { return this.constrs.length; }

  public PDataDef.Constr getConstr(String dcon) {
    PDataDef.Constr c = null;
    for (int i = 0; i < this.constrs.length; i++) {
      if (this.constrs[i].dcon.equals(dcon)) {
        c = this.constrs[i];
        break;
      }
    }
    return c;
  }

  public PDataDef.Constr getConstrAt(int index) { return this.constrs[index]; }

  void checkAcc() throws CompileException {
    if (this.acc == Module.ACC_PRIVATE) { return; }
    for (int i = 0; i < this.tparams.length; i++) {
      this.tparams[i].excludePrivateAcc();
    }
    if (this.acc != Module.ACC_OPAQUE) {
      for (int i = 0; i < this.constrs.length; i++) {
        this.constrs[i].excludePrivateAcc();
      }
    }
  }

  public void normalizeTypes() throws CompileException {
    List<PDefDict.TconInfo> tis = new ArrayList<PDefDict.TconInfo>();
    for (int i = 0; i < this.constrs.length; i++) {
      this.constrs[i].normalizeTypes();
      for (int j = 0; j < constrs[i].attrs.length; j++) {
        constrs[i].attrs[j].nTypeSkel.checkVariance(PTypeSkel.WIDER);
        constrs[i].attrs[j].nTypeSkel.collectTconInfo(tis);
      }
    }
    this.scope.addReferredTcons(tis);
  }

  public void setupExtensionGraph(PDefDict.ExtGraph g) throws CompileException {
    g.addExtension(this.baseTconInfo.key, PDefDict.TconKey.create(this.scope.myModName(), this.tcon));
  }

  public void checkConcreteness() throws CompileException {
    for (int i = 0; i < this.constrs.length; i++) {
      this.constrs[i].checkConcreteness();
    }
  }

  public PDefDict.TconKey getBaseTconKey() { return this.baseTconInfo.key; }
}
