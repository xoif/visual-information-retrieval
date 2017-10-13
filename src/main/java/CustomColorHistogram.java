import net.semanticmetadata.lire.imageanalysis.features.global.SimpleColorHistogram;

import java.awt.image.BufferedImage;

/**
 * Created by paetow on 13.10.17.
 */
public class CustomColorHistogram extends SimpleColorHistogram {

    @Override
    public void extract(BufferedImage image) {

        BufferedImage out = image.getSubimage(image.getWidth() * 1/3, image.getHeight() * 1/3, image.getWidth() * 2/3, image.getHeight() * 2/3);
        super.extract(out);
    }

    @Override
    public String getFeatureName() {
        return "Custom Color Histogram";
    }
}
