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
package org.sango_lang;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class RModMgr implements PDefDict.DefDictGetter {
  RuntimeEngine theEngine;
  Map<Cstr, Module> modDict;
  Map<Cstr, RModule> rmodDict;
  Map<Cstr, PDefDict> defDictDict;
  RModule mainRMod;

  RModMgr(RuntimeEngine e) {
    this.theEngine = e;
    this.defDictDict = new HashMap<Cstr, PDefDict>();
  }

  void loadMain(Cstr modName) throws IOException, FormatException {
    Map<Cstr, Module> loaded = new HashMap<Cstr, Module>();
    List<Cstr> loadQueue = new ArrayList<Cstr>();
    loadQueue.add(modName);
    loadQueue.add(Module.MOD_LANG);
    loadQueue.add(new Cstr("sango.entity.existence"));
    loadQueue.add(new Cstr("sango.entity.box"));
    loadQueue.add(new Cstr("sango.actor"));
    while (!loadQueue.isEmpty()) {
      Cstr m = loadQueue.remove(0);
      if (!loaded.containsKey(m)) {
        Module mm = this.load(m);
        loaded.put(m, mm);
        Cstr[] rs = mm.getModTab();
        for (int i = 1; i < rs.length; i++) {
            loadQueue.add(rs[i]);
        }
      }
    }
    this.modDict = loaded;

    if (this.theEngine.verboseModule) {
      this.theEngine.msgOut.print("Checking modules ... ");
    }
    Iterator<Cstr> mi = this.modDict.keySet().iterator();
    while (mi.hasNext()) {
      this.modDict.get(mi.next()).checkDefsCompat(this.modDict);
    }
    if (this.theEngine.verboseModule) {
      this.theEngine.msgOut.println("Done.");
    }

    Map<Cstr, RModule> loadedr = new HashMap<Cstr, RModule>();
    mi = this.modDict.keySet().iterator();
    while (mi.hasNext()) {
      Cstr m = mi.next();
      Module mm = this.modDict.get(m);
      loadedr.put(m, RModule.create(this.theEngine, mm));
    }
    this.rmodDict = loadedr;

    mi = this.modDict.keySet().iterator();
    while (mi.hasNext()) {
      Cstr m = mi.next();
      RModule rm = this.rmodDict.get(m);
      rm.resolveRefs();
      rm.spawnInitTask();
      RTaskControl it;
      if ((it = rm.getInitTask()) != null) {
        it.start();  // task manager is inactive here, so task is only listed
      }
    }

    this.mainRMod = this.rmodDict.get(modName);
  }

  Module load(Cstr modName) throws IOException, FormatException {
    StringBuffer mmsg = null;
    StringBuffer emsg;
    if (this.theEngine.verboseModule) {
      mmsg = new StringBuffer();
      mmsg.append("Loading ");
      mmsg.append(modName.repr());
      mmsg.append(" = ");
    }
    Module m = null;
    ZipInputStream zis = null;
    try {
      File mf = FileSystem.getInstance().findModuleFileForModuleName(modName, this.theEngine.modPathList);
      if (mf == null) {
        if (mmsg != null) {
          mmsg.append("Not found.");
        }
        emsg = new StringBuffer();
        emsg.append("Module file not found. - ");
        emsg.append(modName.repr());
        throw new IOException(emsg.toString());
      }
      if (mmsg != null) {
        mmsg.append(mf.getCanonicalPath());
        mmsg.append(" ... ");
      }
      zis = new ZipInputStream(new FileInputStream(mf));
      ZipEntry ze = zis.getNextEntry();
      if (ze == null || !ze.getName().equals(FileSystem.MODULE_ZIP_ENTRY)) {
        if (mmsg != null) {
          mmsg.append("Format error.");
        }
        emsg = new StringBuffer();
        emsg.append("Module file format error. - ");
        emsg.append(modName.repr());
        throw new FormatException(emsg.toString());
      }
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setIgnoringElementContentWhitespace(true);
      factory.setIgnoringComments(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(zis);
      m = Module.internalize(doc, modName);
    } catch (SAXException ex) {
      if (mmsg != null) {
        mmsg.append("Format Error.");
      }
      emsg = new StringBuffer();
      emsg.append("Module file format error. - ");
      emsg.append(modName.repr());
      throw new FormatException(emsg.toString());
    } catch (ParserConfigurationException ex) {
      this.theEngine.msgOut.println(ex);
      throw new IOException("Serious error - ParserConfigurationException.", ex);
      // System.exit(1);
    } finally {
      if (mmsg != null) {
        this.theEngine.msgOut.println(mmsg.toString());
      }
      if (zis != null) {
        try {
          zis.closeEntry();
        } catch (Exception ex) {}
        try {
          zis.close();
        } catch (Exception ex) {}
      }
    }
    return m;
  }

  Module getMod(Cstr modName) { return this.modDict.get(modName); }

  RModule getRMod(Cstr modName) { return this.rmodDict.get(modName); }

  RModule getMainRMod() { return this.mainRMod; }

  RActorHItem getModuleInitActorH(Cstr modName) {
    RModule m = this.rmodDict.get(modName);
    if (m == null) {
      throw new IllegalArgumentException("Unknown module name.");
    }
    RTaskControl init = m.getInitTask();
    return (init != null)? init.actorH: null;
  }

  // MT-unsafe
  void loadModuleOnDemand(Cstr modName) throws Exception {
    int msgout = 0;
    if (this.theEngine.verboseModule) {
      msgout = 1;
    }
    List<Cstr> loadQueue = new ArrayList<Cstr>();
    List<Cstr> justLoaded = new ArrayList<Cstr>();
    loadQueue.add(modName);
    Map<Cstr, Module> mergedModDict = new HashMap<Cstr, Module>(this.modDict);
    while (loadQueue.size() > 0) {
      Cstr m = loadQueue.remove(0);
      if (!mergedModDict.containsKey(m)) {
        if (msgout == 1) {
          // this.theEngine.msgOut.println("On-demand module loading started.");
          msgout = 2;
        }
        Module mm = this.load(m);
        mergedModDict.put(m, mm);
        justLoaded.add(m);
        Cstr[] rs = mm.getModTab();
        for (int i = 1; i < rs.length; i++) {
          loadQueue.add(rs[i]);
        }
      }
    }

    if (msgout == 2) {
      this.theEngine.msgOut.print("Checking modules ... ");
    }
    for (int i = 0; i < justLoaded.size(); i++) {
      mergedModDict.get(justLoaded.get(i)).checkDefsCompat(mergedModDict);
    }
    if (msgout == 2) {
      this.theEngine.msgOut.println("Done.");
    }

    this.modDict = mergedModDict;
    for (int i = 0; i < justLoaded.size(); i++) {
      Cstr m = justLoaded.get(i);
      Module mm = this.modDict.get(m);
      this.rmodDict.put(m, RModule.create(this.theEngine, mm));
    }

    List<RTaskControl> initTasks = new ArrayList<RTaskControl>();
    for (int i = 0; i < justLoaded.size(); i++) {
      RModule rm = this.rmodDict.get(justLoaded.get(i));
      rm.resolveRefs();
      rm.spawnInitTask();
      RTaskControl it;
      if ((it = rm.getInitTask()) != null) {
        initTasks.add(it);
      }
    }
    if (msgout == 2) {
      // this.theEngine.msgOut.println("On-demand module loading ended.");
    }

    for (int i = 0; i < initTasks.size(); i++) {
      initTasks.get(i).start();
    }
  }

  // MT-unsafe
  public PDefDict getReferredDefDict(Cstr mod) throws IllegalArgumentException {
    PDefDict d =  this.defDictDict.get(mod);
    if (d == null) {
      Module m = this.modDict.get(mod);
      if (m != null) {
        d = PCompiledModule.create(this, m);  // lazy setup
        this.defDictDict.put(mod, d);
      }
    }
    return d;
  }
}
