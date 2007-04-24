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

package org.apache.tapestry.corelib.components;

import java.util.List;

import org.apache.tapestry.annotations.Parameter;
import org.apache.tapestry.beaneditor.PropertyModel;
import org.apache.tapestry.grid.GridDataSource;
import org.apache.tapestry.grid.GridModelProvider;

/**
 * Renders out a series of rows within the table.
 */
public class GridRows
{
    /**
     * Parameter used to set the CSS class for each row (each &lt;tr&gt; element) within the
     * &lt;tbody&gt;). This is not cached, so it will be recomputed for each row.
     */
    @Parameter(cache = false)
    private String _rowClass;

    /** Object that provides access to the bean and data models used to render the Grid. */
    @Parameter(value = "componentResources.container")
    private GridModelProvider _provider;

    /** Number of rows displayed on each page. Long result sets are split across multiple pages. */
    @Parameter(required = true)
    private int _rowsPerPage;

    /** The current page number within the available pages (indexed from 1). */
    @Parameter(required = true)
    private int _currentPage;

    /**
     * The current row being rendered, this is primarily an output parameter used to allow the Grid,
     * and the Grid's container, to know what object is being rendered.
     */
    @Parameter(required = true)
    private Object _row;

    private int _startRow;

    private int _endRow;

    private int _rowIndex;

    private String _propertyName;

    private PropertyModel _columnModel;

    public String getRowClass()
    {
        return _rowClass;
    }

    public String getCellClass()
    {
        String id = _provider.getDataModel().get(_propertyName).getId();

        return id + "-cell";
    }

    void setupRender()
    {
        GridDataSource dataSource = _provider.getDataSource();

        int availableRows = dataSource.getAvailableRows();

        int maxPages = ((availableRows - 1) / _rowsPerPage) + 1;

        // This can sometimes happen when the number of items shifts between requests.

        if (_currentPage > maxPages)
            _currentPage = maxPages;

        _startRow = (_currentPage - 1) * _rowsPerPage;
        _endRow = Math.min(availableRows - 1, _startRow + _rowsPerPage - 1);

        _rowIndex = _startRow;
    }

    void beginRender()
    {
        _row = _provider.getDataSource().getRowValue(_rowIndex);
    }

    boolean afterRender()
    {
        _rowIndex++;

        return _rowIndex > _endRow;
    }

    public List<String> getPropertyNames()
    {
        return _provider.getDataModel().getPropertyNames();
    }

    public String getPropertyName()
    {
        return _propertyName;
    }

    public void setPropertyName(String propertyName)
    {
        _propertyName = propertyName;

        _columnModel = _provider.getDataModel().get(propertyName);
    }

    public Object getRow()
    {
        return _row;
    }

    public PropertyModel getColumnModel()
    {
        return _columnModel;
    }
}