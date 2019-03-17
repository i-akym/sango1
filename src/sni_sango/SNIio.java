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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.sango_lang.Cstr;
import org.sango_lang.Module;
import org.sango_lang.RArrayItem;
import org.sango_lang.RClosureItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RIntItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RStructItem;
import org.sango_lang.RuntimeEngine;

public class SNIio {
  // public static SNIio getInstance(RuntimeEngine e) {
    // return new SNIio();
  // }

  SNIio() {}

  public static RObjItem createBadDataIOFailureException(RNativeImplHelper helper, Cstr msg, RObjItem org) {
    RDataConstr dcBadData = helper.getDataConstr(new Cstr("sango.io"), "bad_data$");
    RObjItem oBadData = helper.getStructItem(dcBadData, new RObjItem[0]);
    return createIOFailureException(helper, oBadData, msg, org);
  }

  public static RObjItem createNotAvailableIOFailureException(RNativeImplHelper helper, Cstr msg, RObjItem org) {
    RDataConstr dcNotAvailable = helper.getDataConstr(new Cstr("sango.io"), "not_available$");
    RObjItem oNotAvailable = helper.getStructItem(dcNotAvailable, new RObjItem[0]);
    return createIOFailureException(helper, oNotAvailable, msg, org);
  }

  public static RObjItem createInterruptedIOFailureException(RNativeImplHelper helper, Cstr msg, RObjItem org) {
    RDataConstr dcInterrupted = helper.getDataConstr(new Cstr("sango.io"), "interrupted$");
    RObjItem oInterrupted = helper.getStructItem(dcInterrupted, new RObjItem[0]);
    return createIOFailureException(helper, oInterrupted, msg, org);
  }

  public static RObjItem createIOErrorIOFailureException(RNativeImplHelper helper, Cstr msg, RObjItem org) {
    RDataConstr dcError = helper.getDataConstr(new Cstr("sango.io"), "error$");
    RObjItem oError = helper.getStructItem(dcError, new RObjItem[0]);
    return createIOFailureException(helper, oError, msg, org);
  }

  public static RObjItem createIOFailureException(RNativeImplHelper helper, RObjItem cause, Cstr msg, RObjItem org) {
    RDataConstr dcIoFail = helper.getDataConstr(new Cstr("sango.io"), "io_failure$");
    return helper.createException(helper.getStructItem(dcIoFail, new RObjItem[] { cause }), msg, org);
  }

  public static RObjItem getInstreamData(RNativeImplHelper helper, RObjItem d) {
    RDataConstr dcAtEnd = helper.getDataConstr(new Cstr("sango.io"), "instream_data$");
    return helper.getStructItem(dcAtEnd, new RObjItem[] { d });
  }

  public static RObjItem getInstreamAtEnd(RNativeImplHelper helper) {
    RDataConstr dcAtEnd = helper.getDataConstr(new Cstr("sango.io"), "instream_at_end$");
    return helper.getStructItem(dcAtEnd, new RObjItem[0]);
  }

  public static RObjItem openByteInstreamFromJavaInputStream(RNativeImplHelper helper, InputStream in) {
    RObjItem r = null;
    try {
      BufferedInputStream bis = new BufferedInputStream(in);
      ByteInputStream i = new ByteInputStream(bis);
      RObjItem f0 = helper.createClosureOfNativeImplHere(
        "read_f", 0, /* "read_f", */ i,
        i.getClass().getMethod(
          "read", 
          new Class[] { RNativeImplHelper.class, RClosureItem.class }));
      RObjItem f1 = helper.createClosureOfNativeImplHere(
        "read_string_f", 1, /* "read_string_f", */ i,
        i.getClass().getMethod(
          "readString", 
          new Class[] { RNativeImplHelper.class, RClosureItem.class, RObjItem.class }));
      RObjItem f2 = sni_sango.SNIlang.getMaybeItem(helper, null);
      RObjItem f3 = f2;
      RObjItem f4 = helper.createClosureOfNativeImplHere(
        "close_f", 0, /* "close_f", */ i,
        i.getClass().getMethod(
          "close", 
          new Class[] { RNativeImplHelper.class, RClosureItem.class }));
      RObjItem f5 = helper.createClosureOfNativeImplHere(
        "status_f", 0, /* "status_f", */ i,
        i.getClass().getMethod(
          "status", 
          new Class[] { RNativeImplHelper.class, RClosureItem.class }));
      RObjItem[] fs = new RObjItem[] { f0, f1, f2, f3, f4, f5 };
      r = helper.getStructItem(helper.getDataConstr(new Cstr("sango.io"), "instream_h$"), fs);
    } catch (NoSuchMethodException ex) {
      System.out.print("sni_sango.SNIio.openByteInstreamFromJavaInputStream error. ");
      System.out.println(ex);
      System.exit(1);
    }
    return r;
  }

