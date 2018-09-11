import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

import static java.lang.Math.round;
import static java.lang.Math.sqrt;
import static java.lang.System.*;

/*
 *  Program to simulate segregation.
 *  See : http://nifty.stanford.edu/2014/mccown-schelling-model-segregation/
 *
 * NOTE:
 * - JavaFX first calls method init() and then method start() far below.
 * - To test uncomment call to test() first in init() method!
 *
 */
// Extends Application because of JavaFX (just accept for now)
public class Neighbours extends Application {

    class Point {
        public final int x;
        public final int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Enumeration type for the Actors
    enum Actor {
        BLUE, RED, NONE   // NONE used for empty locations
    }

    // Enumeration type for the state of an Actor
    enum State {
        UNSATISFIED,
        SATISFIED,
        NA     // Not applicable (NA), used for NONEs
    }

    // Below is the *only* accepted instance variable (i.e. variables outside any method)
    // This variable may *only* be used in methods init() and updateWorld()
    Actor[][] world;              // The world is a square matrix of Actors

    // This is the method called by the timer to update the world
    // (i.e move unsatisfied) approx each 1/60 sec.
    void updateWorld() {
        // % of surrounding neighbours that are like me
        final double threshold = 0.7;
        // TODO

        // List of all empty points in world
        LinkedList<Point> empty = new LinkedList<>();
        // List of all unsatisfied red points in world
        LinkedList<Point> red = new LinkedList<>();
        // List of all unsatisfied blue points in world
        LinkedList<Point> blue = new LinkedList<>();

        findEmptyAndDissatisfied(world, empty, red, blue, threshold);

        shuffleLists(empty, red, blue);

        moveDissatisfiedPoints(world, empty, red, blue);
    }

    // This method initializes the world variable with a random distribution of Actors
    // Method automatically called by JavaFX runtime (before graphics appear)
    // Don't care about "@Override" and "public" (just accept for now)
    @Override
    public void init() {
        //test();    // <---------------- Uncomment to TEST!

        // %-distribution of RED, BLUE and NONE
        double[] dist = {0.50, 0.45, 0.00};
        // Number of locations (places) in world (square)
        int size = 300;
        int nLocations = size * size;

        world = new Actor[size][size];

        // TODO
        generateWorld(world, dist[0], dist[1]);

        // Should be last
        fixScreenSize(nLocations);
    }

    void generateWorld(Actor[][] world, double redPercentage, double bluePercentage) {
        Random rand = new Random();

        for (int x = 0; x < world.length; ++x) {
            for (int y = 0; y < world.length; ++y) {
                double randomValue = rand.nextDouble();
                if (randomValue < redPercentage) {
                    world[x][y] = Actor.RED;
                } else if (randomValue < redPercentage + bluePercentage) {
                    world[x][y] = Actor.BLUE;
                } else {
                    world[x][y] = Actor.NONE;
                }
            }
        }
    }

