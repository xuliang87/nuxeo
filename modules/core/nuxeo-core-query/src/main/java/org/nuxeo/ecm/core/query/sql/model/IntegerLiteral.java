/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.core.query.sql.model;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class IntegerLiteral extends Literal {

    private static final long serialVersionUID = 4769705314623462546L;

    public final long value;

    public IntegerLiteral(long value) {
        this.value = value;
    }

    public IntegerLiteral(Long value) {
        this.value = value;
    }

    public IntegerLiteral(Integer value) {
        this.value = value;
    }

    public IntegerLiteral(String value) {
        this.value = Long.parseLong(value);
    }

    @Override
    public void accept(IVisitor visitor) {
        visitor.visitIntegerLiteral(this);
    }

    @Override
    public String asString() {
        return String.valueOf(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof IntegerLiteral) {
            return value == ((IntegerLiteral) obj).value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(value).hashCode();
    }

}
