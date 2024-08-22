// Owen Gregson, Peter Zhao
// Algorithms
// Final Project
// May 18, 2024

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.io.BufferedWriter;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Collection;
import java.util.ArrayList;
import java.io.IOException;
import java.io.FileWriter;
import java.util.Random;
import java.util.Arrays;
import java.util.List;

public class Final {
    private static int K = 60;
    private static int SEED = -2124786175;
    private static int MAX_ITERATIONS = 30;
    private static double MIN_MODIFICATIONS_RATIO = 0.007;
    private static final boolean DEBUG = false;
    private static final Random random = new Random(SEED);
	private static final List<Integer> allTimes = new ArrayList<>();
	private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static double[][] distanceMatrix;
    public static double cosineDistance(final Image a, final Image b) {
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        int pixelA, pixelB;
        for (int i = 0; i < a.rows(); i++) {
            for (int j = 0; j < a.columns(); j++) {
                pixelA = a.get(i, j);
                pixelB = b.get(i, j);
                dotProduct += pixelA * pixelB;
                normA += pixelA * pixelA;
                normB += pixelB * pixelB;
            }
        }
        return 1 - (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
    public static void initializeDistanceMatrix(final Image[] items, final Image[] centroids) {
        final int numItems = items.length, numCentroids = centroids.length;
        distanceMatrix = new double[numItems][numCentroids];
        IntStream.range(0, numItems).parallel().forEach(i ->
            IntStream.range(0, numCentroids).parallel().forEach(j ->
                distanceMatrix[i][j] = cosineDistance(items[i], centroids[j])
            )
        );
    }
    public static int nearestCosine(final int itemIndex) {
        return IntStream.range(0, distanceMatrix[itemIndex].length).parallel()
                .mapToObj(i -> new AbstractMap.SimpleEntry<>(i, distanceMatrix[itemIndex][i]))
                .min(Comparator.comparingDouble(AbstractMap.SimpleEntry::getValue))
                .map(AbstractMap.SimpleEntry::getKey)
                .orElse(-1);
    }

	public static Image computeCentroid(final Collection<Image> items) {
		final int rows = items.iterator().next().rows(), columns = items.iterator().next().columns();
		final int[][] centroidPixels = new int[rows][columns];
		items.parallelStream().forEach(item -> {
			for (int i = 0; i < rows; i++)
				for (int j = 0; j < columns; j++)
					centroidPixels[i][j] += item.get(i, j);
		});
		final int itemCount = items.size();
		for (int i = 0; i < rows; i++)
			for (int j = 0; j < columns; j++)
				centroidPixels[i][j] /= itemCount;
		final byte[][] centroidByteArray = new byte[rows][columns];
		for (int i = 0; i < rows; i++)
			for (int j = 0; j < columns; j++)
				centroidByteArray[i][j] = (byte) centroidPixels[i][j];
		if (DEBUG)
			System.out.printf("Computed new centroid from %d items\n", itemCount);
		return new Image(centroidByteArray, -1);
	}
	public static int findClusterWithMostLabel(final ConcurrentLinkedQueue<Image>[] clusters, final int label) {
		int maxCount = 0;
		int clusterIndex = -1;
		for (int i = 0; i < clusters.length; i++) {
			final int count = (int) clusters[i].stream().filter(image -> image.label() == label).count();
			if (count > maxCount) {
				maxCount = count;
				clusterIndex = i;
			}
		}
		return clusterIndex;
	}
    public static Image[] kMeansPlusPlusInitialization(final Image[] items, final int k) {
        final Image[] centroids = new Image[k];
        final Random random = new Random(SEED);
        // Step 1: Choose the first centroid randomly
        centroids[0] = items[random.nextInt(items.length)];
        if (DEBUG) System.out.printf("Initial centroid 0 is image %d\n", centroids[0].id());
        // Step 2: Compute the distance of each point to the nearest centroid
        final double[] distances = new double[items.length];
        Arrays.fill(distances, Double.MAX_VALUE);
        if (DEBUG) System.out.println("Computing distances to nearest centroid...");
        for (int i = 1; i < k; i++) {
            double totalDistance = 0.0;
            for (int j = 0; j < items.length; j++) {
                final double distance = cosineDistance(items[j], centroids[i - 1]);
                distances[j] = Math.min(distances[j], distance);
                totalDistance += distances[j];
            }
            if (DEBUG) System.out.printf("Total distance for centroid %d: %.2f\n", i, totalDistance);
            double r = random.nextDouble() * totalDistance;
            for (int j = 0; j < items.length; j++) {
                r -= distances[j];
                if (r <= 0) {
                    centroids[i] = items[j];
                    if (DEBUG) System.out.printf("Initial centroid %d is image %d\n", i, centroids[i].id());
                    break;
                }
            }
        }
        if (DEBUG) System.out.println("Initialization of centroids complete!");
        return centroids;
    }
    public static void kMeans(final Image[] items, final int k) {
        System.out.println("Initializing clustering with K-Means++ centroids...\n");
        final Image[] centroids = kMeansPlusPlusInitialization(items, k);
        System.out.println("Initialization complete! Beginning K-Means iteration.\n");
        final int[] labels = new int[items.length];
        final AtomicBoolean converged = new AtomicBoolean(true);
        int iterations = 0, iterationTimeMs;
        final ConcurrentLinkedQueue<Image>[] clusters = new ConcurrentLinkedQueue[k];
        for (int i = 0; i < k; i++) clusters[i] = new ConcurrentLinkedQueue<>();
        do {
            converged.set(true);
            final long startTime = System.currentTimeMillis();
            final AtomicInteger changes = new AtomicInteger(0);
            initializeDistanceMatrix(items, centroids);
            IntStream.range(0, items.length).parallel().forEach(i -> {
                final int nearestCentroid = nearestCosine(i);
                if (labels[i] != nearestCentroid) {
                    labels[i] = nearestCentroid;
                    converged.set(false);
                    changes.incrementAndGet();
                }
            });
            for (int i = 0; i < k; i++) clusters[i].clear();
            IntStream.range(0, items.length).parallel().forEach(i -> clusters[labels[i]].add(items[i]));
            final double weightNewCentroid = 0.4 - (0.3 * (iterations / (double) MAX_ITERATIONS));
            final double weightOldCentroid = 1.0 - weightNewCentroid;
            IntStream.range(0, k).forEach(i -> {
                if (clusters[i].isEmpty()) {
                    if (DEBUG) System.out.printf("Cluster %d is empty, reinitializing centroid\n", i);
                    centroids[i] = items[random.nextInt(items.length)];
                } else {
                    final Image newCentroid = computeCentroid(clusters[i]);
                    centroids[i] = weightedCentroidUpdate(centroids[i], newCentroid, weightOldCentroid, weightNewCentroid);
                }
                if (DEBUG) System.out.printf("Centroid %d updated\n", i);
            });
            iterations++;
            if (DEBUG) System.out.println("Iteration: " + iterations);
            iterationTimeMs = (int) (System.currentTimeMillis() - startTime);
            allTimes.add(iterationTimeMs);
            final double changeRatio = (double) changes.get() / items.length;
            printProgressBar(iterations, MAX_ITERATIONS, iterationTimeMs, changeRatio);
            if (changeRatio < MIN_MODIFICATIONS_RATIO) {
                System.out.printf("\n\n[!] Converged early at %d iterations! Only %.2f%% changes during this iteration.\n", iterations, changeRatio * 100);
                break;
            }
        } while (!converged.get() && iterations < MAX_ITERATIONS);
        displayResults(centroids, clusters, items, labels);
    }
    public static Image weightedCentroidUpdate(final Image oldCentroid, final Image newCentroid, final double weightOld, final double weightNew) {
        final int rows = oldCentroid.rows(), columns = oldCentroid.columns();
        final byte[][] updatedPixels = new byte[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                final int oldPixel = oldCentroid.get(i, j) & 0xFF;
                final int newPixel = newCentroid.get(i, j) & 0xFF;
                final int weightedPixel = (int) (oldPixel * weightOld + newPixel * weightNew);
                updatedPixels[i][j] = (byte) Math.min(Math.max(weightedPixel, 0), 255);
            }
        }
        return new Image(updatedPixels, -1);
    }
    public static double calculateClusterAccuracy(final Collection<Image> cluster, final int label) {
        return (double) cluster.stream().filter(image -> image.label() == label).count() / cluster.size();
    }
    public static Image[] classifyCentroids(final Image[] centroids) {
        final Viewer viewer = new Viewer(centroids, "Cluster Centroids", 4, false, true);
        boolean hasClassifiedAll = false;
        while (!hasClassifiedAll) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (Image image : viewer.getImages()) {
                if (image.label() == -1) {
                    hasClassifiedAll = false;
                    break;
                }
                hasClassifiedAll = true;
            }
        }
        final Image[] classified = viewer.getImages();
        viewer.close();
        return classified;
    }
    public static void displayResults(final Image[] centroids, final ConcurrentLinkedQueue<Image>[] clusters, final Image[] items, final int[] labels) {
        final Image[] processedCentroids = postProcessImages(centroids);
        System.out.println("\n");
        final int avg = (int) (allTimes.stream().mapToInt(Integer::intValue).average().orElse(0));
        final int total = allTimes.stream().mapToInt(Integer::intValue).sum();
        System.out.printf("Execution Complete!\nTotal time: %s\nAverage time per iteration: ~%s\n\nWaiting for user to complete classification...\n\n", formatTime(total), formatTime(avg));
        for (int i = 0; i < processedCentroids.length; i++) processedCentroids[i].setId(i + 1);
        final Image[] newLabeled = classifyCentroids(processedCentroids);
        System.out.println("User finished classifying. Results:\n");
        double totalAccuracy = 0.0;
        for (int i = 0; i < processedCentroids.length; i++) {
            final double clusterAccuracy = calculateClusterAccuracy(clusters[i], newLabeled[i].label());
            System.out.printf("Cluster %2d (Classified as %d) Accuracy: %6.2f%% (%s)\n", i + 1, newLabeled[i].label(), clusterAccuracy * 100, makeLetterGrade((int) (clusterAccuracy * 100)));
            totalAccuracy += clusterAccuracy;
        }
        totalAccuracy = (totalAccuracy / processedCentroids.length) * 100;
        System.out.printf("\nOverall Accuracy: %.2f%% (%s)\n", totalAccuracy, makeLetterGrade((int) totalAccuracy));
		System.out.printf("Average time per iteration: ~%s\nTotal time: %s\n", formatTime(avg), formatTime(total));
        final int targetClusterIndex = Math.abs(findClusterWithMostLabel(clusters, 1));
		executor.submit(() -> {
			try {
				System.out.println("\nExporting data to files...");
				exportCentroidImagesAndLabels(processedCentroids);
				exportClusterImages(clusters);
				exportIncorrectImagesAndLabels(items, labels, newLabeled);
				System.out.println("\nFile export complete!");
			} catch (IOException e) {
				System.err.println("Error exporting data: " + e.getMessage());
			}
		});
		final Viewer viewer = new Viewer(clusters[targetClusterIndex].toArray(new Image[0]), "Cluster #" + ((int) targetClusterIndex + 1), 4, true);
		viewer.displayAll(10);
    }
    public static void exportCentroidImagesAndLabels(final Image[] centroids) throws IOException {
		try (final BufferedWriter imageWriter = new BufferedWriter(new FileWriter("centroid-images"));
		final BufferedWriter labelWriter = new BufferedWriter(new FileWriter("centroid-labels"))) {
            for (final Image centroid : centroids) {
                imageWriter.write(centroid.toString() + "\n");
                labelWriter.write(centroid.label() + "\n");
            }
        }
    }
    public static void exportClusterImages(final ConcurrentLinkedQueue<Image>[] clusters) throws IOException {
        for (int i = 0; i < clusters.length; i++) {
            try (final BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("cluster%02d-images", i + 1)))) {
                for (final Image image : clusters[i]) {
                    writer.write(image.toString() + "\n");
                }
            }
        }
    }
    public static void exportIncorrectImagesAndLabels(final Image[] images, final int[] labels, final Image[] newLabeled) throws IOException {
        try (final BufferedWriter imageWriter = new BufferedWriter(new FileWriter("incorrect-images"));
             final BufferedWriter labelWriter = new BufferedWriter(new FileWriter("incorrect-labels"))) {
            for (int i = 0; i < images.length; i++) {
                if (images[i].label() != newLabeled[labels[i]].label()) {
                    imageWriter.write(images[i].toString() + "\n");
                    labelWriter.write(images[i].label() + "\n");
                }
            }
        }
    }
    public static String formatTime(final long ms) {
        if (ms < 1000) return ms + "ms";
        else if (ms < 60000) return String.format("%.1fs", ms / 1000.0);
        else if (ms < 3600000) return String.format("%dm", ms / 60000);
        else return String.format("%.1fh", ms / 3600000.0);

    }
    public static String makeLetterGrade(final int accuracy) {
        if (accuracy >= 93) return "A";
        else if (accuracy >= 90) return "A-";
        else if (accuracy >= 87) return "B+";
        else if (accuracy >= 83) return "B";
        else if (accuracy >= 80) return "B-";
        else if (accuracy >= 77) return "C+";
        else if (accuracy >= 73) return "C";
        else if (accuracy >= 70) return "C-";
        else if (accuracy >= 67) return "D+";
        else if (accuracy >= 63) return "D";
        else if (accuracy >= 60) return "D-";
        else return "F";
    }
    public static void printProgressBar(final int current, final int total, final int lastTime, final double changePercentage) {
        final int width = 50;
        final int progress = (int) ((double) current / total * width);
        System.out.print("\r[");
        for (int i = 0; i < progress; i++) System.out.print("=");
        for (int i = progress; i < width; i++) System.out.print(" ");
        final int percentage = (int) ((double) current / total * 100);
        System.out.printf("] %d%% (Stepped in %s, Change: %.2f%%)", percentage, formatTime(lastTime), changePercentage * 100);
    }
    public static Image[] preprocessImages(final Image[] images) {
        return Arrays.stream(images).parallel().map(image -> {
            final byte[][] pixels = image.pixels();
            // Step 1: Noise Reduction
            final byte[][] denoisedPixels = new byte[image.rows()][image.columns()];
			int sum, value;
			for (int i = 1; i < image.rows() - 1; i++) {
				for (int j = 1; j < image.columns() - 1; j++) {
					sum = 0;
					for (int ki = -1; ki <= 1; ki++) for (int kj = -1; kj <= 1; kj++) sum += pixels[i + ki][j + kj] & 0xFF;
					denoisedPixels[i][j] = (byte) (sum / 9);
				}
			}
            // Step 2: Contrast Enhancement
            for (int i = 0; i < image.rows(); i++) {
                for (int j = 0; j < image.columns(); j++) {
                    value = pixels[i][j] & 0xFF;
                    value = Math.min(Math.max((int) (value * 1.2), 0), 255);
                    pixels[i][j] = (byte) value;
                }
            }
            return new Image(pixels, image.label(), image.id());
        }).toArray(Image[]::new);
    }
    public static Image[] postProcessImages(final Image[] images) {
        return Arrays.stream(images).parallel().map(image -> {
            final byte[][] pixels = image.pixels();
            final byte[][] processedPixels = new byte[image.rows()][image.columns()];
            // Step 1: Gaussian blur
            final byte[][] blurredPixels = new byte[image.rows()][image.columns()];
            final int[][] gaussianKernel = {
                {1, 4, 6, 4, 1},
                {4, 16, 24, 16, 4},
                {6, 24, 36, 24, 6},
                {4, 16, 24, 16, 4},
                {1, 4, 6, 4, 1}
            };
            final int kernelSum = 256;
			int sum;
            for (int i = 2; i < image.rows() - 2; i++) {
                for (int j = 2; j < image.columns() - 2; j++) {
                    sum = 0;
                    for (int ki = -2; ki <= 2; ki++) for (int kj = -2; kj <= 2; kj++) sum += (pixels[i + ki][j + kj] & 0xFF) * gaussianKernel[ki + 2][kj + 2];
                    blurredPixels[i][j] = (byte) (sum / kernelSum);
                }
            }
            // Step 2: Subtract blurred img
            int originalValue, blurredValue, sharpenedValue;
            for (int i = 0; i < image.rows(); i++) {
                for (int j = 0; j < image.columns(); j++) {
                    originalValue = pixels[i][j] & 0xFF;
                    blurredValue = blurredPixels[i][j] & 0xFF;
                    sharpenedValue = originalValue + (originalValue - blurredValue);
                    sharpenedValue = Math.min(Math.max(sharpenedValue, 0), 255);
                    processedPixels[i][j] = (byte) sharpenedValue;
                }
            }
            return new Image(processedPixels, image.label(), image.id());
        }).toArray(Image[]::new);
    }
    public static void printBanner() {
        System.out.print("            _ _         ___         _   \n" + "  /\\  /\\___| | | __ _  / __\\_ _ ___| |_ \n" + " / /_/ / _ \\ | |/ _` |/ _\\/ _` / __| __|\n" + "/ __  /  __/ | | (_| / / | (_| \\__ \\ |_ \n" + "\\/ /_/ \\___|_|_|\\__,_\\/   \\__,_|___/\\__|\n" + "                                        \n\nBy Owen G & Peter Z (2024)\n\n");
    }
    public static void parseArguments(final String[] args) {
        if (args.length > 0) {
            if (args.length == 1) {
                System.out.print("Enter K: ");
                K = Integer.parseInt(System.console().readLine());
                System.out.print("Enter SEED: ");
                SEED = Integer.parseInt(System.console().readLine());
                System.out.print("Enter MAX_ITERATIONS: ");
                MAX_ITERATIONS = Integer.parseInt(System.console().readLine());
                System.out.print("Enter MIN_MODIFICATIONS_RATIO: ");
                MIN_MODIFICATIONS_RATIO = Double.parseDouble(System.console().readLine());
                System.out.println("\nConfiguration Successful!\n");
            } else {
                K = Integer.parseInt(args[0]);
                SEED = Integer.parseInt(args[1]);
                MAX_ITERATIONS = Integer.parseInt(args[2]);
                MIN_MODIFICATIONS_RATIO = Double.parseDouble(args[3]);
                System.out.printf("K = %d\nSEED = %d\nMAX_ITERATIONS = %d\nMIN_MODIFICATIONS_RATIO = %.3f\n\nConfiguration Successful!\n\n", K, SEED, MAX_ITERATIONS, MIN_MODIFICATIONS_RATIO);
            }
        } else System.out.printf("No arguments provided. Using default configuration.\n\nK = %d\nSEED = %d\nMAX_ITERATIONS = %d\nMIN_MODIFICATIONS_RATIO = %.3f\n\n", K, SEED, MAX_ITERATIONS, MIN_MODIFICATIONS_RATIO);
    }
    public static void main(final String[] args) throws IOException {
        printBanner();
        parseArguments(args);
        if (DEBUG) System.out.println("Loading MNIST dataset...");
        Image[] images = Image.read("./MNIST/train-images", "./MNIST/train-labels");
        if (DEBUG) System.out.println("MNIST dataset loaded");
        if (DEBUG) System.out.println("Preprocessing images...");
        images = preprocessImages(images);
        if (DEBUG) System.out.println("Images preprocessed");
        System.out.println("Running K-Means algorithm with K = " + K + " clusters and (up to) " + MAX_ITERATIONS + " iterations...\n");
		kMeans(images, K);
		executor.shutdown();
    }
}