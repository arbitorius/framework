/*
 * Copyright 2000-2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.client.connectors.components.grid;

import java.util.Collection;

import com.vaadin.client.connectors.components.AbstractListingConnector;
import com.vaadin.client.connectors.data.HasSelection;
import com.vaadin.client.data.DataSource;
import com.vaadin.client.data.selection.SelectionModel;
import com.vaadin.client.renderers.Renderer;
import com.vaadin.client.widget.grid.selection.ClickSelectHandler;
import com.vaadin.client.widgets.Grid;
import com.vaadin.client.widgets.Grid.Column;
import com.vaadin.shared.ui.Connect;

import elemental.json.JsonObject;

@Connect(com.vaadin.ui.components.grid.Grid.class)
public class GridConnector extends AbstractListingConnector implements
        HasSelection {

    /**
     * Class for providing selection event stuff from Grid to the new
     * client-side selection model. Only the absolutely necessary methods are
     * implemented.
     * <p>
     * TODO: This should be unified to have only one SelectionModel API.
     */
    private final class SelectionModelAdapter
            implements
            com.vaadin.client.widget.grid.selection.SelectionModel.Single<JsonObject> {
        private final SelectionModel model;

        private SelectionModelAdapter(SelectionModel selectionModel) {
            this.model = selectionModel;
        }

        @Override
        public boolean isSelected(JsonObject row) {
            return model != null && model.isSelected(row);
        }

        @Override
        public Renderer<Boolean> getSelectionColumnRenderer() {
            return null;
        }

        @Override
        public void setGrid(Grid<JsonObject> grid) {
        }

        @Override
        public void reset() {
        }

        @Override
        public Collection<JsonObject> getSelectedRows() {
            return null;
        }

        @Override
        public boolean select(JsonObject row) {
            if (model != null) {
                model.select(row);
            }
            return false;
        }

        @Override
        public boolean deselect(JsonObject row) {
            if (model != null) {
                model.deselect(row);
            }
            return false;
        }

        @Override
        public JsonObject getSelectedRow() {
            return null;
        }

        @Override
        public void setDeselectAllowed(boolean deselectAllowed) {
        }

        @Override
        public boolean isDeselectAllowed() {
            return true;
        }
    }

    @Override
    public Grid<JsonObject> getWidget() {
        return (Grid<JsonObject>) super.getWidget();
    }

    @Override
    protected void init() {
        super.init();

        new ClickSelectHandler<JsonObject>(getWidget());
    }

    @Override
    public void setDataSource(DataSource<JsonObject> dataSource) {
        super.setDataSource(dataSource);
        getWidget().setDataSource(dataSource);
    }

    @Override
    public void setSelectionModel(final SelectionModel selectionModel) {
        super.setSelectionModel(selectionModel);
        getWidget()
                .setSelectionModel(new SelectionModelAdapter(selectionModel));
    }

    public void addColumn(Column<?, JsonObject> column) {
        getWidget().addColumn(column);
    }

    public void removeColumn(Column<?, JsonObject> column) {
        getWidget().removeColumn(column);
    }
}
