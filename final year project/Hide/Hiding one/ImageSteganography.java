import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageSteganography {

    private static final int BITS_PER_CHANNEL = 2; // Use 2 LSBs per channel

    // Hide secret image inside cover image
    public static void hideImage(String coverPath, String secretPath, String outputPath) throws IOException {
        BufferedImage cover = ImageIO.read(new File(coverPath));
        BufferedImage secret = ImageIO.read(new File(secretPath));

        int coverWidth = cover.getWidth();
        int coverHeight = cover.getHeight();
        int secretWidth = secret.getWidth();
        int secretHeight = secret.getHeight();

        // Calculate required bits
        int totalSecretPixels = secretWidth * secretHeight;
        int totalSecretBits = totalSecretPixels * 24; // 24 bits per pixel (RGB)
        int availableBits = coverWidth * coverHeight * BITS_PER_CHANNEL * 3;

        if (availableBits < totalSecretBits) {
            throw new IllegalArgumentException("Cover image is too small to hide the secret image.");
        }

        // Create a copy of the cover image
        BufferedImage stego = new BufferedImage(coverWidth, coverHeight, BufferedImage.TYPE_INT_RGB);

        // Convert secret image to bits
        int[] secretPixels = new int[secretWidth * secretHeight];
        secret.getRGB(0, 0, secretWidth, secretHeight, secretPixels, 0, secretWidth);

        StringBuilder bitStream = new StringBuilder();
        // Add width and height (32 bits each)
        bitStream.append(toBinary(secretWidth, 32));
        bitStream.append(toBinary(secretHeight, 32));
        // Add pixel data
        for (int pixel : secretPixels) {
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;
            bitStream.append(toBinary(r, 8));
            bitStream.append(toBinary(g, 8));
            bitStream.append(toBinary(b, 8));
        }

        int bitIndex = 0;
        for (int y = 0; y < coverHeight; y++) {
            for (int x = 0; x < coverWidth; x++) {
                int coverPixel = cover.getRGB(x, y);
                int r = (coverPixel >> 16) & 0xFF;
                int g = (coverPixel >> 8) & 0xFF;
                int b = coverPixel & 0xFF;

                // Modify only if there are bits left
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
        System.out.println("✅ Secret image hidden in " + outputPath);
    }

    // Extract the secret image
    public static void extractImage(String stegoPath, String outputPath) throws IOException {
        BufferedImage stego = ImageIO.read(new File(stegoPath));
        int width = stego.getWidth();
        int height = stego.getHeight();

        StringBuilder bitStream = new StringBuilder();

        // Extract bits from stego image
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

        // Read secret width and height
        if (bitStream.length() < 64) {
            throw new RuntimeException("Not enough data to read metadata.");
        }
        int secretWidth = Integer.parseInt(bitStream.substring(0, 32), 2);
        int secretHeight = Integer.parseInt(bitStream.substring(32, 64), 2);

        int totalBitsNeeded = 64 + (secretWidth * secretHeight * 24);
        if (bitStream.length() < totalBitsNeeded) {
            throw new RuntimeException("Not enough data for the full secret image. Possibly wrong cover/secret size.");
        }

        BufferedImage secret = new BufferedImage(secretWidth, secretHeight, BufferedImage.TYPE_INT_RGB);

        int index = 64;
        for (int y = 0; y < secretHeight; y++) {
            for (int x = 0; x < secretWidth; x++) {
                int r = Integer.parseInt(bitStream.substring(index, index + 8), 2);
                int g = Integer.parseInt(bitStream.substring(index + 8, index + 16), 2);
                int b = Integer.parseInt(bitStream.substring(index + 16, index + 24), 2);
                int pixel = (r << 16) | (g << 8) | b;
                secret.setRGB(x, y, pixel);
                index += 24;
            }
        }

        ImageIO.write(secret, "png", new File(outputPath));
        System.out.println("✅ Secret image extracted to " + outputPath);
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
        String secretImage = "secret.png";
        String stegoImage = "stego.png";
        String extractedImage = "extracted.png";

        hideImage(coverImage, secretImage, stegoImage);
        extractImage(stegoImage, extractedImage);
    }
}
