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
package sni_sango.sni_num;
 
import java.math.BigInteger;
import org.sango_lang.Cstr;
import org.sango_lang.RClosureItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RIntItem;
import org.sango_lang.Module;
import org.sango_lang.RFrame;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RType;
import org.sango_lang.RuntimeEngine;

public class SNIbigint {
  static final BigInteger intMin = new BigInteger(new byte[] { -128, 0, 0, 0 });
  static final BigInteger intMax = new BigInteger(new byte[] { 127, -1, -1, -1 });

  public static SNIbigint getInstance(RuntimeEngine e) {
    return new SNIbigint();
  }

  public void sni_to_bigint(RNativeImplHelper helper, RClosureItem self, RObjItem i) {
    helper.setReturnValue(createBigintItem(helper.getRuntimeEngine(), ((RIntItem)i).getValue()));
  }

  public void sni_bit_length(RNativeImplHelper helper, RClosureItem self, RObjItem b) {
    BigInteger bi = ((BigintItem)b).value;
    helper.setReturnValue(helper.getIntItem(bi.bitLength()));
  }

  public void sni_compare(RNativeImplHelper helper, RClosureItem self, RObjItem b0, RObjItem b1) {
    BigInteger bi0 = ((BigintItem)b0).value;
    BigInteger bi1 = ((BigintItem)b1).value;
    int c = bi0.compareTo(bi1);
    RDataConstr dc;
    if (c == 0) {
      dc = helper.getDataConstr(Module.MOD_LANG, "eq$");
    } else if (c < 0) {
      dc = helper.getDataConstr(Module.MOD_LANG, "lt$");
    } else {
      dc = helper.getDataConstr(Module.MOD_LANG, "gt$");
    }
    RObjItem r = helper.getStructItem(dc, new RObjItem[0]);
    helper.setReturnValue(r); 
  }

  public void sni_sum(RNativeImplHelper helper, RClosureItem self, RObjItem b0, RObjItem b1) {
    BigInteger bi0 = ((BigintItem)b0).value;
    BigInteger bi1 = ((BigintItem)b1).value;
    BigInteger s = bi0.add(bi1);
    helper.setReturnValue(createBigintItem(helper.getRuntimeEngine(), s)); 
  }

  public void sni_diff(RNativeImplHelper helper, RClosureItem self, RObjItem b0, RObjItem b1) {
    BigInteger bi0 = ((BigintItem)b0).value;
    BigInteger bi1 = ((BigintItem)b1).value;
    BigInteger d = bi0.subtract(bi1);
    helper.setReturnValue(createBigintItem(helper.getRuntimeEngine(), d)); 
  }

  public void sni_prod(RNativeImplHelper helper, RClosureItem self, RObjItem b0, RObjItem b1) {
    BigInteger bi0 = ((BigintItem)b0).value;
    BigInteger bi1 = ((BigintItem)b1).value;
    BigInteger p = bi0.multiply(bi1);
    helper.setReturnValue(createBigintItem(helper.getRuntimeEngine(), p)); 
  }

  public void sni_quot(RNativeImplHelper helper, RClosureItem self, RObjItem b0, RObjItem b1) {
    BigInteger bi0 = ((BigintItem)b0).value;
    BigInteger bi1 = ((BigintItem)b1).value;
    if (bi1.equals(BigInteger.ZERO)) {
      RObjItem oArithFailure = sni_sango.SNIlang.createZeroDivException(helper, new Cstr("Divided by zero."), null);
      helper.setException(oArithFailure);
    } else {
      BigInteger q = bi0.divide(bi1);
      helper.setReturnValue(createBigintItem(helper.getRuntimeEngine(), q)); 
    }
  }

  public void sni_div(RNativeImplHelper helper, RClosureItem self, RObjItem b0, RObjItem b1) {
    BigInteger bi0 = ((BigintItem)b0).value;
    BigInteger bi1 = ((BigintItem)b1).value;
    if (bi1.equals(BigInteger.ZERO)) {
      RObjItem oArithFailure = sni_sango.SNIlang.createZeroDivException(helper, new Cstr("Divided by zero."), null);
      helper.setException(oArithFailure);
    } else {
      BigInteger[] qr = bi0.divideAndRemainder(bi1);
      BigintItem[] qrItems = new BigintItem[] { createBigintItem(helper.getRuntimeEngine(), qr[0]), createBigintItem(helper.getRuntimeEngine(), qr[1]) };
      helper.setReturnValue(helper.getTupleItem(qrItems)); 
    }
  }

  public void sni_to_int(RNativeImplHelper helper, RClosureItem self, RObjItem b) {
    BigInteger bi = ((BigintItem)b).value;
    if (bi.compareTo(intMin) >= 0 && bi.compareTo(intMax) <= 0 ) {
      helper.setReturnValue(helper.getIntItem(bi.intValue())); 
    } else {
      helper.setException(sni_sango.SNIlang.createBadArgException(helper, new Cstr("Out of range."), null));
    }
  }

