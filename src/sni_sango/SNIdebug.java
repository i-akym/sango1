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
 
import java.util.ArrayList;
import java.util.List;
import org.sango_lang.Cstr;
import org.sango_lang.Module;
import org.sango_lang.RClosureItem;
import org.sango_lang.RExcInfoItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RListItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RStructItem;
import org.sango_lang.RuntimeEngine;

 public class SNIdebug {
  static final Cstr myModName = new Cstr("sango.debug");

  public static SNIdebug getInstance(RuntimeEngine e) {
    return new SNIdebug();
  }

  public void sni_repr(RNativeImplHelper helper, RClosureItem self, RObjItem x) {
    x.debugRepr(helper, self);
  }

  public void sni_check_assertion_Q_(RNativeImplHelper helper, RClosureItem self) {
    // tentatively, always true$
    RObjItem r = helper.getBoolItem(true);
    helper.setReturnValue(r); 
  }

  public void sni_call_stack(RNativeImplHelper helper, RClosureItem self, RObjItem excInfo) {
    RExcInfoItem ei = (RExcInfoItem)excInfo;
    List<RExcInfoItem.FrameSnapshot> cs = ei.getCallStack();
    List<RStructItem> fs = new ArrayList<RStructItem>();
    for (int i = 0; i < cs.size(); i++) {
      RExcInfoItem.FrameSnapshot f = cs.get(i);
      RObjItem iMod = helper.cstrToArrayItem(f.impl.getModule().getName());
      RObjItem iName = helper.cstrToArrayItem(new Cstr(f.impl.getName()));  // String -> Cstr -> ArrayItem
      String loc = f.impl.getSrcLoc(f.codeIndex);
      RObjItem iLoc = sni_sango.SNIlang.getMaybeItem(helper, (loc != null)? helper.cstrToArrayItem(new Cstr(loc)): null);
      RObjItem iTrans = helper.getBoolItem(f.transferred);
      RDataConstr dcFrame = helper.getDataConstr(myModName, "frame$");
      RStructItem iFrame = helper.getStructItem(dcFrame, new RObjItem[] { iMod, iName, iLoc, iTrans });
      fs.add(iFrame);
    }
    helper.setReturnValue(helper.listToListItem(fs)); 
  }
}
