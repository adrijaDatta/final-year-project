import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageSteganography {

    private static final int BITS_PER_CHANNEL = 2; // Use 2 LSBs per channel
    private static final int IMAGE_COUNT = 4; // We will hide 4 images

    // Hide 4 secret images inside cover image
    public static void hideImage(String coverPath, String[] secretPaths, String outputPath) throws IOException {
        if (secretPaths.length != IMAGE_COUNT) {
            throw new IllegalArgumentException("Exactly " + IMAGE_COUNT + " secret images are required.");
        }

        BufferedImage cover = ImageIO.read(new File(coverPath));
        int coverWidth = cover.getWidth();
        int coverHeight = cover.getHeight();

        // Read secret images
        BufferedImage[] secrets = new BufferedImage[IMAGE_COUNT];
        int totalSecretPixels = 0;
        for (int i = 0; i < IMAGE_COUNT; i++) {
            secrets[i] = ImageIO.read(new File(secretPaths[i]));
            totalSecretPixels += secrets[i].getWidth() * secrets[i].getHeight();
        }

        // Calculate required bits
        int totalSecretBits = IMAGE_COUNT * 64 + (totalSecretPixels * 24); // 64 bits for metadata per image + pixel bits
        int availableBits = coverWidth * coverHeight * BITS_PER_CHANNEL * 3;

        if (availableBits < totalSecretBits) {
            throw new IllegalArgumentException("Cover image is too small to hide the secret images.");
        }

        BufferedImage stego = new BufferedImage(coverWidth, coverHeight, BufferedImage.TYPE_INT_RGB);

        // Create bit stream for all 4 images
        StringBuilder bitStream = new StringBuilder();

        // Append each secret image's metadata & pixels
        for (int i = 0; i < IMAGE_COUNT; i++) {
            int w = secrets[i].getWidth();
            int h = secrets[i].getHeight();

            bitStream.append(toBinary(w, 32)); // width
            bitStream.append(toBinary(h, 32)); // height

            int[] pixels = new int[w * h];
            secrets[i].getRGB(0, 0, w, h, pixels, 0, w);
            for (int pixel : pixels) {
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                bitStream.append(toBinary(r, 8));
                bitStream.append(toBinary(g, 8));
                bitStream.append(toBinary(b, 8));
            }
        }

        int bitIndex = 0;
        for (int y = 0; y < coverHeight; y++) {
            for (int x = 0; x < coverWidth; x++) {
                int coverPixel = cover.getRGB(x, y);
                int r = (coverPixel >> 16) & 0xFF;
                int g = (coverPixel >> 8) & 0xFF;
                int b = coverPixel & 0xFF;

                if (bitIndex < bitStream.length())
                    r = setLSBs(r, bitStream, bitIndex, BITS_PER_CHANNEL);
                bitIndex += BITS_PER_CHANNEL;
                if (bitIndex < bitStream.length())
                    g = setLSBs(g, bitStream, bitIndex, BITS_PER_CHANNEL);
                bitIndex += BITS_PER_CHANNEL;
                if (bitIndex < bitStream.length())
                    b = setLSBs(b, bitStream, bitIndex, BITS_PER_CHANNEL);
                bitIndex += BITS_PER_CHANNEL;

                int newPixel = (r << 16) | (g << 8) | b;
                stego.setRGB(x, y, newPixel);
            }
        }

        ImageIO.write(stego, "png", new File(outputPath));
        System.out.println("✅ 4 secret images hidden in " + outputPath);
    }

    // Extract 4 secret images
    public static void extractImage(String stegoPath, String outputDir) throws IOException {
        BufferedImage stego = ImageIO.read(new File(stegoPath));
        int width = stego.getWidth();
        int height = stego.getHeight();

        StringBuilder bitStream = new StringBuilder();

        // Extract bits
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = stego.getRGB(x, y);
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;

                bitStream.append(getLSBs(r, BITS_PER_CHANNEL));
                bitStream.append(getLSBs(g, BITS_PER_CHANNEL));
                bitStream.append(getLSBs(b, BITS_PER_CHANNEL));
            }
        }

        int index = 0;
        for (int i = 0; i < IMAGE_COUNT; i++) {
            if (index + 64 > bitStream.length()) {
                throw new RuntimeException("Not enough data for image metadata.");
            }

            int w = Integer.parseInt(bitStream.substring(index, index + 32), 2);
            int h = Integer.parseInt(bitStream.substring(index + 32, index + 64), 2);
            index += 64;

            int totalBitsNeeded = w * h * 24;
            if (index + totalBitsNeeded > bitStream.length()) {
                throw new RuntimeException("Not enough data for image " + (i + 1));
            }

            BufferedImage secret = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int r = Integer.parseInt(bitStream.substring(index, index + 8), 2);
                    int g = Integer.parseInt(bitStream.substring(index + 8, index + 16), 2);
                    int b = Integer.parseInt(bitStream.substring(index + 16, index + 24), 2);
                    int pixel = (r << 16) | (g << 8) | b;
                    secret.setRGB(x, y, pixel);
                    index += 24;
                }
            }

            File outFile = new File(outputDir + "/extracted" + (i + 1) + ".png");
            ImageIO.write(secret, "png", outFile);
            System.out.println("✅ Extracted image " + (i + 1) + " to " + outFile.getAbsolutePath());
        }
    }

    private static String toBinary(int value, int bits) {
        String bin = Integer.toBinaryString(value);
        while (bin.length() < bits)
            bin = "0" + bin;
        return bin;
    }

    private static int setLSBs(int value, StringBuilder bits, int bitIndex, int numBits) {
        int mask = (1 << numBits) - 1;
        int newBits = 0;
        for (int i = 0; i < numBits && (bitIndex + i) < bits.length(); i++) {
            newBits = (newBits << 1) | (bits.charAt(bitIndex + i) - '0');
        }
        value = (value & ~mask) | newBits;
        return value;
    }

    private static String getLSBs(int value, int numBits) {
        StringBuilder sb = new StringBuilder();
        for (int i = numBits - 1; i >= 0; i--) {
            sb.append(((value >> i) & 1));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        String coverImage = "cover.png";
        String[] secretImages = {"secret1.png", "secret2.png", "secret3.png", "secret4.png"};
        String stegoImage = "stego.png";
        String outputDir = "output";

        new File(outputDir).mkdirs();

        hideImage(coverImage, secretImages, stegoImage);
        extractImage(stegoImage, outputDir);
    }
}
