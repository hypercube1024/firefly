package com.firefly.utils.image.component;

import com.firefly.utils.Assert;
import com.firefly.utils.image.ImageUtils;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author Pengtao Qiu
 */
public class ImageComponent extends AbstractGraphicsComponent {

    public void setSrc(File file) {
        try {
            bufferedImage = ImageIO.read(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setSrc(InputStream inputStream) {
        try {
            bufferedImage = ImageIO.read(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setSrc(URL input) {
        try {
            bufferedImage = ImageIO.read(input);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setSrc(ImageInputStream stream) {
        try {
            bufferedImage = ImageIO.read(stream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public BufferedImage draw() {
        Assert.notNull(bufferedImage, "The image is not found. Please set the src");
        if (width > 0 || height > 0) {
            bufferedImage = ImageUtils.resize(bufferedImage, width, height);
        }
        drawChildren();
        return bufferedImage;
    }
}