  public static RObjItem openByteOutstreamToJavaOutputStream(RNativeImplHelper helper, OutputStream out) {
    RObjItem r = null;
    try {
      BufferedOutputStream bos = new BufferedOutputStream(out);
      ByteOutputStream o = new ByteOutputStream(bos);
      RObjItem f0 = helper.createClosureOfNativeImplHere(
        "write_f", 1, /* "write_f", */ o,
        o.getClass().getMethod(
          "write", 
          new Class[] { RNativeImplHelper.class, RClosureItem.class, RObjItem.class }));
      RObjItem f1 = helper.createClosureOfNativeImplHere(
        "write_string_part_f", 3, /* "write_string_part_f", */ o,
        o.getClass().getMethod(
          "writeStringPart", 
          new Class[] { RNativeImplHelper.class, RClosureItem.class, RObjItem.class, RObjItem.class, RObjItem.class }));
      RObjItem f2 = helper.createClosureOfNativeImplHere(
        "flush_f", 0, /* "flush_f", */ o,
        o.getClass().getMethod(
          "flush", 
          new Class[] { RNativeImplHelper.class, RClosureItem.class }));
      RObjItem f3 = helper.createClosureOfNativeImplHere(
        "close_f", 0, /* "close_f", */ o,
        o.getClass().getMethod(
          "close", 
          new Class[] { RNativeImplHelper.class, RClosureItem.class }));
      RObjItem f4 = helper.createClosureOfNativeImplHere(
        "status_f", 0, /* "status_f", */ o,
        o.getClass().getMethod(
          "status", 
          new Class[] { RNativeImplHelper.class, RClosureItem.class }));
      RObjItem[] fs = new RObjItem[] { f0, f1, f2, f3, f4 };
      r = helper.getStructItem(helper.getDataConstr(new Cstr("sango.io"), "outstream_h$"), fs);
    } catch (NoSuchMethodException ex) {
      System.out.print("sni_sango.SNIio.openByteOutstreamToJavaOutputStream error. ");
      System.out.println(ex);
      System.exit(1);
    }
    return r;
  }

  public static class ByteInputStream {
    BufferedInputStream in;  // null if closed
    boolean eof;
    RObjItem error;

    ByteInputStream(BufferedInputStream in) {
      this.in = in;
    }

    public void read(RNativeImplHelper helper, RClosureItem self) {
      if (this.in == null) {
        this.error = createNotAvailableIOFailureException(helper, new Cstr("Closed."), null);
        helper.setException(this.error);
        return;
      }
      if (this.error != null) {
        helper.setException(this.error);
        return;
      }
      if (this.eof) {
        RDataConstr dcAtEnd = helper.getDataConstr(new Cstr("sango.io"), "instream_at_end$");
        RObjItem objAtEnd = helper.getStructItem(dcAtEnd, new RObjItem[0]);
        helper.setReturnValue(objAtEnd);
        return;
      }
      try {
        int b = this.in.read();
        if (b >= 0) {
          RDataConstr dcData = helper.getDataConstr(new Cstr("sango.io"), "instream_data$");
          RObjItem objData = helper.getStructItem(dcData, new RObjItem[] { helper.getByteItem(b) });
          helper.setReturnValue(objData);
        } else {
          this.eof = true;
          RDataConstr dcAtEnd = helper.getDataConstr(new Cstr("sango.io"), "instream_at_end$");
          RObjItem objAtEnd = helper.getStructItem(dcAtEnd, new RObjItem[0]);
          helper.setReturnValue(objAtEnd);
        }
      } catch (Exception ex) {
        this.error = createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null);
        helper.setException(this.error);
      }
    }

