import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TetrisGame extends JFrame {
    private Board board;
    private boolean isFullScreen = false;
    private Rectangle windowedBounds;
    private static final int BLOCK_SIZE = 20;
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final int DEFAULT_WIDTH = BOARD_WIDTH * BLOCK_SIZE + 300; // Extra space for controls
    private static final int DEFAULT_HEIGHT = BOARD_HEIGHT * BLOCK_SIZE + 40; // Extra padding
    
    public TetrisGame() {
        setTitle("Tetris");
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Create center panel to hold game components
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));
        centerPanel.setBackground(new Color(30, 30, 30));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create game panel to hold board and controls
        JPanel gamePanel = new JPanel();
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.X_AXIS));
        gamePanel.setBackground(new Color(30, 30, 30));
        
        // Add board
        board = new Board();
        gamePanel.add(board);
        
        // Add controls panel
        JPanel controlsPanel = createControlsPanel();
        gamePanel.add(Box.createHorizontalStrut(20)); // Add spacing between board and controls
        gamePanel.add(controlsPanel);
        
        // Add game panel to center panel
        centerPanel.add(Box.createHorizontalGlue());
        centerPanel.add(gamePanel);
        centerPanel.add(Box.createHorizontalGlue());
        
        // Add center panel to frame
        add(centerPanel);

        // Add F11 key listener for fullscreen toggle
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_F11) {
                toggleFullScreen();
                return true;
            }
            return false;
        });

        // Ensure board gets focus when window is activated
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                board.requestFocusInWindow();
            }
        });

        board.start();
        pack();
    }

    private JPanel createControlsPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(40, 40, 40));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setPreferredSize(new Dimension(150, DEFAULT_HEIGHT - 40));
        
        // Make panel unfocusable
        panel.setFocusable(false);

        String[] controls = {
            "<html><b>Controls:</b></html>",
            "←/→: Move",
            "↑: Rotate",
            "↓: Soft Drop",
            "Space: Hard Drop",
            "C: Hold Piece",
            "ESC: Pause",
            "R: Restart",
            "F11: Full Screen"
        };

        for (String control : controls) {
            JLabel label = new JLabel(control);
            label.setForeground(Color.WHITE);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            // Make labels unfocusable
            label.setFocusable(false);
            panel.add(label);
            panel.add(Box.createVerticalStrut(5));
        }

        return panel;
    }

    private void toggleFullScreen() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        
        if (!isFullScreen) {
            windowedBounds = getBounds();
            dispose();
            setUndecorated(true);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            gd.setFullScreenWindow(this);
        } else {
            gd.setFullScreenWindow(null);
            dispose();
            setUndecorated(false);
            setBounds(windowedBounds);
        }
        
        isFullScreen = !isFullScreen;
        setVisible(true);
        board.requestFocusInWindow();
    }
    
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            TetrisGame game = new TetrisGame();
            game.setVisible(true);
            game.board.requestFocusInWindow();
        });
    }
}
