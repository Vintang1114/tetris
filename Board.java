import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Board extends JPanel {
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final int BLOCK_SIZE = 20;
    private static final int SIDE_PANEL_WIDTH = 100;
    private static final int NUM_NEXT_PIECES = 3;
    private static final int PREVIEW_BLOCK_SIZE = 15;
    private static final int PREVIEW_SPACING = 60;
    private static final int NORMAL_FALL_SPEED = 400;
    private static final int SOFT_DROP_SPEED = 50;
    private static final int LOCK_DELAY = 1000; // 2.5 seconds in milliseconds
    private static final int LINES_PER_LEVEL = 10;

    private Timer timer;
    private boolean isFallingFinished = false;
    private boolean isStarted = false;
    private boolean isPaused = false;
    private boolean isGameOver = false;
    private int numLinesRemoved = 0;
    private int currentX = 0;
    private int currentY = 0;
    private Shape currentPiece;
    private Shape holdPiece;
    private boolean canHold = true;
    private Shape[] nextPieces;
    private int[][] board;
    private long lastMoveDownTime;
    private boolean isAtBottom = false;
    private int score = 0;
    private int level = 1;
    private int linesCleared = 0;

    public Board() {
        setFocusable(true);
        setPreferredSize(new Dimension(BOARD_WIDTH * BLOCK_SIZE + SIDE_PANEL_WIDTH * 2, BOARD_HEIGHT * BLOCK_SIZE));
        currentPiece = new Shape();
        timer = new Timer(NORMAL_FALL_SPEED, new GameCycle());
        board = new int[BOARD_WIDTH][BOARD_HEIGHT];
        nextPieces = new Shape[NUM_NEXT_PIECES];
        addKeyListener(new TAdapter());
        initBoard();
        initNextPieces();
    }

    private void initBoard() {
        for (int i = 0; i < BOARD_WIDTH; i++) {
            for (int j = 0; j < BOARD_HEIGHT; j++) {
                board[i][j] = 0;
            }
        }
    }

    private void initNextPieces() {
        for (int i = 0; i < NUM_NEXT_PIECES; i++) {
            nextPieces[i] = new Shape();
            nextPieces[i].setRandomShape();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }

    private void doDrawing(Graphics g) {
        Dimension size = getSize();
        int boardTop = (int) size.getHeight() - BOARD_HEIGHT * BLOCK_SIZE;

        // Draw side panels background
        g.setColor(new Color(40, 40, 40));
        g.fillRect(0, 0, SIDE_PANEL_WIDTH, size.height); // Left panel
        g.fillRect(BOARD_WIDTH * BLOCK_SIZE + SIDE_PANEL_WIDTH, 0, SIDE_PANEL_WIDTH, size.height); // Right panel

        // Draw hold piece
        g.setColor(Color.WHITE);
        g.drawString("HOLD", 20, 30);
        if (holdPiece != null) {
            drawPreviewPiece(g, holdPiece, 25, 50);
        }

        // Draw next pieces
        g.drawString("NEXT", BOARD_WIDTH * BLOCK_SIZE + SIDE_PANEL_WIDTH + 20, 30);
        for (int i = 0; i < NUM_NEXT_PIECES; i++) {
            drawPreviewPiece(g, nextPieces[i], 
                BOARD_WIDTH * BLOCK_SIZE + SIDE_PANEL_WIDTH + 25, 
                50 + i * PREVIEW_SPACING);
        }

        // Translate graphics for main board
        g.translate(SIDE_PANEL_WIDTH, 0);

        // Draw board background
        g.setColor(new Color(20, 20, 20));
        g.fillRect(0, 0, BOARD_WIDTH * BLOCK_SIZE, size.height);

        // Draw grid
        g.setColor(new Color(50, 50, 50));
        // Vertical lines
        for (int i = 0; i <= BOARD_WIDTH; i++) {
            g.drawLine(i * BLOCK_SIZE, 0, i * BLOCK_SIZE, size.height);
        }
        // Horizontal lines
        for (int i = 0; i <= BOARD_HEIGHT; i++) {
            g.drawLine(0, boardTop + i * BLOCK_SIZE, 
                      BOARD_WIDTH * BLOCK_SIZE, boardTop + i * BLOCK_SIZE);
        }

        // Draw ghost piece
        if (currentPiece.getShape() != Shape.Tetrominoes.NoShape) {
            int ghostY = findGhostPieceY();
            for (int i = 0; i < 4; i++) {
                int x = currentX + currentPiece.x(i);
                int y = ghostY - currentPiece.y(i);
                drawGhostSquare(g, x * BLOCK_SIZE,
                        boardTop + (BOARD_HEIGHT - y - 1) * BLOCK_SIZE,
                        currentPiece.getShape());
            }
        }

        // Draw current piece
        if (currentPiece.getShape() != Shape.Tetrominoes.NoShape) {
            for (int i = 0; i < 4; i++) {
                int x = currentX + currentPiece.x(i);
                int y = currentY - currentPiece.y(i);
                drawSquare(g, x * BLOCK_SIZE,
                        boardTop + (BOARD_HEIGHT - y - 1) * BLOCK_SIZE,
                        currentPiece.getShape());
            }
        }

        // Draw board
        for (int i = 0; i < BOARD_WIDTH; i++) {
            for (int j = 0; j < BOARD_HEIGHT; j++) {
                Shape.Tetrominoes shape = Shape.Tetrominoes.values()[board[i][j]];
                if (shape != Shape.Tetrominoes.NoShape) {
                    drawSquare(g, i * BLOCK_SIZE,
                            boardTop + (BOARD_HEIGHT - j - 1) * BLOCK_SIZE, shape);
                }
            }
        }
        
        // Reset translation
        g.translate(-SIDE_PANEL_WIDTH, 0);

        // Draw game over screen
        if (isGameOver) {
            drawOverlay(g, "Game Over!", "Score: " + score, "Press R to Play Again");
        } else if (isPaused) {
            drawOverlay(g, "Paused", "Score: " + score, "Press ESC to Resume");
        }

        // Draw score, level, and lines
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        int infoX = BOARD_WIDTH * BLOCK_SIZE + 20;
        int infoY = 20;
        
        g.drawString("Score: " + score, infoX, infoY);
        g.drawString("Level: " + level, infoX, infoY + 20);
        g.drawString("Lines: " + linesCleared, infoX, infoY + 40);
    }

    private void drawOverlay(Graphics g, String mainMsg, String scoreMsg, String actionMsg) {
        Dimension size = getSize();
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, size.width, size.height);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        
        FontMetrics fm = g.getFontMetrics();
        int msgWidth = fm.stringWidth(mainMsg);
        int scoreWidth = fm.stringWidth(scoreMsg);
        int actionWidth = fm.stringWidth(actionMsg);
        
        g.drawString(mainMsg, (size.width - msgWidth) / 2, size.height / 2 - 30);
        g.drawString(scoreMsg, (size.width - scoreWidth) / 2, size.height / 2);
        g.drawString(actionMsg, (size.width - actionWidth) / 2, size.height / 2 + 30);
    }

    private void drawSquare(Graphics g, int x, int y, Shape.Tetrominoes shape) {
        Color[] colors = {
            new Color(0, 0, 0), new Color(204, 102, 102),
            new Color(102, 204, 102), new Color(102, 102, 204),
            new Color(204, 204, 102), new Color(204, 102, 204),
            new Color(102, 204, 204), new Color(218, 170, 0)
        };

        Color color = colors[shape.ordinal()];

        g.setColor(color);
        g.fillRect(x + 1, y + 1, BLOCK_SIZE - 2, BLOCK_SIZE - 2);

        g.setColor(color.brighter());
        g.drawLine(x, y + BLOCK_SIZE - 1, x, y);
        g.drawLine(x, y, x + BLOCK_SIZE - 1, y);

        g.setColor(color.darker());
        g.drawLine(x + 1, y + BLOCK_SIZE - 1,
                x + BLOCK_SIZE - 1, y + BLOCK_SIZE - 1);
        g.drawLine(x + BLOCK_SIZE - 1, y + BLOCK_SIZE - 1,
                x + BLOCK_SIZE - 1, y + 1);
    }

    private void drawGhostSquare(Graphics g, int x, int y, Shape.Tetrominoes shape) {
        Color[] colors = {
            new Color(0, 0, 0), new Color(204, 102, 102),
            new Color(102, 204, 102), new Color(102, 102, 204),
            new Color(204, 204, 102), new Color(204, 102, 204),
            new Color(102, 204, 204), new Color(218, 170, 0)
        };

        Color color = colors[shape.ordinal()];
        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50); // Make it translucent

        g.setColor(color);
        g.fillRect(x + 1, y + 1, BLOCK_SIZE - 2, BLOCK_SIZE - 2);
    }

    private void drawPiece(Graphics g, Shape piece, int x, int y) {
        for (int i = 0; i < 4; i++) {
            int pieceX = x + piece.x(i) * (BLOCK_SIZE / 2);
            int pieceY = y + piece.y(i) * (BLOCK_SIZE / 2);
            drawSquare(g, pieceX, pieceY, piece.getShape());
        }
    }

    private void drawPreviewPiece(Graphics g, Shape piece, int x, int y) {
        // Calculate the bounds of the piece
        int minX = 0, minY = 0, maxX = 0, maxY = 0;
        for (int i = 0; i < 4; i++) {
            minX = Math.min(minX, piece.x(i));
            minY = Math.min(minY, piece.y(i));
            maxX = Math.max(maxX, piece.x(i));
            maxY = Math.max(maxY, piece.y(i));
        }
        
        // Center the piece in the preview area
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int centerX = x + (4 - width) * PREVIEW_BLOCK_SIZE / 2;
        int centerY = y + (2 - height) * PREVIEW_BLOCK_SIZE / 2;
        
        for (int i = 0; i < 4; i++) {
            int pieceX = centerX + (piece.x(i) - minX) * PREVIEW_BLOCK_SIZE;
            int pieceY = centerY + (piece.y(i) - minY) * PREVIEW_BLOCK_SIZE;
            drawPreviewSquare(g, pieceX, pieceY, piece.getShape());
        }
    }

    private void drawPreviewSquare(Graphics g, int x, int y, Shape.Tetrominoes shape) {
        Color[] colors = {
            new Color(0, 0, 0), new Color(204, 102, 102),
            new Color(102, 204, 102), new Color(102, 102, 204),
            new Color(204, 204, 102), new Color(204, 102, 204),
            new Color(102, 204, 204), new Color(218, 170, 0)
        };

        Color color = colors[shape.ordinal()];

        g.setColor(color);
        g.fillRect(x + 1, y + 1, PREVIEW_BLOCK_SIZE - 2, PREVIEW_BLOCK_SIZE - 2);

        g.setColor(color.brighter());
        g.drawLine(x, y + PREVIEW_BLOCK_SIZE - 1, x, y);
        g.drawLine(x, y, x + PREVIEW_BLOCK_SIZE - 1, y);

        g.setColor(color.darker());
        g.drawLine(x + 1, y + PREVIEW_BLOCK_SIZE - 1,
                x + PREVIEW_BLOCK_SIZE - 1, y + PREVIEW_BLOCK_SIZE - 1);
        g.drawLine(x + PREVIEW_BLOCK_SIZE - 1, y + PREVIEW_BLOCK_SIZE - 1,
                x + PREVIEW_BLOCK_SIZE - 1, y + 1);
    }

    private int findGhostPieceY() {
        int ghostY = currentY;
        while (ghostY > 0) {
            if (!canMoveTo(currentPiece, currentX, ghostY - 1)) {
                break;
            }
            ghostY--;
        }
        return ghostY;
    }

    private boolean canMoveTo(Shape piece, int newX, int newY) {
        for (int i = 0; i < 4; i++) {
            int x = newX + piece.x(i);
            int y = newY - piece.y(i);

            if (x < 0 || x >= BOARD_WIDTH || y < 0 || y >= BOARD_HEIGHT) {
                return false;
            }

            if (board[x][y] != 0) {
                return false;
            }
        }
        return true;
    }

    private class GameCycle implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            doGameCycle();
        }
    }

    private void doGameCycle() {
        if (isFallingFinished) {
            isFallingFinished = false;
            newPiece();
        } else {
            if (isAtBottom) {
                // If piece has been at bottom for more than LOCK_DELAY
                if (System.currentTimeMillis() - lastMoveDownTime > LOCK_DELAY) {
                    pieceDropped();
                }
            } else {
                oneLineDown();
            }
        }
    }

    private void newPiece() {
        currentPiece = nextPieces[0];
        // Shift next pieces
        for (int i = 0; i < NUM_NEXT_PIECES - 1; i++) {
            nextPieces[i] = nextPieces[i + 1];
        }
        nextPieces[NUM_NEXT_PIECES - 1] = new Shape();
        nextPieces[NUM_NEXT_PIECES - 1].setRandomShape();
        
        currentX = BOARD_WIDTH / 2;
        currentY = BOARD_HEIGHT - 1 + currentPiece.minY();

        if (!tryMove(currentPiece, currentX, currentY - 1)) {
            currentPiece.setShape(Shape.Tetrominoes.NoShape);
            gameOver();
        }
    }

    private void oneLineDown() {
        if (!tryMove(currentPiece, currentX, currentY - 1)) {
            if (!isAtBottom) {
                isAtBottom = true;
                lastMoveDownTime = System.currentTimeMillis();
            }
        }
    }

    private void pieceDropped() {
        for (int i = 0; i < 4; i++) {
            int x = currentX + currentPiece.x(i);
            int y = currentY - currentPiece.y(i);
            board[x][y] = currentPiece.getShape().ordinal();
        }

        removeFullLines();
        canHold = true;
        isAtBottom = false;
        newPiece();
    }

    private void removeFullLines() {
        int numFullLines = 0;

        for (int i = BOARD_HEIGHT - 1; i >= 0; i--) {
            boolean lineIsFull = true;

            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (board[j][i] == Shape.Tetrominoes.NoShape.ordinal()) {
                    lineIsFull = false;
                    break;
                }
            }

            if (lineIsFull) {
                numFullLines++;
                // Move down all lines above this one
                for (int k = i; k < BOARD_HEIGHT - 1; k++) {
                    for (int j = 0; j < BOARD_WIDTH; j++) {
                        board[j][k] = board[j][k + 1];
                    }
                }
                // Clear the top line
                for (int j = 0; j < BOARD_WIDTH; j++) {
                    board[j][BOARD_HEIGHT - 1] = Shape.Tetrominoes.NoShape.ordinal();
                }
                
                // Move the checking position back one line since we moved everything down
                i++;
            }
        }

        if (numFullLines > 0) {
            updateScore(numFullLines);
            repaint();
        }
    }

    private void updateScore(int numLines) {
        if (numLines == 0) return;
        
        // Score calculation based on number of lines cleared
        int points = switch(numLines) {
            case 1 -> 100 * level;   // Single
            case 2 -> 300 * level;   // Double
            case 3 -> 500 * level;   // Triple
            case 4 -> 800 * level;   // Tetris
            default -> 0;
        };
        
        score += points;
        linesCleared += numLines;
        
        // Level up every LINES_PER_LEVEL lines
        level = (linesCleared / LINES_PER_LEVEL) + 1;
        
        // Update fall speed based on level
        timer.setDelay(Math.max(NORMAL_FALL_SPEED - ((level - 1) * 50), 50));
    }

    private boolean tryMove(Shape newPiece, int newX, int newY) {
        if (!canMoveTo(newPiece, newX, newY)) {
            return false;
        }

        currentPiece = newPiece;
        currentX = newX;
        currentY = newY;

        // Reset lock delay if piece is moved horizontally or rotated while at bottom
        if (isAtBottom && !canMoveTo(currentPiece, currentX, currentY - 1)) {
            lastMoveDownTime = System.currentTimeMillis();
        }

        repaint();
        return true;
    }

    private void dropDown() {
        int newY = currentY;
        while (newY > 0) {
            if (!tryMove(currentPiece, currentX, newY - 1)) {
                break;
            }
            newY--;
        }
        isAtBottom = true;
        lastMoveDownTime = System.currentTimeMillis();
        pieceDropped();
    }

    private class TAdapter extends KeyAdapter {
        private boolean isDownPressed = false;

        @Override
        public void keyPressed(KeyEvent e) {
            if (isGameOver) {
                if (e.getKeyCode() == KeyEvent.VK_R) {
                    restart();
                }
                return;
            }

            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                pause();
                return;
            }

            if (!isStarted || currentPiece.getShape() == Shape.Tetrominoes.NoShape) {
                return;
            }

            int keycode = e.getKeyCode();

            switch (keycode) {
                case KeyEvent.VK_LEFT -> tryMove(currentPiece, currentX - 1, currentY);
                case KeyEvent.VK_RIGHT -> tryMove(currentPiece, currentX + 1, currentY);
                case KeyEvent.VK_DOWN -> {
                    if (!isDownPressed) {
                        isDownPressed = true;
                        timer.setDelay(SOFT_DROP_SPEED);
                    }
                    oneLineDown();
                }
                case KeyEvent.VK_UP -> tryMove(currentPiece.rotateLeft(), currentX, currentY);
                case KeyEvent.VK_SPACE -> dropDown();
                case KeyEvent.VK_D -> oneLineDown();
                case KeyEvent.VK_C -> holdCurrentPiece();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                isDownPressed = false;
                timer.setDelay(NORMAL_FALL_SPEED);
            }
        }
    }

    private void holdCurrentPiece() {
        if (!canHold) return;
        
        Shape tempPiece = new Shape();
        tempPiece.setShape(currentPiece.getShape());
        
        if (holdPiece == null) {
            holdPiece = tempPiece;
            newPiece();
        } else {
            Shape temp = holdPiece;
            holdPiece = tempPiece;
            currentPiece = temp;
            currentX = BOARD_WIDTH / 2;
            currentY = BOARD_HEIGHT - 1 + currentPiece.minY();
        }
        
        canHold = false;
        repaint();
    }

    private void pause() {
        isPaused = !isPaused;
        if (isPaused) {
            timer.stop();
        } else {
            timer.start();
        }
        repaint();
    }

    public void start() {
        if (isPaused) {
            return;
        }

        isStarted = true;
        isFallingFinished = false;
        numLinesRemoved = 0;
        initBoard();
        newPiece();
        timer.setDelay(NORMAL_FALL_SPEED);
        timer.start();
    }

    private void gameOver() {
        isGameOver = true;
        timer.stop();
        repaint();
    }

    public void restart() {
        isGameOver = false;
        isStarted = true;
        isFallingFinished = false;
        numLinesRemoved = 0;
        score = 0;
        level = 1;
        linesCleared = 0;
        initBoard();
        initNextPieces();
        holdPiece = null;
        canHold = true;
        newPiece();
        timer.start();
        repaint();
    }
}
