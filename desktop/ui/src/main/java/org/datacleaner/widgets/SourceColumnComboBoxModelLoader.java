/**
 * DataCleaner (community edition)
 * Copyright (C) 2014 Neopost - Customer Information Management
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.datacleaner.widgets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;

import org.apache.metamodel.MetaModelHelper;
import org.apache.metamodel.schema.Column;
import org.apache.metamodel.schema.Schema;
import org.apache.metamodel.schema.Table;
import org.datacleaner.connection.DatastoreConnection;
import org.datacleaner.util.SchemaComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SourceColumnComboBoxModelLoader extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(SourceColumnComboBoxModelLoader.class);
    private final SourceColumnComboBox _sourceColumnComboBox;
    private final DatastoreConnection _datastoreConnection;
    private final boolean _retainSelection;
    private ModelLoadingProgress _modelLoadingProgress;

    public SourceColumnComboBoxModelLoader(SourceColumnComboBox sourceColumncomboBox,
            DatastoreConnection datastoreConnection, boolean retainSelection) {
        _sourceColumnComboBox = sourceColumncomboBox;
        _datastoreConnection = datastoreConnection;
        _retainSelection = retainSelection;
    }

    public void run() {
        _modelLoadingProgress = new ModelLoadingProgress();
        _modelLoadingProgress.start();
        List<Object> comboBoxList = new ArrayList<>();
        comboBoxList.add(null);
        Schema[] schemas = _datastoreConnection.getSchemaNavigator().getSchemas();
        _modelLoadingProgress.setSchemaCount(schemas.length);
        Arrays.sort(schemas, new SchemaComparator());

        for (Schema schema : schemas) {
            _modelLoadingProgress.newSchema(schema.getTableCount());
            comboBoxList.add(schema);

            if (!MetaModelHelper.isInformationSchema(schema)) {
                try {
                    processSchema(schema.getTables(), comboBoxList, _retainSelection);
                } catch (InterruptedException e) {
                    _modelLoadingProgress.stop();
                    return;
                }
            }
        }

        final ComboBoxModel<Object> model = new DefaultComboBoxModel<>(comboBoxList.toArray());
        _sourceColumnComboBox.setModel(model);
        _modelLoadingProgress.stop();
    }

    private void processSchema(Table[] tables, List<Object> comboBoxList, boolean retainSelection)
            throws InterruptedException {
        final Column previousItem = _sourceColumnComboBox.getSelectedItem();

        for (Table table : tables) {
            if (isInterrupted()) {
                throw new InterruptedException("Model loading was interrupted. ");
            }

            _modelLoadingProgress.newTable();

            try {
                Column[] columns = table.getColumns();

                if (columns != null && columns.length > 0) {
                    comboBoxList.add(table);

                    for (Column column : columns) {
                        comboBoxList.add(column);

                        if (column == previousItem && retainSelection) {
                            _sourceColumnComboBox.setSelectedIndex(comboBoxList.size() - 1);
                        }
                    }
                }
            } catch (Exception e) {
                // errors can occur for experimental datastores (or
                // something like SAS datastores where not all SAS
                // files are supported). Ignore.
                logger.error("Error occurred getting columns of table: {}", table);
            }
        }
    }

    private class ModelLoadingProgress {
        private int _currentSchema;
        private int _schemaCount;
        private int _tableCount;
        private int _currentTable = 0;
        private ProgressBar _progressBar;

        private ModelLoadingProgress() {
        }

        public void start() {
            _progressBar = new ProgressBar();
            _progressBar.show();
        }

        public void setSchemaCount(int schemaCount) {
            _schemaCount = schemaCount;
            _currentSchema = 0;
        }

        public void newSchema(int tableCount) {
            _currentSchema++;
            _tableCount = tableCount;
            _currentTable = 0;
        }

        public void newTable() {
            _currentTable++;
            _progressBar.update(String.format("Loading table %d/%d in schema %d/%d...", _currentTable, _tableCount,
                    _currentSchema, _schemaCount));
        }

        public void stop() {
            _progressBar.hide();
        }
    }
}
