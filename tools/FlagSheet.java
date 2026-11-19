import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/** 16개국 국기를 한 장에 그려서 저장한다. */
public class FlagSheet {
    public static void main(String[] a) throws Exception {
        List<String> codes = FencingWorld.nationCodes();
        int cols = 4, cw = 180, ch = 130;
        BufferedImage img = new BufferedImage(cols * cw + 10, ((codes.size() + cols - 1) / cols) * ch + 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(245, 246, 250));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        for (int i = 0; i < codes.size(); i++) {
            int x = 10 + (i % cols) * cw, y = 10 + (i / cols) * ch;
            FlagArt.draw(g, codes.get(i), x, y, 150, 100);
            g.setColor(Color.DARK_GRAY);
            g.drawString(codes.get(i) + " " + FencingWorld.nationName(codes.get(i)), x, y + 116);
        }
        g.dispose();
        ImageIO.write(img, "png", new File(a.length > 0 ? a[0] : "/home/claude/flags.png"));
        System.out.println("saved");
    }
}
