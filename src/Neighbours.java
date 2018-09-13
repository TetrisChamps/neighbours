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
        final int x;
        final int y;

        Point(int x, int y) {
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
    private Actor[][] world;              // The world is a square matrix of Actors

    // This is the method called by the timer to update the world
    // (i.e move unsatisfied) approx each 1/60 sec.
    void updateWorld() {
        // % of surrounding neighbours that are like me
        final double threshold = 0.7;
        // TODO

        // List of all empty points in world
        LinkedList<Point> empty = getEmptyActors(world);
        // List of all unsatisfied red points in world
        LinkedList<Point> red = getDissatisfiedRedActors(world, threshold);
        // List of all unsatisfied blue points in world
        LinkedList<Point> blue = getDissatisfiedBlueActors(world, threshold);

        Collections.shuffle(empty);
        Collections.shuffle(red);
        Collections.shuffle(blue);

        moveDissatisfiedPoints(world, empty, red, blue);
    }

    // This method initializes the world variable with a random distribution of Actors
    // Method automatically called by JavaFX runtime (before graphics appear)
    // Don't care about "@Override" and "public" (just accept for now)
    @Override
    public void init() {
        test();    // <---------------- Uncomment to TEST!

        // %-distribution of RED, BLUE and NONE
        double[] dist = {0.45, 0.45, 0.00};

        // Number of locations (places) in world (square)
        int nLocations = 500 * 500;

        // TODO
        world = generateWorld(nLocations, dist[0], dist[1]);

        // Should be last
        fixScreenSize(nLocations);
    }

    Actor[][] generateWorld(int nLocations, double redPercentage, double bluePercentage) {
        int size = (int) Math.floor(Math.sqrt(nLocations));
        Actor[][] world = new Actor[size][size];

        nLocations = size * size;

        int nReds = (int) Math.ceil(redPercentage * nLocations);
        int nBlues = (int) Math.floor(bluePercentage * nLocations);

        ArrayList<Actor> actors = new ArrayList<>(nLocations);

        for (int i = 0; i < nLocations; ++i) {
            if (i < nReds) {
                actors.add(Actor.RED);
            } else if (i < nReds + nBlues) {
                actors.add(Actor.BLUE);
            } else {
                actors.add(Actor.NONE);
            }
        }

        Collections.shuffle(actors);

        for (int x = 0; x < world.length; ++x) {
            for (int y = 0; y < world.length; ++y) {
                world[x][y] = actors.get(y * world.length + x);
            }
        }

        return world;
    }

    State getActorSatisfaction(Actor[][] world, int x, int y, double threshold) {
        if (world[x][y] == Actor.NONE) {
            return State.UNSATISFIED;
        }

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
            return State.UNSATISFIED;
        }
        double density = (double) sameNeighbourCount / totalNeighbourCount;
        return density >= threshold ? State.SATISFIED : State.UNSATISFIED;
    }


    // ------- Methods ------------------

    // TODO write the methods here, implement/test bottom up

    void moveDissatisfiedPoints(Actor[][] world, LinkedList<Point> emptyActors, LinkedList<Point> dissatisfiedRedActors, LinkedList<Point> dissatisfiedBlueActors) {
        Random rand = new Random();

        // Percentage of red dissatisfied actors in relation to the sum of red and blue
        // dissatisfied actors, to make sure the chance of an actor moving is directly
        // proportional to the amount of dissatisfied actors of that colour.
        double percentageOfRedActors = (double) dissatisfiedRedActors.size() / (dissatisfiedRedActors.size() + dissatisfiedBlueActors.size());

        while (emptyActors.size() > 0) {
            // Get the next empty location
            Point nextEmptyLocation = emptyActors.removeLast();
            double choice = rand.nextDouble();
            if (choice < percentageOfRedActors) {
                if (dissatisfiedRedActors.size() > 0) {
                    movePoint(world, dissatisfiedRedActors.removeLast(), nextEmptyLocation);
                } else if (dissatisfiedBlueActors.size() > 0) {
                    movePoint(world, dissatisfiedBlueActors.removeLast(), nextEmptyLocation);
                } else {
                    break;
                }
            } else {
                if (dissatisfiedBlueActors.size() > 0) {
                    movePoint(world, dissatisfiedBlueActors.removeLast(), nextEmptyLocation);
                } else if (dissatisfiedRedActors.size() > 0) {
                    movePoint(world, dissatisfiedRedActors.removeLast(), nextEmptyLocation);
                } else {
                    break;
                }
            }
        }
    }

    LinkedList<Point> getEmptyActors(Actor[][] world) {
        LinkedList<Point> emptyActors = new LinkedList<>();
        for (int x = 0; x < world.length; ++x) {
            for (int y = 0; y < world.length; ++y) {
                if (world[x][y] == Actor.NONE) {
                    emptyActors.push(new Point(x, y));
                }
            }
        }
        return emptyActors;
    }

    LinkedList<Point> getDissatisfiedRedActors(Actor[][] world, double threshold) {
        return getDissatisfiedActors(world, Actor.RED, threshold);
    }

    LinkedList<Point> getDissatisfiedBlueActors(Actor[][] world, double threshold) {
        return getDissatisfiedActors(world, Actor.BLUE, threshold);
    }

    LinkedList<Point> getDissatisfiedActors(Actor[][] world, Actor type, double threshold) {
        LinkedList<Point> dissatisfiedActors = new LinkedList<>();
        for (int x = 0; x < world.length; ++x) {
            for (int y = 0; y < world.length; ++y) {
                if (world[x][y] == type && getActorSatisfaction(world, x, y, threshold) == State.UNSATISFIED) {
                    dissatisfiedActors.push(new Point(x, y));
                }
            }
        }
        return dissatisfiedActors;
    }

    void movePoint(Actor[][] world, Point movingFrom, Point movingTo) {
        world[movingTo.x][movingTo.y] = world[movingFrom.x][movingFrom.y];
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
        double threshold = 0.5;   // Simple threshold used for testing
        int size = testWorld.length;

        // TODO test methods

        // Satisfaction of each actor
        System.out.println(getActorSatisfaction(testWorld, 0, 0, threshold) == State.SATISFIED);
        System.out.println(getActorSatisfaction(testWorld, 1, 0, threshold) == State.UNSATISFIED);
        System.out.println(getActorSatisfaction(testWorld, 2, 0, threshold) == State.UNSATISFIED);
        System.out.println(getActorSatisfaction(testWorld, 0, 1, threshold) == State.SATISFIED);
        System.out.println(getActorSatisfaction(testWorld, 1, 1, threshold) == State.UNSATISFIED);
        System.out.println(getActorSatisfaction(testWorld, 2, 1, threshold) == State.UNSATISFIED);
        System.out.println(getActorSatisfaction(testWorld, 0, 2, threshold) == State.UNSATISFIED);
        System.out.println(getActorSatisfaction(testWorld, 1, 2, threshold) == State.UNSATISFIED);
        System.out.println(getActorSatisfaction(testWorld, 2, 2, threshold) == State.SATISFIED);

        // Red dissatisfied actors
        LinkedList<Point> dissatisfiedRedActors = getDissatisfiedRedActors(testWorld, threshold);
        Point p = dissatisfiedRedActors.removeLast();
        System.out.println(p.x == 2 && p.y == 0);
        System.out.println(dissatisfiedRedActors.size() == 0);

        // Blue dissatisfied actors
        LinkedList<Point> dissatisfiedBlueActors = getDissatisfiedBlueActors(testWorld, threshold);
        p = dissatisfiedBlueActors.removeLast();
        System.out.println(p.x == 1 && p.y == 1);
        System.out.println(dissatisfiedBlueActors.size() == 0);

        // Empty actors
        LinkedList<Point> emptyActors = getEmptyActors(testWorld);
        p = emptyActors.removeLast();
        System.out.println(p.x == 0 && p.y == 2);
        System.out.println(emptyActors.size() == 3);

        p = emptyActors.removeLast();
        System.out.println(p.x == 1 && p.y == 0);
        System.out.println(emptyActors.size() == 2);

        p = emptyActors.removeLast();
        System.out.println(p.x == 1 && p.y == 2);
        System.out.println(emptyActors.size() == 1);

        p = emptyActors.removeLast();
        System.out.println(p.x == 2 && p.y == 1);
        System.out.println(emptyActors.size() == 0);

        // Moving a point
        movePoint(testWorld, new Point(0, 0), new Point(0, 2));
        // Old position should be NONE
        System.out.println(testWorld[0][0] == Actor.NONE);
        // New position should be what the old one was (RED)
        System.out.println(testWorld[0][2] == Actor.RED);

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

}
