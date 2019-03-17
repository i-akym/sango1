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
package sni_sango.sni_system;
 
import java.util.List;
import org.sango_lang.Cstr;
import org.sango_lang.RActorHItem;
import org.sango_lang.RArrayItem;
import org.sango_lang.RClosureItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RIntItem;
import org.sango_lang.RListItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RuntimeEngine;
import org.sango_lang.Version;

public class SNIruntime {
  public static SNIruntime getInstance(RuntimeEngine e) {
    return new SNIruntime();
  }

  public void sni_version(RNativeImplHelper helper, RClosureItem self) {
    Version v = helper.getVersion();
    RDataConstr dcVersion = helper.getDataConstr(new Cstr("sango.system.runtime"), "version$");
    RObjItem oMajor = helper.getIntItem(v.major);
    RObjItem oMinor = helper.getIntItem(v.minor);
    RObjItem oMicro = helper.getIntItem(v.micro);
    RObjItem oLevel = sni_sango.SNIlang.getMaybeItem(helper, (v.level != null)? helper.cstrToArrayItem(new Cstr(v.level)): null);
    RObjItem oBuild = helper.getIntItem(v.build);
    RObjItem oFull = helper.cstrToArrayItem(new Cstr(v.full));
    helper.setReturnValue(helper.getStructItem(dcVersion, new RObjItem[] { oMajor, oMinor, oMicro, oLevel, oBuild, oFull }));
  }

  public void sni_prog_name(RNativeImplHelper helper, RClosureItem self) {
    helper.setReturnValue(helper.cstrToArrayItem(helper.getProgName()));
  }

  public void sni_args(RNativeImplHelper helper, RClosureItem self) {
    List<Cstr> args = helper.getArgs();
    RListItem L = helper.getListNilItem();
    for (int i = args.size() - 1; i >= 0; i--) {
      RListItem.Cell c = helper.createListCellItem();
      c.tail = L;
      c.head = helper.cstrToArrayItem(args.get(i));
      L = c;
    }
    helper.setReturnValue(L);
  }

  public void sni_shutdown(RNativeImplHelper helper, RClosureItem self, RObjItem exitCode, RObjItem timeout) {
    helper.getCore().requestShutdown(((RIntItem)exitCode).getValue(), ((RIntItem)timeout).getValue());
  }

  public void sni_terminate_on_abnormal_end(RNativeImplHelper helper, RClosureItem self, RObjItem actor) {
    helper.getCore().terminateOnAbnormalEnd((RActorHItem)actor);
  }

  public void sni_gc(RNativeImplHelper helper, RClosureItem self) {
    System.gc();
  }
}
