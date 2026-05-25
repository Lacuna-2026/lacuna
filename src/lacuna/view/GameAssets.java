package lacuna.view;

import lacuna.model.FlowerColor;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class GameAssets {
    private static final BufferedImage BOARD = load("board.png");
    private static final BufferedImage BLUE_FLOWER = load("flowers/blue.png");
    private static final BufferedImage CYAN_FLOWER = load("flowers/cyan.png");
    private static final BufferedImage GREEN_FLOWER = load("flowers/green.png");
    private static final BufferedImage ORANGE_FLOWER = load("flowers/orange.png");
    private static final BufferedImage PINK_FLOWER = load("flowers/pink.png");
    private static final BufferedImage PURPLE_FLOWER = load("flowers/purple.png");
    private static final BufferedImage RED_FLOWER = load("flowers/red.png");
    private static final BufferedImage SILVER_PAWN = load("silver_pawn.png");
    private static final BufferedImage GOLD_PAWN = load("gold_pawn.png");
    private static final BufferedImage LETS_PLAY = load("lets_play.png");
    private static final BufferedImage HOLD_UP = load("hold_up.png");
    private static final BufferedImage GAME_OVER = load("game_over.png");
    private static final BufferedImage LACUNA_CYLINDER = load("lacuna_cylinder.png");

    private GameAssets() {
    }

    public static BufferedImage board() {
        return BOARD;
    }

    public static BufferedImage flower(FlowerColor color) {
        return switch (color) {
            case BLUE -> BLUE_FLOWER;
            case GREEN -> GREEN_FLOWER;
            case ORANGE -> ORANGE_FLOWER;
            case RED -> RED_FLOWER;
            case PINK -> PINK_FLOWER;
            case PURPLE -> PURPLE_FLOWER;
            case CYAN -> CYAN_FLOWER;
        };
    }

    public static BufferedImage pawn(int playerIndex) {
        return playerIndex == 0 ? SILVER_PAWN : GOLD_PAWN;
    }

    public static BufferedImage letsPlay() {
        return LETS_PLAY;
    }

    public static BufferedImage holdUp() {
        return HOLD_UP;
    }

    public static BufferedImage gameOver() {
        return GAME_OVER;
    }

    public static BufferedImage lacunaCylinder() {
        return LACUNA_CYLINDER;
    }



    public static ImageIcon icon(BufferedImage image, int size) {
        if (image == null) {
            return null;
        }
        Image scaled = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    public static void prepare(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    }

    public static Rectangle coverBounds(BufferedImage image, int width, int height) {
        if (image == null || width <= 0 || height <= 0) {
            return new Rectangle(0, 0, Math.max(0, width), Math.max(0, height));
        }

        double imageRatio = image.getWidth() / (double) image.getHeight();
        double panelRatio = width / (double) height;
        int drawW;
        int drawH;

        if (panelRatio > imageRatio) {
            drawW = width;
            drawH = (int) Math.round(width / imageRatio);
        } else {
            drawH = height;
            drawW = (int) Math.round(height * imageRatio);
        }

        return new Rectangle((width - drawW) / 2, (height - drawH) / 2, drawW, drawH);
    }

    public static void drawFit(Graphics2D g2, BufferedImage image, int centerX, int centerY, int size) {
        if (image == null || size <= 0) {
            return;
        }
        g2.drawImage(image, centerX - size / 2, centerY - size / 2, size, size, null);
    }

    private static BufferedImage load(String name) {
        List<Path> candidates = List.of(
                Path.of("assets", name),
                Path.of("lacuna", "assets", name),
                Path.of("..", "lacuna", "assets", name));

        for (Path path : candidates) {
            if (Files.exists(path)) {
                try {
                    return ImageIO.read(path.toFile());
                } catch (IOException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
