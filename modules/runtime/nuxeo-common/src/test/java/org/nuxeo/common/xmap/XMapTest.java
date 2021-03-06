/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *     Bogdan Stefanescu
 *     Anahide Tchertchian
 */
package org.nuxeo.common.xmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;

import org.junit.Test;
import org.nuxeo.common.xmap.Author.Gender;

public class XMapTest {

    protected Object load(XMap xmap, String src) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(src);
        return xmap.load(url);
    }

    @Test
    public void testMapping() throws IOException {
        XMap xmap = new XMap();
        xmap.register(Author.class);
        checkAuthor((Author) load(xmap, "test-xmap.xml"));
    }

    @Test
    public void testInheritedMapping() throws IOException {
        XMap xmap = new XMap();
        xmap.register(InheritedAuthor.class);
        InheritedAuthor inheritedAuthor = (InheritedAuthor) load(xmap, "second-test-xmap.xml");
        checkAuthor(inheritedAuthor);
        assertEquals("dummyContent", inheritedAuthor.notInherited);
        assertEquals("test1", inheritedAuthor.inheritedId);
    }

    protected void checkAuthor(Author author) {
        assertEquals("First test 22", author.title);
        assertEquals("bla bla", author.description);
        assertEquals(author, author.name.owner);
        assertEquals("my first name", author.name.firstName);
        assertEquals("my last name", author.name.lastName);
        assertEquals("The content", author.content.trim());
        assertEquals("author", author.nameType);
        assertEquals(Gender.MALE, author.gender);
        assertEquals(32, author.age);
        assertEquals("test1", author.getId());
        assertEquals(4, author.items.size());
        assertEquals(4, author.itemIds.size());
        assertEquals("Item 1", author.items.get(0));
        assertEquals("Item 2", author.items.get(1));
        assertEquals("Item 3", author.items.get(2));
        assertEquals("Item with parameters to < unescape", author.items.get(3));
        assertEquals("item1", author.itemIds.get(0));
        assertEquals("item2", author.itemIds.get(1));
        assertEquals("item3", author.itemIds.get(2));
        assertEquals("item4", author.itemIds.get(3));
        assertEquals(3, author.friends.size());
        assertEquals("friend1_fn", author.friends.get(0).firstName);
        assertEquals("friend1_ln", author.friends.get(0).lastName);
        assertEquals("friend2_fn", author.friends.get(1).firstName);
        assertEquals("friend2_ln", author.friends.get(1).lastName);
        assertEquals("toUnescape", author.friends.get(2).firstName);
        assertEquals("Map with parameters to < unescape", author.friends.get(2).lastName);

        assertEquals(4, author.properties.size());
        assertEquals("theName", author.properties.get("name"));
        assertEquals("theColor", author.properties.get("color"));
        assertEquals("theWeight", author.properties.get("weight"));
        assertEquals("Prop with parameters to < unescape", author.properties.get("toUnescape"));

        // note the additional \n char after each tag (not sure if it's wanted)
        assertEquals("Test\n      <b>content</b>\n not to &lt; unescape", author.testContent.trim());
        String t = author.testContent2.getFirstChild().getTextContent().trim();
        assertEquals("Test", t);

        assertEquals("SELECT * FROM Document WHERE dc:created < DATE '2013-08-19'", author.textToUnescape);

        assertNotEquals(author.content, author.content.trim());

        assertNull(author.testNullByDefaultForList);
        assertNull(author.testNullByDefaultForMap);
        assertNull(author.testNullByDefaultForListHashSet);
        assertNotNull(author.itemsHashSet);
        assertEquals(2, author.itemsHashSet.size());

        assertEquals(1, author.aliases.length);
        assertEquals("test2", author.aliases[0].name);
        assertEquals("text to be < unescaped", author.aliases[0].description);

        assertEquals(Duration.ofDays(1), author.durationDay);
        assertEquals(Duration.ofHours(1), author.durationHour);
        assertEquals(Duration.ofMinutes(1), author.durationMinute);
        assertEquals(Duration.ofSeconds(1), author.durationSecond);
        assertEquals(Duration.ofMillis(1), author.durationMillis);
        assertEquals(Duration.ofDays(1).plusHours(2).plusMinutes(3).plusSeconds(4).plusMillis(5), author.durationAll);
        assertEquals(Duration.ofSeconds(1), author.durationJdk);

        assertEquals("myDefaultValue", author.stringWithDefault);
        assertEquals("fallbackValue", author.stringWithFallback);
        assertEquals((Integer) 5, author.intWithDefault);
        assertTrue(author.boolWithDefault);

        assertEquals("test1:32", author.combinedString);
        assertEquals("defaultCombined", author.combinedStringWithDefault);
    }

    @Test
    public void testInvalidClass() throws IOException {
        XMap xmap = new XMap();
        xmap.register(Author.class);
        try {
            load(xmap, "test-xmap-invalid-class.xml");
            fail("Should not allow loading with invalid class");
        } catch (XMapException e) {
            assertEquals("Cannot load class: this-is-not-a-class", e.getMessage());
        }
    }

    /** @since 11.5 **/
    @Test
    public void testDOMElementMapping() throws IOException {
        XMap xmap = new XMap();
        xmap.register(Author.class);
        Author author = (Author) load(xmap, "test-xmap.xml");

        assertNotNull(author.metadata);
        assertEquals(13, author.metadata.getChildNodes().getLength());

        assertNotNull(author.personElements);
        assertEquals(2, author.personElements.size());
        assertEquals(5, author.personElements.get(0).getChildNodes().getLength());
    }

    /** @since 11.5 **/
    @Test
    public void testResourceMapping() throws IOException {
        XMap xmap = new XMap();
        xmap.register(ResourceReference.class);

        ResourceReference res = (ResourceReference) load(xmap, "test-resource-1.xml");
        assertNotNull(res);
        assertEquals("ok", res.getName());
        assertNotNull(res.getSrc());
        assertEquals("test-data/hello.odt", res.getSrc().getPath());
        assertNotNull(res.getSrc().getContext());
        assertNotNull(res.getSrc().toURL());

        res = (ResourceReference) load(xmap, "test-resource-2.xml");
        assertNotNull(res);
        assertEquals("ko", res.getName());
        assertNotNull(res.getSrc());
        assertEquals("does-not-exist.txt", res.getSrc().getPath());
        assertNotNull(res.getSrc().getContext());
        assertNull(res.getSrc().toURL());
    }

}
