import net.semanticmetadata.lire.builders.DocumentBuilder;
import net.semanticmetadata.lire.builders.GlobalDocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.features.global.CEDD;
import net.semanticmetadata.lire.imageanalysis.features.global.SimpleColorHistogram;
import net.semanticmetadata.lire.searchers.GenericFastImageSearcher;
import net.semanticmetadata.lire.searchers.ImageSearchHits;
import net.semanticmetadata.lire.searchers.ImageSearcher;
import net.semanticmetadata.lire.utils.FileUtils;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Main {

    public static void main(String[] args) {

        String simplicity50Path = "src\\main\\resources\\simplicity50";
        String searchImageFilePath = "src\\main\\resources\\simplicity50\\306.jpg";

        try {
            index(simplicity50Path);
            search(searchImageFilePath, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void index(String sourcePath) throws IOException {
        ArrayList<String> images = FileUtils.getAllImages(new File(sourcePath), true);

        // Creating a CEDD document builder and indexing all files.
        GlobalDocumentBuilder globalDocumentBuilder = new GlobalDocumentBuilder(CEDD.class);
        globalDocumentBuilder.addExtractor(CEDD.class);
        globalDocumentBuilder.addExtractor(SimpleColorHistogram.class);

        // Creating an Lucene IndexWriter
        IndexWriterConfig conf = new IndexWriterConfig(new WhitespaceAnalyzer());
        IndexWriter iw = new IndexWriter(FSDirectory.open(Paths.get("src\\main\\resources\\index")), conf);
        // Iterating through images building the low level features
        for (Iterator<String> it = images.iterator(); it.hasNext(); ) {
            String imageFilePath = it.next();
            System.out.println("Indexing " + imageFilePath);
            try {
                BufferedImage img = ImageIO.read(new FileInputStream(imageFilePath));
                Document document = globalDocumentBuilder.createDocument(img, imageFilePath);
                iw.addDocument(document);
            } catch (Exception e) {
                System.err.println("Error reading image or indexing it.");
                e.printStackTrace();
            }
        }
        // closing the IndexWriter
        iw.close();
    }

    public static void search(String queryImagePath, Boolean withCEDD) throws IOException {

        BufferedImage img = null;
        File f = new File(queryImagePath);
        if (f.exists()) {
            try {
                img = ImageIO.read(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        IndexReader ir = DirectoryReader.open(FSDirectory.open(Paths.get("src\\main\\resources\\index")));

        ImageSearcher searcher;
        if (withCEDD) {
            searcher = new GenericFastImageSearcher(50, CEDD.class);
        } else {
            searcher = new GenericFastImageSearcher(50, SimpleColorHistogram.class);
        }

        ImageSearchHits hits = searcher.search(img, ir);
        List<Integer> hitAtPosition = new ArrayList<>();

        for (int i = 0; i < hits.length(); i++) {
            String fileName = ir.document(hits.documentID(i)).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0];
            System.out.println(hits.score(i) + ": \t" + fileName + " in group: " + calculateGroup(fileName).toString());
            if (calculateGroup(fileName) == ImageGroup.BUSSES) {
                hitAtPosition.add(i + 1);
                System.out.println("correct image found at position " + (i + 1));
            }
        }
    }


    protected enum ImageGroup {
        BUSSES, DINOSAURS, ELEPHANTS, FLOWERS,
        HORSES, NONE
    }

    private static ImageGroup calculateGroup(String filename) {
        String group = filename.substring(filename.length() - 7, filename.length() - 6);
        switch (Integer.parseInt(group)) {
            case 3: return ImageGroup.BUSSES;
            case 4: return ImageGroup.DINOSAURS;
            case 5: return ImageGroup.ELEPHANTS;
            case 6: return ImageGroup.FLOWERS;
            case 7: return ImageGroup.HORSES;
            default: return ImageGroup.NONE;
        }
    }

}

