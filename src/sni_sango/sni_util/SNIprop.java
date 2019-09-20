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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import org.sango_lang.CompileException;
import org.sango_lang.Cstr;
import org.sango_lang.Lex;
import org.sango_lang.LToken;
import org.sango_lang.Module;
import org.sango_lang.RArrayItem;
import org.sango_lang.RClosureItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RListItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RStructItem;
import org.sango_lang.RuntimeEngine;

public class SNIprop {
  public static SNIprop getInstance(RuntimeEngine e) {
    return new SNIprop();
  }

  public void sni_read_file(RNativeImplHelper helper, RClosureItem self, RObjItem file) {
    Cstr fn = helper.arrayItemToCstr((RArrayItem)file);
    try {
      helper.setReturnValue(readFile(helper, new File(fn.toJavaString())));
    } catch (CompileException ex) {
      helper.setException(sni_sango.SNIio.createBadDataIOFailureException(helper, new Cstr(ex.toString()), null));
    } catch (IOException ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public static RObjItem readFile(RNativeImplHelper helper, File file) throws CompileException, IOException {
    RObjItem kvs = null;
    LineNumberReader r = null;
    try {
      r = new LineNumberReader(new FileReader(file));
      r.setLineNumber(1);
      Lex lex = new Lex(new Cstr(file.toString()), r);
      kvs = parse(helper, lex);
    } finally {
      if (r != null) { try { r.close(); } catch (Exception ex) {} }
    }
    return kvs;
  }

  private static RObjItem parse(RNativeImplHelper helper, Lex lex) throws CompileException, IOException {
    StringBuffer emsg;
    List<RObjItem> kvs = new ArrayList<RObjItem>();
    int s = 0;
    RObjItem keyItem = null;
    List<RObjItem> values = null;
    LToken t;
    while ((t = lex.readToken()) != null) {
      switch (s) {
      case 0:
        switch (t.tag) {
        case LToken.CSTR:
          keyItem = helper.cstrToArrayItem(t.cstrValue); s = 1; break;
        default:
          emsg = new StringBuffer();
          emsg.append("Invalid key '");
          emsg.append(t.token);
          emsg.append("' at ");
          emsg.append(lex.getSrcName().toJavaString());
          emsg.append(":L");
          emsg.append(t.lineNum);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        break;
      case 1:
        switch (t.tag) {
        case LToken.EQ:
          values = new ArrayList<RObjItem>(); s = 2; break;
        default:
          emsg = new StringBuffer();
          emsg.append("'=' missing at ");
          emsg.append(lex.getSrcName().toJavaString());
          emsg.append(":L");
          emsg.append(t.lineNum);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        break;
      case 2:
        switch (t.tag) {
        case LToken.INT:
          values.add(getIntPropItem(helper, t.intValue)); s = 3; break;
        case LToken.REAL:
          values.add(getRealPropItem(helper, t.realValue)); s = 3; break;
        case LToken.CHAR:
          values.add(getCharPropItem(helper, t.charValue)); s = 3; break;
        case LToken.CSTR:
          values.add(getCstrPropItem(helper, t.cstrValue)); s = 3; break;
        case LToken.SEM:
          kvs.add(helper.getTupleItem(new RObjItem[] { keyItem, helper.listToListItem(values) }));
          s = 0;
          keyItem = null;
          values = null;
          break;
        default:
          emsg = new StringBuffer();
          emsg.append("Invalid value '");
          emsg.append(t.token);
          emsg.append("' at ");
          emsg.append(lex.getSrcName().toJavaString());
          emsg.append(":L");
          emsg.append(t.lineNum);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        break;
      case 3:
        switch (t.tag) {
        case LToken.COMMA:
          s = 4; break;
        case LToken.SEM:
          kvs.add(helper.getTupleItem(new RObjItem[] { keyItem, helper.listToListItem(values) }));
          s = 0;
          keyItem = null;
          values = null;
          break;
        default:
          emsg = new StringBuffer();
          emsg.append("',' missing at ");
          emsg.append(lex.getSrcName().toJavaString());
          emsg.append(":L");
          emsg.append(t.lineNum);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        break;
      case 4:
        switch (t.tag) {
        case LToken.INT:
          values.add(getIntPropItem(helper, t.intValue)); s = 3; break;
        case LToken.REAL:
          values.add(getRealPropItem(helper, t.realValue)); s = 3; break;
        case LToken.CHAR:
          values.add(getCharPropItem(helper, t.charValue)); s = 3; break;
        case LToken.CSTR:
          values.add(getCstrPropItem(helper, t.cstrValue)); s = 3; break;
        default:
          emsg = new StringBuffer();
          emsg.append("Invalid value '");
          emsg.append(t.token);
          emsg.append("' at ");
          emsg.append(lex.getSrcName().toJavaString());
          emsg.append(":L");
          emsg.append(t.lineNum);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        break;
      default:
        throw new RuntimeException("Internal error.");
      }
    }
    if (s != 0) {
      emsg = new StringBuffer();
      emsg.append("Last definition is not completed.");
      throw new CompileException(emsg.toString());
    }
    return helper.listToListItem(kvs);
  }

  public static RStructItem getIntPropItem(RNativeImplHelper helper, int i) {
    return getIntPropItem(helper, helper.getIntItem(i));
  }

  public static RStructItem getIntPropItem(RNativeImplHelper helper, RObjItem i) {
    RDataConstr dc = helper.getDataConstr(new Cstr("sango.util.prop"), "int_prop$");
    return helper.getStructItem(dc, new RObjItem[] { i });
  }

  public static RStructItem getRealPropItem(RNativeImplHelper helper, double r) {
    return getRealPropItem(helper, helper.getRealItem(r));
  }

  public static RStructItem getRealPropItem(RNativeImplHelper helper, RObjItem r) {
    RDataConstr dc = helper.getDataConstr(new Cstr("sango.util.prop"), "real_prop$");
    return helper.getStructItem(dc, new RObjItem[] { r });
  }

  public static RStructItem getCharPropItem(RNativeImplHelper helper, int c) {
    return getCharPropItem(helper, helper.getCharItem(c));
  }

  public static RStructItem getCharPropItem(RNativeImplHelper helper, RObjItem c) {
    RDataConstr dc = helper.getDataConstr(new Cstr("sango.util.prop"), "char_prop$");
    return helper.getStructItem(dc, new RObjItem[] { c });
  }

  public static RStructItem getCstrPropItem(RNativeImplHelper helper, Cstr s) {
    return getCstrPropItem(helper, helper.cstrToArrayItem(s));
  }

  public static RStructItem getCstrPropItem(RNativeImplHelper helper, RObjItem s) {
    RDataConstr dc = helper.getDataConstr(new Cstr("sango.util.prop"), "cstr_prop$");
    return helper.getStructItem(dc, new RObjItem[] { s });
  }
}
