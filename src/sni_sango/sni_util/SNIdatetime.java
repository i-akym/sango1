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
package sni_sango.sni_util;
 
import java.util.TimeZone;
import org.sango_lang.Cstr;
import org.sango_lang.RArrayItem;
import org.sango_lang.RClosureItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RuntimeEngine;

public class SNIdatetime {
  public static SNIdatetime getInstance(RuntimeEngine e) {
    return new SNIdatetime();
  }

  public void sni_millisecs_from_1970(RNativeImplHelper helper, RClosureItem self) {
    helper.setReturnValue(sni_sango.sni_num.SNIbigint.createBigintItem(helper.getRuntimeEngine(), System.currentTimeMillis()));
  }

  public void sni_default_time_zone_id(RNativeImplHelper helper, RClosureItem self) {
    helper.setReturnValue(helper.cstrToArrayItem(new Cstr(TimeZone.getDefault().getID())));
  }

  public void sni_time_zone_offset(RNativeImplHelper helper, RClosureItem self, RObjItem tzId, RObjItem millisecs) {
    // Note: If unknown time zone id is specified, GMT is returned.
    TimeZone tz = TimeZone.getTimeZone(helper.arrayItemToCstr((RArrayItem)tzId).toJavaString());
    // Note: Conversion from BigInteger to long does not throw exception.
    long ms = ((sni_sango.sni_num.SNIbigint.BigintItem)millisecs).getValue().longValue();
    helper.setReturnValue(helper.getIntItem(tz.getOffset(ms)));
  }
}
