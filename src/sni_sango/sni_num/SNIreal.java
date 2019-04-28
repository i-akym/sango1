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
 
import java.lang.Double;
import org.sango_lang.Cstr;
import org.sango_lang.RArrayItem;
import org.sango_lang.RClosureItem;
import org.sango_lang.RListItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RRealItem;
import org.sango_lang.RuntimeEngine;

public class SNIreal {
  public static SNIreal getInstance(RuntimeEngine e) {
    return new SNIreal();
  }

  public void sni__parse_floating_point(RNativeImplHelper helper, RClosureItem self, RObjItem s) {
    Cstr str = helper.arrayItemToCstr((RArrayItem)s);
    helper.setReturnValue(helper.getRealItem(Double.parseDouble(str.toJavaString())));
  }

  public void sni_ieee754_bin64_bits(RNativeImplHelper helper, RClosureItem self, RObjItem r) {
    double v = ((RRealItem)r).getValue();
    long bs = Double.doubleToLongBits(v);
    RListItem L = helper.getListNilItem();
    for (int i = 0; i < 64; i++) {
      RListItem.Cell lc = helper.createListCellItem();
      lc.tail = L;
      lc.head = helper.getIntItem((int)(bs & 1));
      bs >>>= 1;
      L = lc;
    }
    helper.setReturnValue(L);
  }
}
