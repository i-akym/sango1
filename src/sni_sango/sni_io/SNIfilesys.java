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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.sango_lang.Cstr;
import org.sango_lang.Module;
import org.sango_lang.RArrayItem;
import org.sango_lang.RClosureItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RIntItem;
import org.sango_lang.RListItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RStructItem;
import org.sango_lang.RuntimeEngine;

public class SNIfilesys {
  public static SNIfilesys getInstance(RuntimeEngine e) {
    return new SNIfilesys();
  }

  SNIfilesys() {}

  public void sni__exist_Q_(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    try {
      File f = new File(helper.arrayItemToCstr((RArrayItem)pathRepr).toJavaString());
      helper.setReturnValue(helper.getBoolItem(f.exists()));
    } catch (SecurityException ex) {
      helper.setException(sni_sango.SNIlang.createSecurityErrorException(helper, new Cstr("Permission violation."), null));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni__directory_Q_(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    try {
      File f = new File(helper.arrayItemToCstr((RArrayItem)pathRepr).toJavaString());
      helper.setReturnValue(helper.getBoolItem(f.isDirectory()));
    } catch (SecurityException ex) {
      helper.setException(sni_sango.SNIlang.createSecurityErrorException(helper, new Cstr("Permission violation."), null));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni__file_Q_(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    try {
      File f = new File(helper.arrayItemToCstr((RArrayItem)pathRepr).toJavaString());
      helper.setReturnValue(helper.getBoolItem(f.isFile()));
    } catch (SecurityException ex) {
      helper.setException(sni_sango.SNIlang.createSecurityErrorException(helper, new Cstr("Permission violation."), null));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni__last_updated(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    try {
      File f = new File(helper.arrayItemToCstr((RArrayItem)pathRepr).toJavaString());
      helper.setReturnValue(sni_sango.sni_num.SNIbigint.createBigintItem(helper.getRuntimeEngine(), f.lastModified()));
    } catch (SecurityException ex) {
      helper.setException(sni_sango.SNIlang.createSecurityErrorException(helper, new Cstr("Permission violation."), null));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni__same_path_Q_(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr0, RObjItem pathRepr1) {
    try {
      Path p0 = Paths.get(helper.arrayItemToCstr((RArrayItem)pathRepr0).toJavaString());
      Path p1 = Paths.get(helper.arrayItemToCstr((RArrayItem)pathRepr1).toJavaString());
      helper.setReturnValue(helper.getBoolItem(Files.isSameFile(p0, p1)));
    } catch (SecurityException ex) {
      helper.setException(sni_sango.SNIlang.createSecurityErrorException(helper, new Cstr("Permission violation."), null));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni__absolute_path(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    try {
      File f = new File(helper.arrayItemToCstr((RArrayItem)pathRepr).toJavaString());
      helper.setReturnValue(helper.cstrToArrayItem(new Cstr(f.getAbsolutePath())));
    } catch (SecurityException ex) {
      helper.setException(sni_sango.SNIlang.createSecurityErrorException(helper, new Cstr("Permission violation."), null));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni__delete(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    try {
      Path p = Paths.get(helper.arrayItemToCstr((RArrayItem)pathRepr).toJavaString());
      Files.delete(p);
    } catch (SecurityException ex) {
      helper.setException(sni_sango.SNIlang.createSecurityErrorException(helper, new Cstr("Permission violation."), null));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni__move(RNativeImplHelper helper, RClosureItem self, RObjItem pathReprCur, RObjItem pathReprNew) {
    try {
      Path p0 = Paths.get(helper.arrayItemToCstr((RArrayItem)pathReprCur).toJavaString());
      Path p1 = Paths.get(helper.arrayItemToCstr((RArrayItem)pathReprNew).toJavaString());
      String p = Files.move(p0, p1).toString();
      helper.setReturnValue(helper.cstrToArrayItem(new Cstr(p)));
    } catch (SecurityException ex) {
      helper.setException(sni_sango.SNIlang.createSecurityErrorException(helper, new Cstr("Permission violation."), null));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni__dir_entries(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    try {
      File dir = new File(helper.arrayItemToCstr((RArrayItem)pathRepr).toJavaString());
      String[] es = dir.list();
      if (es != null) {
        RListItem L = helper.getListNilItem();
        for (int i = 0; i < es.length; i++) {
          RListItem.Cell c = helper.createListCellItem();
          c.tail = L;
          c.head = helper.cstrToArrayItem(new Cstr(es[i]));
          L = c;
        }
        helper.setReturnValue(L);
      } else {
        helper.setException(sni_sango.SNIlang.createBadArgException(helper, new Cstr("Not directory."), null));
      }
    } catch (SecurityException ex) {
      helper.setException(sni_sango.SNIlang.createSecurityErrorException(helper, new Cstr("Permission violation."), null));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni__create_dir(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    try {
      Path p = Paths.get(helper.arrayItemToCstr((RArrayItem)pathRepr).toJavaString());
      String d = Files.createDirectory(p).toString();
      helper.setReturnValue(helper.cstrToArrayItem(new Cstr(d)));
    } catch (SecurityException ex) {
      helper.setException(sni_sango.SNIlang.createSecurityErrorException(helper, new Cstr("Permission violation."), null));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni__create_dirs(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    try {
      Path p = Paths.get(helper.arrayItemToCstr((RArrayItem)pathRepr).toJavaString());
      String d = Files.createDirectories(p).toString();
      helper.setReturnValue(helper.cstrToArrayItem(new Cstr(d)));
    } catch (SecurityException ex) {
      helper.setException(sni_sango.SNIlang.createSecurityErrorException(helper, new Cstr("Permission violation."), null));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni__create_temp_file(RNativeImplHelper helper, RClosureItem self, RObjItem prefix, RObjItem suffix, RObjItem dirPathRepr) {
    try {
      String p = helper.arrayItemToCstr((RArrayItem)prefix).toJavaString();
      String s = helper.arrayItemToCstr((RArrayItem)suffix).toJavaString();
      File d = new File(helper.arrayItemToCstr((RArrayItem)dirPathRepr).toJavaString());
      File tmp = File.createTempFile(p, s, d);
      helper.setReturnValue(helper.cstrToArrayItem(new Cstr(tmp.getCanonicalPath())));
    } catch (SecurityException ex) {
      helper.setException(sni_sango.SNIlang.createSecurityErrorException(helper, new Cstr("Permission violation."), null));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni__create_byte_instream_impl(RNativeImplHelper helper, RClosureItem self, RObjItem file) {
    try {
      String fn= helper.arrayItemToCstr((RArrayItem)file).toJavaString();
      FileInputStream fis = new FileInputStream(fn);
      helper.setReturnValue(sni_sango.SNIio.openByteInstreamFromJavaInputStream(helper, fis));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni__create_byte_outstream_impl(RNativeImplHelper helper, RClosureItem self, RObjItem file, RObjItem append) {
    try {
      String fn = helper.arrayItemToCstr((RArrayItem)file).toJavaString();
      boolean a = helper.boolItemToBoolean((RStructItem)append);
      FileOutputStream fos = new FileOutputStream(fn, a);
      helper.setReturnValue(sni_sango.SNIio.openByteOutstreamToJavaOutputStream(helper, fos));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }
}
