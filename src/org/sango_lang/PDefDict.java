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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface PDefDict {
  Module.Availability getModAvailability();

  Cstr[] getForeignMods();

  EidProps resolveEid(String id, int catOpts, Option.Set<Module.Access> accOpts) throws CompileException;

  TconInfo resolveTcon(String tcon, int subcatOpts, Option.Set<Module.Access> accOpts) throws CompileException;

  interface GlobalDefDict {

    boolean isBaseOf(TconKey b, TconKey e);

  }

  interface DefDictGetter {

    PDefDict getReferredDefDict(Cstr mod) throws CompileException;

    GlobalDefDict getGlobalDefDict();

  }

  interface ExprDefGetter {

    void setSearchInLang();

    PDataDef getDataDef() throws CompileException;

    FunSelRes selectFunDef(PTypeSkel[] paramTypes, List<PTypeVarSlot> givenTVarList) throws CompileException;

    PFunDef getFunDef() throws CompileException;  // get by official name

  }

  interface DataDefGetter {

    PDataDef getDataDef();

    PAliasTypeDef getAliasTypeDef();

  }

  static class EidProps {
    Cstr modName;
    int cat;
    Module.Access acc;
    ExprDefGetter defGetter;

    static EidProps create(Cstr modName, int cat, Module.Access acc, ExprDefGetter getter) {
      return new EidProps(modName, cat, acc, getter);
    }

    EidProps(Cstr modName, int cat, Module.Access acc, ExprDefGetter getter) {
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
      buf.append(this.modName.repr());
      buf.append(",tcon=");
      buf.append(this.tcon);
      buf.append("]");
      return buf.toString();
    }

    String repr() {
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
    TparamProps[] paramProps;  // null means variable
    Module.Access acc;
    DataDefGetter defGetter;

    public static TconProps create(int subcat, TparamProps[] paramProps, Module.Access acc, DataDefGetter getter) {
      return new TconProps(subcat, paramProps, acc, getter);
    }

    TconProps(int subcat, TparamProps[] paramProps, Module.Access acc, DataDefGetter getter) {
      this.subcat = subcat;
      this.paramProps = paramProps;
      this.acc = acc;
      this.defGetter = getter;
    }

    int paramCount() {
      return (this.paramProps != null)? this.paramProps.length: -1;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("tconprops[subcat=");
      buf.append(this.subcat);
      buf.append(",paramcount=");
      buf.append(this.paramCount());
      buf.append(",acc=");
      buf.append(this.acc);
      buf.append("]");
      return buf.toString();
    }
  }

  static class FeatureProps {
    int paramCount;
    Module.Access acc;
    DataDefGetter defGetter;

    public static FeatureProps create(int paramCount, Module.Access acc, DataDefGetter getter) {
      return new FeatureProps(paramCount, acc, getter);
    }

    FeatureProps(int paramCount, Module.Access acc, DataDefGetter getter) {
      this.paramCount = paramCount;
      this.acc = acc;
      this.defGetter = getter;
    }

    int paramCount() {
      return this.paramCount;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("featueprops[paramcount=");
      buf.append(this.paramCount());
      buf.append(",acc=");
      buf.append(this.acc);
      buf.append("]");
      return buf.toString();
    }
  }

  static class TparamProps {
    Module.Variance variance;
    boolean concrete;

    public static TparamProps create(Module.Variance variance, boolean concrete) {
      return new TparamProps(variance, concrete);
    }

    private TparamProps(Module.Variance variance, boolean concrete) {
      this.variance = variance;
      this.concrete = concrete;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("tparamprops[variance=");
      buf.append(this.variance);
      buf.append(",concrete=");
      buf.append(this.concrete);
      buf.append("]");
      return buf.toString();
    }
  }

  static ExtGraph createExtGraph() {
    return new ExtGraph();
  }

  static class ExtGraph {
    Map<PDefDict.TconKey, ExtNode> nodeMap;

    ExtGraph() {
      this.nodeMap = new HashMap<PDefDict.TconKey, ExtNode>();
    }

    void addExtension(PDefDict.TconKey base, PDefDict.TconKey ext) throws CompileException {
      ExtNode en;
      if ((en = this.nodeMap.get(ext)) == null) {
        en = this.createNode(ext);
      } else if (en.includesInDescendant(base)) {
        throw new CompileException("Detected cyclic extension definition. " + base.toString());
      } else {
        ;
      }
      ExtNode bn;
      if ((bn = this.nodeMap.get(base)) == null) {
        bn = this.createNode(base);
        en.base = bn;
        bn.exts.add(en);
      } else if (bn.includesInAncestor(ext)) {
        throw new CompileException("Detected cyclic extension definition. " + ext.toString());
      } else {
        en.base = bn;
        bn.exts.add(en);
      }
    }

    private ExtNode createNode(PDefDict.TconKey tcon) {
      ExtNode n = new ExtNode(tcon);
      this.nodeMap.put(tcon, n);
      return n;
    }

    boolean isBaseOf(PDefDict.TconKey b, PDefDict.TconKey e) {
      ExtNode en = this.nodeMap.get(e);
      return (en != null)? en.includesInAncestor(b): false;
    }

    private class ExtNode {
      PDefDict.TconKey tcon;
      ExtNode base;  // maybe null
      List<ExtNode> exts;

      ExtNode(PDefDict.TconKey tcon) {
        this.tcon = tcon;
        this.base = null;
        this.exts = new ArrayList<ExtNode>();
      }

      boolean includesInDescendant(PDefDict.TconKey t) {
        boolean b;
        if (this.tcon.equals(t)) {
          b = true;
        } else {
          b = false;
          for (int i = 0; !b && i < this.exts.size(); i++) {
            this.exts.get(i).includesInDescendant(t);
          }
        }
        return b;
      }

      boolean includesInAncestor(PDefDict.TconKey t) {
        boolean b = false;
        ExtNode n = this;
        while (!b && n != null) {
          if (n.tcon.equals(t)) {
            b = true;
          }
          n = n.base;
        }
        return b;
      }
    }
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
