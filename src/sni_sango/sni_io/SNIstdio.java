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
package sni_sango.sni_io;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintStream;
import org.sango_lang.Cstr;
import org.sango_lang.Module;
import org.sango_lang.RArrayItem;
import org.sango_lang.RClosureItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RIntItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RStructItem;
import org.sango_lang.RObjItem;
import org.sango_lang.RuntimeEngine;

public class SNIstdio {
  public static final int LF = 10;
  public static final int CR = 13;

  LineInStream cstdin;
  PrintOutStream cstdout;
  PrintOutStream cstderr;

  public static SNIstdio getInstance(RuntimeEngine e) {
    return new SNIstdio();
  }

  SNIstdio() {
    this.cstdin = new LineInStream(System.in);
    this.cstdout = new PrintOutStream(System.out);
    this.cstderr = new PrintOutStream(System.err);
  }
  public void sni_cstdin_org_impl(RNativeImplHelper helper, RClosureItem self) {
    try {
      helper.setReturnValue(newInImpl(helper, self, this.cstdin));
    } catch (Exception ex) {
      System.out.print("sango.io.stdio.cstdin_org_impl error. ");
      System.out.println(ex);
      System.exit(1);
    }
  }

  public void sni_cstdout_org_impl(RNativeImplHelper helper, RClosureItem self) {
    try {
      helper.setReturnValue(newOutImpl(helper, self, this.cstdout));
    } catch (Exception ex) {
      System.out.print("sango.io.stdio.cstdout_org_impl error. ");
      System.out.println(ex);
      System.exit(1);
    }
  }

  public void sni_cstderr_org_impl(RNativeImplHelper helper, RClosureItem self) {
    try {
      helper.setReturnValue(newOutImpl(helper, self, this.cstderr));
    } catch (Exception ex) {
      System.out.print("sango.io.stdio.cstderr_org_impl error. ");
      System.out.println(ex);
      System.exit(1);
    }
  }

  static RObjItem newInImpl(RNativeImplHelper helper, RClosureItem self, LineInStream i) throws Exception {
    RObjItem f0 = helper.createClosureOfNativeImplHere(
      "read_line_f", 0, /* "read_line_f", */ i,
      i.getClass().getMethod(
        "readLine", 
        new Class[] { RNativeImplHelper.class, RClosureItem.class }));
    // RObjItem f1 = helper.createClosureOfNativeImplHere(
      // "try_read_line_f", 0, /* "try_read_line_f", */ i,
      // i.getClass().getMethod(
        // "tryReadLine", 
        // new Class[] { RNativeImplHelper.class, RClosureItem.class }));
    RObjItem f2 = helper.createClosureOfNativeImplHere(
      "close_f", 0, /* "close_f", */ i,
      i.getClass().getMethod(
        "close", 
        new Class[] { RNativeImplHelper.class, RClosureItem.class }));
    RObjItem f3 = helper.createClosureOfNativeImplHere(
      "status_f", 0, /* "status_f", */ i,
      i.getClass().getMethod(
        "status", 
        new Class[] { RNativeImplHelper.class, RClosureItem.class }));
    RObjItem[] fs = new RObjItem[] { f0, /* f1, */ f2, f3 };
    return helper.getStructItem(helper.getDataConstr(new Cstr("sango.io"), "line_instream_h$"), fs);
  }

  public static class LineInStream {
    BufferedReader in;  // null if closed
    RObjItem atEnd;
    RObjItem error;
    StringBuffer text;
    StringBuffer term;
    Integer nextChar;

    LineInStream(InputStream instream) {
      this.in = new BufferedReader(new InputStreamReader(instream));
      this.text = new StringBuffer();
      this.term = new StringBuffer();
    }

