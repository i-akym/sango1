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
 package sni_sango;
 
 import org.sango_lang.Cstr;
 import org.sango_lang.RClosureItem;
 import org.sango_lang.RDataConstr;
 import org.sango_lang.RIntItem;
 import org.sango_lang.Module;
 import org.sango_lang.RNativeImplHelper;
 import org.sango_lang.RObjItem;
 import org.sango_lang.RRealItem;
 import org.sango_lang.RStructItem;
 import org.sango_lang.RuntimeEngine;

 public class SNIlang {
  public static SNIlang getInstance(RuntimeEngine e) {
    return new SNIlang();
  }

  public void sni_nan(RNativeImplHelper helper, RClosureItem self) {
    helper.setReturnValue(helper.getNaNItem());
  }

  public void sni_pos_inf(RNativeImplHelper helper, RClosureItem self) {
    helper.setReturnValue(helper.getPosInfItem());
  }

  public void sni_neg_inf(RNativeImplHelper helper, RClosureItem self) {
    helper.setReturnValue(helper.getNegInfItem());
  }

  public void sni_nan_Q_(RNativeImplHelper helper, RClosureItem self, RObjItem r) {
    double rv = ((RRealItem)r).getValue();
    helper.setReturnValue(helper.getBoolItem(Double.isNaN(rv)));
  }

  public void sni_infinite_Q_(RNativeImplHelper helper, RClosureItem self, RObjItem r) {
    double rv = ((RRealItem)r).getValue();
    helper.setReturnValue(helper.getBoolItem(Double.isInfinite(rv)));
  }

  public void sni_int_compare(RNativeImplHelper helper, RClosureItem self, RObjItem i0, RObjItem i1) {
    int iv0 = ((RIntItem)i0).getValue();
    int iv1 = ((RIntItem)i1).getValue();
    RDataConstr dc;
    if (iv0 == iv1) {
      dc = helper.getDataConstr(Module.MOD_LANG, "eq$");
    } else if (iv0 < iv1) {
      dc = helper.getDataConstr(Module.MOD_LANG, "lt$");
    } else {
      dc = helper.getDataConstr(Module.MOD_LANG, "gt$");
    }
    RObjItem r = helper.getStructItem(dc, new RObjItem[0]);
    helper.setReturnValue(r); 
  }

  public void sni_real_compare(RNativeImplHelper helper, RClosureItem self, RObjItem r0, RObjItem r1) {
    double rv0 = ((RRealItem)r0).getValue();
    double rv1 = ((RRealItem)r1).getValue();
    RDataConstr dc;
    if (rv0 == rv1) {
      dc = helper.getDataConstr(Module.MOD_LANG, "eq$");
    } else if (rv0 < rv1) {
      dc = helper.getDataConstr(Module.MOD_LANG, "lt$");
    } else {
      dc = helper.getDataConstr(Module.MOD_LANG, "gt$");
    }
    RObjItem r = helper.getStructItem(dc, new RObjItem[0]);
    helper.setReturnValue(r); 
  }

  public void sni_int_sum(RNativeImplHelper helper, RClosureItem self, RObjItem i0, RObjItem i1) {
    long iv0 = ((RIntItem)i0).getValue();
    long iv1 = ((RIntItem)i1).getValue();
    long s = iv0 + iv1;
    if (inIntRange(s)) {
      helper.setReturnValue(helper.getIntItem((int)s)); 
    } else {
      RObjItem oArithFail = createOverflowException(helper, new Cstr("Out of integer range."), null);
      helper.setException(oArithFail);
    }
  }

  public void sni_real_sum(RNativeImplHelper helper, RClosureItem self, RObjItem r0, RObjItem r1) {
    double rv0 = ((RRealItem)r0).getValue();
    double rv1 = ((RRealItem)r1).getValue();
    double s = rv0 + rv1;
    helper.setReturnValue(helper.getRealItem(s)); 
  }

  public void sni_int_diff(RNativeImplHelper helper, RClosureItem self, RObjItem i0, RObjItem i1) {
    long iv0 = ((RIntItem)i0).getValue();
    long iv1 = ((RIntItem)i1).getValue();
    long d = iv0 - iv1;
    if (inIntRange(d)) {
      helper.setReturnValue(helper.getIntItem((int)d)); 
    } else {
      RObjItem oArithFail = createOverflowException(helper, new Cstr("Out of integer range."), null);
      helper.setException(oArithFail);
    }
  }

  public void sni_real_diff(RNativeImplHelper helper, RClosureItem self, RObjItem r0, RObjItem r1) {
    double rv0 = ((RRealItem)r0).getValue();
    double rv1 = ((RRealItem)r1).getValue();
    double d = rv0 - rv1;
    helper.setReturnValue(helper.getRealItem(d)); 
  }

  public void sni_int_prod(RNativeImplHelper helper, RClosureItem self, RObjItem i0, RObjItem i1) {
    long iv0 = ((RIntItem)i0).getValue();
    long iv1 = ((RIntItem)i1).getValue();
    long p = iv0 * iv1;
    if (inIntRange(p)) {
      helper.setReturnValue(helper.getIntItem((int)p)); 
    } else {
      RObjItem oArithFail = createOverflowException(helper, new Cstr("Out of integer range."), null);
      helper.setException(oArithFail);
    }
  }

  public void sni_real_prod(RNativeImplHelper helper, RClosureItem self, RObjItem r0, RObjItem r1) {
    double rv0 = ((RRealItem)r0).getValue();
    double rv1 = ((RRealItem)r1).getValue();
    double p = rv0 * rv1;
    helper.setReturnValue(helper.getRealItem(p)); 
  }

  public void sni_int_quot(RNativeImplHelper helper, RClosureItem self, RObjItem i0, RObjItem i1) {
    int iv0 = ((RIntItem)i0).getValue();
    int iv1 = ((RIntItem)i1).getValue();
    if (iv1 != 0) {
      int q = iv0 / iv1;
      helper.setReturnValue(helper.getIntItem(q)); 
    } else {
      RObjItem oArithFail = createZeroDivException(helper, new Cstr("Divided by zero."), null);
      helper.setException(oArithFail);
    }
  }

  public void sni_real_quot(RNativeImplHelper helper, RClosureItem self, RObjItem r0, RObjItem r1) {
    double rv0 = ((RRealItem)r0).getValue();
    double rv1 = ((RRealItem)r1).getValue();
    double q = rv0 / rv1;
    helper.setReturnValue(helper.getRealItem(q)); 
  }

  public void sni_int_div(RNativeImplHelper helper, RClosureItem self, RObjItem i0, RObjItem i1) {
    int iv0 = ((RIntItem)i0).getValue();
    int iv1 = ((RIntItem)i1).getValue();
    if (iv1 != 0) {
      int q = iv0 / iv1;
      int r = iv0 % iv1;
      RIntItem iq = helper.getIntItem(q);
      RIntItem ir = helper.getIntItem(r);
      helper.setReturnValue(helper.getTupleItem(new RObjItem[] { iq, ir })); 
    } else {
      RObjItem oArithFail = createZeroDivException(helper, new Cstr("Divided by zero."), null);
      helper.setException(oArithFail);
    }
  }

  public void sni_int_bit_not(RNativeImplHelper helper, RClosureItem self, RObjItem i) {
    int iv = ((RIntItem)i).getValue();
    helper.setReturnValue(helper.getIntItem(~iv)); 
  }

  public void sni_int_bit_and(RNativeImplHelper helper, RClosureItem self, RObjItem i0, RObjItem i1) {
    int iv0 = ((RIntItem)i0).getValue();
    int iv1 = ((RIntItem)i1).getValue();
    helper.setReturnValue(helper.getIntItem(iv0 & iv1)); 
  }

  public void sni_int_bit_or(RNativeImplHelper helper, RClosureItem self, RObjItem i0, RObjItem i1) {
    int iv0 = ((RIntItem)i0).getValue();
    int iv1 = ((RIntItem)i1).getValue();
    helper.setReturnValue(helper.getIntItem(iv0 | iv1)); 
  }

  public void sni_int_bit_xor(RNativeImplHelper helper, RClosureItem self, RObjItem i0, RObjItem i1) {
    int iv0 = ((RIntItem)i0).getValue();
    int iv1 = ((RIntItem)i1).getValue();
    helper.setReturnValue(helper.getIntItem(iv0 ^ iv1)); 
  }

  public void sni_int_shift_right(RNativeImplHelper helper, RClosureItem self, RObjItem i, RObjItem count) {
    int iv = ((RIntItem)i).getValue();
    int cv = ((RIntItem)count).getValue();
    helper.setReturnValue(helper.getIntItem(iv >>> cv)); 
  }

  public void sni_int_shift_right_keep_sign(RNativeImplHelper helper, RClosureItem self, RObjItem i, RObjItem count) {
    int iv = ((RIntItem)i).getValue();
    int cv = ((RIntItem)count).getValue();
    helper.setReturnValue(helper.getIntItem(iv >> cv)); 
  }

  public void sni_int_shift_left(RNativeImplHelper helper, RClosureItem self, RObjItem i, RObjItem count) {
    int iv = ((RIntItem)i).getValue();
    int cv = ((RIntItem)count).getValue();
    helper.setReturnValue(helper.getIntItem(iv << cv)); 
  }

  public void sni_byte_to_int(RNativeImplHelper helper, RClosureItem self, RObjItem b) {
    int bv = ((RIntItem)b).getValue();
    helper.setReturnValue(helper.getIntItem(bv)); 
  }

  public void sni_int_to_byte(RNativeImplHelper helper, RClosureItem self, RObjItem i) {
    int iv = ((RIntItem)i).getValue();
    if (0 <= iv && iv <= 255) {
      helper.setReturnValue(helper.getByteItem(iv)); 
    } else {
      RObjItem oBadArg = createBadArgException(helper, new Cstr("Out of byte range."), null);
      helper.setException(oBadArg);
    }
  }

  public void sni_int_byte_part(RNativeImplHelper helper, RClosureItem self, RObjItem i) {
    int iv = ((RIntItem)i).getValue();
    helper.setReturnValue(helper.getByteItem(iv & 0xff)); 
  }

  public void sni_int_to_real(RNativeImplHelper helper, RClosureItem self, RObjItem i) {
    int iv = ((RIntItem)i).getValue();
    helper.setReturnValue(helper.getRealItem(iv)); 
  }

  public void sni_real_to_int(RNativeImplHelper helper, RClosureItem self, RObjItem r) {
    double rv = ((RRealItem)r).getValue();
    if (Double.isNaN(rv)) {
      RObjItem oArithFail = createArithErrorException(helper, new Cstr("Not a number."), null);
      helper.setException(oArithFail);
    } else if (Double.isInfinite(rv)) {
      RObjItem oArithFail = createOverflowException(helper, new Cstr("Infinite."), null);
      helper.setException(oArithFail);
    } else if (inIntConvertibleRange(rv)) {
      helper.setReturnValue(helper.getIntItem((int)rv)); 
    } else {
      RObjItem oArithFail = createOverflowException(helper, new Cstr("Out of integer range."), null);
      helper.setException(oArithFail);
    }
  }

  public void sni_char_code(RNativeImplHelper helper, RClosureItem self, RObjItem c) {
    RIntItem ic = (RIntItem)c;
    helper.setReturnValue(helper.getIntItem(ic.getValue()));
  }

  public void sni_char_value(RNativeImplHelper helper, RClosureItem self, RObjItem i) {
    int iv = ((RIntItem)i).getValue();
    if (0 <= iv && iv <= 0x10ffff) {
      helper.setReturnValue(helper.getCharItem(iv));
    } else {
      RObjItem oBadArg = createBadArgException(helper, new Cstr("Out of range."), null);
      helper.setException(oBadArg);
    }
  }

  public void sni_catch_all_try(RNativeImplHelper helper, RClosureItem self, RObjItem f) {
    if (helper.getAndClearResumeInfo() == null) {
      helper.setCatchException(true);
      helper.scheduleInvocation((RClosureItem)f, new RObjItem[0], self);
    } else {
      helper.setReturnValue(helper.getInvocationResult().toResultItem()); 
    }
  }

  public void sni_new_exception(RNativeImplHelper helper, RClosureItem self, RObjItem desc, RObjItem msg, RObjItem originalExc_) {
    RDataConstr dc = helper.getDataConstr(Module.MOD_LANG, "exception$");
    RObjItem[] attrs = new RObjItem[] { desc, msg, helper.getExcInfo(), originalExc_ };
    helper.setReturnValue(helper.getStructItem(dc, attrs));
  }

  public void sni_throw(RNativeImplHelper helper, RClosureItem self, RObjItem e) {
    helper.setException(e);
  }

  public void sni_expose(RNativeImplHelper helper, RClosureItem self, RObjItem x) {
    helper.setReturnValue(x); 
  }

  static boolean inIntRange(long L) {
    return -2147483648 <= L && L <= 2147483647;
  }

  static boolean inIntConvertibleRange(double d) {
    return -2147483649.0 < d && d < 2147483648.0;
  }

  public static RObjItem createBadArgException(RNativeImplHelper helper, Cstr msg, RObjItem org) {
    RDataConstr edc = helper.getDataConstr(Module.MOD_LANG, "bad_arg$");
    RObjItem ed = helper.getStructItem(edc, new RObjItem[0]);
    return helper.createException(ed, msg, org);
  }

  public static RObjItem createUnsupportedException(RNativeImplHelper helper, Cstr msg, RObjItem org) {
    RDataConstr edc = helper.getDataConstr(Module.MOD_LANG, "unsupported$");
    RObjItem ed = helper.getStructItem(edc, new RObjItem[0]);
    return helper.createException(ed, msg, org);
  }

  public static RObjItem createSecurityErrorException(RNativeImplHelper helper, Cstr msg, RObjItem org) {
    RDataConstr edc = helper.getDataConstr(Module.MOD_LANG, "security_error$");
    RObjItem ed = helper.getStructItem(edc, new RObjItem[0]);
    return helper.createException(ed, msg, org);
  }

  public static RObjItem createNoElemException(RNativeImplHelper helper, Cstr msg, RObjItem org) {
    RDataConstr dcNoElem = helper.getDataConstr(Module.MOD_LANG, "no_elem$");
    RObjItem oNoElem = helper.getStructItem(dcNoElem, new RObjItem[0]);
    return createRuntimeFailureException(helper, oNoElem, msg, org);
  }

  public static RObjItem createRuntimeFailureException(RNativeImplHelper helper, RObjItem cause, Cstr msg, RObjItem org) {
    RDataConstr dcRuntimeErr = helper.getDataConstr(Module.MOD_LANG, "runtime_failure$");
    RObjItem oRuntimeErr = helper.getStructItem(dcRuntimeErr, new RObjItem[] { cause });
    return helper.createException(oRuntimeErr, msg, org);
  }

  public static RObjItem createZeroDivException(RNativeImplHelper helper, Cstr msg, RObjItem org) {
    RDataConstr dcZeroDiv = helper.getDataConstr(Module.MOD_LANG, "zero_div$");
    RObjItem oZeroDiv = helper.getStructItem(dcZeroDiv, new RObjItem[0]);
    return createArithFailureException(helper, oZeroDiv, msg, org);
  }

  public static RObjItem createOverflowException(RNativeImplHelper helper, Cstr msg, RObjItem org) {
    RDataConstr dcOverflow = helper.getDataConstr(Module.MOD_LANG, "overflow$");
    RObjItem oOverflow = helper.getStructItem(dcOverflow, new RObjItem[0]);
    return createArithFailureException(helper, oOverflow, msg, org);
  }

  public static RObjItem createArithErrorException(RNativeImplHelper helper, Cstr msg, RObjItem org) {
    RDataConstr dcArithErr = helper.getDataConstr(Module.MOD_LANG, "arith_error$");
    RObjItem oArithErr = helper.getStructItem(dcArithErr, new RObjItem[0]);
    return createArithFailureException(helper, oArithErr, msg, org);
  }

  public static RObjItem createArithFailureException(RNativeImplHelper helper, RObjItem cause, Cstr msg, RObjItem org) {
    RDataConstr dcArithFail = helper.getDataConstr(Module.MOD_LANG, "arith_failure$");
    RObjItem oArithFail = helper.getStructItem(dcArithFail, new RObjItem[] { cause });
    return helper.createException(oArithFail, msg, org);
  }

  public static RObjItem createSysErrorException(RNativeImplHelper helper, Cstr msg, RObjItem org) {
    RDataConstr edc = helper.getDataConstr(Module.MOD_LANG, "sys_error$");
    RObjItem ed = helper.getStructItem(edc, new RObjItem[0]);
    return helper.createException(ed, msg, org);
  }

  public static RObjItem getMaybeItem(RNativeImplHelper helper, RObjItem o) {
    RObjItem r;
    if (o != null) {
      RDataConstr dcValue = helper.getDataConstr(Module.MOD_LANG, "value$");
      r = helper.getStructItem(dcValue, new RObjItem[] { o });
    } else {
      RDataConstr dcNone = helper.getDataConstr(Module.MOD_LANG, "none$");
      r = helper.getStructItem(dcNone, new RObjItem[0]);
    }
    return r;
  }

  public static RObjItem unwrapMaybeItem(RNativeImplHelper helper, RObjItem o) {
    RStructItem s = (RStructItem)o;
    RDataConstr sd = s.getDataConstr();
    RDataConstr dcValue = helper.getDataConstr(Module.MOD_LANG, "value$");
    RDataConstr dcNone = helper.getDataConstr(Module.MOD_LANG, "none$");
    RObjItem r = null;
    if (sd.equals(dcValue)) {
      r = s.getFieldAt(0);
    } else if (sd.equals(dcNone)) {
      ;
    } else {
      throw new IllegalArgumentException("Not <maybe> value.");
    }
    return r;
  }
}
