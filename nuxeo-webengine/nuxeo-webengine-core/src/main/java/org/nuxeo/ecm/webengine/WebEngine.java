/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.GenericServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.url.URLFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.ecm.webengine.app.impl.DefaultApplicationManager;
import org.nuxeo.ecm.webengine.debug.ReloadManager;
import org.nuxeo.ecm.webengine.loader.WebLoader;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.impl.GlobalTypes;
import org.nuxeo.ecm.webengine.model.impl.ModuleConfiguration;
import org.nuxeo.ecm.webengine.model.impl.ModuleManager;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.scripting.Scripting;
import org.nuxeo.runtime.annotations.AnnotationManager;
import org.nuxeo.runtime.api.Framework;

import freemarker.ext.jsp.TaglibFactory;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.ext.servlet.HttpRequestParametersHashModel;
import freemarker.ext.servlet.ServletContextHashModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebEngine implements ResourceLocator {

    public static final String SKIN_PATH_PREFIX_KEY = "org.nuxeo.ecm.webengine.skinPathPrefix";

    protected static final Map<Object, Object> mimeTypes = loadMimeTypes();

    private static final Log log = LogFactory.getLog(WebEngine.class);

    private static final ThreadLocal<WebContext> CTX = new ThreadLocal<WebContext>();

    static Map<Object, Object> loadMimeTypes() {
        Map<Object, Object> mimeTypes = new HashMap<Object, Object>();
        Properties p = new Properties();
        URL url = WebEngine.class.getClassLoader().getResource(
                "OSGI-INF/mime.properties");
        InputStream in = null;
        try {
            in = url.openStream();
            p.load(in);
            mimeTypes.putAll(p);
        } catch (IOException e) {
            throw new Error("Failed to load mime types");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
        return mimeTypes;
    }

    public static WebContext getActiveContext() {
        return CTX.get();
    }

    public static void setActiveContext(WebContext ctx) {
        CTX.set(ctx);
    }

    protected final File root;

    protected ApplicationManager apps;
    
    /**
     * moduleMgr use double-check idiom and needs to be volatile. See
     * http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
     */
    protected volatile ModuleManager moduleMgr;

    protected final Scripting scripting;

    protected final RenderingEngine rendering;

    protected final Map<String, Object> env;

    protected String devMode;

    protected final GlobalTypes globalTypes;

    protected final AnnotationManager annoMgr;

    protected final ResourceRegistry registry;

    protected String skinPathPrefix;

    protected final List<File> registeredModules = new ArrayList<File>();

    protected final WebLoader webLoader;

    protected ReloadManager reloadMgr;

    public WebEngine(File root) {
        this (new EmptyRegistry(), root);
    }
    
    public WebEngine(ResourceRegistry registry, File root) {
        this.registry = registry;
        this.root = root;
        devMode = Framework.getProperty("org.nuxeo.dev");
        if (devMode != null) {
            reloadMgr = new ReloadManager(this);
        }
        webLoader = new WebLoader(this);
        apps = new DefaultApplicationManager(this);
        scripting = new Scripting(webLoader);
        annoMgr = new AnnotationManager();

        globalTypes = new GlobalTypes(this);

        skinPathPrefix = Framework.getProperty(SKIN_PATH_PREFIX_KEY);
        if (skinPathPrefix == null) {
            // TODO: should put this in web.xml and not use jboss.home.dir to
            // test if on jboss
            skinPathPrefix = System.getProperty("jboss.home.dir") != null ? "/nuxeo/site/skin"
                    : "/skin";
        }

        env = new HashMap<String, Object>();
        env.put("installDir", root);
        env.put("engine", "Nuxeo Web Engine");
        // TODO this should be put in the MANIFEST
        env.put("version", "1.0.0.rc");

        rendering = new FreemarkerEngine();
        rendering.setResourceLocator(this);
        rendering.setSharedVariable("env", getEnvironment());

    }

    /**
     * TODO: This is deprecating ModuleManager
     * @return
     */
    public ApplicationManager getApplicationManager() {
        return apps;
    }
    
    /**
     * JSP taglib support
     */
    public void loadJspTaglib(GenericServlet servlet) {
        if (rendering instanceof FreemarkerEngine) {
            FreemarkerEngine fm = (FreemarkerEngine)rendering;
            ServletContextHashModel servletContextModel = new ServletContextHashModel(servlet, fm.getObjectWrapper());
            fm.setSharedVariable("Application", servletContextModel);
            fm.setSharedVariable("__FreeMarkerServlet.Application__", servletContextModel);
            fm.setSharedVariable("Application", servletContextModel);
            fm.setSharedVariable("__FreeMarkerServlet.Application__", servletContextModel);
            fm.setSharedVariable("JspTaglibs", new TaglibFactory(servlet.getServletContext()));
        }
    }

    public void initJspRequestSupport(GenericServlet servlet, HttpServletRequest request, HttpServletResponse response) {
        if (rendering instanceof FreemarkerEngine) {
            FreemarkerEngine fm = (FreemarkerEngine)rendering;
            HttpRequestHashModel requestModel = new HttpRequestHashModel(request, response, fm.getObjectWrapper());
            fm.setSharedVariable("__FreeMarkerServlet.Request__", requestModel);
            fm.setSharedVariable("Request", requestModel);
            fm.setSharedVariable("RequestParameters", new HttpRequestParametersHashModel(request));

//            HttpSessionHashModel sessionModel = null;
//            HttpSession session = request.getSession(false);
//            if(session != null) {
//                sessionModel = (HttpSessionHashModel) session.getAttribute(ATTR_SESSION_MODEL);
//                if (sessionModel == null || sessionModel.isZombie()) {
//                    sessionModel = new HttpSessionHashModel(session, wrapper);
//                    session.setAttribute(ATTR_SESSION_MODEL, sessionModel);
//                    if(!sessionModel.isZombie()) {
//                        initializeSession(request, response);
//                    }
//                }
//            }
//            else {
//                sessionModel = new HttpSessionHashModel(servlet, request, response, fm.getObjectWrapper());
//            }
//            sessionModel = new HttpSessionHashModel(request, response, fm.getObjectWrapper());
            //fm.setSharedVariable("Session", sessionModel);
        }
    }

    public WebLoader getWebLoader() {
        return webLoader;
    }

    public void setSkinPathPrefix(String skinPathPrefix) {
        this.skinPathPrefix = skinPathPrefix;
    }

    public String getSkinPathPrefix() {
        return skinPathPrefix;
    }

    @Deprecated 
    public ResourceRegistry getRegistry() {
        return registry;
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return webLoader.loadClass(className);
    }

    public GlobalTypes getGlobalTypes() {
        return globalTypes;
    }

    public String getMimeType(String ext) {
        return (String) mimeTypes.get(ext);
    }

    public AnnotationManager getAnnotationManager() {
        return annoMgr;
    }

    public boolean isDevMode() {
        return devMode != null;
    }

    public String getDevMode() {
        return devMode;
    }

    public void setDevMode(String devModeId) {
        this.devMode = devModeId;
    }

    public void registerRenderingExtension(String id, Object obj) {
        rendering.setSharedVariable(id, obj);
    }

    public void unregisterRenderingExtension(String id) {
        rendering.setSharedVariable(id, null);
    }

    public Map<String, Object> getEnvironment() {
        return env;
    }

    public Scripting getScripting() {
        return scripting;
    }

    /**
     * Registers a module reference given its configuration file. The module
     * configuration is not yet loaded. It will be loaded the first time an HTTP
     * request will be made.
     */
    public void registerModule(File config) {
        registerModule(config, true);
    }
    
    public void registerModule(File config, boolean addToClassPath) {
        if (addToClassPath) {
            getWebLoader().addClassPathElement(config.getParentFile());
        }
        registeredModules.add(config);
        if (moduleMgr != null) { // avoid synchronizing if not needed
            synchronized (this) {
                if (moduleMgr != null) {
                    moduleMgr.loadModule(config);
                }
            }
        }
    }

    /**
     * Make a copy to avoid concurrent modification exceptions
     *
     * @return
     */
    public File[] getRegisteredModules() {
        return registeredModules.toArray(new File[registeredModules.size()]);
    }

    public ModuleManager getModuleManager() {
        if (moduleMgr == null) { // avoid synchronizing if not needed
            synchronized (this) {
                /**
                 * the duplicate if is used avoid synchronizing when no needed.
                 * note that the this.moduleMgr member must be set at the end of the synchronized block
                 * after the module manager is completely initialized
                 */
                if (moduleMgr == null) {
                    ModuleManager moduleMgr = new ModuleManager(this);
                    File deployRoot = getDeploymentDirectory();
                    if (deployRoot.isDirectory()) {
                        moduleMgr.loadModules(deployRoot);
                    }
                    // make a copy to avoid concurrent modifications with registerModule
                    for (File mod : registeredModules.toArray(new File[registeredModules.size()])) {
                        moduleMgr.loadModule(mod);
                    }
                    // set member at the end to be sure moduleMgr is completely initialized
                    this.moduleMgr = moduleMgr;
                }
            }
        }
        return moduleMgr;
    }

    public Module getModule(String name) {
        ModuleConfiguration md = getModuleManager().getModule(name);
        if (md != null) {
            return md.get();
        }
        return null;
    }

    public File getRootDirectory() {
        return root;
    }

    public File getDeploymentDirectory() {
        return new File(root, "deploy");
    }

    public File getModulesDirectory() {
        return new File(root, "modules");
    }

    public ReloadManager getReloadManager() {
        return reloadMgr;
    }

    public RenderingEngine getRendering() {
        return rendering;
    }

    /**
     * Manage jax-rs root resource bindings
     */
    public void addResourceBinding(ResourceBinding binding) {
        try {
            binding.resolve(this);
            registry.addBinding(binding);
        } catch (Exception e) {
            throw WebException.wrap("Failed o register binding: " + binding, e);
        }
    }

    public void removeResourceBinding(ResourceBinding binding) {
        registry.removeBinding(binding);
    }

    public ResourceBinding[] getBindings() {
        return registry.getBindings();
    }

    /**
     * reload for we 2.
     */
    public synchronized void reload2() {
        log.info("Reloading WebEngine");
        webLoader.flushCache();
        apps.reload();
    }
    /**
     * Reloads configuration.
     */
    public synchronized void reload() {
        log.info("Reloading WebEngine");
        apps.reload();
        if (moduleMgr != null) { // avoid synchronizing if not needed
            for (ModuleConfiguration mc : moduleMgr.getModules()) {
                if (mc.isLoaded()) {
                    // this is needed even if the module manager
                    // is rebuild since it may remove groovy file caches
                    mc.get().flushCache();
                }
            }
            synchronized (this) {
                if (moduleMgr != null) {
                    moduleMgr = null;
                }
            }
        }
        webLoader.flushCache();
        apps.reload();
    }

    public synchronized void reloadModules() {
        moduleMgr.reloadModules();
    }

    public void start() {
        if (reloadMgr != null) {
            reloadMgr.start();
        }
    }

    public void stop() {
        if (reloadMgr != null) {
            reloadMgr.stop();
        }
        registry.clear();
    }

    protected ModuleConfiguration getModuleFromPath(String rootPath, String path) {
        path = path.substring(rootPath.length() + 1);
        int p = path.indexOf('/');
        String moduleName = path;
        if (p > -1) {
            moduleName = path.substring(0, p);
        }
        return moduleMgr.getModule(moduleName);
    }

    /* ResourceLocator API */

    public URL getResourceURL(String key) {
        try {
            return URLFactory.getURL(key);
        } catch (Exception e) {
            return null;
        }
    }

    public File getResourceFile(String key) {
        WebContext ctx = getActiveContext();
        if (key.startsWith("@")) {
            Resource rs = ctx.getTargetObject();
            if (rs != null) {
                return rs.getView(key.substring(1)).script().getFile();
            }
        } else {
            ScriptFile file = ctx.getFile(key);
            if (file != null) {
                return file.getFile();
            }
        }
        return null;
    }

}