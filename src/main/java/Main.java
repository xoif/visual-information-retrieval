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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Main {

    private static String dash;
    private static boolean useCEDD = true;

    static {
        dash = System.getProperty("file.separator");
    }

    public static void main(String[] args) {

        String simplicity50Path = "src"+dash+"main"+dash+"resources"+dash+"simplicity50";
        String searchImageFilePath = "src"+dash+"main"+dash+"resources"+dash+"simplicity50"+dash+"306.jpg";

        try {
            index(simplicity50Path);
            search(searchImageFilePath);
            useCEDD = false;
            search(searchImageFilePath);
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
        Path indexPath = Paths.get("src"+dash+"main"+dash+"resources"+dash+"index");

        deleteDirectory(indexPath.toFile());  //deleting index directory if there is one from previous indexing
        IndexWriter iw = new IndexWriter(FSDirectory.open(indexPath), conf);
        // Iterating through images building the low level features
        for (Iterator<String> it = images.iterator(); it.hasNext(); ) {
            String imageFilePath = it.next();
            //System.out.println("Indexing " + imageFilePath);
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

    public static void search(String queryImagePath) throws IOException {

        BufferedImage img = null;
        File f = new File(queryImagePath);
        if (f.exists()) {
            try {
                img = ImageIO.read(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        IndexReader ir = DirectoryReader.open(FSDirectory.open(Paths.get("src"+dash+"main"+dash+"resources"+dash+"index")));

        ImageSearcher searcher;
        if (useCEDD) {
            searcher = new GenericFastImageSearcher(50, CEDD.class);
        } else {
            searcher = new GenericFastImageSearcher(50, SimpleColorHistogram.class);
        }

        ImageSearchHits hits = searcher.search(img, ir);
        List<Double> hitsAtPosition = new ArrayList<>();

        for (int i = 0; i < hits.length(); i++) {
            String fileName = ir.document(hits.documentID(i)).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0];
            System.out.println(hits.score(i) + ": \t" + fileName.substring(fileName.length() - 7) + " in group: " + calculateGroup(fileName).toString());
            if (calculateGroup(fileName) == ImageGroup.BUSSES) {
                hitsAtPosition.add((double) (i + 1));
                System.out.println("correct image found at position " + (i + 1));
            }
        }
        System.out.println("Mean Average Precision of " + (useCEDD ? "CEDD is " : "Color Histogram is ") + calculateMap(hitsAtPosition));
    }

    private static double calculateMap(List<Double> hitsAtPosition) {

        List<Double> precision = new ArrayList<>();

        for (int i = 1; i <= hitsAtPosition.size(); i++) {
            precision.add(i / hitsAtPosition.get(i - 1));
        }
        return precision.parallelStream().mapToDouble(precisionValue -> precisionValue).reduce(0, Double::sum)/precision.size();
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

    public static boolean deleteDirectory(File directory) {
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null!=files){
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
    }

}

