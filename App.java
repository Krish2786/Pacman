import javax.swing.JFrame;

public class App {
    public static void main(String[] args) throws Exception {
       int rowCount = 21;
       int columnCount = 19;
       int tileSize = 32;
       int boardWidth = columnCount * tileSize;
       int boardHeight = rowCount * tileSize;

       JFrame frame = new JFrame("Pacman");
       // frame.setVisible(true);
       frame.setSize(boardWidth, boardHeight);
       frame.setLocationRelativeTo(null);
       frame.setResizable(false);
       frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

       PacMan pacman = new PacMan();
       frame.add(pacman);
       frame.pack();
       frame.setVisible(true);
    }
}
