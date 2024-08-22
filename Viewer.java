// Owen Gregson, Peter Zhao
// Algorithms
// Final Project
// May 18, 2024

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class Viewer {

    private final Image[] images;
    private Image image;
    private int current;
    boolean classify;

    // -- Buttons ----------------------------------------------------------------------

    private static abstract class SimpleButton extends JButton implements ActionListener {
        public SimpleButton(String label) {
            super(label);
            this.addActionListener(this);
        }

        public abstract void actionPerformed(ActionEvent event);
    }

    private JButton nextButton = new SimpleButton("Next") {
        @Override
        public void actionPerformed(ActionEvent event) {
            if (current < images.length-1) {
                display(++current);
            }
        }
    };

    private JButton prevButton = new SimpleButton("Prev") {
        @Override
        public void actionPerformed(ActionEvent event) {
            if (current > 0) {
                display(--current);
            }
        }
    };


    private class DigitButton extends SimpleButton {

        private int digit;

        public DigitButton(int digit) {
            super(Integer.toString(digit));
            this.digit = digit;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            image.label(digit);
            if (current < images.length-1) {
                display(++current);
            }
        }
    }

    // -- Labels -----------------------------------------------------------------------

    private static class CenteredLabel extends JLabel {
        public CenteredLabel(int fontsize) {
            super("0");
            Font font = this.getFont();
            this.setFont(new Font(font.getName(), Font.BOLD, fontsize));
            this.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            this.setAlignmentY(JLabel.CENTER_ALIGNMENT);
        }
    }

    private class ImageID extends CenteredLabel {
        public ImageID() {
            super(15);
        }

        public void setID(int id) {
            if (classify) {
                this.setText(String.format("Cluster #%d", id));
            } else {
                this.setText(String.format("Image #%d", id));
            }
        }
    }

    private class ImageLabel extends CenteredLabel {
        public ImageLabel() {
            super(50);
        }

        public void setLabel(int label) {
            this.setText(Integer.toString(label));
        }
    }

    // -- Image Panel ------------------------------------------------------------------

    private class ImagePanel extends JPanel {

        private Image image;
        private final int scale;
        private final int height;
        private final int width;

        public ImagePanel(int rows, int columns, int scale) {
            this.image = null;
            this.scale = scale;
            this.height = scale * rows;
            this.width = scale * columns;
            this.setPreferredSize(new Dimension(width, height));
        }

        public void setImage(Image image) {
            this.image = image;
        }

        @Override
        public void paint(Graphics g) {
            int offset = (this.getWidth() - this.width) / 2;
            for (int row = 0; row < image.rows(); row++) {
                for (int col = 0; col < image.columns(); col++) {
                    g.setColor(grayToColor(image.get(row, col)));
                    g.fillRect(offset + col*scale, row*scale, scale, scale);
                }
            }
        }

        private Color grayToColor(int shade) {
            shade = 255 - shade;
            return new Color(shade, shade, shade);
        }
    }

    // -- GUI Constructor --------------------------------------------------------------

    private JFrame     frame;
    private ImageID    imageID;
    private ImageLabel imageLabel;
    private ImagePanel imagePanel;

    public Viewer(Image[] images, String title, int scaling, boolean labels, boolean classify) {
        this.classify = classify;
        this.image = images[0];
        this.images = images;
        this.current = 0;

        this.frame = new JFrame(title);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        int rows = Image.rows(images);
        int cols = Image.columns(images);
        this.imageID = new ImageID();
        this.imageLabel = new ImageLabel();
        this.imagePanel = new ImagePanel(rows, cols, scaling);
        this.imagePanel.setImage(this.image);

        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));
        if (labels && !classify) labelPanel.add(this.imageLabel);
        labelPanel.add(this.imageID);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);

        JPanel digitPanel = new JPanel();
        digitPanel.setLayout(new GridLayout(2,5));
        digitPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        digitPanel.setAlignmentY(JPanel.TOP_ALIGNMENT);
        for (int digit = 0; digit < 10; digit++) {
            digitPanel.add(new DigitButton(digit));
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        panel.setAlignmentY(JPanel.TOP_ALIGNMENT);
        panel.add(imagePanel);
        panel.add(labelPanel);
        panel.add(buttonPanel);
        if (classify) panel.add(digitPanel);
        panel.doLayout();

        Container content = frame.getContentPane();
        content.add(panel);
        frame.pack();
        frame.setVisible(true);
        nextButton.requestFocus();
        display(0);
    }

    public Viewer(Image[] images, String title, int scaling, boolean labels) {
        this(images, title, scaling, labels, false);
    }

    // -- Display Updater --------------------------------------------------------------

    private void display(Image image) {
        this.image = image;
        this.imagePanel.setImage(image);
        this.imageID.setID(image.id());
        this.imageLabel.setLabel(image.label());
        this.frame.update(frame.getGraphics());
        // frame.update();
    }

    private void display(int index) {
        this.current = index;
        display(this.images[index]);
    }

	private void display() {
		display(this.current);
	}

	public Image[] getImages() {
		return images;
	}

	public void close() {
		frame.dispose();
	}

    public void displayAll(int milliseconds) {
        for (Image image : images) {
            display(image);
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
            }
        }
    }

    public void displayAll() {
        displayAll(100);
    }
}
