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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class PDataStmt extends PDefaultProgElem implements PDataDef {
  PTypeDesc sig;  // null means variable params
  String tcon;
  int acc;
  PVarDef[] tparams;  // null means variable params
  PDataConstrDef[] constrs;  // null means native impl

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("data[src=");
    buf.append(this.srcInfo);
    if (this.sig != null) {
      buf.append(",sig=");
      buf.append(this.sig);
    } else {
      buf.append(",tcon=");
      buf.append(this.tcon);
    }
    buf.append("],acc=");
    buf.append(this.acc);
    if (this.constrs != null) {
      buf.append(",constrs=");
      buf.append("[");
      for (int i = 0; i < this.constrs.length; i++) {
        buf.append(this.constrs[i]);
        buf.append(",");
      }
      buf.append("]");
    } else {
      buf.append(",NATIVE_IMPL");
    }
    buf.append("]");
    return buf.toString();
  }

  static class Builder {
    PDataStmt dat;
    List<PDataConstrDef> constrList;
    Set<String> nameSet;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.dat = new PDataStmt();
      this.constrList = new ArrayList<PDataConstrDef>();
      this.nameSet = new HashSet<String>();
    }

    void setSrcInfo(Parser.SrcInfo si) {
      this.dat.srcInfo = si;
    }

    void setSig(PTypeDesc sig) {
      this.dat.sig = sig;
    }

    void setAcc(int acc) {
      this.dat.acc = acc;
    }

    // void setNativeImpl() {
      // this.constrList = null;
    // }

    void addConstr(PDataConstrDef constr) throws CompileException {
      StringBuffer emsg;
      constr.parent = this.dat;
      for (int i = 0; i < constr.attrs.length; i++) {
        String name = constr.attrs[i].name;
        if (name != null) {
          if (this.nameSet.contains(name)) {
            emsg = new StringBuffer();
            emsg.append("Attribute name duplicate at ");
            emsg.append(constr.attrs[i].getSrcInfo());
            emsg.append(". - ");
            emsg.append(name);
            throw new CompileException(emsg.toString());
          }
          this.nameSet.add(name);
        }
      }
      this.constrList.add(constr);
    }

    void addConstrList(List<PDataConstrDef> constrList) throws CompileException {
      if (constrList != null) {
        for (int i = 0; i < constrList.size(); i++) {
          this.addConstr(constrList.get(i));
        }
      // } else {
        // this.setNativeImpl();
      }
    }

    PDataStmt create() throws CompileException {
      StringBuffer emsg;
      if (this.dat.sig == null) {
        throw new RuntimeException("No signature.");
      } else if (this.dat.sig instanceof PTypeId) {
        PTypeId ti = (PTypeId)this.dat.sig;
        this.dat.tcon = ti.name;
        this.dat.tparams = new PVarDef[0];
      } else if (this.dat.sig instanceof PTypeRef) {
        PTypeRef tr = (PTypeRef)this.dat.sig;
        this.dat.tcon = tr.tcon;
        this.dat.tparams = new PVarDef[tr.params.length];
        for (int i = 0; i < tr.params.length; i++) {
          this.dat.tparams[i] = (PVarDef)tr.params[i];
        }
      } else {
        throw new RuntimeException("Unexpected type.");
      }
      if (!this.constrList.isEmpty()) {
        this.dat.constrs = this.constrList.toArray(new PDataConstrDef[this.constrList.size()]);
      } else if (dat.acc == Module.ACC_OPAQUE || dat.acc == Module.ACC_PRIVATE) {
        ;
      } else {
        emsg = new StringBuffer();
        emsg.append("Native data must be opaque or private at ");
        emsg.append(this.dat.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      return this.dat;
    }
  }

  static PDataStmt createForVariableParams(Parser.SrcInfo srcInfo, String tcon, int acc) {  // 'tuple', 'fun'
    PDataStmt dat = new PDataStmt();
    dat.srcInfo = srcInfo;
    dat.tcon = tcon;
    dat.acc = acc;
    return dat;
  }

  static PDataStmt accept(ParserA.TokenReader reader) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptSpecifiedWord(reader, "data", ParserA.SPACE_DO_NOT_CARE)) == null) {
      return null;
    }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(t.getSrcInfo());
    PTypeDesc tsig;
    if ((tsig = PType.acceptSig(reader, PExprId.ID_NO_QUAL)) == null) {
      emsg = new StringBuffer();
      emsg.append("Type description missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setSig(tsig);
    int acc = PModule.acceptAcc(reader, PModule.ACC_OPTS_FOR_DATA, PModule.ACC_DEFAULT_FOR_DATA);
    builder.setAcc(acc);
    if (ParserA.acceptToken(reader, LToken.COL_EQ, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\":=\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    DataDefBody body;
    if ((body = acceptDataDefBody(reader)) == null) {
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

  static PDataStmt acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("data-def")) { return null; }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(elem.getSrcInfo());

    String tcon = elem.getAttrValueAsId("tcon");
    if (tcon == null) {
      emsg = new StringBuffer();
      emsg.append("Type constructor missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PTypeId tconItem = PTypeId.create(elem.getSrcInfo(), null, tcon, false);
    tconItem.setTcon();

    int acc = PModule.acceptXAccAttr(elem, PModule.ACC_OPTS_FOR_DATA, PModule.ACC_DEFAULT_FOR_DATA);
    builder.setAcc(acc);

    PType.Builder tb = PType.Builder.newInstance();
    ParserB.Elem e = elem.getFirstChild();
    if (e != null && e.getName().equals("params")) {
      ParserB.Elem ee = e.getFirstChild();
      while (ee != null) {
        PVarDef var = PVarDef.acceptXTvar(ee);
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
    tb.addItem(tconItem);
    builder.setSig(tb.create());

    DataDefBody body = null;
    if (e != null) {
      if ((body = acceptXDataDefBody(e))!= null) {
        builder.addConstrList(body.constrList);
      }
      e = e.getNextSibling();
    }
    return builder.create();
  }

  static DataDefBody acceptDataDefBody(ParserA.TokenReader reader) throws CompileException, IOException {
    DataDefBody b;
    if ((b = acceptDataDefBodyNative(reader)) != null) {
      return b;
    } else if ((b = acceptDataDefBodyConstructions(reader)) != null) {
      return b;
    } else {
      return null;
    }
  }

  static DataDefBody acceptXDataDefBody(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("constrs")) { return null; }
    DataDefBody body = new DataDefBody();
    body.constrList = new ArrayList<PDataConstrDef>();
    ParserB.Elem e = elem.getFirstChild();
    while (e != null) {
      PDataConstrDef constr = PDataConstrDef.acceptX(e);
      if (constr == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(e.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
      }
      body.constrList.add(constr);
      e = e.getNextSibling();
    }
    return body.constrList.isEmpty()? null: body;
  }

  private static DataDefBody acceptDataDefBodyNative(ParserA.TokenReader reader) throws CompileException, IOException {
    if (ParserA.acceptSpecifiedWord(reader, PModule.IMPL_WORD_NATIVE, ParserA.SPACE_DO_NOT_CARE) != null) {
      return new DataDefBody();
    } else {
      return null;
    }
  }

  private static DataDefBody acceptDataDefBodyConstructions(ParserA.TokenReader reader) throws CompileException, IOException {
    DataDefBody body = new DataDefBody();
    body.constrList = new ArrayList<PDataConstrDef>();
    PDataConstrDef constr;
    boolean newConstr = true;
    boolean cont = true;
    while (cont) {
      if (newConstr && (constr = PDataConstrDef.accept(reader)) != null) {
        body.constrList.add(constr);
        newConstr = false;
      } else if (ParserA.acceptToken(reader, LToken.VBAR, ParserA.SPACE_DO_NOT_CARE) != null) {
        newConstr = true;
      } else {
        cont = false;
      }
    }
    return body.constrList.isEmpty()? null: body;
  }

  static class DataDefBody {
    List<PDataConstrDef> constrList;
  }

  public PDataStmt setupScope(PScope scope) throws CompileException {
    StringBuffer emsg;
    PScope s = scope.start();
    if (s == this.scope) { return this; }
    this.scope = s;
    this.idResolved = false;
    if (this.sig != null) {  // tuple, fun
      this.sig = this.sig.setupScope(s);
    }
    if (this.constrs != null) {  // skip if native impl
      for (int i = 0; i < this.constrs.length; i++) {
        this.constrs[i] = this.constrs[i].setupScope(this.scope);
        this.constrs[i].setDataType(this.sig);
      }
    }
    return this;
  }

  public PDataStmt resolveId() throws CompileException {
    if (this.idResolved) { return this; }
    if (this.sig != null) {
      this.sig = this.sig.resolveId();
    }
    // if (this.tparams != null) {
      // for (int i = 0; i < this.tparams.length; i++) {
        // this.tparams[i] = this.tparams[i].resolveId();
      // }
    // }
    if (this.constrs != null) {
      for (int i = 0; i < this.constrs.length; i++) {
        this.constrs[i] = this.constrs[i].resolveId();
      }
    }
    this.idResolved = true;
    return this;
  }

  public String getFormalTcon() { return this.tcon; }

  public PVarSlot[] getParamVarSlots() {
    PVarSlot[] pvs;
    if (this.tparams != null) {
      pvs = new PVarSlot[this.tparams.length];
      for (int i = 0; i < this.tparams.length; i++) {
        pvs[i] = this.tparams[i].varSlot;
      }
    } else {
      pvs = null;
    }
    return pvs;
  }

  public PTypeSkel getTypeSig() {
    return (this.sig != null)? this.sig.getSkel(): null;
  }

  public int getAcc() { return this.acc; }

  public int getConstrCount() {
    return (this.constrs != null)? this.constrs.length: 0;
  }

  public PDataDef.Constr getConstr(String dcon) {
    PDataDef.Constr c = null;
    if (this.constrs != null) {
      for (int i = 0; i < this.constrs.length; i++) {
        if (this.constrs[i].dcon.equals(dcon)) {
          c = this.constrs[i];
          break;
        }
      }
    }
    return c;
  }

  public PDataDef.Constr getConstrAt(int index) {
    return (this.constrs != null)? this.constrs[index]: null;
  }

  public PDefDict.TconKey getBaseTconKey() { return null; }

  void checkAcc() throws CompileException {
    if (this.acc == Module.ACC_PRIVATE) { return; }
    if (this.tparams != null) {
      for (int i = 0; i < this.tparams.length; i++) {
        this.tparams[i].excludePrivateAcc();
      }
    }
    if (this.acc != Module.ACC_OPAQUE && this.constrs != null) {
      for (int i = 0; i < this.constrs.length; i++) {
        this.constrs[i].excludePrivateAcc();
      }
    }
  }

  public void normalizeTypes() {
    if (this.constrs != null) {
      for (int i = 0; i < this.constrs.length; i++) {
        this.constrs[i].normalizeTypes();
      }
    }
  }
}
