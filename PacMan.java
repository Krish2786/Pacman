import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;
import javax.sound.sampled.*;
import java.io.File;

public class PacMan extends JPanel implements ActionListener, KeyListener {
    class Block {
        int x;
        int y;
        int width;
        int height;
        Image image;

        int startX;
        int startY;
        char direction = 'U'; 
        char nextDirection = 'U'; 
        int velocityX = 0;
        int velocityY = 0;

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char direction) {
            char prevDirection = this.direction;
            this.direction = direction;
            updateVelocity();
            this.x += this.velocityX;
            this.y += this.velocityY;
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                }
            }
        }

        void updateVelocity() {
            if (this.direction == 'U') {
                this.velocityX = 0;
                this.velocityY = -tileSize/4;
            }
            else if (this.direction == 'D') {
                this.velocityX = 0;
                this.velocityY = tileSize/4;
            }
            else if (this.direction == 'L') {
                this.velocityX = -tileSize/4;
                this.velocityY = 0;
            }
            else if (this.direction == 'R') {
                this.velocityX = tileSize/4;
                this.velocityY = 0;
            }
        }

        void reset() {
            this.x = this.startX;
            this.y = this.startY;
        }
    }

    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 32;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;

    private Image wallImage;
    private Image blueGhostImage;
    private Image orangeGhostImage;
    private Image pinkGhostImage;
    private Image redGhostImage;

    private Image pacmanUpImage;
    private Image pacmanDownImage;
    private Image pacmanLeftImage;
    private Image pacmanRightImage;

  
    private Clip eatFoodSound;
    private Clip ghostCollisionSound;
    private Clip gameOverSound;

   
    private Image scarceGhostImage;
    private HashSet<Block> powerups;
    private boolean powerupActive = false;
    private int powerupDuration = 0;
    private static final int POWERUP_DURATION = 250; 
    private long gameStartTime;
    private long gameEndTime;

    private String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X       X    X",
        "XXXX XXXX XXXX XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXrXX X XXXX",
        "O       bpo       O",
        "XXXX X XXXXX X XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXXXX X XXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X  X     P     X  X",
        "XX X X XXXXX X X XX",
        "X    X   X   X    X",
        "X XXXXXX X XXXXXX X",
        "X                 X",
        "XXXXXXXXXXXXXXXXXXX" 
    };

    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;
    Block pacman;

    Timer gameLoop;
    char[] directions = {'U', 'D', 'L', 'R'}; 
    Random random = new Random();
    int score = 0;
    int lives = 3;
    boolean gameOver = false;
    private static final int SPEED = 2; 
    private int foodEatenCount = 0;
    private static final int FOOD_FOR_POWERUP = 70; 

    PacMan() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        //images
        wallImage = new ImageIcon(getClass().getResource("./wall.png")).getImage();
        blueGhostImage = new ImageIcon(getClass().getResource("./blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("./orangeGhost.png")).getImage();
        pinkGhostImage = new ImageIcon(getClass().getResource("./pinkGhost.png")).getImage();
        redGhostImage = new ImageIcon(getClass().getResource("./redGhost.png")).getImage();
        scarceGhostImage = new ImageIcon(getClass().getResource("./scaredGhost.png")).getImage();

        pacmanUpImage = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();
        pacmanDownImage = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();
        pacmanLeftImage = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacmanRightImage = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();

        
        loadSounds();

        loadMap();
        for (Block ghost : ghosts) {
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
        
        gameLoop = new Timer(50, this); //20fps 
        gameLoop.start();
        gameStartTime = System.currentTimeMillis();

    }

    public void loadSounds() {
        try {
            // Load eat food sound
            eatFoodSound = loadClip("src/pacman_chomp.wav");
        } catch (Exception e) {
            System.out.println("Could not load eat sound: " + e.getMessage());
        }

        try {
            // Load ghost collision sound
            ghostCollisionSound = loadClip("src/pacman_death.wav");
        } catch (Exception e) {
            System.out.println("Could not load collision sound: " + e.getMessage());
        }

        try {
            // Load game over sound
            gameOverSound = loadClip("src/pacman_intermission.wav");
        } catch (Exception e) {
            System.out.println("Could not load game over sound: " + e.getMessage());
        }
    }

    private Clip loadClip(String path) throws Exception {
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(path));
        Clip clip = AudioSystem.getClip();
        clip.open(audioInputStream);
        return clip;
    }

    public void playSoundEffect(Clip clip) {
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public void loadMap() {
        walls = new HashSet<Block>();
        foods = new HashSet<Block>();
        ghosts = new HashSet<Block>();
        powerups = new HashSet<Block>();

        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                String row = tileMap[r];
                char tileMapChar = row.charAt(c);

                int x = c*tileSize;
                int y = r*tileSize;

                if (tileMapChar == 'X') { //block wall
                    Block wall = new Block(wallImage, x, y, tileSize, tileSize);
                    walls.add(wall);
                }
                else if (tileMapChar == 'b') { //blue ghost
                    Block ghost = new Block(blueGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'o') { //orange ghost
                    Block ghost = new Block(orangeGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'p') { //pink ghost
                    Block ghost = new Block(pinkGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'r') { //red ghost
                    Block ghost = new Block(redGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'P') { //pacman
                    pacman = new Block(pacmanRightImage, x, y, tileSize, tileSize);
                }
                else if (tileMapChar == ' ') { //food
                    Block food = new Block(null, x + 14, y + 14, 4, 4);
                    foods.add(food);
                }
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);

        for (Block ghost : ghosts) {
            // Draw scared ghost if powerup is active
            Image ghostImage = powerupActive ? scarceGhostImage : ghost.image;
            g.drawImage(ghostImage, ghost.x, ghost.y, ghost.width, ghost.height, null);
        }

        for (Block wall : walls) {
            g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        }

        g.setColor(Color.WHITE);
        for (Block food : foods) {
            g.fillRect(food.x, food.y, food.width, food.height);
        }

        // Draw powerup duration indicator
        if (powerupActive) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("POWER! " + (powerupDuration / 5) + "s", boardWidth / 2 - 60, tileSize * 2);
        }

        // Score and lives at top
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        if (!gameOver) {
            g.drawString("x" + String.valueOf(lives) + " Score: " + String.valueOf(score), tileSize/2, tileSize/2);
        } else {
            // Game Over popup in center
            drawGameOverPopup(g);
        }
    }

    private void drawGameOverPopup(Graphics g) {
        int popupWidth = 400;
        int popupHeight = 250;
        int popupX = (boardWidth - popupWidth) / 2;
        int popupY = (boardHeight - popupHeight) / 2;

        // Semi-transparent background
        Graphics2D g2d = (Graphics2D) g;
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(popupX, popupY, popupWidth, popupHeight);

        // Border
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRect(popupX, popupY, popupWidth, popupHeight);

        // Game Over text
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String gameOverText = "GAME OVER";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = popupX + (popupWidth - fm.stringWidth(gameOverText)) / 2;
        g2d.drawString(gameOverText, textX, popupY + 70);

        // Score
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        String scoreText = "Score: " + score;
        fm = g2d.getFontMetrics();
        textX = popupX + (popupWidth - fm.stringWidth(scoreText)) / 2;
        g2d.drawString(scoreText, textX, popupY + 130);

        // Game Duration
        long duration = (gameEndTime - gameStartTime) / 1000; // Convert to seconds
        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        String durationText = "Duration: " + duration + "s";
        fm = g2d.getFontMetrics();
        textX = popupX + (popupWidth - fm.stringWidth(durationText)) / 2;
        g2d.drawString(durationText, textX, popupY + 170);

        // Press any key to restart
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        String restartText = "Press any key to restart";
        fm = g2d.getFontMetrics();
        textX = popupX + (popupWidth - fm.stringWidth(restartText)) / 2;
        g2d.drawString(restartText, textX, popupY + 220);
    }

    public void move() {
        // Powerup duration countdown
        if (powerupActive) {
            powerupDuration--;
            if (powerupDuration <= 0) {
                powerupActive = false;
            }
        }

        // Try to move in the next queued direction
        if (pacman.nextDirection != pacman.direction) {
            char prevDirection = pacman.direction;
            pacman.direction = pacman.nextDirection;
            pacman.updateVelocity();
            pacman.x += pacman.velocityX;
            pacman.y += pacman.velocityY;
            
            // Check if next direction causes collision
            boolean hasCollision = false;
            for (Block wall : walls) {
                if (collision(pacman, wall)) {
                    pacman.x -= pacman.velocityX;
                    pacman.y -= pacman.velocityY;
                    pacman.direction = prevDirection;
                    pacman.updateVelocity();
                    hasCollision = true;
                    break;
                }
            }
            
            // Check boundary
            if (!hasCollision && (pacman.x < 0 || pacman.x + pacman.width > boardWidth ||
                pacman.y < 0 || pacman.y + pacman.height > boardHeight)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                pacman.direction = prevDirection;
                pacman.updateVelocity();
            }
        }
        
        // Continue moving in current direction
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;

        // Boundary check for continuous movement
        if (pacman.x < 0 || pacman.x + pacman.width > boardWidth ||
            pacman.y < 0 || pacman.y + pacman.height > boardHeight) {
            pacman.x -= pacman.velocityX;
            pacman.y -= pacman.velocityY;
        }

        //wall collisions
        for (Block wall : walls) {
            if (collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                break;
            }
        }

        //ghost collisions
        for (Block ghost : ghosts) {
            if (collision(ghost, pacman)) {
                if (powerupActive) {
                    // Eat the ghost
                    score += 200;
                    ghost.reset();
                } else {
                    playSoundEffect(ghostCollisionSound);
                    lives -= 1;
                    if (lives == 0) {
                        gameOver = true;
                        gameEndTime = System.currentTimeMillis();
                        playSoundEffect(gameOverSound);
                        return;
                    }
                    resetPositions();
                }
            }

            if (ghost.y == tileSize*9 && ghost.direction != 'U' && ghost.direction != 'D') {
                ghost.updateDirection('U');
            }
            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;
            for (Block wall : walls) {
                if (collision(ghost, wall) || ghost.x <= 0 || ghost.x + ghost.width >= boardWidth) {
                    ghost.x -= ghost.velocityX;
                    ghost.y -= ghost.velocityY;
                    char newDirection = directions[random.nextInt(4)];
                    ghost.updateDirection(newDirection);
                }
            }
        }

        //food collision
        Block foodEaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) {
                foodEaten = food;
                score += 10;
                foodEatenCount++;
                playSoundEffect(eatFoodSound);

                // Activate powerup after eating enough food
                if (foodEatenCount >= FOOD_FOR_POWERUP && !powerupActive) {
                    powerupActive = true;
                    powerupDuration = POWERUP_DURATION;
                    foodEatenCount = 0; // Reset counter for next powerup
                }
            }
        }
        foods.remove(foodEaten);

        //powerup collision
        Block powerupEaten = null;
        for (Block powerup : powerups) {
            if (collision(pacman, powerup)) {
                powerupEaten = powerup;
                powerupActive = true;
                powerupDuration = POWERUP_DURATION;
                score += 50;
            }
        }
        powerups.remove(powerupEaten);

        if (foods.isEmpty()) {
            loadMap();
            resetPositions();
        }
    }

    public boolean collision(Block a, Block b) {
        return  a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    public void resetPositions() {
        pacman.reset();
        pacman.direction = 'R';
        pacman.nextDirection = 'R';
        pacman.velocityX = 0;
        pacman.velocityY = 0;
        pacman.image = pacmanRightImage;
        powerupActive = false;
        powerupDuration = 0;
        foodEatenCount = 0;
        for (Block ghost : ghosts) {
            ghost.reset();
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) {
            gameLoop.stop();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            loadMap();
            resetPositions();
            lives = 3;
            score = 0;
            gameOver = false;
            powerupActive = false;
            powerupDuration = 0;
            foodEatenCount = 0;
            gameStartTime = System.currentTimeMillis();
            gameLoop.start();
        }
        
        // Queue the next direction for smooth movement
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            pacman.nextDirection = 'U';
        }
        else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            pacman.nextDirection = 'D';
        }
        else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            pacman.nextDirection = 'L';
        }
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            pacman.nextDirection = 'R';
        }
        
        // Update image based on current direction
        updatePacmanImage();
    }

    @Override
    public void keyReleased(KeyEvent e) {}
    
    private void updatePacmanImage() {
        // Use nextDirection to show image immediately when player presses key
        char directionToShow = pacman.nextDirection;
        if (directionToShow == 'U') {
            pacman.image = pacmanUpImage;
        }
        else if (directionToShow == 'D') {
            pacman.image = pacmanDownImage;
        }
        else if (directionToShow == 'L') {
            pacman.image = pacmanLeftImage;
        }
        else if (directionToShow == 'R') {
            pacman.image = pacmanRightImage;
        }
    }
}
