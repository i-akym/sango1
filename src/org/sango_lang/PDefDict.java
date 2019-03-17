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

import java.util.List;

public interface PDefDict {
  Cstr[] getForeignMods();

  EidProps resolveEid(String id, int catOpts, int accOpts) throws CompileException;

  TconInfo resolveTcon(String tcon, int subcatOpts, int accOpts) throws CompileException;

  interface DefDictGetter {

    PDefDict getReferredDefDict(Cstr mod) throws CompileException;

  }

  static class EidProps {
    Cstr modName;
    int cat;
    int acc;
    ExprDefGetter defGetter;

    static EidProps create(Cstr modName, int cat, int acc, ExprDefGetter getter) {
      return new EidProps(modName, cat, acc, getter);
    }

    EidProps(Cstr modName, int cat, int acc, ExprDefGetter getter) {
      this.modName = modName;
      this.cat = cat;
      this.acc = acc;
      this.defGetter = getter;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("eidprops[cat=");
      buf.append(this.cat);
      buf.append(",acc=");
      buf.append(this.acc);
      buf.append("]");
      return buf.toString();
    }
  }

  interface ExprDefGetter {

    void setSearchInLang();

    PDataDef getDataDef() throws CompileException;

    FunSelRes selectFunDef(PTypeSkel[] paramTypes, List<PVarSlot> givenVarList) throws CompileException;

    PFunDef getFunDef() throws CompileException;  // get by official name

  }

  static class TconKey {
    Cstr modName;
    String tcon;

    public static TconKey create(Cstr modName, String tcon) {
      return new TconKey(modName, tcon);
    }

    TconKey(Cstr modName, String tcon) {
      /* DEBUG */ if (modName == null) { throw new IllegalArgumentException("Tcon key's mod name is null."); }
      this.modName = modName;
      this.tcon = tcon;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("tconkey[mod=");
      buf.append(this.modName);
      buf.append(",tcon=");
      buf.append(this.tcon);
      buf.append("]");
      return buf.toString();
    }

    String toRepr() {
      StringBuffer buf = new StringBuffer();
      buf.append(this.modName.repr());
      buf.append(".");
      buf.append(this.tcon);
      return buf.toString();
    }

    public int hashCode() {
      return this.modName.hashCode() ^ this.tcon.hashCode();
    }

    public boolean equals(Object o) {
      boolean b;
      if (o == this) {
        b = true;
      } else if (!(o instanceof TconKey)) {
        b = false;
      } else {
        TconKey tk = (TconKey)o;
        b = tk.modName.equals(this.modName) && tk.tcon.equals(this.tcon); 
      }
      return b;
    }
  }

  static class TconProps {
    int subcat;
    int paramCount;  // -1 means variable
    int acc;
    DataDefGetter defGetter;

    public static TconProps create(int subcat, int paramCount, int acc, DataDefGetter getter) {
      return new TconProps(subcat, paramCount, acc, getter);
    }

    TconProps(int subcat, int paramCount, int acc, DataDefGetter getter) {
      this.subcat = subcat;
      this.paramCount = paramCount;
      this.acc = acc;
      this.defGetter = getter;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("tconprops[subcat=");
      buf.append(this.subcat);
      buf.append(",paramcount=");
      buf.append(this.paramCount);
      buf.append(",acc=");
      buf.append(this.acc);
      buf.append("]");
      return buf.toString();
    }
  }

  static class TconInfo {
    TconKey key;
    TconProps props;

    public static TconInfo create(TconKey key, TconProps props) {
      return new TconInfo(key, props);
    }

    TconInfo(TconKey key, TconProps props) {
      this.key = key;
      this.props = props;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("tconinfo[tconkey=");
      buf.append(this.key);
      buf.append(",tconprops=");
      buf.append(this.props);
      buf.append("]");
      return buf.toString();
    }

    public boolean equals(Object o) {
      boolean b;
      if (o == this) {
        b = true;
      } else if (!(o instanceof TconInfo)) {
        b = false;
      } else {
        TconInfo ti = (TconInfo)o;
        b = ti.key.equals(this.key);
      }
      return b;
    }
  }

  interface DataDefGetter {

    PDataDef getDataDef();

    PAliasDef getAliasDef();

  }

  static class FunSelRes {
    PFunDef funDef;
    PTypeSkelBindings bindings;

    static FunSelRes create(PFunDef funDef, PTypeSkelBindings bindings) {
      return new FunSelRes(funDef, bindings);
    }

    FunSelRes(PFunDef funDef, PTypeSkelBindings bindings) {
      this.funDef = funDef;
      this.bindings = bindings;
    }
  }
}
