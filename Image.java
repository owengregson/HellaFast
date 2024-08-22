// Owen Gregson, Peter Zhao
// Algorithms
// Final Project
// May 18, 2024

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

public class Image {

    public static final int WHITE = 0;
    public static final int BLACK = 255;
    public static final int UNKNOWN = -1;

    private static int count = 0;

    private int id;            // A (sequential) identifier for this image
    private int label;         // A label (typically the digit) for this image
    private int rows;          // Number of rows in this image
    private int columns;       // Number of columns in this image
    private byte[][] pixels;   // The pixels (grayscale) for this image

    public Image(int rows, int columns, int label, int id) {
        if (rows <= 0) {
            throw new IllegalArgumentException("Rows: " + rows);
        }
        if (columns <= 0) {
            throw new IllegalArgumentException("Columns: " + columns);
        }

        this.id = id;
        this.label = label;
        this.rows = rows;
        this.columns = columns;
        this.pixels = new byte[rows][columns];
    }

    public Image(int rows, int columns, int label) {
        this(rows, columns, label, Image.count++);
    }


    public Image(int rows, int columns) {
        this(rows, columns, UNKNOWN);
    }

    public Image(byte[][] pixels, int label, int id) {
        this.id = id;
        this.label = label;
        this.rows = pixels.length;
        this.columns = pixels[0].length;
        this.pixels = pixels;
    }

    public Image(byte[][] pixels, int label) {
        this(pixels, label, Image.count++);
    }

    public Image(byte[][] pixels) {
        this(pixels, UNKNOWN);
    }

    public int id() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int label() {
        return this.label;
    }

    public void label(int d) {
        this.label = d;
    }

    public int rows() {
        return this.rows;
    }

    public int columns() {
        return this.columns;
    }

    public byte[][] pixels() {
        return this.pixels;
    }

    public int get(int row, int column) {
        int value = this.pixels[row][column];
        return (value < 0) ? value + 256 : value;
    }

