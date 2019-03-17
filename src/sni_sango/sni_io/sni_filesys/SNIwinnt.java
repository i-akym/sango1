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
package sni_sango.sni_io.sni_filesys;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

public class SNIwinnt {
  public static SNIwinnt getInstance(RuntimeEngine e) {
    return new SNIwinnt(e);
  }

  sni_sango.sni_io.SNIfilesys commonImpl;

  SNIwinnt(RuntimeEngine e) {
    this.commonImpl = sni_sango.sni_io.SNIfilesys.getInstance(e);
  }

  public void sni_exist_Q_(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    this.commonImpl.sni__exist_Q_(helper, self, pathRepr);
  }

  public void sni_directory_Q_(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    this.commonImpl.sni__directory_Q_(helper, self, pathRepr);
  }

  public void sni_file_Q_(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    this.commonImpl.sni__file_Q_(helper, self, pathRepr);
  }

  public void sni_last_updated(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    this.commonImpl.sni__last_updated(helper, self, pathRepr);
  }

  public void sni_same_path_Q_(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr0, RObjItem pathRepr1) {
    this.commonImpl.sni__same_path_Q_(helper, self, pathRepr0, pathRepr1);
  }

  public void sni_absolute_path(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    this.commonImpl.sni__absolute_path(helper, self, pathRepr);
  }

  public void sni_delete(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    this.commonImpl.sni__delete(helper, self, pathRepr);
  }

  public void sni_move(RNativeImplHelper helper, RClosureItem self, RObjItem pathReprCur, RObjItem pathReprNew) {
    this.commonImpl.sni__move(helper, self, pathReprCur, pathReprNew);
  }

  public void sni_dir_entries(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    this.commonImpl.sni__dir_entries(helper, self, pathRepr);
  }

  public void sni_create_dir(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    this.commonImpl.sni__create_dir(helper, self, pathRepr);
  }

  public void sni_create_dirs(RNativeImplHelper helper, RClosureItem self, RObjItem pathRepr) {
    this.commonImpl.sni__create_dirs(helper, self, pathRepr);
  }

  public void sni_create_temp_file(RNativeImplHelper helper, RClosureItem self, RObjItem prefix, RObjItem suffix, RObjItem dirPathRepr) {
    this.commonImpl.sni__create_temp_file(helper, self, prefix, suffix, dirPathRepr);
  }

  public void sni_create_byte_instream_impl(RNativeImplHelper helper, RClosureItem self, RObjItem file) {
    this.commonImpl.sni__create_byte_instream_impl(helper, self, file);
  }

  public void sni_create_byte_outstream_impl(RNativeImplHelper helper, RClosureItem self, RObjItem file, RObjItem append) {
    this.commonImpl.sni__create_byte_outstream_impl(helper, self, file, append);
  }
}