    public void readLine(RNativeImplHelper helper, RClosureItem self) {
      if (this.in == null) {
        helper.setException(sni_sango.SNIio.createNotAvailableIOFailureException(helper, new Cstr("Closed."), null));
        return;
      }
      if (this.atEnd != null) {
        helper.setReturnValue(this.atEnd);
        return;
      }
      if (this.error != null) {
        helper.setException(this.error);
        return;
      }
      try {
        boolean cont = true;
        while (cont) {
          if (this.nextChar == null) {
            this.nextChar =  this.in.read();
          }
          int c = this.nextChar;
          this.nextChar = null;
          int termLen = this.term.length();
          if (termLen > 0) {  // actually term == "\r"
            switch (c) {
            case -1:
              helper.setReturnValue(
                sni_sango.SNIio.getInstreamData(helper,
                  sni_sango.SNIcstr.getLine(helper, new Cstr(this.text.toString()),new Cstr(this.term.toString()))));
              this.text = new StringBuffer();
              this.term = new StringBuffer();
              cont = false;
              break;
            case CR:
              helper.setReturnValue(
                sni_sango.SNIio.getInstreamData(helper,
                  sni_sango.SNIcstr.getLine(helper, new Cstr(this.text.toString()),new Cstr(this.term.toString()))));
              this.text = new StringBuffer();
              this.term = new StringBuffer();
              this.nextChar = c;  // restart at current char
              cont = false;
              break;
            case LF:
              this.term.append((char)LF);
              helper.setReturnValue(
                sni_sango.SNIio.getInstreamData(helper,
                  sni_sango.SNIcstr.getLine(helper, new Cstr(this.text.toString()),new Cstr(this.term.toString()))));
              this.text = new StringBuffer();
              this.term = new StringBuffer();
              cont = false;
              break;
            default:
              helper.setReturnValue(
                sni_sango.SNIio.getInstreamData(helper,
                  sni_sango.SNIcstr.getLine(helper, new Cstr(this.text.toString()),new Cstr(this.term.toString()))));
              this.text = new StringBuffer();
              this.term = new StringBuffer();
              this.nextChar = c;  // restart at current char
              cont = false;
              break;
            }
          } else {
            switch (c) {
            case -1:
              if (this.text.length() > 0) {
                helper.setReturnValue(
                  sni_sango.SNIio.getInstreamData(helper,
                    sni_sango.SNIcstr.getLine(helper, new Cstr(this.text.toString()),new Cstr(this.term.toString()))));
                this.text = new StringBuffer();
                this.term = new StringBuffer();
                this.nextChar = c;  // restart at current char
              } else {
                this.atEnd = sni_sango.SNIio.getInstreamAtEnd(helper);
                helper.setReturnValue(this.atEnd);
              }
              cont = false;
              break;
            case CR:
              this.term.append((char)CR);
              break;
            case LF:
              this.term.append((char)LF);
              helper.setReturnValue(
                sni_sango.SNIio.getInstreamData(helper,
                  sni_sango.SNIcstr.getLine(helper, new Cstr(this.text.toString()),new Cstr(this.term.toString()))));
              this.text = new StringBuffer();
              this.term = new StringBuffer();
              cont = false;
              break;
            default:
              this.text.append((char)c);
              break;
            }
          }
        }
      } catch (Exception ex) {
        this.error = sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null);
        helper.setException(this.error);
      }
    }