    public void readString(RNativeImplHelper helper, RClosureItem self, RObjItem len) {
      int count = ((RIntItem)len).getValue();
      if (this.in == null) {
        helper.setException(createNotAvailableIOFailureException(helper, new Cstr("Closed."), null));
        return;
      }
      if (this.error != null) {
        helper.setException(this.error);
        return;
      }
      if (this.eof) {
        RDataConstr dcAtEnd = helper.getDataConstr(new Cstr("sango.io"), "instream_at_end$");
        RObjItem objAtEnd = helper.getStructItem(dcAtEnd, new RObjItem[0]);
        helper.setReturnValue(objAtEnd);
        return;
      }
      try {
        byte[] bs = new byte[count];
        int c = this.in.read(bs);
        if (c >= 0) {
          RArrayItem a = helper.createArrayItem(c);
          for (int i = 0; i < c; i++) {
            a.setElemAt(i, helper.getByteItem(byteToUnsignedInt(bs[i])));
          }
          RDataConstr dcData = helper.getDataConstr(new Cstr("sango.io"), "instream_data$");
          RObjItem objData = helper.getStructItem(dcData, new RObjItem[] { a });
          helper.setReturnValue(objData);
        } else {
          this.eof = true;
          RDataConstr dcAtEnd = helper.getDataConstr(new Cstr("sango.io"), "instream_at_end$");
          RObjItem objAtEnd = helper.getStructItem(dcAtEnd, new RObjItem[0]);
          helper.setReturnValue(objAtEnd);
        }
      } catch (Exception ex){
        this.error = createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null);
        helper.setException(this.error);
      }
    }

    public void close(RNativeImplHelper helper, RClosureItem self) {
      if (this.in == null) {
        return;
      }
      // even if error already occurred, try closing
      try {
        this.in.close();
        this.in = null;
        this.error = null;
      } catch (Exception ex) {
        this.error = (this.error == null)? createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null): this.error;
        helper.setException(this.error);
      }
    }

    public void status(RNativeImplHelper helper, RClosureItem self) {
      RObjItem ret;
      if (this.in == null) {
        RDataConstr dcClosed = helper.getDataConstr(new Cstr("sango.io"), "instream_closed$");
        ret = helper.getStructItem(dcClosed, new RObjItem[0]);
      } else if (this.error != null) {
        RDataConstr dcError = helper.getDataConstr(new Cstr("sango.io"), "instream_error$");
        ret = helper.getStructItem(dcError, new RObjItem[] { this.error });
      } else if (this.eof) {
        RDataConstr dcEof = helper.getDataConstr(new Cstr("sango.io"), "instream_eof$");
        ret = helper.getStructItem(dcEof, new RObjItem[0]);
      } else {
        RDataConstr dcOpen = helper.getDataConstr(new Cstr("sango.io"), "instream_open$");
        ret = helper.getStructItem(dcOpen, new RObjItem[0]);
      }
      helper.setReturnValue(ret);
    }
  }

  public static class ByteOutputStream {
    BufferedOutputStream out;  // null if closed
    RObjItem error;

    ByteOutputStream(BufferedOutputStream out) {
      this.out = out;
    }

    public void write(RNativeImplHelper helper, RClosureItem self, RObjItem b) {
      if (this.out == null) {
        this.error = createNotAvailableIOFailureException(helper, new Cstr("Closed."), null);
        helper.setException(this.error);
        return;
      }
      if (this.error != null) {
        helper.setException(this.error);
        return;
      }
      try {
        this.out.write((byte)((RIntItem)b).getValue());
      } catch (Exception ex) {
        this.error = createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null);
        helper.setException(this.error);
      }
    }

    public void writeStringPart(RNativeImplHelper helper, RClosureItem self, RObjItem str, RObjItem start, RObjItem len) {
      int startPos = ((RIntItem)start).getValue();
      int count = ((RIntItem)len).getValue();
      RArrayItem array = (RArrayItem)str;
      int arrayLen = array.getElemCount();
      if (startPos < 0 || count < 0 || startPos + count > arrayLen) {
        // TODO: improve for case (startPos + count) exceeds int max
        helper.setException(sni_sango.SNIlang.createBadArgException(helper, new Cstr("Invalid start position and/or length."), null));
        return;
      }
      if (this.out == null) {
        helper.setException(createNotAvailableIOFailureException(helper, new Cstr("Closed."), null));
        return;
      }
      if (this.error != null) {
        helper.setException(this.error);
        return;
      }
      try {
        for (int i = startPos; i < startPos + count; i++) {
          this.out.write((byte)((RIntItem)array.getElemAt(i)).getValue());
        }
      } catch (Exception ex){
        this.error = createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null);
        helper.setException(this.error);
      }
    }

    public void flush(RNativeImplHelper helper, RClosureItem self) {
      if (this.out == null) {
        helper.setException(createNotAvailableIOFailureException(helper, new Cstr("Closed."), null));
        return;
      }
      if (this.error != null) {
        helper.setException(this.error);
        return;
      }
      try {
        this.out.flush();
      } catch (Exception ex) {
        this.error = createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null);
        helper.setException(this.error);
      }
    }

    public void close(RNativeImplHelper helper, RClosureItem self) {
      if (this.out == null) {
        return;
      }
      // even if error already occurred, try closing
      try {
        // this.out.flush();  // executed in close()
        this.out.close();
        this.out = null;
        this.error = null;
      } catch (Exception ex) {
        this.error = (this.error == null)? createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null): this.error;
        helper.setException(this.error);
      }
    }

    public void status(RNativeImplHelper helper, RClosureItem self) {
      RObjItem ret;
      if (this.out == null) {
        RDataConstr dcClosed = helper.getDataConstr(new Cstr("sango.io"), "outstream_closed$");
        ret = helper.getStructItem(dcClosed, new RObjItem[0]);
      } else if (this.error != null) {
        RDataConstr dcError = helper.getDataConstr(new Cstr("sango.io"), "outstream_error$");
        ret = helper.getStructItem(dcError, new RObjItem[] { this.error });
      } else {
        RDataConstr dcOpen = helper.getDataConstr(new Cstr("sango.io"), "outstream_open$");
        ret = helper.getStructItem(dcOpen, new RObjItem[0]);
      }
      helper.setReturnValue(ret);
    }
  }

  public static int byteToUnsignedInt(byte b) {
    return (b >= 0)? b: b + 256;
  }
}
