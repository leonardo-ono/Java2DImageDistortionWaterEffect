package imagedistortioneffecttest;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author leonardo
 */
public class View extends JPanel {

    private BufferedImage texture;
    private boolean running;
    private Point2D.Double[][] points;
    private long[][] startTime;
    private double[][] intensity;
    private double[][] intensity2;
    private List<Face> faces = new ArrayList<Face>();
    
    private Point mouse = new Point();
    private Rectangle mouseRectangle = new Rectangle();
    
    private boolean wireframeVisible = true;
    
    private class Face {
        Point2D[] points;
        Polygon polygon = new Polygon();
        AffineTransform t1 = new AffineTransform();
        AffineTransform t2 = new AffineTransform();
        
        public Face(Point2D ... points) {
            this.points = points;
            Point2D a = points[0];
            Point2D b = points[1];
            Point2D c = points[2];
            t1.setTransform(a.getX() - c.getX(), a.getY() - c.getY(), b.getX() - c.getX(), b.getY() - c.getY(), c.getX(), c.getY());
            try {
                t1.invert();
            } catch (NoninvertibleTransformException ex) {
                Logger.getLogger(View.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(-1);
            }
        }
        
        public void draw(Graphics2D g) {
            polygon.reset();
            for (Point2D p : points) {
                polygon.addPoint((int) p.getX(), (int) p.getY());
            }
            
            Shape originalClip = g.getClip();
            g.clip(polygon);

            Point2D a = points[0];
            Point2D b = points[1];
            Point2D c = points[2];
            t2.setTransform(a.getX() - c.getX(), a.getY() - c.getY(), b.getX() - c.getX(), b.getY() - c.getY(), c.getX(), c.getY());
            t2.concatenate(t1);
            g.drawImage(texture, t2, null);
            g.setClip(originalClip);

            if (wireframeVisible) {
                g.setColor(Color.WHITE);
                g.draw(polygon);
            }
        }
    }
    
    public View() {
        setPreferredSize(new Dimension(800, 600));
        
        addMouseMotionListener(new MouseHandler());
        
        // load texture
        try {
            //texture = ImageIO.read(getClass().getResourceAsStream("sea.png"));
            texture = ImageIO.read(getClass().getResourceAsStream("joker.jpg"));
        } catch (IOException ex) {
            Logger.getLogger(View.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        
        points = new Point2D.Double[17][13];
        startTime = new long[20][15];
        intensity = new double[20][15];
        intensity2 = new double[20][15];
        for (int y=0; y<points[0].length; y++) {
            for (int x=0; x<points.length; x++) {
                points[x][y] = new Point2D.Double(x * 20, y * 20);
                startTime[x][y] = (long) (Long.MAX_VALUE * Math.random());
                intensity[x][y] = 1 + 1 * Math.random();
                intensity2[x][y] = 0;
            }
        }
        
        // create faces
        for (int y=0; y<points[0].length - 1; y++) {
            for (int x=0; x<points.length - 1; x++) {
                Point2D pa1 = points[x][y];
                Point2D pa2 = points[x + 1][y];
                Point2D pa3 = points[x][y + 1];
                
                Point2D pb1 = points[x + 1][y];
                Point2D pb2 = points[x][y + 1];
                Point2D pb3 = points[x + 1][y + 1];
                
                faces.add(new Face(pa1, pa2, pa3));
                faces.add(new Face(pb1, pb2, pb3));
            }
        }
    }

    private class MainLoop implements Runnable {
        @Override
        public void run() {
            long frameRate = 1000 / 30;
            while (running) {
                long startTime = System.currentTimeMillis();
                update();
                repaint();
                while (System.currentTimeMillis() - startTime < frameRate) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                    }
                }                
            }
        }
    }
    
    public void start() {
        if (running) {
            return;
        }
        running = true;
        Thread thread = new Thread(new MainLoop());
        thread.start();
    }
    
    public void update() {
        mouseRectangle.setBounds(mouse.x - 40, mouse.y - 40, 80, 80);
        for (int y=0; y<points[0].length; y++) {
            for (int x=0; x<points.length; x++) {
                Point2D p = points[x][y];
                if (mouseRectangle.contains(p)) {
                    intensity2[x][y] += 5;
                    intensity2[x][y] = intensity2[x][y] > 15 ? 15 : intensity2[x][y];
                }
            }
        }
        
        for (int y=0; y<points[0].length; y++) {
            for (int x=0; x<points.length; x++) {
                Point2D p = points[x][y];
                double px = x * 40 + (intensity[x][y] + intensity2[x][y]) * Math.sin((startTime[x][y] + System.nanoTime()) * 0.00000000973);
                double py = y * 40 + (intensity[x][y] + intensity2[x][y]) * Math.cos((startTime[x][y] + System.nanoTime()) * 0.00000000791);
                intensity2[x][y] *= 0.98;
                p.setLocation(px, py);
            }
        }
        
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); 
        
        Graphics2D g2d = (Graphics2D) g;
        //g2d.drawString("time " + System.nanoTime(), 10, 10);
        
        g2d.translate(20, 20);
        
        //g2d.drawImage(texture, 0, 0, this);
        
        for (Face face : faces) {
            face.draw(g2d);
        }

        if (wireframeVisible) {
            for (int y=0; y<points[0].length; y++) {
                for (int x=0; x<points.length; x++) {
                    Point2D p = points[x][y];
                    g2d.setColor(Color.RED);
                    g2d.fillOval((int) (p.getX() - 3), (int) (p.getY() - 3), 6, 6);
                }
            }
            g2d.setColor(Color.GREEN);
            g2d.draw(mouseRectangle);
        }
        
    }
    
    private class MouseHandler extends MouseAdapter {
        
        @Override
        public void mouseMoved(MouseEvent me) {
            mouse.x = me.getX() - 20;
            mouse.y = me.getY() - 20;
        }
        
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                View view = new View();
                JFrame frame = new JFrame();
                frame.setTitle("Image Distortion Effect Test");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().add(view);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                view.requestFocus();
                view.start();
            }

        });
    }
    
}
