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
import org.sango_lang.PAliasDef;
import org.sango_lang.PDataDef;
import org.sango_lang.PDefDict;
import org.sango_lang.PTypeId;
import org.sango_lang.PTypeSkel;
import org.sango_lang.PTypeSkelBindings;
import org.sango_lang.PTypeRefSkel;
import org.sango_lang.PTypeVarSkel;
import org.sango_lang.PVarSlot;
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

  public void sni_do_new_tuple(RNativeImplHelper helper, RClosureItem self, RObjItem elems
) {
    List<RStructItem> os = new ArrayList<RStructItem>();
    RListItem L = (RListItem)elems;
    while (L instanceof RListItem.Cell) {
      RListItem.Cell c = (RListItem.Cell)L;
      os.add((RStructItem)c.head);
      L = c.tail;
    }
    if (os.size() >= 2) {
      helper.setReturnValue(TupleObj.create(helper, os));
    } else {
      helper.setException(sni_sango.SNIlang.createBadArgException(
        helper, new Cstr("Insufficient elements."), null));
    }
  }

  public void sni_do_tuple_elems(RNativeImplHelper helper, RClosureItem self, RObjItem tuple) {
    TupleObj o = (TupleObj)tuple;
    RStructItem s = (RStructItem)o.value;
    PTypeSkel[] pts = ((PTypeRefSkel)o.type).getParams();
    RListItem L = helper.getListNilItem();
    for (int i = s.getFieldCount() - 1; i >= 0; i--) {
      RListItem.Cell c = helper.createListCellItem();
      c.head = wrap(helper, pts[i], s.getFieldAt(i));
      c.tail = L;
      L = c;
    }
    helper.setReturnValue(L);
  }

  public void sni_do_empty_list(RNativeImplHelper helper, RClosureItem self) {
    helper.setReturnValue(ListObj.createEmpty(helper));
  }

  public void sni_do_list_cons(RNativeImplHelper helper, RClosureItem self, RObjItem head, RObjItem tail) {
/* DEBUG */ try {
    TnV htv = unwrap(helper, (RStructItem)head);
    ListObj lo = (ListObj)tail;
// /* DEBUG */ System.out.println(lo.type);
    PTypeSkel tep = ((PTypeRefSkel)lo.type).getParams()[0];
    try {
      PTypeSkelBindings b = PTypeSkelBindings.create();
      PTypeSkel tep2 = htv.type.join(tep, b);
      if (tep2 != null) {
        ListObj L = ListObj.create(helper, tep2, htv.value, (RListItem)lo.value);
        helper.setReturnValue(L);
      } else {
        helper.setException(sni_sango.SNIlang.createBadArgException(
          helper, new Cstr("Type mismatch."), null));
      }
    } catch (CompileException ex) {
      throw new RuntimeException("Unexpected exception. - " + ex.toString());
      // helper.setException(sni_sango.SNIlang.createBadArgException(
        // helper, new Cstr("Type mismatch."), null));
    }
/* DEBUG */ } catch (Exception ex) {
/* DEBUG */ ex.printStackTrace(System.out);
/* DEBUG */ }
  }

  public void sni_do_list_empty_Q_(RNativeImplHelper helper, RClosureItem self, RObjItem list) {
    helper.setReturnValue(helper.getBoolItem(((ListObj)list).isEmpty()));
  }

  public void sni_do_list_head(RNativeImplHelper helper, RClosureItem self, RObjItem list) {
    ListObj L = (ListObj)list;
    RObjItem h;
    if (L.isEmpty()) {
      h = null;
    } else {
      RListItem.Cell c = (RListItem.Cell)L.value;
      h = wrap(helper, L.getElemType(), c.head);
    }
    helper.setReturnValue(sni_sango.SNIlang.getMaybeItem(helper, h));
  }

  public void sni_do_list_tail(RNativeImplHelper helper, RClosureItem self, RObjItem list) {
    ListObj L = (ListObj)list;
    RObjItem t;
    if (L.isEmpty()) {
      t = null;
    } else {
      RListItem.Cell c = (RListItem.Cell)L.value;
      t = ListObj.createWithType(helper, L.type, c.tail);
    }
    helper.setReturnValue(sni_sango.SNIlang.getMaybeItem(helper, t));
  }

  public void sni_do_list_to_string(RNativeImplHelper helper, RClosureItem self, RObjItem list) {
    ListObj lo = (ListObj)list;
    RListItem L = (RListItem)lo.value;
    int count = 0;
    while (L instanceof RListItem.Cell) {
      count++;
      RListItem.Cell c = (RListItem.Cell)L;
      L = c.tail;
    }
    RArrayItem a = helper.createArrayItem(count);
    L = (RListItem)lo.value;
    for (int i = 0; i < count; i++) {
      RListItem.Cell c = (RListItem.Cell)L;
      a.setElemAt(i, c.head);
      L = c.tail;
    }
    helper.setReturnValue(StringObj.create(helper, ((PTypeRefSkel)lo.type).getParams()[0], a));
  }

  public void sni_do_string_length(RNativeImplHelper helper, RClosureItem self, RObjItem string) {
    StringObj so = (StringObj)string;
    helper.setReturnValue(helper.getIntItem(((RArrayItem)so.value).getElemCount()));
  }

  public void sni_do_string_elem(RNativeImplHelper helper, RClosureItem self, RObjItem string, RObjItem pos) {
    StringObj so = (StringObj)string;
    RArrayItem a = (RArrayItem)so.value;
    int len = a.getElemCount();
    int idx = ((RIntItem)pos).getValue();
    if (0 <= idx && idx < len) {
      helper.setReturnValue(wrap(helper, so.getElemType(), a.getElemAt(idx)));
    } else {
      helper.setException(sni_sango.SNIlang.createBadArgException(
        helper, new Cstr("Invalid index."), null));
    }
  }

  public void sni_do_new_data(RNativeImplHelper helper, RClosureItem self, RObjItem attrs, RObjItem mod, RObjItem dcon) {
/* DEBUG */ try {
    RListItem L = (RListItem)attrs;
    List<TnV> as = new ArrayList<TnV>();
    while (L instanceof RListItem.Cell) {
      RListItem.Cell lc = (RListItem.Cell)L;
      as.add(unwrap(helper, (RStructItem)lc.head));
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
    PTypeSkelBindings b = PTypeSkelBindings.create();
    RObjItem[] vs = new RObjItem[as.size()];
    try {
      for (int i = 0; i < c.getAttrCount(); i++) {
        TnV tv = as.get(i);
        // if (PTypeRefSkel.willNotReturn(tv.type)) { ... }  // HERE
        PDataDef.Attr a = c.getAttrAt(i);
        PTypeSkel at = a.getNormalizedType();
        PTypeSkelBindings bb = b;
        if ((b = at.applyTo(tv.type, b)) == null) {
          StringBuffer emsg = new StringBuffer();
          emsg.append("Type mismatch at ");
          emsg.append(Integer.toString(i));
          emsg.append(".");
          emsg.append("\n  attribute def: ");
          emsg.append(PTypeSkel.Util.repr(at));
          emsg.append("\n  attribute def in context: ");
          emsg.append(PTypeSkel.Util.repr(at.resolveBindings(bb)));
          emsg.append("\n  actual attribute: ");
          emsg.append(PTypeSkel.Util.repr(tv.type));
          helper.setException(sni_sango.SNIlang.createBadArgException(
            helper, new Cstr(emsg.toString()), null));
          return;
        }
        vs[i] = tv.value;
      }
    } catch (CompileException ex) {
      throw new RuntimeException("Unexpected exception. - " + ex.toString());
    }
    RDataConstr dc = helper.getDataConstr(modName, dconName);
    RStructItem data = helper.getStructItem(dc, vs);
    ConstrObj o = ConstrObj.createWithType(helper, c.getType(b).instance, data);
    helper.setReturnValue(o);
/* DEBUG */ } catch (Exception ex) {
/* DEBUG */ ex.printStackTrace(System.out);
/* DEBUG */ throw ex;
/* DEBUG */ }
  }

  public void sni_do_constr_attrs(RNativeImplHelper helper, RClosureItem self, RObjItem constr) {
    ConstrObj co = (ConstrObj)constr;
    RStructItem s = (RStructItem)co.value;
    RDataConstr dc = s.getDataConstr();
    Cstr modName = dc.getModName();
    String dconName = dc.getName();
    PDataDef dd = helper.getCore().getDataDef(modName, dconName);
    PTypeSkel ts = dd.getTypeSig();
    PDataDef.Constr c = dd.getConstr(dconName);
    PTypeSkelBindings b = PTypeSkelBindings.create();
    try {
      b = ts.applyTo(co.type, b);
    } catch (CompileException ex) {
      throw new RuntimeException("Unexpected exception. - " + ex.toString());
    }
    PTypeSkel.InstanciationBindings ib = PTypeSkel.InstanciationBindings.create(b);
    RListItem L = helper.getListNilItem();
    for (int i = s.getFieldCount() - 1; i >= 0; i--) {
      PDataDef.Attr a = c.getAttrAt(i);
      PTypeSkel at = a.getNormalizedType().instanciate(ib, null);
      RListItem.Cell lc = helper.createListCellItem();
      lc.head = wrap(helper, at, s.getFieldAt(i));
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
      helper.setReturnValue(ClosureObj.createWithType(helper, t, c));
    } else {
      helper.setException(sni_sango.SNIlang.createBadArgException(
        helper, new Cstr("Not found."), null));
    }
  }

  public void sni_prepare_apply(RNativeImplHelper helper, RClosureItem self, RObjItem params, RObjItem closure) {
/* DEBUG */ try {
    ClosureObj co = (ClosureObj)closure;
    PTypeRefSkel ft = (PTypeRefSkel)co.type;  // <A B C ... R fun>
    PTypeSkel[] pts = ft.getParams();
    RObjItem[] ps = new RObjItem[pts.length - 1];
    RObjItem L = params;
    PTypeSkelBindings b = PTypeSkelBindings.create();
    try {
      for (int i = 0; i < pts.length - 1; i++) {
        if (!(L instanceof RListItem.Cell)) {
          helper.setException(sni_sango.SNIlang.createBadArgException(
            helper, new Cstr("Insufficient parameters."), null));
          return;
        }
        RListItem.Cell lc = (RListItem.Cell)L;
        RStructItem p = (RStructItem)lc.head;  // <obj>
        TnV tnv = unwrap(helper, p);
        PTypeSkelBindings bb = pts[i].applyTo(tnv.type, b);
        if (bb == null) {
          helper.setException(sni_sango.SNIlang.createBadArgException(
            helper, new Cstr("Parameter type mismatch."), null));
          return;
        }
        ps[i] = tnv.value;
        b = bb;
        L = lc.tail;
      }
    } catch (CompileException ex) {
      helper.setException(sni_sango.SNIlang.createBadArgException(
        helper, new Cstr("Parameter type mismatch."), null));
      return;
    }
    if (!(L instanceof RListItem.Nil)) {
      helper.setException(sni_sango.SNIlang.createBadArgException(
        helper, new Cstr("Too many parameters."), null));
      return;
    }
    PTypeSkel rt = pts[pts.length - 1]
      .instanciate(PTypeSkel.InstanciationBindings.create(b), null);
    helper.setReturnValue(RunObj.create(helper, rt, (RClosureItem)co.value, ps));
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
          helper.getStructItem(dc, new RObjItem[] { wrap(helper, (PTypeSkel)ri, r) })); 
      } else {
        helper.setReturnValue(res.toResultItem());
      }
    }
