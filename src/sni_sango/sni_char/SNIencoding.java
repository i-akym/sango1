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
package sni_sango.sni_char;

import org.sango_lang.Cstr;
import org.sango_lang.RClosureItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RuntimeEngine;

public class SNIencoding {
  // public static SNIencoding getInstance(RuntimeEngine e) {
    // return new SNIencoding();
  // }

  SNIencoding() {}

  public static RObjItem createEncoderHItem(RNativeImplHelper helper, RClosureItem encodeF, RClosureItem endF) {
    RDataConstr dcEncoderH = helper.getDataConstr(new Cstr("sango.char.encoding"), "encoder_h$");
    return helper.getStructItem(dcEncoderH, new RObjItem[] { encodeF, endF });
  }

  public static RObjItem createDecoderHItem(RNativeImplHelper helper, RClosureItem decodeF, RClosureItem endF) {
    RDataConstr dcDecoderH = helper.getDataConstr(new Cstr("sango.char.encoding"), "decoder_h$");
    return helper.getStructItem(dcDecoderH, new RObjItem[] { decodeF, endF });
  }

  public static RObjItem createDecodeFailureException(RNativeImplHelper helper, Cstr msg, RObjItem org) {
    RDataConstr dcDecodeFail = helper.getDataConstr(new Cstr("sango.char.encoding"), "decode_failure$");
    RObjItem oDecodeFail = helper.getStructItem(dcDecodeFail, new RObjItem[0]);
    return helper.createException(oDecodeFail, msg, org);
  }
}