    public void tryReadLine(RNativeImplHelper helper, RClosureItem self) {
      helper.setException(sni_sango.SNIlang.createUnsupportedException(helper, new Cstr("try_read_line is not supported."), null));
      // if (this.in == null) {
        // helper.setException(sni_sango.SNIio.createNotAvailableIOFailureException(helper, new Cstr("Closed."), null));
        // return;
      // }
      // if (this.atEnd != null) {
        // helper.setReturnValue(this.atEnd);
        // return;
      // }
      // if (this.error != null) {
        // helper.setException(this.error);
        // return;
      // }
      // try {
        // if (this.in.ready()) {
          // String s = this.in.readLine();
          // if (s != null) {
            // RDataConstr dcData = helper.getDataConstr(new Cstr("sango.io"), "instream_data$", 1, "instream_read_res", 1);
            // RArrayItem line = helper.cstrToArrayItem(new Cstr(s));
            // RObjItem data = helper.getStructItem(dcData, new RObjItem[] { line });
            // RDataConstr dcDone = helper.getDataConstr(new Cstr("sango.io"), "done$", 1, "trial_res", 1);
            // RObjItem ret = helper.getStructItem(dcDone, new RObjItem[] { data });
            // helper.setReturnValue(ret);
          // } else {  // not reachable?
            // RDataConstr dcAtEnd = helper.getDataConstr(new Cstr("sango.io"), "instream_at_end$", 0, "instream_read_res", 1);
            // this.atEnd = helper.getStructItem(dcAtEnd, new RObjItem[0]);
            // RDataConstr dcDone = helper.getDataConstr(new Cstr("sango.io"), "done$", 1, "trial_res", 1);
            // RObjItem ret = helper.getStructItem(dcDone, new RObjItem[] { this.atEnd });
            // helper.setReturnValue(ret);
          // }
        // } else if (this.streamReader.ready()) {
          // RDataConstr dcAtEnd = helper.getDataConstr(new Cstr("sango.io"), "instream_at_end$", 0, "instream_read_res", 1);
          // this.atEnd = helper.getStructItem(dcAtEnd, new RObjItem[0]);
          // RDataConstr dcDone = helper.getDataConstr(new Cstr("sango.io"), "done$", 1, "trial_res", 1);
          // RObjItem ret = helper.getStructItem(dcDone, new RObjItem[] { this.atEnd });
          // helper.setReturnValue(ret);
        // } else {
          // RDataConstr dcNotReay = helper.getDataConstr(new Cstr("sango.io"), "not_ready$", 0, "trial_res", 1);
          // RObjItem ret = helper.getStructItem(dcNotReay, new RObjItem[0]);
          // helper.setReturnValue(ret);
        // }
      // } catch (Exception ex) {
        // this.error = sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null);
        // helper.setException(this.error);
      // }
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
        this.error = (this.error == null)?
          sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null):
          this.error;
      }
    }

    public void status(RNativeImplHelper helper, RClosureItem self) {
      RObjItem ret;
      if (this.in == null) {
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

  static RObjItem newOutImpl(RNativeImplHelper helper, RClosureItem self, PrintOutStream o) throws Exception {
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
    RObjItem f5 = helper.createClosureOfNativeImplHere(
      "newline_f", 0, /* "newline_f", */ o,
      o.getClass().getMethod(
        "newline", 
        new Class[] { RNativeImplHelper.class, RClosureItem.class }));
    RObjItem[] fs = new RObjItem[] { f0, f1, f2, f3, f4, f5 };
    return helper.getStructItem(helper.getDataConstr(new Cstr("sango.io"), "print_stream_h$"), fs);
  }

  public static class PrintOutStream {
    PrintStream out;  // null if closed
    RObjItem error;

    PrintOutStream(PrintStream out) {
      this.out = out;
    }

    public void write(RNativeImplHelper helper, RClosureItem self, RObjItem c) {
      if (this.out == null) {
        this.error = sni_sango.SNIio.createNotAvailableIOFailureException(helper, new Cstr("Closed."), null);
        helper.setException(this.error);
        return;
      }
      if (this.error != null) {
        helper.setException(this.error);
        return;
      }
      try {
        this.out.print(Character.toChars(((RIntItem)c).getValue()));
      } catch (Exception ex) {
        this.error = sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null);
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
        helper.setException(sni_sango.SNIio.createNotAvailableIOFailureException(helper, new Cstr("Closed."), null));
        return;
      }
      if (this.error != null) {
        helper.setException(this.error);
        return;
      }
      try {
        for (int i = startPos; i < startPos + count; i++) {
          this.out.print(Character.toChars(((RIntItem)array.getElemAt(i)).getValue()));
        }
      } catch (Exception ex){
        this.error = sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null);
        helper.setException(this.error);
      }
    }

    public void flush(RNativeImplHelper helper, RClosureItem self) {
      if (this.out == null) {
        helper.setException(sni_sango.SNIio.createNotAvailableIOFailureException(helper, new Cstr("Closed."), null));
        return;
      }
      if (this.error != null) {
        helper.setException(this.error);
        return;
      }
      try {
        this.out.flush();
      } catch (Exception ex) {
        this.error = sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null);
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
        this.error = (this.error == null)?
          sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null):
          this.error;
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

    public void newline(RNativeImplHelper helper, RClosureItem self) {
      if (this.out == null) {
        helper.setException(sni_sango.SNIio.createNotAvailableIOFailureException(helper, new Cstr("Closed."), null));
        return;
      }
      if (this.error != null) {
        helper.setException(this.error);
        return;
      }
      try {
        this.out.println();
      } catch (Exception ex) {
        this.error = sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null);
        helper.setException(this.error);
      }
    }
  }
}
