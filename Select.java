// Owen Gregson, Peter Zhao
// Algorithms
// Final Project
// May 18, 2024

import java.util.ArrayList;
import java.io.IOException;

public class Select {

    private static void help() {
        System.out.println("Usage: java Select <options> <image IDs>");
        System.out.println();
        System.out.println("   Produces an output image data set containing selected images from");
        System.out.println("   a specified input data set.  Images may be selected explicitly (a");
        System.out.println("   list of image IDs) or for a specific label or from a specific starting");
        System.out.println("   image ID.  The number of images selected can be controled by specifying");
        System.out.println("   a count.  Output file name(s) must be given to produce a new image data");
        System.out.println("   set (otherwise, the -verbose and -statistics options can be used to");
        System.out.println("   display information about the selected images");
        System.out.println();
        System.out.println("   -help               Display this help message");
        System.out.println("   -test               Use the test-images & test-labels data files for intput");
        System.out.println("   -train              Use the train-images & train-labels data files for intput");
        System.out.println("   -in <prefix>        Shorthand for -input");
        System.out.println("   -input <prefix>     Input file names are <prefix>-images & <prefix>-labels");
        System.out.println("   -out <prefix>       Shorthand for -output");
        System.out.println("   -output <prefix>    Output file names are <prefix>-images & <prefix>-labels");
        System.out.println("   -images <filename>  Use <filename> for the input images data file");
        System.out.println("   -labels <filename>  Use <filename> for the input labels data file");
        System.out.println("   -label <label>      Select images having the specified label");
        System.out.println("   -digit <digit>      Alternative for -label");
        System.out.println("   -start <ID>         Select images staring with the given image ID");
        System.out.println("   -count <count>      Count of the number of images to select");
        System.out.println("   -verbose            Display the image IDs of the selected images");
        System.out.println("   -statistics         Display statistics for the selected images");
        System.out.println("   -stats              Shorthand for -statistics");
        System.out.println("   <image id>          Explicit set of image IDs to select");
        System.out.println();
    }

    private static int count(Image[] images, int start, int digit, int limit) {
        int count = 0;
        for (Image image : images) {
            if (digit >= 0 && image.label() != digit) continue;
            if (image.id() < start) continue;
            if (count >= limit) break;
            count++;
        }
        return count;
    }

    public static Image[] select(Image[] images, int start, int digit, int limit) {
        int count = count(images, start, digit, limit);
        Image[] result = new Image[count];

        int index = 0;
        for (Image image : images) {
            if (digit >= 0 && image.label() != digit) continue;
            if (image.id() < start) continue;
            if (index >= count) break;
            result[index++] = image;
        }
        return result;
    }

    public static Image[] select(Image[] images, int digit, int count) {
        return select(images, 0, digit, count);
    }

    public static Image[] select(Image[] images, int limit) {
        return select(images, -1, images.length);
    }

    public static Image[] selectDigit(Image[] images, int digit) {
        return select(images, digit, images.length);
    }

    public static Image[] select(Image[] images, ArrayList<Integer> list) {
        Image[] result = new Image[list.size()];
        int count = 0;
        for (int index : list) {
            result[count++] = images[index];
        }
        return result;
    }


    private static void printStatistics(Image[] images) {
        int[] count = new int[10];
        int unknown = 0;

        for (Image image : images) {
            int label = image.label();
            if (label < 0) {
                unknown++;
            } else {
                count[label]++;
            }
        }

        System.out.printf("Unknown: %d\n", unknown);
        for (int i = 0; i < 10; i++) {
            System.out.printf("Digit %d: %d\n", i, count[i]);
        }
        System.out.printf("  TOTAL: %d\n", images.length);
    }


    private static int check(String option, String arg) {
        if (option.length() == 0) return 0;
        if (arg.startsWith("-") || arg.startsWith("+")) {
            System.err.println("No value for " + option);
            return 1;
        } else {
            return 0;
        }
    }

    private static int getIntegerValue(String arg, int min, int max) {
        int value = Integer.parseInt(arg);
        if (value < min) {
            throw new IllegalArgumentException("Value too small");
        } else if (value > max) {
            throw new IllegalArgumentException("Value too big");
        }
        return value;
    }

    private static int getIntegerValue(String arg, int min) {
        return getIntegerValue(arg, min, Integer.MAX_VALUE);
    }

    public static void main(String[] args) {
        ArrayList<Integer> list = new ArrayList<>();
        String inputImageFileName = "train-images";
        String inputLabelFileName = "train-labels";
        String outputImageFileName = "";
        String outputLabelFileName = "";
        String option = "";
        boolean statistics = false;
        boolean output = false;
        boolean silent = true;
        int start = 0;
        int count = 0;
        int digit = -1;
        int errors = 0;

        for (String arg : args) {
            errors += check(option, arg);
            switch (arg) {
                case "-in":
                case "-out":
                case "-input":
                case "-output":
                case "-images":
                case "-labels":

                case "-start":
                case "-count":
                case "-digit":
                case "-label":
                    option = arg;
                    continue;

                case "-train":
                    inputImageFileName = "train-images";
                    inputLabelFileName = "train-labels";
                    continue;

                case "-test":
                    inputImageFileName = "test-images";
                    inputLabelFileName = "test-labels";
                    continue;

                case "-stats":
                case "-statistics":
                    statistics = true;
                    continue;

                case "+stats":
                case "+statistics":
                    statistics = false;
                    continue;

                case "-silent":
                case "+verbose":
                    silent = true;
                    continue;

                case "+silent":
                case "-verbose":
                    silent = false;
                    continue;

                case "-help":
                    help();
                    return;

                default:
                    if (arg.startsWith("-") || arg.startsWith("+")) {
                        System.err.println("Invalid option: " + arg);
                        errors++;
                        continue;
                    }
            }

            try {
                switch (option) {
                    case "-start":
                        start = getIntegerValue(arg, 0);
                        break;

                    case "-count":
                        count = getIntegerValue(arg, 0);
                        break;

                    case "-labe;":
                    case "-digit":
                        digit = getIntegerValue(arg, 0);
                        break;

                    case "-images":
                        inputImageFileName = arg;
                        break;

                    case "-labels":
                        inputLabelFileName = arg;
                        break;

                    case "-in":
                    case "-input":
                        inputImageFileName = arg + "-images";
                        inputLabelFileName = arg + "-labels";
                        break;

                    case "-out":
                    case "-output":
                        outputImageFileName = arg + "-images";
                        outputLabelFileName = arg + "-labels";
                        output = true;
                        break;

                    default:
                        list.add(getIntegerValue(arg, 0));
                        break;
                }

            } catch (NumberFormatException e) {
                System.err.println("Invalid value for " + option + ": " + arg);
                errors++;
            }
            option = "";
        }

        if (errors > 0) return;

        Image[] images;
        try {
            images = Image.read(inputImageFileName, inputLabelFileName);
        } catch (IOException e) {
            System.err.println("Could not read input files: " + e.getMessage());
            return;
        }

        if (count <= 0) count = images.length;
        if (list.size() > 0) {
            images = select(images, list);
        } else {
            images = select(images, start, digit, count);
        }

        if (!silent) {
            for (Image image : images) {
                System.out.println(image.id());
            }
        }

        if (output) {
            try {
                Image.write(images, outputImageFileName, outputLabelFileName);
            } catch (IOException e) {
                System.err.println("Could not write output files: " + e.getMessage());
            }
        }

        if (statistics) {
            printStatistics(images);
        }
    }
}
