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
package sni_sango.sni_arith;
 
import org.sango_lang.RClosureItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RRealItem;
import org.sango_lang.RuntimeEngine;

public class SNImath {
  public static SNImath getInstance(RuntimeEngine e) {
    return new SNImath();
  }

  public void sni_real_pow(RNativeImplHelper helper, RClosureItem self, RObjItem r0, RObjItem r1) {
    double d0 = ((RRealItem)r0).getValue();
    double d1 = ((RRealItem)r1).getValue();
    helper.setReturnValue(helper.getRealItem(Math.pow(d0, d1)));
  }

  public void sni_exp(RNativeImplHelper helper, RClosureItem self, RObjItem r) {
    double d = ((RRealItem)r).getValue();
    helper.setReturnValue(helper.getRealItem(Math.exp(d)));
  }

  public void sni_log(RNativeImplHelper helper, RClosureItem self, RObjItem r) {
    double d = ((RRealItem)r).getValue();
    helper.setReturnValue(helper.getRealItem(Math.log(d)));
  }

  public void sni_log10(RNativeImplHelper helper, RClosureItem self, RObjItem r) {
    double d = ((RRealItem)r).getValue();
    helper.setReturnValue(helper.getRealItem(Math.log10(d)));
  }

  public void sni_sqrt(RNativeImplHelper helper, RClosureItem self, RObjItem r) {
    double d = ((RRealItem)r).getValue();
    helper.setReturnValue(helper.getRealItem(Math.sqrt(d)));
  }

  public void sni_sin(RNativeImplHelper helper, RClosureItem self, RObjItem r) {
    double d = ((RRealItem)r).getValue();
    helper.setReturnValue(helper.getRealItem(Math.sin(d)));
  }

  public void sni_cos(RNativeImplHelper helper, RClosureItem self, RObjItem r) {
    double d = ((RRealItem)r).getValue();
    helper.setReturnValue(helper.getRealItem(Math.cos(d)));
  }

  public void sni_tan(RNativeImplHelper helper, RClosureItem self, RObjItem r) {
    double d = ((RRealItem)r).getValue();
    helper.setReturnValue(helper.getRealItem(Math.tan(d)));
  }

  public void sni_asin(RNativeImplHelper helper, RClosureItem self, RObjItem r) {
    double d = ((RRealItem)r).getValue();
    helper.setReturnValue(helper.getRealItem(Math.asin(d)));
  }

  public void sni_acos(RNativeImplHelper helper, RClosureItem self, RObjItem r) {
    double d = ((RRealItem)r).getValue();
    helper.setReturnValue(helper.getRealItem(Math.acos(d)));
  }

  public void sni_atan(RNativeImplHelper helper, RClosureItem self, RObjItem r) {
    double d = ((RRealItem)r).getValue();
    helper.setReturnValue(helper.getRealItem(Math.atan(d)));
  }
}
