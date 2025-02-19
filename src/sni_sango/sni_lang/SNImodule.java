/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2019 Isao Akiyama                                         *
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
package sni_sango.sni_lang;
 
import org.sango_lang.Cstr;
import org.sango_lang.RActorHItem;
import org.sango_lang.RArrayItem;
import org.sango_lang.RClosureItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RuntimeEngine;

public class SNImodule {
  static final Cstr myModName = new Cstr("sango.lang.module");

  public static SNImodule getInstance(RuntimeEngine e) {
    return new SNImodule();
  }

  public void sni_load_module_on_demand(RNativeImplHelper helper, RClosureItem self, RObjItem moduleName) {
    try {
      helper.getCore().loadModuleOnDemand(helper.arrayItemToCstr((RArrayItem)moduleName));
    } catch (Exception ex) {
      RObjItem e = sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null);
      helper.setException(e);
    }
  }

  public void sni_module_init_actor_h(RNativeImplHelper helper, RClosureItem self, RObjItem moduleName) {
    try {
      RActorHItem a = helper.getCore().getModuleInitActorH(helper.arrayItemToCstr((RArrayItem)moduleName));
      RObjItem r = sni_sango.SNIlang.getMaybeItem(helper, a);
      helper.setReturnValue(r);
    } catch (IllegalArgumentException ex) {
      helper.setException(sni_sango.SNIlang.createBadArgException(helper, new Cstr(ex.toString()), null));
    }
  }
}