    public void set(int row, int column, int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Pixel: " + value);
        }
        this.pixels[row][column] = (byte) value;
    }

    public boolean equals(Image other) {
        if (this.rows != other.rows) return false;
        if (this.columns != other.columns) return false;
        if (this.label != other.label) return false;

        for (int row = 0; row < this.rows; row++) {
            for (int col = 0; col < this.columns; col++) {
                if (this.pixels[row][col] != other.pixels[row][col]) return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Image && this.equals((Image) other);
    }


    @Override
    public int hashCode() {
        int hash = this.rows * this.columns;
        for (int row = 0; row < this.rows; row++) {
            for (int col = 0; col < this.columns; col++) {
                hash = 257 * hash + this.pixels[row][col];
            }
        }
        return this.label * hash;
    }


    @Override
    public String toString() {
        // The numerical pixel values are formatted for each row so that
        // the columns are all aligned; newlines separate the rows.
        String result = "";
        String lineSeparator = "";
        String itemSeparator = "";
        for (int row = 0; row < this.rows; row++) {
            result += lineSeparator;
            itemSeparator = " ";
            for (int col = 0; col < this.columns; col++) {
                result += itemSeparator;
                result += String.format("%3s", get(row, col));
                itemSeparator = " ";
            }
            lineSeparator = "\n";
        }
        return result;
    }

    public String toString(int threshold) {
        // ASCII art version of the image using * (black) and spaces (white)
        // This image contains newlines ... and is meant to be printed
        // The threshold parameter determins the level at which pixels are
        // considered to be black (pixels < threshold are white)
        String result = "";
        String lineSeparator = "";
        String itemSeparator = "";
        for (int row = 0; row < this.rows; row++) {
            result += lineSeparator;
            itemSeparator = " ";
            for (int col = 0; col < this.columns; col++) {
                result += (get(row, col) >= threshold) ? '*' : ' ';
            }
            lineSeparator = "\n";
        }
        return result;
    }

    public static int rows(Image[] images) {
        return images.length > 0 ? images[0].rows() : 0;
    }

    public static int columns(Image[] images) {
        return images.length > 0 ? images[0].columns() : 0;
    }

    // -- Image File Reader/Writer ---------------------------------------------------------------

    public static class FileFormatException extends IOException {
        public FileFormatException(String message) {
            super(message);
        }
    }

    private static int readInt(BufferedInputStream input) throws IOException {
        // Reads an integer as four bytes (MSB first)
        int result = 0;
        for(int i = 0; i < 4; i++) {
            int value = input.read();
            result = 256 * result + value;
        }

        return result;
    }

    private static class ImageFileHeader {
        public static final int MAGIC = 2051;
        public int magic;
        public int count;
        public int rows;
        public int cols;
    }

    private static ImageFileHeader readImageFileHeader(BufferedInputStream input) throws IOException {
        ImageFileHeader header = new ImageFileHeader();
        header.magic = readInt(input);
        header.count = readInt(input);
        header.rows = readInt(input);
        header.cols = readInt(input);

        if (header.magic != ImageFileHeader.MAGIC) {
            throw new Image.FileFormatException("Bad magic (images): " + header.magic);
        } else if (header.count <= 0) {
            throw new Image.FileFormatException("Invalid image count: " + header.count);
        } else if (header.rows <= 0) {
            throw new Image.FileFormatException("Invalid row size: " + header.rows);
        } else if (header.cols <= 0) {
            throw new Image.FileFormatException("Invalid columns size: " + header.cols);
        }
        return header;
    }

    private static Image readImage(BufferedInputStream input, int rows, int columns) throws IOException {
        byte[][] pixels = new byte[rows][columns];
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                pixels[row][column] = (byte) input.read();
            }
        }
        return new Image(pixels);
    }

    private static Image[] readImages(BufferedInputStream input, int rows, int columns, int count) throws IOException {
        Image[] images = new Image[count];
        for (int i = 0; i < count; i++) {
            images[i] = readImage(input, rows, columns);
        }
        return images;
    }

    public static Image[] readImages(String filename) throws IOException {
        BufferedInputStream input = new BufferedInputStream(new FileInputStream(new File(filename)));
        ImageFileHeader header = readImageFileHeader(input);
        return readImages(input, header.rows, header.cols, header.count);
    }


    private static void writeInt(BufferedOutputStream output, int value) throws IOException {
        // Writes an integer as 4 bytes (MSB first)
        int b1 = (value >> 24) & 0xFF;
        int b2 = (value >> 16) & 0xFF;
        int b3 = (value >> 8) & 0xFF;
        int b4 = value & 0xFF;
        output.write(b1);
        output.write(b2);
        output.write(b3);
        output.write(b4);
    }

    private static void writeImageFileHeader(BufferedOutputStream output, Image[] images) throws IOException {
        int count = images.length;
        int rows = count > 0 ? images[0].rows() : 0;
        int cols = count > 0 ? images[0].columns() : 0;
        writeInt(output, ImageFileHeader.MAGIC);
        writeInt(output, count);
        writeInt(output, rows);
        writeInt(output, cols);
    }

    private static void writeImage(BufferedOutputStream output, Image image) throws IOException {
        for (int row = 0; row < image.rows(); row++) {
            for (int column = 0; column < image.columns(); column++) {
                output.write(image.get(row, column));
            }
        }
    }

    private static void writeImages(BufferedOutputStream output, Image[] images) throws IOException {
        for (Image image : images) {
            writeImage(output, image);
        }
    }

    public static void writeImages(Image[] images, String filename) throws IOException {
        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(new File(filename)));
        writeImageFileHeader(output, images);
        writeImages(output, images);
        output.close();
    }

    // -- Label File Reader/Writer ---------------------------------------------------------------

    private static class LabelFileHeader {
        public static final int MAGIC = 2049;
        public int magic;
        public int count;
    }

    private static LabelFileHeader readLabelFileHeader(BufferedInputStream input) throws IOException {
        LabelFileHeader header = new LabelFileHeader();
        header.magic = readInt(input);
        header.count = readInt(input);

        if (header.magic != LabelFileHeader.MAGIC) {
            throw new Image.FileFormatException("Bad magic (labels): " + header.magic);
        } else if (header.count < 0) {
            throw new Image.FileFormatException("Invalid label count: " + header.count);
        }
        return header;
    }

    private static int[] readLabels(BufferedInputStream input, int count) throws IOException {
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            result[i] = input.read();
        }
        return result;
    }

    public static int[] readLabels(String filename) throws IOException {
        BufferedInputStream input = new BufferedInputStream(new FileInputStream(new File(filename)));
        LabelFileHeader header = readLabelFileHeader(input);
        return readLabels(input, header.count);
    }

    private static void writeLabels(BufferedOutputStream output, Image[] images) throws IOException {
        writeInt(output, LabelFileHeader.MAGIC);
        writeInt(output, images.length);
        for (int i = 0; i < images.length; i++) {
            output.write(images[i].label());
        }
    }

    public static void writeLabels(Image[] images, String filename) throws IOException {
        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(new File(filename)));
        writeLabels(output, images);
        output.close();
    }


    public static Image[] read(String imageFilename, String labelFilename) throws IOException {
        // Reads an image file and optionally the associated labels
        Image[] images = readImages(imageFilename);
        if (labelFilename.length() > 0) {
            int[] labels = readLabels(labelFilename);
            assert (images.length == labels.length);
            for (int i = 0; i < images.length; i++) {
                images[i].label(labels[i]);
            }
        }
        return images;
    }

    public static void write(Image[] images, String imageFilename, String labelFilename) throws IOException {
        writeImages(images, imageFilename);
        writeLabels(images, labelFilename);
    }
}
