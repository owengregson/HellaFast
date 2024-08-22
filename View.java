// Owen Gregson, Peter Zhao
// Algorithms
// Final Project
// May 18, 2024

import java.util.ArrayList;
import java.io.IOException;

public class View {

    private static ArrayList<Integer> list = new ArrayList<>();
    public static Image[] images;
    private static int current = 0;
    private static int label = -1;


    private static void help() {
        System.out.println("Usage: java View <options> <image IDs>");
        System.out.println();
        System.out.println("   Allows images in the MNIST data set to be viewed (in a GUI) or dumped");
        System.out.println("   to standard output (either as an array of numeric values or as ASCII art");
        System.out.println("   (white => ' '; black => '*').  Options allow the selection of only those");
        System.out.println("   for a particular label (digit) or a specific list of image IDs to be viewed");
        System.out.println("   A classification option for the GUI allows users to classify (set the image");
        System.out.println("   labels) which will be written to labels file when done");
        System.out.println();
        System.out.println("   -help               Display this help message");
        System.out.println("   -test               Use the test-images & test-labels data files");
        System.out.println("   -train              Use the train-images & train-labels data files");
        System.out.println("   -nolabels           Do not read the labels data file");
        System.out.println("   -in <prefix>        Shorthand for -input");
        System.out.println("   -input <prefix>     File names are <prefix>-images & <prefix>-labels");
        System.out.println("   -out <prefix>       Shorthand for -output");
        System.out.println("   -output <prefix>    Use <prefix>-labels for the output labels file name (-classify)");
        System.out.println("   -images <filename>  Use <filename> for the images data file");
        System.out.println("   -labels <filename>  Use <filename> for the labels data file");
        System.out.println("   -classify           Allow users to classify (label) each image");
        System.out.println("   -dump               Dump the images to standard output");
        System.out.println("   -threshold 0        Dump numerical values for each pixel (default)");
        System.out.println("   -threshold <value>  Threshold value for black level (displayed as *)");
        System.out.println("   -label <label>      Only display images for the specified label");
        System.out.println("   -digit <digit>      Alternative for -label");
        System.out.println("   -scale <scale>      Scale each image by the specified scaling factor");
        System.out.println("   -sleep <msec>       Display all images for <msec> milliseconds each");
        System.out.println("   <image id>          Explicit set of image IDs to display");
        System.out.println();
    }

    private static void dump(Image image, int threshold) {
        System.out.printf("Image #%d:  %d\n", image.id(), image.label());
        if (threshold > 0) {
            System.out.println(image.toString(threshold));
        } else {
            System.out.println(image.toString());
        }
        System.out.println();
    }

    private static void dump(Image[] images, int threshold) {
        for (Image image : images) {
            dump(image, threshold);
        }
    }

    private static Image[] filter(Image[] images, int label) {
        int count = 0;
        for (Image image : images) {
            if (image.label() == label) count++;
        }

        Image[] result = new Image[count];
        count = 0;
        for (Image image : images) {
            if (image.label() == label) {
                result[count++] = image;
            }
        }

        return result;
    }

    private static Image[] get(Image[] images, ArrayList<Integer> list) {
        Image[] result = new Image[list.size()];
        int count = 0;
        for (int index : list) {
            result[count++] = images[index];
        }
        return result;
    }

    private static int checkValue(String arg, int lo, int hi) {
        int value = Integer.parseInt(arg);
        if (value < 0 || value > hi) {
            throw new IllegalArgumentException();
        }
        return value;
    }

    public static void main(String[] args) {
        String imageFileName = "train-images";
        String labelFileName = "train-labels";
		String outputFileName = "";
        String option = "";
        boolean classify = false;
        boolean dump = false;
        int threshold = 0;
        int scale = 10;
        int sleep = 0;
        int errors = 0;

        for (String arg : args) {
            if (option.length() > 0 && arg.startsWith("-")) {
                System.err.println("No value for : " + option);
                option = "";
                errors++;
            }

            switch (arg) {
                case "-test":
                    imageFileName = "test-images";
                    labelFileName = "test-labels";
                    continue;

                case "-train":
                    imageFileName = "train-images";
                    labelFileName = "train-labels";
                    continue;

                case "-nolabels":
                    labelFileName = "";
                    continue;

                case "-classify":
                    classify = true;
                    continue;

                case "-dump":
                    dump = true;
                    continue;

                case "-help":
                    help();
                    return;

                case "-in":
                case "-out":
                case "-input":
                case "-output":
                case "-images":
                case "-labels":
                case "-digit":
                case "-label":
                case "-sleep":
                case "-scale":
                case "-threshold":
                    option = arg;
                    continue;

                default:
                    if (arg.startsWith("-")) {
                        System.err.println("Invalid option: " + arg);
                        errors++;
                        continue;
                    }
            }

            try {
                switch (option) {
                    case "-in":
                    case "-input":
                        imageFileName = arg + "-images";
                        labelFileName = arg + "-labels";
                        option = "";
                        continue;

                    case "-out":
                    case "-output":
                        outputFileName = arg + "-labels";
                        option = "";
                        continue;

                    case "-images":
                        imageFileName = arg;
                        option = "";
                        continue;

                    case "-labels":
                        labelFileName = arg;
                        option = "";
                        continue;

                    case "-digit":
                    case "-label":
                        label = checkValue(arg, 0, 9);
                        option = "";
                        continue;

                    case "-scale":
                        scale = checkValue(arg, 0, 25);
                        option = "";
                        continue;

                    case "-sleep":
                        sleep = checkValue(arg, 0, 60_000);
                        option = "";
                        continue;

                    case "-threshold":
                        threshold = checkValue(arg, 0, 255);
                        option = "";
                        continue;

                    case "":
                    default:
                        list.add(Integer.parseInt(arg));
                        option = "";
                        continue;
                }

            } catch (IllegalArgumentException e) {
                if (option.length() > 0) {
                    System.err.println("Invalid value for " + option + ": " + arg);
                } else {
                    System.err.println("Invalid image number: " + arg);
                }
                option = "";
                errors++;
                continue;
            }
        }

        if (errors > 0) return;

        try {
            images = Image.read(imageFileName, labelFileName);
        } catch (IOException e) {
            System.err.println("Could not read dataset: " + e.getMessage());
            return;
        }

        if (list.size() > 0) {
            images = get(images, list);
        }
        if (label >= 0) {
            images = filter(images, label);
        }

        if (dump) {
            dump(images, threshold);

        } else {
            String title = classify ? "Clusters" : "MNIST";
            boolean labels = labelFileName.length() > 0 && !classify;
            Viewer viewer = new Viewer(images, title, scale, labels, classify);
            if (sleep > 0) {
                viewer.displayAll(sleep);
            }
        }
    }
}
