/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2019 Isao Akiyama                                         *
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
package sni_sango.sni_lang;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sango_lang.Module;
import org.sango_lang.CompileException;
import org.sango_lang.Cstr;
import org.sango_lang.PAliasTypeDef;
import org.sango_lang.PDataDef;
import org.sango_lang.PDefDict;
import org.sango_lang.PTypeId;
import org.sango_lang.PTypeSkel;
import org.sango_lang.PTypeSkelBindings;
import org.sango_lang.PTypeRefSkel;
import org.sango_lang.PTypeVarSkel;
import org.sango_lang.PTypeVarSlot;
import org.sango_lang.RActorHItem;
import org.sango_lang.RArrayItem;
import org.sango_lang.RClosureItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RFrame;
import org.sango_lang.RIntItem;
import org.sango_lang.RListItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RResult;
import org.sango_lang.RStructItem;
import org.sango_lang.RType;
import org.sango_lang.RuntimeEngine;

public class SNImodule {
  static final Cstr myModName = new Cstr("sango.lang.module");

  public static SNImodule getInstance(RuntimeEngine e) {
    return new SNImodule();
  }

  public void sni_load_module_on_demand(RNativeImplHelper helper, RClosureItem self, RObjItem moduleName) {
    try {
      helper.getCore().loadModuleOnDemand(helper.arrayItemToCstr((RArrayItem)moduleName));
    } catch (Exception ex) {
      RObjItem e = sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null);
      helper.setException(e);
    }
  }

  public void sni_module_init_actor_h(RNativeImplHelper helper, RClosureItem self, RObjItem moduleName) {
    try {
      RActorHItem a = helper.getCore().getModuleInitActorH(helper.arrayItemToCstr((RArrayItem)moduleName));
      RObjItem r = sni_sango.SNIlang.getMaybeItem(helper, a);
      helper.setReturnValue(r);
    } catch (IllegalArgumentException ex) {
      helper.setException(sni_sango.SNIlang.createBadArgException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_tao_value(RNativeImplHelper helper, RClosureItem self, RObjItem tao) {
    helper.setReturnValue(((TaoItem)tao).value);
  }

  public void sni_do_byte_tao(RNativeImplHelper helper, RClosureItem self, RObjItem b) {
    helper.setReturnValue(TaoItem.create(helper, createPrimitiveType(helper, Module.TCON_BYTE), b));
  }

  public void sni_do_int_tao(RNativeImplHelper helper, RClosureItem self, RObjItem i) {
    helper.setReturnValue(TaoItem.create(helper, createPrimitiveType(helper, Module.TCON_INT), i));
  }

  public void sni_do_real_tao(RNativeImplHelper helper, RClosureItem self, RObjItem r) {
    helper.setReturnValue(TaoItem.create(helper, createPrimitiveType(helper, Module.TCON_REAL), r));
  }

  public void sni_do_char_tao(RNativeImplHelper helper, RClosureItem self, RObjItem c) {
    helper.setReturnValue(TaoItem.create(helper, createPrimitiveType(helper, Module.TCON_CHAR), c));
  }

  public void sni_do_new_tuple(RNativeImplHelper helper, RClosureItem self, RObjItem elems
) {
    List<TaoItem> os = new ArrayList<TaoItem>();
    RListItem L = (RListItem)elems;
    while (L instanceof RListItem.Cell) {
      RListItem.Cell c = (RListItem.Cell)L;
      os.add((TaoItem)c.head);
      L = c.tail;
    }
    if (os.size() < 2) {
      helper.setException(sni_sango.SNIlang.createBadArgException(
        helper, new Cstr("Insufficient elements."), null));
      return;
    }
    PTypeSkel[] ts = new PTypeSkel[os.size()];
    RObjItem[] vs = new RObjItem[os.size()];
    for (int i = 0; i < os.size(); i++) {
      TaoItem o = os.get(i);
      ts[i] = o.type;
      vs[i] = o.value;
    }
    helper.setReturnValue(TaoItem.create(helper, createTupleType(helper, ts), helper.getTupleItem(vs)));
  }

  public void sni_do_tuple_elems(RNativeImplHelper helper, RClosureItem self, RObjItem tuple) {
    TaoItem tupleTao = (TaoItem)tuple;
    if (!PTypeRefSkel.isLangType(tupleTao.type, Module.TCON_TUPLE)) {
      helper.setException(sni_sango.SNIlang.createBadArgException(
        helper, new Cstr("Not a tuple."), null));
      return;
    }
    RStructItem s = (RStructItem)tupleTao.value;
    PTypeSkel[] pts = ((PTypeRefSkel)tupleTao.type).getParams();
    RListItem L = helper.getListNilItem();
    for (int i = s.getFieldCount() - 1; i >= 0; i--) {
      RListItem.Cell c = helper.createListCellItem();
      c.head = TaoItem.create(helper, pts[i], s.getFieldAt(i));
      c.tail = L;
      L = c;
    }
    helper.setReturnValue(L);
  }

  public void sni_do_new_data(RNativeImplHelper helper, RClosureItem self, RObjItem attrs, RObjItem mod, RObjItem dcon) {
/* DEBUG */ try {
    RListItem L = (RListItem)attrs;
    List<TaoItem> as = new ArrayList<TaoItem>();
    while (L instanceof RListItem.Cell) {
      RListItem.Cell lc = (RListItem.Cell)L;
      as.add((TaoItem)lc.head);
      L = lc.tail;
    }
    Cstr modName = helper.arrayItemToCstr((RArrayItem)mod);
    String dconName = helper.arrayItemToCstr((RArrayItem)dcon).toJavaString();
    PDataDef dd = helper.getCore().getDataDef(modName, dconName);
    if (dd == null) {
      helper.setException(sni_sango.SNIlang.createBadArgException(
        helper, new Cstr("Invalid module name or data constructor. - " + modName.toJavaString() + " " + dconName), null));
      return;
    }
    PDataDef.Constr c = dd.getConstr(dconName);
    if (c.getAttrCount() != as.size()) {
      helper.setException(sni_sango.SNIlang.createBadArgException(
        helper, new Cstr("Attribute count mismatch."), null));
      return;
    }
    PTypeSkelBindings bindings = PTypeSkelBindings.create();
    RObjItem[] vs = new RObjItem[as.size()];
    for (int i = 0; i < c.getAttrCount(); i++) {
      TaoItem tv = as.get(i);
      // if (PTypeRefSkel.willNotReturn(tv.type)) { ... }  // HERE
      PDataDef.Attr a = c.getAttrAt(i);
      PTypeSkel at = a.getFixedType();
      if (!at.accept(PTypeSkel.NARROWER, true, tv.type, bindings)) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Type mismatch at ");
        emsg.append(Integer.toString(i));
        emsg.append(".");
        emsg.append("\n  attribute def: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(at));
        emsg.append("\n  attribute def in context: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(at.resolveBindings(bindings)));
        emsg.append("\n  actual attribute: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(tv.type));
        helper.setException(sni_sango.SNIlang.createBadArgException(
          helper, new Cstr(emsg.toString()), null));
        return;
      }
      vs[i] = tv.value;
    }
    RDataConstr dc = helper.getDataConstr(modName, dconName);
    RStructItem data = helper.getStructItem(dc, vs);
    TaoItem o = TaoItem.create(helper, c.getType(bindings), data);
    helper.setReturnValue(o);
/* DEBUG */ } catch (Exception ex) {
/* DEBUG */ ex.printStackTrace(System.out);
/* DEBUG */ throw ex;
/* DEBUG */ }
  }

  public void sni_do_constr_attrs(RNativeImplHelper helper, RClosureItem self, RObjItem constr) {
    TaoItem constrTao = (TaoItem)constr;
    if (!(constrTao.value instanceof RStructItem)) {  // TODO: need more conditions
      helper.setException(sni_sango.SNIlang.createBadArgException(
        helper, new Cstr("Not a constructed data."), null));
      return;
    }
    
    RStructItem s = (RStructItem)constrTao.value;
    RDataConstr dc = s.getDataConstr();
    Cstr modName = dc.getModName();
    String dconName = dc.getName();
    PDataDef dd = helper.getCore().getDataDef(modName, dconName);
    PTypeSkel ts = dd.getTypeSig();
    PDataDef.Constr c = dd.getConstr(dconName);
    PTypeSkelBindings bindings = PTypeSkelBindings.create();
    ts.accept(PTypeSkel.NARROWER, true, constrTao.type, bindings);
    PTypeSkel.InstanciationBindings ib = PTypeSkel.InstanciationBindings.create(bindings);
    RListItem L = helper.getListNilItem();
    for (int i = s.getFieldCount() - 1; i >= 0; i--) {
      PDataDef.Attr a = c.getAttrAt(i);
      PTypeSkel at = a.getFixedType().instanciate(ib);
      RListItem.Cell lc = helper.createListCellItem();
      lc.head = TaoItem.create(helper, at, s.getFieldAt(i));
      lc.tail = L;
      L = lc;
    }
    helper.setReturnValue(L);
  }

  public void sni_do_closure(RNativeImplHelper helper, RClosureItem self, RObjItem mod, RObjItem official) {
    Cstr modName = helper.arrayItemToCstr((RArrayItem)mod);
    String funName = helper.arrayItemToCstr((RArrayItem)official).toJavaString();
    RClosureItem c = helper.getCore().getClosureItem(modName, funName);
    if (c != null) {
      PTypeSkel t = helper.getCore().getFunType(modName, funName);
      helper.setReturnValue(TaoItem.create(helper, t, c));
    } else {
      helper.setException(sni_sango.SNIlang.createBadArgException(
        helper, new Cstr("Not found."), null));
    }
  }

  public void sni_prepare_apply(RNativeImplHelper helper, RClosureItem self, RObjItem params, RObjItem closure) {
/* DEBUG */ try {
    TaoItem closureTao = (TaoItem)closure;
    if (!PTypeRefSkel.isLangType(closureTao.type, Module.TCON_FUN)) {
      helper.setException(sni_sango.SNIlang.createBadArgException(
        helper, new Cstr("Not a closure."), null));
      return;
    }
    PTypeRefSkel ft = (PTypeRefSkel)closureTao.type;  // <A B C ... R fun>
    PTypeSkel[] pts = ft.getParams();
    RObjItem[] ps = new RObjItem[pts.length - 1];
    RObjItem L = params;
    PTypeSkelBindings bindings = PTypeSkelBindings.create();
    for (int i = 0; i < pts.length - 1; i++) {
      if (!(L instanceof RListItem.Cell)) {
        helper.setException(sni_sango.SNIlang.createBadArgException(
          helper, new Cstr("Insufficient parameters."), null));
        return;
      }
      RListItem.Cell lc = (RListItem.Cell)L;
      TaoItem p = (TaoItem)lc.head;
      if (!pts[i].accept(PTypeSkel.NARROWER, true, p.type, bindings)) {
        helper.setException(sni_sango.SNIlang.createBadArgException(
          helper, new Cstr("Parameter type mismatch."), null));
        return;
      }
      ps[i] = p.value;
      L = lc.tail;
    }
    if (!(L instanceof RListItem.Nil)) {
      helper.setException(sni_sango.SNIlang.createBadArgException(
        helper, new Cstr("Too many parameters."), null));
      return;
    }
    PTypeSkel rt = pts[pts.length - 1]
      .instanciate(PTypeSkel.InstanciationBindings.create(bindings));
    helper.setReturnValue(RunObj.create(helper, rt, (RClosureItem)closureTao.value, ps));
/* DEBUG */ } catch (Exception ex) {
  /* DEBUG */ ex.printStackTrace(System.out);
  /* DEBUG */ throw ex;
/* DEBUG */ }
  }

  public void sni_do_apply(RNativeImplHelper helper, RClosureItem self, RObjItem run) {
/* DEBUG */ try {
    Object ri;
    if ((ri = helper.getAndClearResumeInfo()) == null) {
      RunObj ro = (RunObj)run;
      helper.setCatchException(true);
      helper.scheduleInvocation(ro.closure, ro.params, ro.retType);
    } else {
      RResult res = helper.getInvocationResult(); 
      if (res.endCondition() == RResult.NORMAL_END) {
        RObjItem r = res.getReturnValue();
        RDataConstr dc = helper.getDataConstr(Module.MOD_LANG, "fin$");
        helper.setReturnValue(
          helper.getStructItem(dc, new RObjItem[] { TaoItem.create(helper, (PTypeSkel)ri, r) })); 
      } else {
        helper.setReturnValue(res.toResultItem());
      }
    }
/* DEBUG */ } catch (Exception ex) {
  /* DEBUG */ ex.printStackTrace(System.out);
  /* DEBUG */ throw ex;
/* DEBUG */ }
  }

  static class TaoItem extends RObjItem {
    PTypeSkel type;
    RObjItem value;

    TaoItem(RuntimeEngine e, PTypeSkel type, RObjItem value) {
      super(e);
      this.type = type;
      this.value = value;
    }

    static TaoItem create(RNativeImplHelper helper, PTypeSkel type, RObjItem value) {
      return new TaoItem(helper.getRuntimeEngine(), type, value);
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      boolean eq;
      if (item == this) {
        eq = true;
      } else if (!(item instanceof TaoItem)) {
        eq = false;
      } else {
        TaoItem o = (TaoItem)item;
        eq = this.type.equals(o.type) && this.value.objEquals(frame, o.value);
      }
      return eq;
    }

    public void doHash(RNativeImplHelper helper, RClosureItem self) {
      helper.setReturnValue(helper.getIntItem(0));  // TODO: improve!
    }

    public Cstr dumpInside() {
      return new Cstr(PTypeSkel.Repr.topLevelRepr(this.type) + this.toString());
    }

    public RType.Sig getTsig() {
      return RType.createTsig(myModName, "tao", 0);
    }
  }

  static class RunObj extends RObjItem {
    PTypeSkel retType;
    RClosureItem closure;
    RObjItem[] params;

    RunObj(RuntimeEngine e, PTypeSkel retType, RClosureItem closure, RObjItem[] params) {
      super(e);
      this.retType = retType;
      this.closure = closure;
      this.params = params;
    }

    static RunObj create(RNativeImplHelper helper, PTypeSkel retType, RClosureItem closure, RObjItem[] params) {
      return new RunObj(helper.getRuntimeEngine(), retType, closure, params);
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      boolean eq;
      if (item == this) {
        eq = true;
      } else if (!(item instanceof RunObj)) {
        eq = false;
      } else {
        RunObj r = (RunObj)item;
        eq = this.retType.equals(r.retType) && this.closure.objEquals(frame, r.closure) && this.params.length == r.params.length;
        for (int i = 0; eq && i < this.params.length; i++) {
          eq = this.params[i].objEquals(frame, r.params[i]);
        }
      }
      return eq;
    }

    public void doHash(RNativeImplHelper helper, RClosureItem self) {
      helper.setReturnValue(helper.getIntItem(0));  // TODO: improve!
    }

    public Cstr dumpInside() {
      return new Cstr(this.toString());
    }

    public RType.Sig getTsig() {
      return RType.createTsig(myModName, "run_obj", 0);
    }
  }

  static PTypeRefSkel createPrimitiveType(RNativeImplHelper helper, String tcon) {
    DataDef dd = new DataDef(helper.getCore().getDefDictGetter());
    dd.mod = Module.MOD_LANG;
    dd.sigTcon = tcon;
    dd.sigParams = new PTypeVarSkel[0];
    dd.acc = Module.ACC_PUBLIC;
    // dd.baseTconKey = null;
    PDefDict.IdKey tk = PDefDict.IdKey.create(Module.MOD_LANG, tcon);
    PDefDict.DataDefGetter ddg = new DataDefGetter(dd);
    PDefDict.TconProps tp = PDefDict.TconProps.create(
        tk, PTypeId.SUBCAT_DATA, new PDefDict.TparamProps[0], Module.ACC_PUBLIC, ddg);
    return PTypeRefSkel.create(
      helper.getCore().getDefDictGetter(), null, tp, false, dd.sigParams);
  }

  static PTypeRefSkel createTupleType(RNativeImplHelper helper, PTypeSkel[] elemTypes) {
    DataDef dd = new DataDef(helper.getCore().getDefDictGetter());
    dd.mod = Module.MOD_LANG;
    dd.sigTcon = Module.TCON_TUPLE;
    dd.sigParams = new PTypeVarSkel[elemTypes.length];
    for (int i = 0; i < elemTypes.length; i++) {
      dd.sigParams[i] = PTypeVarSkel.create(null, null, PTypeVarSlot.createInternal(false), null, null);  // HERE
    };
    dd.acc = Module.ACC_PUBLIC;
    PDefDict.IdKey tk = PDefDict.IdKey.create(Module.MOD_LANG, Module.TCON_TUPLE);
    PDefDict.DataDefGetter ddg = new DataDefGetter(dd);
    PDefDict.TconProps tp = PDefDict.TconProps.create(
        tk, PTypeId.SUBCAT_DATA, null, Module.ACC_PUBLIC, ddg);
    return PTypeRefSkel.create(
      helper.getCore().getDefDictGetter(), null, tp, false, elemTypes);
  }

  static class DataDefGetter implements PDefDict.DataDefGetter {
    PDataDef dataDef;

    DataDefGetter(PDataDef dataDef) {
      this.dataDef = dataDef;
    }

    public PDataDef getDataDef() { return this.dataDef; }

    public PAliasTypeDef getAliasTypeDef() { return null; }
  }

  static class DataDef implements PDataDef {
    Module.Availability availability;  // actually needed?
    PDefDict.DefDictGetter defDictGetter;
    Cstr mod;
    PTypeSkel sig;  // lazy setup
    String sigTcon;
    PTypeVarSkel[] sigParams;
    Module.Access acc;
    List<String> constrList;
    Map<String, PDataDef.Constr> constrDict;
    PDefDict.IdKey baseTconKey;

    DataDef(PDefDict.DefDictGetter defDictGetter) {
      this.defDictGetter = defDictGetter;
      this.constrList = new ArrayList<String>();
      this.constrDict = new HashMap<String, PDataDef.Constr>();
    }

    public String getFormalTcon() { return this.sigTcon; }

    public PDefDict.IdKey getBaseTconKey() { return this.baseTconKey; }

    public int getParamCount() { return this.sigParams.length; }

    public PTypeSkel getTypeSig() {
      if (this.sig == null) {
        if (this.sigTcon.equals(Module.TCON_BOTTOM)) {  // needed?
          throw new RuntimeException("Attempted to make sig of BOTTOM.");
        } else if (this.sigTcon.equals(Module.TCON_EXPOSED)) {  // needed?
          throw new RuntimeException("Attempted to make sig of EXPOSED.");
        } else {
          PDefDict.IdKey tk = PDefDict.IdKey.create(this.mod, this.sigTcon);
          PDefDict.DataDefGetter ddg = new DataDefGetter(this);
          PDefDict.TparamProps[] paramPropss = new PDefDict.TparamProps[this.sigParams.length];
          for (int i = 0; i < this.sigParams.length; i++) {
            paramPropss[i] = PDefDict.TparamProps.create(this.getParamVarianceAt(i), this.sigParams[i].isConcrete());
          }
          PDefDict.TconProps tp = PDefDict.TconProps.create(
            tk, (this.baseTconKey != null)? PTypeId.SUBCAT_EXTEND: PTypeId.SUBCAT_DATA,
            paramPropss, this.acc, ddg);
          this.sig = PTypeRefSkel.create(
            this.defDictGetter, null, tp, false, this.sigParams);
        }
      }
      return this.sig;
    }

    public Module.Variance getParamVarianceAt(int pos) {
      throw new RuntimeException("getParamVariance not implemtented.");
    }

    public Module.Availability getAvailability() { return this.availability; }

    public Module.Access getAcc() { return this.acc; }

    public int getConstrCount() { return this.constrDict.size(); }

    public PDataDef.Constr getConstr(String dcon) { return this.constrDict.get(dcon); }

    public PDataDef.Constr getConstrAt(int index) { return this.constrDict.get(this.constrList.get(index)); }

    public int getFeatureImplCount() {
      throw new RuntimeException("SNImodule.DataDef#getFeatureImplCount() not implemented.");
    }

    public PDataDef.FeatureImpl getFeatureImplAt(int index) {
      throw new RuntimeException("SNImodule.DataDef#getFeatureImplAt() not implemented.");
    }

    PDataDef.Constr addConstr(String dcon) {
      throw new RuntimeException("Not implemented");
      // ConstrDef cd = new ConstrDef(dcon);
      // this.constrList.add(dcon);
      // this.constrDict.put(dcon, cd);
      // cd.dataDef = this;
      // return cd;
    }
  }
}
