import java.util.Random;

public class Shape {
    protected enum Tetrominoes {
        NoShape, ZShape, SShape, LineShape, TShape, SquareShape, LShape, MirroredLShape
    }
    
    private Tetrominoes pieceShape;
    private int[][] coords;
    private static final int[][][] coordsTable = new int[][][]{
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}},  // NoShape
            {{0, -1}, {0, 0}, {-1, 0}, {-1, 1}},  // ZShape
            {{0, -1}, {0, 0}, {1, 0}, {1, 1}},   // SShape
            {{0, -1}, {0, 0}, {0, 1}, {0, 2}},   // LineShape (I)
            {{-1, 0}, {0, 0}, {1, 0}, {0, 1}},   // TShape
            {{0, 0}, {1, 0}, {0, 1}, {1, 1}},    // SquareShape
            {{-1, -1}, {0, -1}, {0, 0}, {0, 1}}, // LShape
            {{1, -1}, {0, -1}, {0, 0}, {0, 1}}   // MirroredLShape
    };
    
    public Shape() {
        coords = new int[4][2];
        setShape(Tetrominoes.NoShape);
    }
    
    public void setShape(Tetrominoes shape) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 2; j++) {
                coords[i][j] = coordsTable[shape.ordinal()][i][j];
            }
        }
        pieceShape = shape;
    }
    
    public Tetrominoes getShape() {
        return pieceShape;
    }
    
    public void setRandomShape() {
        Random r = new Random();
        int x = r.nextInt(140); // Increased range for weighted probabilities
        
        // Weighted distribution:
        // LineShape (I): 10% chance (x < 14)
        // Other pieces: 15% chance each (x ranges of 21)
        
        Tetrominoes shape;
        if (x < 14) {
            shape = Tetrominoes.LineShape;      // 10% chance
        } else if (x < 35) {
            shape = Tetrominoes.ZShape;         // 15% chance
        } else if (x < 56) {
            shape = Tetrominoes.SShape;         // 15% chance
        } else if (x < 77) {
            shape = Tetrominoes.TShape;         // 15% chance
        } else if (x < 98) {
            shape = Tetrominoes.SquareShape;    // 15% chance
        } else if (x < 119) {
            shape = Tetrominoes.LShape;         // 15% chance
        } else {
            shape = Tetrominoes.MirroredLShape; // 15% chance
        }
        
        setShape(shape);
    }

    private void setX(int index, int x) {
        coords[index][0] = x;
    }

    private void setY(int index, int y) {
        coords[index][1] = y;
    }

    public int x(int index) {
        return coords[index][0];
    }

    public int y(int index) {
        return coords[index][1];
    }

    public int minX() {
        int m = coords[0][0];
        for (int i = 0; i < 4; i++) {
            m = Math.min(m, coords[i][0]);
        }
        return m;
    }

    public int minY() {
        int m = coords[0][1];
        for (int i = 0; i < 4; i++) {
            m = Math.min(m, coords[i][1]);
        }
        return m;
    }

    public Shape rotateLeft() {
        if (pieceShape == Tetrominoes.SquareShape)
            return this;

        Shape result = new Shape();
        result.pieceShape = pieceShape;

        for (int i = 0; i < 4; i++) {
            result.setX(i, y(i));
            result.setY(i, -x(i));
        }
        return result;
    }

    public Shape rotateRight() {
        if (pieceShape == Tetrominoes.SquareShape)
            return this;

        Shape result = new Shape();
        result.pieceShape = pieceShape;

        for (int i = 0; i < 4; i++) {
            result.setX(i, -y(i));
            result.setY(i, x(i));
        }
        return result;
    }
}