/* DEBUG */ } catch (Exception ex) {
  /* DEBUG */ ex.printStackTrace(System.out);
  /* DEBUG */ throw ex;
/* DEBUG */ }
  }

  abstract static class WrappedObj extends RObjItem {
    PTypeSkel type;
    RObjItem value;

    WrappedObj(RuntimeEngine e, PTypeSkel type, RObjItem value) {
      super(e);
      this.type = type;
      this.value = value;
    }

    public Cstr debugReprOfContents() {
      return new Cstr(PTypeSkel.Util.repr(this.type) + this.toString());
    }
  }

  public static class TupleObj extends WrappedObj {
    TupleObj(RuntimeEngine e, PTypeSkel type, RStructItem tuple) {
      super(e, type, tuple);
    }

    static TupleObj create(RNativeImplHelper helper, List<RStructItem> elems) {
      PTypeSkel[] ts = new PTypeSkel[elems.size()];
      RObjItem[] vs = new RObjItem[elems.size()];
      for (int i = 0; i < elems.size(); i++) {
        TnV tnv = unwrap(helper, elems.get(i));
        ts[i] = tnv.type;
        vs[i] = tnv.value;
      }
      return createWithType(helper, createTupleType(helper, ts), helper.getTupleItem(vs));
    }

    static TupleObj createWithType(RNativeImplHelper helper, PTypeSkel type, RStructItem tuple) {
      return new TupleObj(helper.getRuntimeEngine(), type, tuple);
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      boolean eq;
      if (item == this) {
        eq = true;
      } else if (!(item instanceof TupleObj)) {
        eq = false;
      } else {
        TupleObj t = (TupleObj)item;
        eq = this.type.equals(t.type) && this.value.objEquals(frame, t.value);
      }
      return eq;
    }

    public RType.Sig getTsig() {
      return RType.createTsig(myModName, "tuple_obj", 0);
    }
  }

  public static class ListObj extends WrappedObj {
    ListObj(RuntimeEngine e, PTypeSkel type, RListItem list) {
      super(e, type, list);
    }

    static ListObj createEmpty(RNativeImplHelper helper) {
      return createEmptyWithType(helper, createPolymorphicListType(helper));
    }

    static ListObj createEmpty(RNativeImplHelper helper, PTypeSkel elemType) {
      return createEmptyWithType(helper, createListType(helper, elemType));
    }

    static ListObj createEmptyWithType(RNativeImplHelper helper, PTypeSkel type) {
      return createWithType(helper, type, helper.getListNilItem());
    }

    static ListObj create(RNativeImplHelper helper, PTypeSkel elemType, RObjItem head, RListItem tail) {
      RListItem.Cell c = helper.createListCellItem();
      c.head = head;
      c.tail = tail;
      return create(helper, elemType, c);
    }

    static ListObj create(RNativeImplHelper helper, PTypeSkel elemType, RListItem list) {
      return createWithType(helper, createListType(helper, elemType), list);
    }

    static ListObj createWithType(RNativeImplHelper helper, PTypeSkel type, RListItem list) {
      return new ListObj(helper.getRuntimeEngine(), type, list);
    }

    PTypeSkel getElemType() {
      PTypeRefSkel t = (PTypeRefSkel)this.type;
      return t.getParams()[0];
    }

    public boolean isEmpty() {
      return this.value instanceof RListItem.Nil;
    }

    public RType.Sig getTsig() {
      return RType.createTsig(myModName, "list_obj", 0);
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      boolean eq;
      if (item == this) {
        eq = true;
      } else if (!(item instanceof ListObj)) {
        eq = false;  // logically not reached
      } else {
        ListObj L = (ListObj)item;
        eq = this.type.equals(L.type) && this.value.objEquals(frame, L.value);
      }
      return eq;
    }
  }

  public static class StringObj extends WrappedObj {
    StringObj(RuntimeEngine e, PTypeSkel type, RArrayItem string) {
      super(e, type, string);
    }

    static StringObj create(RNativeImplHelper helper, PTypeSkel elemType, RArrayItem string) {
      return createWithType(helper, createStringType(helper, elemType), string);
    }

    static StringObj createWithType(RNativeImplHelper helper, PTypeSkel type, RArrayItem string) {
      return new StringObj(helper.getRuntimeEngine(), type, string);
    }

    PTypeSkel getElemType() {
      PTypeRefSkel t = (PTypeRefSkel)this.type;
      return t.getParams()[0];
    }

    public RType.Sig getTsig() {
      return RType.createTsig(myModName, "string_obj", 0);
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      boolean eq;
      if (item == this) {
        eq = true;
      } else if (!(item instanceof StringObj)) {
        eq = false;  // logically not reached
      } else {
        StringObj so = (StringObj)item;
        eq = this.type.equals(so.type) && this.value.objEquals(frame, so.value);
      }
      return eq;
    }
  }

  public static class ConstrObj extends WrappedObj {
    ConstrObj(RuntimeEngine e, PTypeSkel type, RStructItem data) {
      super(e, type, data);
    }

    static ConstrObj createWithType(RNativeImplHelper helper, PTypeSkel type, RStructItem data) {
      return new ConstrObj(helper.getRuntimeEngine(), type, data);
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      boolean eq;
      if (item == this) {
        eq = true;
      } else if (!(item instanceof ConstrObj)) {
        eq = false;
      } else {
        ConstrObj c = (ConstrObj)item;
        RStructItem s = (RStructItem)c.value;
        RStructItem ss = (RStructItem)this.value;
        eq = this.type.equals(c.type);
        for (int i = 0; eq && i < s.getFieldCount(); i++) {
          eq = ss.getFieldAt(i).objEquals(frame, s.getFieldAt(i));
        }
      }
      return eq;
    }

    public RType.Sig getTsig() {
      return RType.createTsig(myModName, "constr_obj", 0);
    }
  }

  public static class ClosureObj extends WrappedObj {
    ClosureObj(RuntimeEngine e, PTypeSkel type, RClosureItem closure) {
      super(e, type, closure);
    }

    static ClosureObj createWithType(RNativeImplHelper helper, PTypeSkel type, RClosureItem closure) {
      return new ClosureObj(helper.getRuntimeEngine(), type, closure);
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      boolean eq;
      if (item == this) {
        eq = true;
      } else if (!(item instanceof ClosureObj)) {
        eq = false;
      } else {
        ClosureObj c = (ClosureObj)item;
        eq = this.type.equals(c.type) && this.value.objEquals(frame, c.value);
      }
      return eq;
    }

    public RType.Sig getTsig() {
      return RType.createTsig(myModName, "closure_obj", 0);
    }
  }

  public static class CapsuleObj extends WrappedObj {
    CapsuleObj(RuntimeEngine e, PTypeSkel type, RObjItem item) {
      super(e, type, item);
    }

    static CapsuleObj createWithType(RNativeImplHelper helper, PTypeSkel type, RObjItem item) {
      return new CapsuleObj(helper.getRuntimeEngine(), type, item);
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      boolean eq;
      if (item == this) {
        eq = true;
      } else if (!(item instanceof CapsuleObj)) {
        eq = false;
      } else {
        CapsuleObj c = (CapsuleObj)item;
        eq = this.type.equals(c.type) && this.value.objEquals(frame, c.value);
      }
      return eq;
    }

    public RType.Sig getTsig() {
      return RType.createTsig(myModName, "capsule_obj", 0);
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

    public Cstr debugReprOfContents() {
      return new Cstr(this.toString());
    }

    public RType.Sig getTsig() {
      return RType.createTsig(myModName, "run_obj", 0);
    }
  }

  static class TnV {
    PTypeSkel type;
    RObjItem value;

    TnV(PTypeSkel t, RObjItem v) {
      this.type = t;
      this.value = v;
    }
  }

  static TnV unwrap(RNativeImplHelper helper, RStructItem obj) {
    PTypeSkel t = null;
    RObjItem v = null;
    RDataConstr dc = obj.getDataConstr();
    String dcn = dc.getName();
    if (dcn.equals("int_obj$")) {
      t = createPrimitiveType(helper, Module.TCON_INT);
      v = obj.getFieldAt(0);
    } else if (dcn.equals("byte_obj$")) {
      t = createPrimitiveType(helper, Module.TCON_BYTE);
      v = obj.getFieldAt(0);
    } else if (dcn.equals("char_obj$")) {
      t = createPrimitiveType(helper, Module.TCON_CHAR);
      v = obj.getFieldAt(0);
    } else if (dcn.equals("real_obj$")) {
      t = createPrimitiveType(helper, Module.TCON_REAL);
      v = obj.getFieldAt(0);
    } else if (dcn.equals("tuple_obj$")
        || dcn.equals("list_obj$")
        || dcn.equals("string_obj$")
        || dcn.equals("constr_obj$")
        || dcn.equals("closure_obj$")
        || dcn.equals("capsule_obj$")) {
      WrappedObj o = (WrappedObj)obj.getFieldAt(0);
      t = o.type;
      v = o.value;
    } else {
      throw new IllegalArgumentException("Unexpected object. dcon =  " + dcn);
    }
    return new TnV(t, v);
  }

  static RObjItem wrap(RNativeImplHelper helper, PTypeSkel type, RObjItem obj) {
    RObjItem mo = null;
    if (PTypeRefSkel.isLangType(type, Module.TCON_INT)) {
      RDataConstr dc = helper.getDataConstr(myModName, "int_obj$");
      mo = helper.getStructItem(dc, new RObjItem[] { obj });
    } else if (PTypeRefSkel.isLangType(type, Module.TCON_BYTE)) {
      RDataConstr dc = helper.getDataConstr(myModName, "byte_obj$");
      mo = helper.getStructItem(dc, new RObjItem[] { obj });
    } else if (PTypeRefSkel.isLangType(type, Module.TCON_CHAR)) {
      RDataConstr dc = helper.getDataConstr(myModName, "char_obj$");
      mo = helper.getStructItem(dc, new RObjItem[] { obj });
    } else if (PTypeRefSkel.isLangType(type, Module.TCON_REAL)) {
      RDataConstr dc = helper.getDataConstr(myModName, "real_obj$");
      mo = helper.getStructItem(dc, new RObjItem[] { obj });
    } else if (PTypeRefSkel.isLangType(type, Module.TCON_TUPLE)) {
      RDataConstr dc = helper.getDataConstr(myModName, "tuple_obj$");
      TupleObj o = TupleObj.createWithType(helper, type, (RStructItem)obj);
      mo = helper.getStructItem(dc, new RObjItem[] { obj });
    } else if (PTypeRefSkel.isLangType(type, Module.TCON_LIST)) {
      RDataConstr dc = helper.getDataConstr(myModName, "list_obj$");
      ListObj lo = ListObj.createWithType(helper, type, (RListItem)obj);
      mo = helper.getStructItem(dc, new RObjItem[] { lo });
    } else if (PTypeRefSkel.isLangType(type, Module.TCON_STRING)) {
      RDataConstr dc = helper.getDataConstr(myModName, "string_obj$");
      StringObj o = StringObj.createWithType(helper, type, (RArrayItem)obj);
      mo = helper.getStructItem(dc, new RObjItem[] { o });
    } else if (PTypeRefSkel.isLangType(type, Module.TCON_FUN)) {
      RDataConstr dc = helper.getDataConstr(myModName, "closure_obj$");
      ClosureObj o = ClosureObj.createWithType(helper, type, (RClosureItem)obj);
      mo = helper.getStructItem(dc, new RObjItem[] { o });
    } else if (obj instanceof RStructItem) {  // TODO: should check definition
      RDataConstr dc = helper.getDataConstr(myModName, "constr_obj$");
      ConstrObj o = ConstrObj.createWithType(helper, type, (RStructItem)obj);
      mo = helper.getStructItem(dc, new RObjItem[] { o });
    } else {
      RDataConstr dc = helper.getDataConstr(myModName, "capsule_obj$");
      CapsuleObj o = CapsuleObj.createWithType(helper, type, obj);
      mo = helper.getStructItem(dc, new RObjItem[] { o });
    }
    return mo;
  }

  static PTypeRefSkel createPrimitiveType(RNativeImplHelper helper, String tcon) {
    DataDef dd = new DataDef(helper.getCore().getDefDictGetter());
    dd.mod = Module.MOD_LANG;
    dd.sigTcon = tcon;
    dd.sigParams = new PTypeVarSkel[0];
    dd.acc = Module.ACC_PUBLIC;
    // dd.baseTconKey = null;
    PDefDict.TconKey tk = PDefDict.TconKey.create(Module.MOD_LANG, tcon);
    PDefDict.DataDefGetter ddg = new DataDefGetter(dd);
    PDefDict.TconProps tp = PDefDict.TconProps.create(
        PTypeId.SUBCAT_DATA, 0, Module.ACC_PUBLIC, ddg);
    PDefDict.TconInfo ti = PDefDict.TconInfo.create(tk, tp);
    return PTypeRefSkel.create(
      helper.getCore().getDefDictGetter(), null, ti, false, dd.sigParams);
  }

  static PTypeRefSkel createTupleType(RNativeImplHelper helper, PTypeSkel[] elemTypes) {
    DataDef dd = new DataDef(helper.getCore().getDefDictGetter());
    dd.mod = Module.MOD_LANG;
    dd.sigTcon = Module.TCON_TUPLE;
    dd.sigParams = new PTypeVarSkel[elemTypes.length];
    for (int i = 0; i < elemTypes.length; i++) {
      dd.sigParams[i] = PTypeVarSkel.create(null, null, PVarSlot.createInternal());
    };
    dd.acc = Module.ACC_PUBLIC;
    // dd.baseTconKey = null;
    PDefDict.TconKey tk = PDefDict.TconKey.create(Module.MOD_LANG, Module.TCON_STRING);
    PDefDict.DataDefGetter ddg = new DataDefGetter(dd);
    PDefDict.TconProps tp = PDefDict.TconProps.create(
        PTypeId.SUBCAT_DATA, 1, Module.ACC_PUBLIC, ddg);
    PDefDict.TconInfo ti = PDefDict.TconInfo.create(tk, tp);
    return PTypeRefSkel.create(
      helper.getCore().getDefDictGetter(), null, ti, false, elemTypes);
  }

  static PTypeRefSkel createPolymorphicListType(RNativeImplHelper helper) {
    DataDef dd = new DataDef(helper.getCore().getDefDictGetter());
    dd.mod = Module.MOD_LANG;
    dd.sigTcon = Module.TCON_LIST;
    dd.sigParams = new PTypeVarSkel[] {
      PTypeVarSkel.create(null, null, PVarSlot.createInternal())
    };
    dd.acc = Module.ACC_PUBLIC;
    // dd.baseTconKey = null;
    PDefDict.TconKey tk = PDefDict.TconKey.create(Module.MOD_LANG, Module.TCON_LIST);
    PDefDict.DataDefGetter ddg = new DataDefGetter(dd);
    PDefDict.TconProps tp = PDefDict.TconProps.create(
        PTypeId.SUBCAT_DATA, 1, Module.ACC_PUBLIC, ddg);
    PDefDict.TconInfo ti = PDefDict.TconInfo.create(tk, tp);
    return PTypeRefSkel.create(
      helper.getCore().getDefDictGetter(), null, ti, false,
      new PTypeSkel[] { PTypeVarSkel.create(null, null, PVarSlot.createInternal()) });
  }

  static PTypeRefSkel createListType(RNativeImplHelper helper, PTypeSkel elemType) {
    DataDef dd = new DataDef(helper.getCore().getDefDictGetter());
    dd.mod = Module.MOD_LANG;
    dd.sigTcon = Module.TCON_LIST;
    dd.sigParams = new PTypeVarSkel[] {
      PTypeVarSkel.create(null, null, PVarSlot.createInternal())
    };
    dd.acc = Module.ACC_PUBLIC;
    // dd.baseTconKey = null;
    PDefDict.TconKey tk = PDefDict.TconKey.create(Module.MOD_LANG, Module.TCON_LIST);
    PDefDict.DataDefGetter ddg = new DataDefGetter(dd);
    PDefDict.TconProps tp = PDefDict.TconProps.create(
        PTypeId.SUBCAT_DATA, 1, Module.ACC_PUBLIC, ddg);
    PDefDict.TconInfo ti = PDefDict.TconInfo.create(tk, tp);
    return PTypeRefSkel.create(
      helper.getCore().getDefDictGetter(), null, ti, false,
      new PTypeSkel[] { elemType });
  }

  static PTypeRefSkel createStringType(RNativeImplHelper helper, PTypeSkel elemType) {
    DataDef dd = new DataDef(helper.getCore().getDefDictGetter());
    dd.mod = Module.MOD_LANG;
    dd.sigTcon = Module.TCON_STRING;
    dd.sigParams = new PTypeVarSkel[] {
      PTypeVarSkel.create(null, null, PVarSlot.createInternal())
    };
    dd.acc = Module.ACC_PUBLIC;
    // dd.baseTconKey = null;
    PDefDict.TconKey tk = PDefDict.TconKey.create(Module.MOD_LANG, Module.TCON_STRING);
    PDefDict.DataDefGetter ddg = new DataDefGetter(dd);
    PDefDict.TconProps tp = PDefDict.TconProps.create(
        PTypeId.SUBCAT_DATA, 1, Module.ACC_PUBLIC, ddg);
    PDefDict.TconInfo ti = PDefDict.TconInfo.create(tk, tp);
    return PTypeRefSkel.create(
      helper.getCore().getDefDictGetter(), null, ti, false,
      new PTypeSkel[] { elemType });
  }

  static class DataDefGetter implements PDefDict.DataDefGetter {
    PDataDef dataDef;
    // PAliasDef aliasDef;

    DataDefGetter(PDataDef dataDef /* , PAliasDef aliasDef */) {
      this.dataDef = dataDef;
      // this.aliasDef = aliasDef;
    }

    public PDataDef getDataDef() { return this.dataDef; }

    public PAliasDef getAliasDef() { return null; /* this.aliasDef; */ }
  }

  static class DataDef implements PDataDef {
    PDefDict.DefDictGetter defDictGetter;
    Cstr mod;
    PTypeSkel sig;  // lazy setup
    String sigTcon;
    PTypeVarSkel[] sigParams;
    int acc;
    List<String> constrList;
    Map<String, PDataDef.Constr> constrDict;
    PDefDict.TconKey baseTconKey;

    DataDef(PDefDict.DefDictGetter defDictGetter) {
      this.defDictGetter = defDictGetter;
      this.constrList = new ArrayList<String>();
      this.constrDict = new HashMap<String, PDataDef.Constr>();
    }

    public String getFormalTcon() { return this.sigTcon; }

    public PVarSlot[] getParamVarSlots() {
      PVarSlot[] pvs;
      if (this.sigParams != null) {
        pvs = new PVarSlot[this.sigParams.length];
        for (int i = 0; i < this.sigParams.length; i++) {
          pvs[i] = this.sigParams[i].getVarSlot();
        }
      } else {
        pvs = null;
      }
      return pvs;
    }

    public PTypeSkel getTypeSig() {
      if (this.sig == null) {
        if (this.sigTcon.equals(Module.TCON_NORET)) {
          throw new RuntimeException("Attempted to make sig of NORET.");
        } else if (this.sigTcon.equals(Module.TCON_EXPOSED)) {
          throw new RuntimeException("Attempted to make sig of EXPOSED.");
        } else {
          PDefDict.TconKey tk = PDefDict.TconKey.create(this.mod, this.sigTcon);
          PDefDict.DataDefGetter ddg = new DataDefGetter(this);
          PDefDict.TconProps tp = PDefDict.TconProps.create(
            (this.baseTconKey != null)? PTypeId.SUBCAT_EXTEND: PTypeId.SUBCAT_DATA,
            this.sigParams.length, this.acc, ddg);
          this.sig = PTypeRefSkel.create(
            this.defDictGetter, null, PDefDict.TconInfo.create(tk, tp), false, this.sigParams);
        }
      }
      return this.sig;
    }

    public int getAcc() { return this.acc; }

    public int getConstrCount() { return this.constrDict.size(); }

    public PDataDef.Constr getConstrAt(int index) { return this.constrDict.get(this.constrList.get(index)); }

    public PDataDef.Constr getConstr(String dcon) { return this.constrDict.get(dcon); }

    public PDefDict.TconKey getBaseTconKey() { return this.baseTconKey; }

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
