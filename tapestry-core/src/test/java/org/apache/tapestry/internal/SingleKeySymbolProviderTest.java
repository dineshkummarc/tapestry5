// Copyright 2007 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry.internal;

import org.apache.tapestry.ioc.services.SymbolProvider;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SingleKeySymbolProviderTest extends Assert
{
    private final SymbolProvider _provider = new SingleKeySymbolProvider("fred", "flintstone");

    @Test
    public void exact_match()
    {
        assertEquals(_provider.valueForSymbol("fred"), "flintstone");
    }

    @Test
    public void case_insensitive()
    {
        assertEquals(_provider.valueForSymbol("FRED"), "flintstone");
    }

    @Test
    public void non_match()
    {
        assertNull(_provider.valueForSymbol("barney"));
    }
}