  public void sni_bit_not(RNativeImplHelper helper, RClosureItem self, RObjItem b) {
    BigInteger bi = ((BigintItem)b).value;
    BigInteger n = bi.not();
    helper.setReturnValue(createBigintItem(helper.getRuntimeEngine(), n)); 
  }

  public void sni_bit_and(RNativeImplHelper helper, RClosureItem self, RObjItem b0, RObjItem b1) {
    BigInteger bi0 = ((BigintItem)b0).value;
    BigInteger bi1 = ((BigintItem)b1).value;
    BigInteger a = bi0.and(bi1);
    helper.setReturnValue(createBigintItem(helper.getRuntimeEngine(), a)); 
  }

  public void sni_bit_or(RNativeImplHelper helper, RClosureItem self, RObjItem b0, RObjItem b1) {
    BigInteger bi0 = ((BigintItem)b0).value;
    BigInteger bi1 = ((BigintItem)b1).value;
    BigInteger o = bi0.or(bi1);
    helper.setReturnValue(createBigintItem(helper.getRuntimeEngine(), o)); 
  }

  public void sni_bit_xor(RNativeImplHelper helper, RClosureItem self, RObjItem b0, RObjItem b1) {
    BigInteger bi0 = ((BigintItem)b0).value;
    BigInteger bi1 = ((BigintItem)b1).value;
    BigInteger x = bi0.xor(bi1);
    helper.setReturnValue(createBigintItem(helper.getRuntimeEngine(), x)); 
  }

  public void sni_shift_left(RNativeImplHelper helper, RClosureItem self, RObjItem b, RObjItem i) {
    BigInteger bi = ((BigintItem)b).value;
    int iv = ((RIntItem)i).getValue();
    try {
      BigInteger x = bi.shiftLeft(iv);
      helper.setReturnValue(createBigintItem(helper.getRuntimeEngine(), x)); 
    } catch (Exception ex) {
      RObjItem oArithFailure = sni_sango.SNIlang.createArithErrorException(helper, new Cstr(ex.toString()), null);
      helper.setException(oArithFailure);
    }
  }

  public void sni_shift_right_keep_sign(RNativeImplHelper helper, RClosureItem self, RObjItem b, RObjItem i) {
    BigInteger bi = ((BigintItem)b).value;
    int iv = ((RIntItem)i).getValue();
    try {
      BigInteger x = bi.shiftRight(iv);
      helper.setReturnValue(createBigintItem(helper.getRuntimeEngine(), x)); 
    } catch (Exception ex) {
      RObjItem oArithFailure = sni_sango.SNIlang.createArithErrorException(helper, new Cstr(ex.toString()), null);
      helper.setException(oArithFailure);
    }
  }

  static byte[] intToBytes(int i) {
    byte[] bs = new byte[4];
    bs[3] = (byte)(i & 255);
    i >>= 8;
    bs[2] = (byte)(i & 255);
    i >>= 8;
    bs[1] = (byte)(i & 255);
    i >>= 8;
    bs[0] = (byte)i;
    return bs;
  }

  static byte[] longToBytes(long L) {
    byte[] bs = new byte[8];
    bs[7] = (byte)(L & 255);
    L >>= 8;
    bs[6] = (byte)(L & 255);
    L >>= 8;
    bs[5] = (byte)(L & 255);
    L >>= 8;
    bs[4] = (byte)(L & 255);
    L >>= 8;
    bs[3] = (byte)(L & 255);
    L >>= 8;
    bs[2] = (byte)(L & 255);
    L >>= 8;
    bs[1] = (byte)(L & 255);
    L >>= 8;
    bs[0] = (byte)L;
    return bs;
  }

  public static BigintItem createBigintItem(RuntimeEngine e, int i) {
    return new BigintItem(e, intToBytes(i));
  }

  public static BigintItem createBigintItem(RuntimeEngine e, long L) {
    return new BigintItem(e, longToBytes(L));
  }

  public static BigintItem createBigintItem(RuntimeEngine e, BigInteger bi) {
    return new BigintItem(e, bi);
  }

  public static class BigintItem extends RObjItem {
    BigInteger value;

    BigintItem(RuntimeEngine e, byte[] value) {
      this(e, new BigInteger(value));
    }

    BigintItem(RuntimeEngine e, BigInteger value) {
      super(e);
      this.value = value;
    }

    public BigInteger getValue() { return this.value; }

    public boolean objEquals(RFrame frame, RObjItem item) {
      boolean b;
      if (item == this) {
        b = true;
      } else if (item instanceof BigintItem) {
        BigintItem bi = (BigintItem)item;
	b = this.value.compareTo(bi.value) == 0;
      } else {
        b = false;
      }
      return b;
    }

    public RType.Sig getTsig() {
      return RType.createTsig(new Cstr("sango.num.bigint"), "bigint", 0);
    }

    public Cstr dumpInside() {
      return new Cstr(this.value.toString());
    }
  }
}
