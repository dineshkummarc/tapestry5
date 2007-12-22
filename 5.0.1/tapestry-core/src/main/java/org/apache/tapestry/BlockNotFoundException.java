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

package org.apache.tapestry;

import org.apache.tapestry.ioc.Locatable;
import org.apache.tapestry.ioc.Location;

/**
 * Exception thrown when a a {@link Block} is requested but not found.
 */
public class BlockNotFoundException extends RuntimeException implements Locatable
{
    private final Location _location;

    public BlockNotFoundException(String message, Location location)
    {
        super(message);

        _location = location;
    }

    public Location getLocation()
    {
        return _location;
    }
}