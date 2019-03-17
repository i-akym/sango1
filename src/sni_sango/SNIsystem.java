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
 
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.sango_lang.CompileException;
import org.sango_lang.Cstr;
import org.sango_lang.RClosureItem;
import org.sango_lang.RListItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RuntimeEngine;

public class SNIsystem {
  public static SNIsystem getInstance(RuntimeEngine e) {
    return new SNIsystem();
  }

  public void sni_sys_props_from_conf(RNativeImplHelper helper, RClosureItem self) {
    List<File> fileList = helper.getSysLibPaths();
    File sysProps = null;
    File f = null;
    for (int i = 0; sysProps == null && i < fileList.size(); i++) {
      f = new File(new File(fileList.get(i), "etc"), "system.props");
      if (f.exists()) {
        sysProps = f;
      }
    }
    if (f == null) {
      helper.setException(sni_sango.SNIlang.createSysErrorException(helper, new Cstr("'system.props' not found."), null));
      return;
    }
    try {
      helper.setReturnValue(sni_sango.sni_util.SNIprop.readFile(helper, f));
    } catch (CompileException ex) {
      helper.setException(sni_sango.SNIio.createBadDataIOFailureException(helper, new Cstr(ex.toString()), null));
    } catch (IOException ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_sys_props_from_env(RNativeImplHelper helper, RClosureItem self) {
    List<RObjItem> list = new ArrayList<RObjItem>();
    list.add(tempDirProp(helper));
    list.add(userNameProp(helper));
    list.add(userHomeProp(helper));
    helper.setReturnValue(listToListItem(helper, list));
  }

  private static RObjItem tempDirProp(RNativeImplHelper helper) {
    String d = System.getProperty("java.io.tmpdir");
    String s = System.getProperty("file.separator");
    d = d.endsWith(s)? d: d + s;
    return propKeyValue(helper,
      "io.temp_dir",
      sni_sango.sni_util.SNIprop.getCstrPropItem(helper, helper.cstrToArrayItem(new Cstr(d))));
  }

  private static RObjItem userNameProp(RNativeImplHelper helper) {
    String n = System.getProperty("user.name");
    return propKeyValue(helper,
      "user.name",
      sni_sango.sni_util.SNIprop.getCstrPropItem(helper, helper.cstrToArrayItem(new Cstr(n))));
  }

  private static RObjItem userHomeProp(RNativeImplHelper helper) {
    String d = System.getProperty("user.home");
    String s = System.getProperty("file.separator");
    d = d.endsWith(s)? d: d + s;
    return propKeyValue(helper,
      "user.home_dir",
      sni_sango.sni_util.SNIprop.getCstrPropItem(helper, helper.cstrToArrayItem(new Cstr(d))));
  }

  private static RObjItem propKeyValue(RNativeImplHelper helper, String key, RObjItem value) {
    RListItem.Cell c = helper.createListCellItem();
    c.tail = helper.getListNilItem();
    c.head = value;
    return helper.getTupleItem(new RObjItem[] { helper.cstrToArrayItem(new Cstr(key)), c });
  }

  private static RObjItem listToListItem(RNativeImplHelper helper, List<RObjItem> list) {
    RListItem L = helper.getListNilItem();
    for (int i = list.size() - 1; i >= 0; i--) {
      RListItem.Cell c = helper.createListCellItem();
      c.tail = L;
      c.head = list.get(i);
      L = c;
    }
    return L;
  }
}
