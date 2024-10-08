package org.jabref.logic.pdf.search;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.FilePreferences;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.NIOFSDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdfIndexerTest {

    private PdfIndexer indexer;
    private BibDatabase database;
    private BibDatabaseContext context = mock(BibDatabaseContext.class);

    @BeforeEach
    void setUp(@TempDir Path indexDir) throws IOException {
        FilePreferences filePreferences = mock(FilePreferences.class);
        this.database = new BibDatabase();

        this.context = mock(BibDatabaseContext.class);
        when(context.getDatabasePath()).thenReturn(Optional.of(Path.of("src/test/resources/pdfs/")));
        when(context.getFileDirectories(Mockito.any())).thenReturn(Collections.singletonList(Path.of("src/test/resources/pdfs")));
        when(context.getFulltextIndexPath()).thenReturn(indexDir);
        when(context.getDatabase()).thenReturn(database);
        when(context.getEntries()).thenReturn(database.getEntries());
        this.indexer = PdfIndexer.of(context, filePreferences);
    }

    @Test
    void exampleThesisIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(entry);

        // when
        indexer.rebuildIndex();

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(33, reader.numDocs());
        }
    }

    @Test
    void doNotIndexNonPdf() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis)
                .withFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.aux", StandardFileType.AUX.getName())));
        database.insertEntry(entry);

        // when
        indexer.rebuildIndex();

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.indexWriter)) {
            assertEquals(0, reader.numDocs());
        }
    }

    @Test
    void dontIndexOnlineLinks() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "https://raw.githubusercontent.com/JabRef/jabref/main/src/test/resources/pdfs/thesis-example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(entry);

        // when
        indexer.rebuildIndex();

        // then
        try (IndexReader reader = DirectoryReader.open(indexer.indexWriter)) {
            assertEquals(0, reader.numDocs());
        }
    }

    @Test
    void exampleThesisIndexWithKey() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.PhdThesis);
        entry.setCitationKey("Example2017");
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(entry);

        // when
        indexer.rebuildIndex();

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(33, reader.numDocs());
        }
    }

    @Test
    void metaDataIndex() throws IOException {
        // given
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "metaData.pdf", StandardFileType.PDF.getName())));

        database.insertEntry(entry);

        // when
        indexer.rebuildIndex();

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(1, reader.numDocs());
        }
    }

    @Test
    void exampleThesisIndexAppendMetaData() throws IOException {
        // given
        BibEntry exampleThesis = new BibEntry(StandardEntryType.PhdThesis);
        exampleThesis.setCitationKey("ExampleThesis2017");
        exampleThesis.setFiles(Collections.singletonList(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())));
        database.insertEntry(exampleThesis);

        indexer.rebuildIndex();

        // index with first entry
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(33, reader.numDocs());
        }

        BibEntry metadata = new BibEntry(StandardEntryType.Article);
        metadata.setCitationKey("MetaData2017");
        metadata.setFiles(Collections.singletonList(new LinkedFile("Metadata file", "metaData.pdf", StandardFileType.PDF.getName())));

        // when
        indexer.addToIndex(metadata);

        // then
        try (IndexReader reader = DirectoryReader.open(new NIOFSDirectory(context.getFulltextIndexPath()))) {
            assertEquals(34, reader.numDocs());
        }
    }
}
