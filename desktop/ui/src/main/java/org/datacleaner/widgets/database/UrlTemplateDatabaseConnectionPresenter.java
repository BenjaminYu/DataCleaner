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
package org.datacleaner.widgets.database;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.datacleaner.connection.JdbcDatastore;
import org.datacleaner.database.DatabaseDriverDescriptor;
import org.datacleaner.panels.DCPanel;
import org.datacleaner.util.HasGroupLiteral;
import org.datacleaner.util.NamedPattern;
import org.datacleaner.util.NamedPatternMatch;
import org.datacleaner.util.NumberDocument;
import org.datacleaner.util.WidgetUtils;
import org.datacleaner.widgets.DCLabel;
import org.jdesktop.swingx.JXTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DatabaseConnectionPresenter} for database connections based on URL
 * templates.
 * 
 * The template should have the format of a JDBC URL where HOSTNAME,PORT and
 * DATABASE tokens are inlined. For example:
 * "jdbc:mysql://HOSTNAME:PORT/DATABASE".
 * 
 * In addition to there's four optional tokens which can be used: PARAM1,
 * PARAM2, PARAM3, PARAM4. Make sure to use corresponding getLabelFor... methods
 * to provide user names.
 */
public abstract class UrlTemplateDatabaseConnectionPresenter extends AbstractDatabaseConnectionPresenter {

    private static final Logger logger = LoggerFactory.getLogger(UrlTemplateDatabaseConnectionPresenter.class);

    public static enum UrlPart implements HasGroupLiteral {
        HOSTNAME, PORT, DATABASE, PARAM1, PARAM2, PARAM3, PARAM4;

        @Override
        public String getGroupLiteral() {
            if (this == PORT) {
                return "([0-9]+)";
            } else {
                return "(.+)";
            }
        }
    }

    private final JXTextField _hostnameTextField;
    private final JXTextField _portTextField;
    private final JXTextField _databaseTextField;
    private final JXTextField _param1TextField;
    private final JXTextField _param2TextField;
    private final JXTextField _param3TextField;
    private final JXTextField _param4TextField;
    private final List<NamedPattern<UrlPart>> _urlTemplates;

    /**
     * Constructs a {@link DatabaseConnectionPresenter} based on URL templates.
     * 
     * @param urlTemplates
     */
    public UrlTemplateDatabaseConnectionPresenter(String... urlTemplates) {
        super();
        if (urlTemplates == null || urlTemplates.length == 0) {
            throw new IllegalArgumentException("URL templates cannot be null or empty");
        }
        _urlTemplates = new ArrayList<NamedPattern<UrlPart>>(urlTemplates.length);
        for (String urlTemplate : urlTemplates) {
            NamedPattern<UrlPart> template = new NamedPattern<UrlPart>(urlTemplate, UrlPart.class);
            _urlTemplates.add(template);
        }
        _hostnameTextField = createTextField("Hostname");
        _hostnameTextField.setText("localhost");
        _portTextField = createTextField("Port");
        _portTextField.setDocument(new NumberDocument(false));
        _portTextField.setText("" + getDefaultPort());
        _databaseTextField = createTextField(getLabelForDatabase());
        _param1TextField = createTextField(getLabelForParam1());
        _param2TextField = createTextField(getLabelForParam2());
        _param3TextField = createTextField(getLabelForParam3());
        _param4TextField = createTextField(getLabelForParam4());
    }
    
    protected JXTextField getDatabaseTextField() {
        return _databaseTextField;
    }

    protected JXTextField getParam1TextField() {
        return _param1TextField;
    }

    protected JXTextField getParam2TextField() {
        return _param2TextField;
    }

    protected JXTextField getParam3TextField() {
        return _param3TextField;
    }

    protected JXTextField getParam4TextField() {
        return _param4TextField;
    }

    protected String getLabelForDatabase() {
        return "Database";
    }

    protected String getLabelForParam1() {
        return "Parameter 1";
    }

    protected String getLabelForParam2() {
        return "Parameter 2";
    }

    protected String getLabelForParam3() {
        return "Parameter 3";
    }

    protected String getLabelForParam4() {
        return "Parameter 4";
    }

    protected abstract int getDefaultPort();

    @Override
    public final String getJdbcUrl() {
        final String portText = _portTextField.getText();
        final int port;
        if (portText == null || portText.length() == 0) {
            port = getDefaultPort();
        } else {
            port = Integer.parseInt(portText);
        }
        return getJdbcUrl(_hostnameTextField.getText(), port, _databaseTextField.getText(), _param1TextField.getText(),
                _param2TextField.getText(), _param3TextField.getText(), _param4TextField.getText());
    }

    protected abstract String getJdbcUrl(String hostname, int port, String database, String param1, String param2,
            String param3, String param4);

