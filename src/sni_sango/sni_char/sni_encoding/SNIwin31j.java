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
package sni_sango.sni_char.sni_encoding;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import org.sango_lang.Cstr;
import org.sango_lang.Module;
import org.sango_lang.RArrayItem;
import org.sango_lang.RClosureItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RIntItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RuntimeEngine;
import org.sango_lang.RStructItem;

public class SNIwin31j {
  public static SNIwin31j getInstance(RuntimeEngine e) {
    return new SNIwin31j();
  }

  public void sni_start_encoder_impl(RNativeImplHelper helper, RClosureItem self) {
    try {
      Encoder e = new Encoder();
      RClosureItem f0 = helper.createClosureOfNativeImplHere(
        "encode_f", 1, /* "encode_f", */ e,
        e.getClass().getMethod(
          "encode", 
          new Class[] { RNativeImplHelper.class, RClosureItem.class, RObjItem.class }));
      RClosureItem f1 = helper.createClosureOfNativeImplHere(
        "end_f", 0, /* "end_f", */ e,
        e.getClass().getMethod(
          "end", 
          new Class[] { RNativeImplHelper.class, RClosureItem.class }));
      helper.setReturnValue(sni_sango.sni_char.SNIencoding.createEncoderHItem(helper, f0, f1));
    } catch (NoSuchMethodException ex) {
      System.out.print("sango.char.encoding.start_encoder error. ");
      System.out.println(ex);
      System.exit(1);
    }
  }

  public static class Encoder {
    Encoder() {}

    public void encode(RNativeImplHelper helper, RClosureItem self, RObjItem c) {
      String s = new String(new int[] { ((RIntItem)c).getValue() }, 0, 1);
      byte[] bz;
      try {
        bz = s.getBytes("MS932");
      } catch (Exception ex) {
        throw new RuntimeException("Encoding error. " + ex.toString());
      }
      RArrayItem a = helper.createArrayItem(bz.length);
      for (int i = 0; i < bz.length; i++) {
        a.setElemAt(i, helper.getByteItem(signedByteToUnsignedInt(bz[i])));
      }
      helper.setReturnValue(a);
    }

    public void end(RNativeImplHelper helper, RClosureItem self) {
      helper.setReturnValue(helper.createArrayItem(0));
    }
  }

  public void sni_start_decoder_impl(RNativeImplHelper helper, RClosureItem self, RObjItem props) {
    try {
      Decoder d = new Decoder(helper, props);
      RClosureItem f0 = helper.createClosureOfNativeImplHere(
        "decode_f", 1, /* "decode_f", */ d,
        d.getClass().getMethod(
          "decode", 
          new Class[] { RNativeImplHelper.class, RClosureItem.class, RObjItem.class }));
      RClosureItem f1 = helper.createClosureOfNativeImplHere(
        "end_f", 0, /* "end_f", */ d,
        d.getClass().getMethod(
          "end", 
          new Class[] { RNativeImplHelper.class, RClosureItem.class }));
      helper.setReturnValue(sni_sango.sni_char.SNIencoding.createDecoderHItem(helper, f0, f1));
    } catch (NoSuchMethodException ex) {
      System.out.print("sango.char.encoding.start_decoder error. ");
      System.out.println(ex);
      System.exit(1);
    }
  }

  public static class Decoder {
    CharsetDecoder impl;
    ByteBuffer in;
    CharBuffer out;

    Decoder(RNativeImplHelper helper, RObjItem props) {
      this.impl = Charset.forName("MS932").newDecoder().reset();
      this.in = ByteBuffer.allocate(5);
      this.out = CharBuffer.allocate(5);
      RStructItem p = (RStructItem)props;
      RStructItem errorSeqAction = (RStructItem)p.getFieldAt(0);
      if (errorSeqAction.getDataConstr().getName().equals("ignore_decode_error_input$")) {
        this.impl.onMalformedInput(CodingErrorAction.IGNORE);
      } else if (errorSeqAction.getDataConstr().getName().equals("recover_decoding$")) {
        this.impl.onMalformedInput(CodingErrorAction.REPLACE);
      } else {
        this.impl.onMalformedInput(CodingErrorAction.REPORT);
      }
      RStructItem mappingErrorAction = (RStructItem)p.getFieldAt(1);
      if (mappingErrorAction.getDataConstr().getName().equals("ignore_decode_error_input$")) {
        this.impl.onUnmappableCharacter(CodingErrorAction.IGNORE);
      } else if (mappingErrorAction.getDataConstr().getName().equals("recover_decoding$")) {
        this.impl.onUnmappableCharacter(CodingErrorAction.REPLACE);
      } else {
        this.impl.onUnmappableCharacter(CodingErrorAction.REPORT);
      }
      RArrayItem replace = (RArrayItem)p.getFieldAt(2);
      Cstr replaceWith = helper.arrayItemToCstr(replace);
      this.impl.replaceWith(replaceWith.toJavaString());
    }

    public synchronized void decode(RNativeImplHelper helper, RClosureItem self, RObjItem b) {
      byte bb = (byte)((RIntItem)b).getValue();
      this.in.put(bb);
      this.in.flip();
      CoderResult r = this.impl.decode(this.in, this.out, false);
      if (r.isError()) {
        helper.setException(sni_sango.sni_char.SNIencoding.createDecodeFailureException(helper, new Cstr("Decoding failed."), null));
        return;
      }
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < this.out.position(); i++) {
        buf.append(this.out.get(i));
      }
      this.in.compact();
      this.out.clear();
      helper.setReturnValue(helper.cstrToArrayItem(new Cstr(buf.toString())));
    }

    public synchronized void end(RNativeImplHelper helper, RClosureItem self) {
      this.in.flip();
      CoderResult r = this.impl.decode(this.in, this.out, true);
      if (r.isError()) {
        helper.setException(sni_sango.sni_char.SNIencoding.createDecodeFailureException(helper, new Cstr("Decoding failed."), null));
        return;
      }
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < this.out.position(); i++) {
        buf.append(this.out.get(i));
      }
      this.in.compact();
      this.out.clear();
      helper.setReturnValue(helper.cstrToArrayItem(new Cstr(buf.toString())));
    }
  }

  static int signedByteToUnsignedInt(byte b) {
    return (b < 0)? b + 256: b;
  }
}
