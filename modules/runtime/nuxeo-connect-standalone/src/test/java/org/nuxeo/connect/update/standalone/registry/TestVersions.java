/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.connect.update.standalone.registry;

import org.junit.Test;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestVersions extends SharedFilesTest {

    /**
     * Test with downgrade off
     */
    @Test
    public void testUpgrade() throws Exception {
        createFakeBundles();

        Pkg1 pkg1 = new Pkg1();
        pkg1.setAllowDowngrade(false);
        Pkg2 pkg2 = new Pkg2();

        ensureBaseVersion();

        // +pkg1, +pkg2, -pkg1, -pkg2
        pkg1.install();
        ensurePkg1();

        pkg2.install();
        ensurePkg12();

        pkg1.uninstall();
        ensurePkg2();

        pkg2.uninstall();
        ensureBaseVersion();

        // +pkg1, +pkg2, -pkg2, -pkg1
        pkg1.install();
        ensurePkg1();

        pkg2.install();
        ensurePkg12();

        pkg2.uninstall();
        ensurePkg1();

        pkg1.uninstall();
        ensureBaseVersion();

        // +pkg2, +pkg1, -pkg2, -pkg1
        pkg2.install();
        ensurePkg2();

        pkg1.install();
        ensurePkg21();

        pkg2.uninstall();
        ensurePkg1();
        // ensurePkg1AfterBlockingDowngrade();

        pkg1.uninstall();
        ensureBaseVersion();

        // +pkg2, +pkg1, -pkg1, -pkg2
        pkg2.install();
        ensurePkg2();

        pkg1.install();
        ensurePkg21();

        pkg1.uninstall();
        ensurePkg2();

        pkg2.uninstall();
        ensureBaseVersion();
    }

}
