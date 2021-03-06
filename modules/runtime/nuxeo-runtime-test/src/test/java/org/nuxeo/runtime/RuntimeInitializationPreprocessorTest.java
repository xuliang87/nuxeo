/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */

package org.nuxeo.runtime;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeInitializationPreprocessorErrorFeature.class, RuntimeFeature.class })
public class RuntimeInitializationPreprocessorTest {

    @Test
    public void testDeploymentPreprocessorErrors() {
        List<String> errors = Framework.getRuntime().getMessageHandler().getMessages(Level.ERROR);
        assertEquals(2, errors.size());
        assertEquals("Runtime test feature init error", errors.get(0));
        assertEquals("Runtime test start error", errors.get(1));
        // configure error came too late --> not included here
    }

}