    @Override
    public boolean initialize(JdbcDatastore datastore) {
        super.initialize(datastore);

        final String url = datastore.getJdbcUrl();

        NamedPatternMatch<UrlPart> match = null;
        NamedPattern<UrlPart> matchingUrlTemplate = null;
        for (NamedPattern<UrlPart> urlTemplate : _urlTemplates) {
            matchingUrlTemplate = urlTemplate;
            match = urlTemplate.match(url);
            if (match != null) {
                logger.info("URL '{}' matched with template: {}", url, urlTemplate);
                break;
            }
        }

        if (match == null) {
            logger.info("Cannot handle jdbc url '{}', expected something to match: {}", url, _urlTemplates);
            return false;
        }

        return initializeFromMatch(datastore, matchingUrlTemplate, match);
    }

    protected boolean initializeFromMatch(JdbcDatastore datastore, NamedPattern<UrlPart> matchingUrlTemplate,
            NamedPatternMatch<UrlPart> match) {
        _hostnameTextField.setText(match.get(UrlPart.HOSTNAME));
        _portTextField.setText(match.get(UrlPart.PORT));
        _databaseTextField.setText(match.get(UrlPart.DATABASE));
        _param1TextField.setText(match.get(UrlPart.PARAM1));
        _param2TextField.setText(match.get(UrlPart.PARAM2));
        _param3TextField.setText(match.get(UrlPart.PARAM3));
        _param4TextField.setText(match.get(UrlPart.PARAM4));

        return true;
    }

    private EnumSet<UrlPart> getUrlParts() {
        // build a set of all URL parts in all templates.
        EnumSet<UrlPart> urlParts = EnumSet.noneOf(UrlPart.class);
        for (NamedPattern<UrlPart> urlTemplate : _urlTemplates) {
            urlParts.addAll(urlTemplate.getUsedGroups());
        }
        return urlParts;
    }

    protected boolean showDatabaseAboveCredentials() {
        return true;
    }

    @Override
    protected int layoutGridBagAboveCredentials(DCPanel panel) {
        final EnumSet<UrlPart> urlParts = getUrlParts();

        int row = -1;

        row = layoutGridBagHostnameAndPort(panel, row);

        if (showDatabaseAboveCredentials() && urlParts.contains(UrlPart.DATABASE)) {
            row = layoutGridBagDatabase(panel, row);
        }

        return row;
    }

    protected int layoutGridBagDatabase(DCPanel panel, int row) {
        row++;
        WidgetUtils.addToGridBag(DCLabel.dark(getLabelForDatabase() + ":"), panel, 0, row);
        WidgetUtils.addToGridBag(_databaseTextField, panel, 1, row);
        return row;
    }

    protected int layoutGridBagHostnameAndPort(DCPanel panel, int row) {
        final EnumSet<UrlPart> urlParts = getUrlParts();
        if (urlParts.contains(UrlPart.HOSTNAME)) {
            row++;
            WidgetUtils.addToGridBag(DCLabel.dark("Hostname:"), panel, 0, row);
            WidgetUtils.addToGridBag(_hostnameTextField, panel, 1, row);
        }

        if (urlParts.contains(UrlPart.PORT)) {
            row++;
            WidgetUtils.addToGridBag(DCLabel.dark("Port:"), panel, 0, row);
            WidgetUtils.addToGridBag(_portTextField, panel, 1, row);
        }
        return row;
    }

    @Override
    protected void layoutGridBagBelowCredentials(DCPanel panel, int row) {
        final EnumSet<UrlPart> urlParts = getUrlParts();

        if (!showDatabaseAboveCredentials() && urlParts.contains(UrlPart.DATABASE)) {
            row = layoutGridBagDatabase(panel, row);
        }

        layoutGridBagParams(panel, row);
    }

    protected int layoutGridBagParams(DCPanel panel, int row) {
        final EnumSet<UrlPart> urlParts = getUrlParts();

        if (urlParts.contains(UrlPart.PARAM1)) {
            row++;
            WidgetUtils.addToGridBag(DCLabel.dark(getLabelForParam1() + ":"), panel, 0, row);
            WidgetUtils.addToGridBag(_param1TextField, panel, 1, row);
        }

        if (urlParts.contains(UrlPart.PARAM2)) {
            row++;
            WidgetUtils.addToGridBag(DCLabel.dark(getLabelForParam2() + ":"), panel, 0, row);
            WidgetUtils.addToGridBag(_param2TextField, panel, 1, row);
        }

        if (urlParts.contains(UrlPart.PARAM3)) {
            row++;
            WidgetUtils.addToGridBag(DCLabel.dark(getLabelForParam3() + ":"), panel, 0, row);
            WidgetUtils.addToGridBag(_param3TextField, panel, 1, row);
        }

        if (urlParts.contains(UrlPart.PARAM4)) {
            row++;
            WidgetUtils.addToGridBag(DCLabel.dark(getLabelForParam4() + ":"), panel, 0, row);
            WidgetUtils.addToGridBag(_param4TextField, panel, 1, row);
        }

        return row;
    }

    @Override
    public void setSelectedDatabaseDriver(DatabaseDriverDescriptor driver) {
        // do nothing
    }
}
