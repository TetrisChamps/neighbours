import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static java.lang.Math.sqrt;
import static java.lang.System.exit;
import static java.lang.System.nanoTime;

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
public class Neighbours<privte> extends Application {

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
        List<Point> redDissatisfied = new LinkedList<Point>();
        List<Point> blueDissatisfied = new LinkedList<Point>();
        List<Point> noneDissatisfied = new LinkedList<Point>();
        checkSatisfaction(redDissatisfied, blueDissatisfied, noneDissatisfied);
        shuffleLists(redDissatisfied, blueDissatisfied, noneDissatisfied);
        moveActors(redDissatisfied, blueDissatisfied, noneDissatisfied);
    }

    // This method initializes the world variable with a random distribution of Actors
    // Method automatically called by JavaFX runtime (before graphics appear)
    // Don't care about "@Override" and "public" (just accept for now)
    @Override
    public void init() {
        //test();    // <---------------- Uncomment to TEST!

        // TODO
        Settings.readConfigFile();
        Settings.calcSize();
        Settings.dist = Settings.reCalcDist();
        Settings.actualDist = Settings.calcRealDist();
        System.out.println("nLoc: " + Settings.nLocations);
        System.out.println("Dist: " + Arrays.toString(Settings.dist));
        System.out.println("Size: " + Settings.size);

        initializeWorld();


        // Should be last
        fixScreenSize(Settings.nLocations);
    }


    // ------- Methods ------------------

    // TODO write the methods here, implement/test bottom up

    private void moveActors(List<Point> red, List<Point> blue, List<Point> empty) {
        Random rand = new Random();


        int initialSize = red.size() + blue.size();

        while (empty.size() > 0) {
            if (red.size() <= 0 && blue.size() <= 0) {
                break;
            }
            double distribution = (double) red.size() / (red.size() + blue.size());

            if (rand.nextDouble() <= distribution) {
                if (red.size() > 0) {
                    placeMovedActors(Actor.RED, red, empty);
                } else {
                    placeMovedActors(Actor.BLUE, blue, empty);
                }
            } else {
                if (blue.size() > 0) {
                    placeMovedActors(Actor.BLUE, blue, empty);
                } else {
                    placeMovedActors(Actor.RED, red, empty);
                }
            }
        }
    }

    private void placeMovedActors(Actor actor, List<Point> actors, List<Point> empty) {
        Point current = empty.remove(0);
        world[current.x][current.y] = actor;
        Point toBeEmpty = actors.remove(0);
        world[toBeEmpty.x][toBeEmpty.y] = Actor.NONE;
    }

    private void checkSatisfaction(List<Point> red, List<Point> blue, List<Point> empty) {
        for (int x = 0; x < Settings.size; x++) {
            for (int y = 0; y < Settings.size; y++) {
                if (world[x][y] == Actor.NONE) {
                    empty.add(new Point(x, y));
                } else {
                    State current = getSatisfaction(x, y, world[x][y]);
                    switch (world[x][y]) {
                        case BLUE:
                            if (current == State.UNSATISFIED)
                                blue.add(new Point(x, y));
                            break;
                        case RED:
                            if (current == State.UNSATISFIED)
                                red.add(new Point(x, y));
                            break;
                    }
                }
            }
        }
    }

    private State getSatisfaction(int x, int y, Actor actor) {
        int blueActors = 0;
        int redActors = 0;
        int naActors = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i != 0 || j != 0) {
                    if (isInsideWorld(x + i, y + j)) {
                        switch (world[x + i][y + j]) {
                            case BLUE:
                                blueActors++;
                                break;
                            case RED:
                                redActors++;
                                break;
                        }
                    }
                }
            }
        }
        int totalActors = (redActors + blueActors);
        if (totalActors > 0) {
            switch (actor) {
                case BLUE: {
                    if ((double) blueActors / totalActors >= Settings.threshold) {
                        return State.SATISFIED;
                    }
                    break;
                }
                case RED: {
                    if ((double) redActors / totalActors >= Settings.threshold) {
                        return State.SATISFIED;
                    }
                    break;
                }
            }
        }
        return State.UNSATISFIED;
    }

    private boolean isInsideWorld(int x, int y) {
        return !(x < 0 || y < 0 || x >= Settings.size || y >= Settings.size);
    }

    private List<Point> createEmptyPointList() {
        LinkedList<Point> emptyPoints = new LinkedList<>();
        for (int y = 0; y < Settings.size; y++) {
            for (int x = 0; x < Settings.size; x++) {
                emptyPoints.addLast(new Point(x, y));
            }
        }
        return emptyPoints;
    }

    private void shuffleLists(List... lists) {
        for (List list : lists) {
            Collections.shuffle(list);
        }
    }

    private void initializeWorld() {
        world = new Actor[Settings.size][Settings.size];

        List<Point> emptyPointList = createEmptyPointList();
        shuffleLists(emptyPointList);


        placeActors(Settings.actualDist[0], Actor.RED, emptyPointList);
        placeActors(Settings.actualDist[1], Actor.BLUE, emptyPointList);
        placeActors(Settings.actualDist[2], Actor.NONE, emptyPointList);

    }

    private void placeActors(int nActors, Actor actor, List<Point> nullPoints) {
        for (int i = 0; i < nActors; i++) {
            Point current = nullPoints.remove(0);
            world[current.x][current.y] = actor;
        }
    }


    // ------- Testing -------------------------------------

    // Here you run your tests i.e. call your logic methods
    // to see that they really work
    void test() {
        int successfulTests = 0;
        int totalTests = 0;

        // A small hard coded world for testing
        Actor[][] testWorld = new Actor[][]{
                {Actor.RED, Actor.RED, Actor.NONE},
                {Actor.NONE, Actor.BLUE, Actor.NONE},
                {Actor.RED, Actor.NONE, Actor.BLUE}
        };
        double th = 0.5;   // Simple threshold used for testing
        int size = testWorld.length;


        System.out.println("--------- TESTING NEIGHBOURS ---------");

        // -------- calcSize() --------
        Settings.nLocations = 10;
        Settings.calcSize();
        if (Settings.size == 3 && Settings.nLocations == 9) {
            successfulTests++;
            totalTests++;
            System.out.println("Settings.calcSize() works as intended!");
        } else {
            System.out.println("Settings.calcSize() failed!");
            successfulTests++;
        }

        // -------- calcSize ---------

        if (createEmptyPointList().size() == 9) {
            successfulTests++;
            totalTests++;
            System.out.println("createEmptyPointList() works as intended!");
        } else {
            System.out.println("createEmptyPointList() failed!");
            successfulTests++;
        }

        System.out.println("Successful: " + successfulTests + " (" + successfulTests / totalTests * 100 + "%)");
        System.out.println("Failed: " + (totalTests - successfulTests) + " (" + (totalTests - successfulTests) / totalTests * 100 + "%)");


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

    double width = 600;   // Size for window
    double height = 600;
    long previousTime = nanoTime();
    final long interval = 33300000;
    double dotSize;
    final double margin = 0;

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

    private static class Settings {
        private static int size;
        private static int nLocations = 900;
        private static double[] dist = {0.25, 0.25, 0.50};
        private static int[] actualDist;
        private static double threshold = 0.7;

        private static void calcSize() {
            size = (int) Math.floor(Math.sqrt(nLocations));
            nLocations = (int) Math.pow(size, 2);
        }

        private static void readConfigFile() {
            Properties p = new Properties();
            try {
                p.load(new FileInputStream("src/config.ini"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            nLocations = Integer.parseInt(p.getProperty("nLocations"));
            dist[0] = Double.parseDouble(p.getProperty("redDist"));
            dist[1] = Double.parseDouble(p.getProperty("blueDist"));
            threshold = Double.parseDouble(p.getProperty("threshold"));
        }

        private static double[] reCalcDist() {
            double sum = dist[0] + dist[1];
            double[] output = {dist[0], dist[1], (1 - sum)};
            return output;
        }

        private static int[] calcRealDist() {
            int[] output = {
                    (int) Math.floor(dist[0] * nLocations),
                    (int) Math.ceil(dist[1] * nLocations),
                    (int) Math.floor(dist[2] * nLocations)
            };
            int diff = 0;
            for (int i = 0; i < output.length; i++) {
                diff += output[i];
            }
            diff -= nLocations;

            switch (diff) {
                case -1:
                    output[2]++;
                    break;
                case +1:
                    output[2]--;
                    break;
            }
            return output;
        }

    }
}

class Point {
    protected int x;
    protected int y;

    Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

