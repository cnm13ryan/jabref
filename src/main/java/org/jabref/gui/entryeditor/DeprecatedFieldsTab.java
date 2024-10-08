package org.jabref.gui.entryeditor;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.search.IndexingTaskManager;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public class DeprecatedFieldsTab extends FieldsEditorTab {

    public static final String NAME = "Deprecated fields";
    private final BibEntryTypesManager entryTypesManager;

    public DeprecatedFieldsTab(BibDatabaseContext databaseContext,
                               SuggestionProviders suggestionProviders,
                               UndoManager undoManager,
                               UndoAction undoAction,
                               RedoAction redoAction,
                               DialogService dialogService,
                               PreferencesService preferences,
                               ThemeManager themeManager,
                               IndexingTaskManager indexingTaskManager,
                               BibEntryTypesManager entryTypesManager,
                               TaskExecutor taskExecutor,
                               JournalAbbreviationRepository journalAbbreviationRepository,
                               OptionalObjectProperty<SearchQuery> searchQueryProperty) {
        super(false, databaseContext, suggestionProviders, undoManager, undoAction, redoAction, dialogService, preferences, themeManager, taskExecutor, journalAbbreviationRepository, indexingTaskManager, searchQueryProperty);
        this.entryTypesManager = entryTypesManager;

        setText(Localization.lang("Deprecated fields"));
        EasyBind.subscribe(preferences.getWorkspacePreferences().showAdvancedHintsProperty(), advancedHints -> {
            if (advancedHints) {
                setTooltip(new Tooltip(Localization.lang("Shows fields having a successor in biblatex.\nFor instance, the publication month should be part of the date field.\nUse the Cleanup Entries functionality to convert the entry to biblatex.")));
            } else {
                setTooltip(new Tooltip(Localization.lang("Shows fields having a successor in biblatex.")));
            }
        });
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected SequencedSet<Field> determineFieldsToShow(BibEntry entry) {
        BibDatabaseMode mode = databaseContext.getMode();
        Optional<BibEntryType> entryType = entryTypesManager.enrich(entry.getType(), mode);
        if (entryType.isPresent()) {
            return entryType.get().getDeprecatedFields(mode).stream().filter(field -> !entry.getField(field).isEmpty()).collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            // Entry type unknown -> treat all fields as required (thus no optional fields)
            return new LinkedHashSet<>();
        }
    }
}