    double neighbourDensity(Actor[][] world, int x, int y) {
        int totalNeighbourCount = 0;
        int sameNeighbourCount = 0;
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                if (i == 0 && j == 0) {
                    continue;
                }
                if (x < 0 || y < 0 || x >= world.length || y >= world.length) {
                    continue;
                }
                if (x + i < 0 || y + j < 0 || x + i >= world.length || y + j >= world.length) {
                    continue;
                }
                if (world[x][y] == world[x + i][y + j]) {
                    sameNeighbourCount++;
                }
                if (world[x + i][y + j] != Actor.NONE) {
                    totalNeighbourCount++;
                }
            }
        }
        if (totalNeighbourCount == 0) {
            return 0.0;
        }
        return (double) sameNeighbourCount / totalNeighbourCount;
    }


    // ------- Methods ------------------

    // TODO write the methods here, implement/test bottom up

    void moveDissatisfiedPoints(Actor[][] world, LinkedList<Point> empty, LinkedList<Point> red, LinkedList<Point> blue) {
        Random rand = new Random();

        while (empty.size() > 0) {
            Point p = empty.removeLast();
            int choice = rand.nextInt(2);
            if (choice == 0) {
                if (red.size() > 0) {
                    movePoint(world, red, p, Actor.RED);
                } else if (blue.size() > 0) {
                    movePoint(world, blue, p, Actor.BLUE);
                } else {
                    break;
                }
            } else if (choice == 1) {
                if (blue.size() > 0) {
                    movePoint(world, blue, p, Actor.BLUE);
                } else if (red.size() > 0) {
                    movePoint(world, red, p, Actor.RED);
                } else {
                    break;
                }
            }
        }
    }

    void findEmptyAndDissatisfied(Actor[][] world, LinkedList<Point> empty, LinkedList<Point> red, LinkedList<Point> blue, double threshold) {
        for (int x = 0; x < world.length; ++x) {
            for (int y = 0; y < world.length; ++y) {
                if (world[x][y] == Actor.NONE) {
                    empty.addLast(new Point(x, y));
                } else if (neighbourDensity(world, x, y) < threshold) {
                    switch (world[x][y]) {
                        case RED:
                            red.addLast(new Point(x, y));
                            break;
                        case BLUE:
                            blue.addLast(new Point(x, y));
                            break;
                    }
                }
            }
        }
    }

    void shuffleLists(LinkedList<Point> empty, LinkedList<Point> red, LinkedList<Point> blue) {
        Collections.shuffle(empty);
        Collections.shuffle(red);
        Collections.shuffle(blue);
    }

    void movePoint(Actor[][] world, LinkedList<Point> points, Point movingTo, Actor moverType) {
        Point movingFrom = points.removeLast();
        world[movingTo.x][movingTo.y] = moverType;
        world[movingFrom.x][movingFrom.y] = Actor.NONE;
    }

    // ------- Testing -------------------------------------

    // Here you run your tests i.e. call your logic methods
    // to see that they really work
    void test() {
        // A small hard coded world for testing
        Actor[][] testWorld = new Actor[][]{
                {Actor.RED, Actor.RED, Actor.NONE},
                {Actor.NONE, Actor.BLUE, Actor.NONE},
                {Actor.RED, Actor.NONE, Actor.BLUE}
        };
        double th = 0.5;   // Simple threshold used for testing
        int size = testWorld.length;

        // TODO test methods

        exit(0);
    }

    // Helper method for testing (NOTE: reference equality)
    <T> int count(T[] arr, T toFind) {
        int count = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == toFind) {
                count++;
            }
        }
        return count;
    }

    // *****   NOTHING to do below this row, it's JavaFX stuff  ******

    double width = 400;   // Size for window
    double height = 400;
    long previousTime = nanoTime();
    final long interval = 450000000;
    double dotSize;
    final double margin = 50;

    void fixScreenSize(int nLocations) {
        // Adjust screen window depending on nLocations
        dotSize = (width - 2 * margin) / sqrt(nLocations);
        if (dotSize < 1) {
            dotSize = 2;
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Build a scene graph
        Group root = new Group();
        Canvas canvas = new Canvas(width, height);
        root.getChildren().addAll(canvas);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Create a timer
        AnimationTimer timer = new AnimationTimer() {
            // This method called by FX, parameter is the current time
            public void handle(long currentNanoTime) {
                long elapsedNanos = currentNanoTime - previousTime;
                if (elapsedNanos > interval) {
                    updateWorld();
                    renderWorld(gc, world);
                    previousTime = currentNanoTime;
                }
            }
        };

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Simulation");
        primaryStage.show();

        timer.start();  // Start simulation
    }


    // Render the state of the world to the screen
    public void renderWorld(GraphicsContext g, Actor[][] world) {
        g.clearRect(0, 0, width, height);
        int size = world.length;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                double x = dotSize * col + margin;
                double y = dotSize * row + margin;

                if (world[row][col] == Actor.RED) {
                    g.setFill(Color.RED);
                } else if (world[row][col] == Actor.BLUE) {
                    g.setFill(Color.BLUE);
                } else {
                    g.setFill(Color.WHITE);
                }
                g.fillOval(x, y, dotSize, dotSize);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
